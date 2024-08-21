package io.esper.android.files.filelist

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import api.EsperEndpoints
import io.esper.android.files.R
import io.esper.android.files.adapter.ContentAdapter
import io.esper.android.files.data.ContentDb
import io.esper.android.files.databinding.DlcFragmentBinding
import io.esper.android.files.model.AllContent
import io.esper.android.files.model.CMItem
import io.esper.android.files.ui.FixQueryChangeSearchView
import io.esper.android.files.util.Constants
import io.esper.android.files.util.Constants.DlcFragmentTag
import io.esper.android.files.util.GeneralUtils
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class DlcFragment : Fragment() {
    private var progressDialog: AlertDialog? = null
    private var allowedContent: String? = null
    private var specificContentList: MutableList<AllContent>? = ArrayList()
    private var sharedPrefManaged: SharedPreferences? = null
    private var mContentAdapter: ContentAdapter? = null
    private var db: ContentDb? = null
    private var contentList: MutableList<AllContent> = ArrayList()
    private var mRecyclerDialogItems: RecyclerView? = null
    private var mEmptyDialogView: RelativeLayout? = null
    private lateinit var menuBinding: MenuBinding
    private lateinit var binding: DlcFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = DlcFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.title = getString(R.string.file_list_action_dlc)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        view?.let { init(it) }
    }

    private fun init(view: View) {
        mEmptyDialogView = view.findViewById(R.id.layout_empty_view_dialog)
        mRecyclerDialogItems = view.findViewById(R.id.recyclerView)

        sharedPrefManaged = requireContext().getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )

        mContentAdapter = ContentAdapter()
        mRecyclerDialogItems?.adapter = mContentAdapter
        mRecyclerDialogItems?.layoutManager = LinearLayoutManager(
            context, LinearLayoutManager.VERTICAL, false
        )

        db = context?.applicationContext?.let {
            Room.databaseBuilder(
                it, ContentDb::class.java, "ContentDb"
            ).allowMainThreadQueries().fallbackToDestructiveMigration().build()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menuBinding = MenuBinding.inflate(menu, inflater)
        getDataFromDb(db!!)
        setUpSearchView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                mContentAdapter?.filter?.filter("")
                collapseSearchView()
                fetchContent(true)
                true
            }

            R.id.action_search -> {
                menuBinding.searchItem.expandActionView()
                true
            }

            R.id.sort_item -> {
                collapseSearchView()
                GeneralUtils.hideKeyboard(requireActivity())

                // Sort the currently displayed list (either filtered or full list)
                val ascending =
                    sharedPrefManaged?.getBoolean(Constants.SORT_ASCENDING, true) ?: true
                mContentAdapter?.sortItems(!ascending)

                // Toggle the sort order and save the preference
                sharedPrefManaged?.edit()?.putBoolean(Constants.SORT_ASCENDING, !ascending)?.apply()

                val sortMessage = if (!ascending) {
                    getString(R.string.sort_ascending)
                } else {
                    getString(R.string.sort_descending)
                }
                Toast.makeText(context, sortMessage, Toast.LENGTH_SHORT).show()

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setUpSearchView() {
        val searchView = menuBinding.searchItem.actionView as FixQueryChangeSearchView
        // SearchView.OnCloseListener.onClose() is not always called.
        menuBinding.searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                context?.let {
                    mContentAdapter?.setContentItems(
                        it, mContentAdapter?.mItemContentListOriginal
                    )
                }
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                GeneralUtils.hideKeyboard(requireActivity())
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                if (query.isEmpty()) {
                    context?.let {
                        mContentAdapter?.setContentItems(
                            it, mContentAdapter?.mItemContentListOriginal
                        )
                    }
                } else {
                    mContentAdapter?.filter?.filter(query)
                }
                return false
            }
        })
    }


    private fun collapseSearchView() {
        if (this::menuBinding.isInitialized && menuBinding.searchItem.isActionViewExpanded) {
            menuBinding.searchItem.collapseActionView()
        }
    }

    private fun fetchContent(doInBackground: Boolean, offset: Int = 0) {
        val methodTag = "fetchContent"
        try {
            Log.i(DlcFragmentTag, "$methodTag: doInBackground: $doInBackground")
            startRefreshAnim()
            val cacheSize1 = (5 * 1024 * 1024).toLong()
            val myCache1 = Cache(requireContext().cacheDir, cacheSize1)
            val okHttpClient = OkHttpClient.Builder().connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS)
                .cache(myCache1).addInterceptor { chain ->
                    var request = chain.request()
                    val hasNetwork =
                        context?.let { GeneralUtils.hasActiveInternetConnection(it) } == true
                    request = if (hasNetwork) {
                        request.newBuilder().header("Cache-Control", "public, max-age=" + 5).build()
                    } else {
                        request.newBuilder().header(
                            "Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7
                        ).build()
                    }
                    chain.proceed(request)
                }.build()

            val tenant = GeneralUtils.getTenant(requireContext())
            val enterprise = GeneralUtils.getEnterpriseId(requireContext())
            val token = GeneralUtils.getApiKey(requireContext())

            val getUrl = "$tenant/api/v0/enterprise/$enterprise/"
            val retrofit = Retrofit.Builder().baseUrl(getUrl)
                .addConverterFactory(GsonConverterFactory.create()).client(okHttpClient).build()
            val request = retrofit.create(EsperEndpoints::class.java)
            var localOffset = offset
            request.getAllContent(token = "Bearer $token", 50, localOffset)
                .enqueue(object : Callback<CMItem> {
                    override fun onResponse(
                        call: Call<CMItem>, response: Response<CMItem>
                    ) {
                        if (response.isSuccessful) {
                            if (response.body() != null) {
                                response.body()?.results?.let { contentList.addAll(it) }
                                if (response.body()?.next != null) {
                                    localOffset += 50
                                    fetchContent(doInBackground, localOffset)
                                } else {
                                    val existingDb = db?.contentDao()?.getAllContent()
                                    Log.i(
                                        DlcFragmentTag,
                                        "$methodTag: Existing DB Size: ${existingDb?.size}"
                                    )
                                    Log.i(
                                        DlcFragmentTag, "$methodTag: CM Size: ${contentList.size}"
                                    )
                                    if (existingDb?.size != contentList.size) {
                                        updateDb()
                                    } else {
                                        for (item in existingDb) {
                                            var found = false
                                            for (item1 in contentList) {
                                                if (item.name == item1.name && item.download_url == item1.download_url) {
                                                    found = true
                                                    break
                                                }
                                            }
                                            if (!found) {
                                                Log.i(
                                                    DlcFragmentTag,
                                                    "$methodTag: Changes in CM -> DB Updated"
                                                )
                                                updateDb()
                                                break
                                            } else {
                                                Log.i(
                                                    DlcFragmentTag, "$methodTag: No Changes in CM"
                                                )
                                            }
                                        }
                                    }
                                    stopRefreshAnim()
                                }
                            }
                        } else {
                            stopRefreshAnim()
                            when (response.code()) {
                                400 -> Toast.makeText(
                                    context, getString(R.string.data_not_found), Toast.LENGTH_SHORT
                                ).show()

                                401 -> Toast.makeText(
                                    context, getString(R.string.auth_failure), Toast.LENGTH_SHORT
                                ).show()

                                404 -> Toast.makeText(
                                    context, getString(R.string.data_not_found), Toast.LENGTH_SHORT
                                ).show()

                                500 -> Toast.makeText(
                                    context, getString(R.string.broken_server), Toast.LENGTH_SHORT
                                ).show()

                                else -> Toast.makeText(
                                    context, R.string.network_issue, Toast.LENGTH_SHORT
                                ).show()

                            }
                        }
                    }

                    override fun onFailure(call: Call<CMItem>, t: Throwable) {
                        stopRefreshAnim()
                        if (requireContext() != null) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.could_not_load_data),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
        } catch (e: Exception) {
            Log.i(DlcFragmentTag, "$methodTag: Exception: $e")
            stopRefreshAnim()
        }
    }

    private fun updateDb() {
        db?.contentDao()?.deleteAll()
        for (element in contentList) {
            db?.contentDao()?.insert(element)
        }
        specificContentList?.clear()
        contentList.clear()
        getDataFromDb(db!!, false)
    }

    private fun getDataFromDb(db: ContentDb, fetchNewContent: Boolean = true) {
        try {
            val methodDlcFragmentTag = "getDataFromDb"
            progressDialog?.let { GeneralUtils.dismissMaterialLoadingDialog(it) }
            if (db.contentDao().getAllContent().isNotEmpty()) {
                if (allowedContent.isNullOrEmpty() || allowedContent.equals("[]")) {
                    Log.d(
                        DlcFragmentTag, "$methodDlcFragmentTag: All Content Allowed: size: ${
                            db.contentDao().getAllContent().size
                        }"
                    )
                    binding.toolbar.subtitle = "${db.contentDao().getAllContent().size} files"
                    mContentAdapter?.setContentItems(
                        requireContext(), db.contentDao().getAllContent()
                    )
                } else {
                    Log.d(DlcFragmentTag, "$methodDlcFragmentTag: Specific Content Allowed")
                    val array = db.contentDao().getAllContent()
                    val replace: String? = allowedContent?.replace("[", "")
                    println(replace)
                    val replace1 = replace?.replace("]", "")
                    println(replace1)
                    val myList = listOf(replace1?.split(", ", ","))
                    for (i in array.indices) {
                        for (j in myList.iterator()) {
                            if (j?.contains(array[i].name.toString()) == true) {
                                db.contentDao().getContentWithName(array[i].name.toString()).let {
                                    specificContentList?.add(
                                        it
                                    )
                                }
                            }
                        }
                    }
                    binding.toolbar.subtitle = "${specificContentList?.size} files"
                    mContentAdapter?.setContentItems(requireContext(), specificContentList)
                }
                setEmptyViewVisibility(View.GONE)
                setRecyclerViewVisibility(View.VISIBLE)
                if (fetchNewContent) {
                    fetchContent(true)
                }
            } else {
                Log.d(DlcFragmentTag, "$methodDlcFragmentTag: No Existing Data Found in DB.")
                progressDialog = context?.let { GeneralUtils.showMaterialLoadingDialog(it) }
                binding.toolbar.subtitle = "No files available"
                setEmptyViewVisibility(View.VISIBLE)
                setRecyclerViewVisibility(View.GONE)
                if (fetchNewContent) {
                    fetchContent(true)
                }
            }
        } catch (e: Exception) {
            Log.e(DlcFragmentTag, "getDataFromDb: Exception: $e")
        }
    }

    override fun onStop() {
        db?.close()
        GeneralUtils.hideKeyboard(requireActivity())
        super.onStop()
    }

    private class MenuBinding private constructor(
        val menu: Menu, val searchItem: MenuItem, val refreshItem: MenuItem, val sortItem: MenuItem
    ) {
        companion object {
            fun inflate(menu: Menu, inflater: MenuInflater): MenuBinding {
                inflater.inflate(R.menu.dlc_menu, menu)
                return MenuBinding(
                    menu,
                    menu.findItem(R.id.action_search),
                    menu.findItem(R.id.action_refresh),
                    menu.findItem(R.id.sort_item)
                )
            }
        }
    }

    fun startRefreshAnim() {
        binding.progressDialog.visibility = View.VISIBLE
        disableRefreshBtn()
    }

    fun stopRefreshAnim() {
        binding.progressDialog.visibility = View.GONE
        enableRefreshBtn()
    }

    fun disableRefreshBtn() {
        menuBinding.refreshItem.isEnabled = false
    }

    fun enableRefreshBtn() {
        menuBinding.refreshItem.isEnabled = true
    }

    fun setEmptyViewVisibility(visibility: Int) {
        mEmptyDialogView?.visibility = visibility
    }

    fun setRecyclerViewVisibility(visibility: Int) {
        mRecyclerDialogItems?.visibility = visibility
    }
}
