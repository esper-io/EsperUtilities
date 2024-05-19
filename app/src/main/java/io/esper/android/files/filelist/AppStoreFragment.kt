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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import api.EsperEndpoints
import io.esper.android.files.R
import io.esper.android.files.adapter.ApplicationsAdapter
import io.esper.android.files.data.AppDb
import io.esper.android.files.data.AppRepository
import io.esper.android.files.databinding.AppStoreFragmentBinding
import io.esper.android.files.ui.FixQueryChangeSearchView
import io.esper.android.files.util.Constants
import io.esper.android.files.util.Constants.AppStoreFragmentTag
import io.esper.android.files.util.GeneralUtils
import io.esper.android.files.util.GeneralUtils.hideKeyboard
import io.esper.appstore.model.AllApps
import io.esper.appstore.model.AppData1
import io.esper.appstore.model.ApplicationsInfo
import io.esper.appstore.model.ApplicationsInfo2
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppStoreFragment : Fragment() {
    private var mAppsList: MutableList<AllApps>? = ArrayList()
    private var mAppsInstalledList: MutableList<AppData1>? = ArrayList()
    private var sharedPrefManaged: SharedPreferences? = null
    private var mAdapter: ApplicationsAdapter? = null
    private var db: AppDb? = null
    private var mRecyclerDialogItems: RecyclerView? = null
    private var mEmptyDialogView: RelativeLayout? = null
    private lateinit var menuBinding: MenuBinding
    private lateinit var binding: AppStoreFragmentBinding
    private var offset: Int = 0
    private var offset1: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = AppStoreFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.title = getString(R.string.esper_app_store)
        sharedPrefManaged = requireContext().getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        if (!sharedPrefManaged!!.getBoolean(Constants.SHARED_MANAGED_CONFIG_CONVERT_FILES_TO_APP_STORE, false)) {
            activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        setHasOptionsMenu(true)
        view?.let { init(it) }
    }

    private fun init(view: View) {
        mEmptyDialogView = view.findViewById(R.id.layout_empty_view_dialog)
        mRecyclerDialogItems = view.findViewById(R.id.recyclerView)

        mAdapter = ApplicationsAdapter()
        mRecyclerDialogItems?.adapter = mAdapter
        mRecyclerDialogItems?.layoutManager = LinearLayoutManager(
            context, LinearLayoutManager.VERTICAL, false
        )

        db = context?.applicationContext?.let {
            Room.databaseBuilder(
                it, AppDb::class.java, "AppDB"
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
                collapseSearchView()
                getAllApplications()
                true
            }

            R.id.action_search -> {
                menuBinding.searchItem.expandActionView()
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
                mAdapter?.filter?.filter("")
                return true
            }
        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                hideKeyboard(requireActivity())
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                if (searchView.shouldIgnoreQueryChange) {
                    return false
                }
                mAdapter?.filter?.filter(query)
                return false
            }
        })
    }

    private fun collapseSearchView() {
        if (this::menuBinding.isInitialized && menuBinding.searchItem.isActionViewExpanded) {
            menuBinding.searchItem.collapseActionView()
        }
    }

    private fun getDataFromDb(
        db: AppDb, fetchNewAppList: Boolean = true, hasValueChanged: Boolean = true
    ) {
        val methodDlcFragmentTag = "getDataFromDb"
        if (db.appDao().getApplications().isNotEmpty()) {
            if (hasValueChanged) {
                binding.toolbar.subtitle = "${db.appDao().getApplications().size} files"
                setEmptyViewVisibility(View.GONE)
                setRecyclerViewVisibility(View.VISIBLE)
                mAdapter!!.setAppsListItems(db.appDao().getApplications())
                if (fetchNewAppList) {
                    getAllApplications()
                }
            } else {
                mAdapter!!.notifyDataChanged()
            }
        } else {
            Log.d(AppStoreFragmentTag, "$methodDlcFragmentTag: No Existing Data Found in DB.")
            binding.toolbar.subtitle = "No files available"
            setEmptyViewVisibility(View.VISIBLE)
            setRecyclerViewVisibility(View.GONE)
            if (fetchNewAppList) {
                getAllApplications()
            }
        }
    }

    override fun onStop() {
        db?.close()
        hideKeyboard(requireActivity())
        super.onStop()
    }

    private class MenuBinding private constructor(
        val menu: Menu, val searchItem: MenuItem, val refreshItem: MenuItem
    ) {
        companion object {
            fun inflate(menu: Menu, inflater: MenuInflater): MenuBinding {
                inflater.inflate(R.menu.app_store_menu, menu)
                return MenuBinding(
                    menu, menu.findItem(R.id.action_search), menu.findItem(R.id.action_refresh)
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

    private fun getAllApplications() {
        startRefreshAnim()
        val cacheSize1 = (5 * 1024 * 1024).toLong()
        val myCache1 = Cache(requireContext().cacheDir, cacheSize1)
        val okHttpClient = OkHttpClient.Builder().connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).cache(myCache1)
            .addInterceptor { chain ->
                var request = chain.request()
                request =
                    if (GeneralUtils.hasActiveInternetConnection(requireContext())) request.newBuilder()
                        .header("Cache-Control", "public, max-age=" + 5).build()
                    else request.newBuilder().header(
                        "Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7
                    ).build()
                chain.proceed(request)
            }.build()

        val tenant = GeneralUtils.getTenant(requireContext())
        val enterprise = GeneralUtils.getEnterpriseId(requireContext())
        val token = GeneralUtils.getApiKey(requireContext())

        val getUrl = "$tenant/api/v0/enterprise/$enterprise/"

        val retrofit =
            Retrofit.Builder().baseUrl(getUrl).addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient).build()
        val request = retrofit.create(EsperEndpoints::class.java)

        request.getAllApplications("Bearer $token", false, 50, offset)
            .enqueue(object : Callback<ApplicationsInfo> {
                override fun onResponse(
                    call: Call<ApplicationsInfo>, response: Response<ApplicationsInfo>
                ) {
                    if (response.isSuccessful) {
                        if (response.body() != null) {
                            mAppsList?.addAll(response.body()?.results!!)
                            if (response.body()!!.next != null) {
                                offset += 50
                                getAllApplications()
                            } else {
                                val dataChanged = AppRepository.hasAllAppsChanged(
                                    db!!.appDao().getApplications(), mAppsList!!
                                )
                                if (dataChanged) {
                                    db!!.appDao().deleteAll()
                                    for (element in mAppsList!!) {
                                        db!!.appDao().insert(element)
                                    }
                                }
                                mAppsList?.clear()
                                getDataFromDb(db!!, false, dataChanged)
                                stopRefreshAnim()
                            }
                        }
                    } else {
                        stopRefreshAnim()
                        when (response.code()) {
                            400 -> Toast.makeText(
                                requireContext(), "Data Not Found. Try Later.", Toast.LENGTH_SHORT
                            ).show()

                            401 -> Toast.makeText(
                                requireContext(),
                                "Authorization Failure. Try Later.",
                                Toast.LENGTH_SHORT
                            ).show()

                            404 -> Toast.makeText(
                                requireContext(), "Data Not Found. Try Later", Toast.LENGTH_SHORT
                            ).show()

                            500 -> Toast.makeText(
                                requireContext(),
                                "Server Broken. Please Try Again Later",
                                Toast.LENGTH_SHORT
                            ).show()

                            else -> {
                                Toast.makeText(
                                    requireContext(), R.string.network_issue, Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ApplicationsInfo>, t: Throwable) {
                    stopRefreshAnim()
                    Toast.makeText(
                        requireContext(), "Couldn't load Data. Error!", Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
