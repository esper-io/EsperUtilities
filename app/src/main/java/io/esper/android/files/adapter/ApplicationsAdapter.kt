package io.esper.android.files.adapter

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import api.EsperEndpoints
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.ferfalk.simplesearchview.SimpleSearchView
import io.esper.android.files.R
import io.esper.android.files.data.AppDb
import io.esper.android.files.ui.CheckableForegroundLinearLayout
import io.esper.android.files.util.Constants
import io.esper.android.files.util.FileUtils
import io.esper.android.files.util.FileUtils.installApkWithEsperSDK
import io.esper.android.files.util.GeneralUtils
import io.esper.appstore.model.AllApps
import io.esper.appstore.model.AppData1
import io.esper.appstore.model.ApplicationsInfo1
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit


private var mRecyclerDialogItems: RecyclerView? = null

@SuppressLint("StaticFieldLeak")
private var mEmptyDialogView: RelativeLayout? = null

class ApplicationsAdapter : RecyclerView.Adapter<ApplicationsAdapter.ItemViewHolder>(), Filterable {

    private lateinit var bottomSheet: BottomSheetFragment
    private var db: AppDb? = null
    private var sharedPrefManaged: SharedPreferences? = null
    private val TAG: String = "ApplicationsAdapter"
    private var appsList: MutableList<AllApps>? = ArrayList()
    private var appsList1: MutableList<AppData1>? = ArrayList()
    private var prevCharLength: Int = 0
    private var mContext: Context? = null

    private var mItemListFiltered: MutableList<AllApps>? = ArrayList()
    private var mItemListOriginal: MutableList<AllApps>? = ArrayList()
    private var mItemPrevList: MutableList<AllApps>? = ArrayList()
    private var mItemReadyForPrev: MutableList<AllApps>? = ArrayList()

    init {
        registerPackageInstalledReceiver()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        mContext = parent.context
        val view: View = LayoutInflater.from(mContext).inflate(
            R.layout.application_item, parent, false
        )

        sharedPrefManaged = mContext!!.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        mItemListOriginal = appsList

        db = Room.databaseBuilder(
            mContext!!.applicationContext, AppDb::class.java, "AppDB"
        ).allowMainThreadQueries().fallbackToDestructiveMigration().build()

        return ItemViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        if (sharedPrefManaged == null) {
            sharedPrefManaged = mContext!!.getSharedPreferences(
                Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
            )
        }
        val currentItem = appsList!![position]

        holder.txtTitle.text = currentItem.application_name
        holder.txtInfo.text = currentItem.package_name

        Glide.with(mContext!!).load(currentItem.versions!![0].icon_url)
            .placeholder(R.mipmap.file_shortcut_icon).optionalCenterCrop().priority(Priority.HIGH)
            .into(holder.imgThumbnail)

        holder.installBtn.setOnClickListener { }
        holder.updateBtn.setOnClickListener { }

        val isAppInstalled = FileUtils.isAppInstalled(mContext!!, currentItem.package_name!!)
        if (isAppInstalled) {
            holder.progressBtn.visibility = View.GONE
            val installedVersion = FileUtils.getVersionName(mContext!!, currentItem.package_name!!)
            holder.txtItemHeader2.visibility = View.VISIBLE
            if (installedVersion != currentItem.versions!![0].version_code) {
                holder.installBtn.text = mContext!!.getString(R.string.open)
                holder.installBtn.setOnClickListener {
                    if (!FileUtils.openApp(mContext!!, currentItem.package_name!!)) {
                        Toast.makeText(mContext, "App Not Installed Properly", Toast.LENGTH_LONG)
                            .show()
                    }
                }
                holder.updateBtn.visibility = View.VISIBLE
                holder.updateBtn.setOnClickListener {
                    downloadApk(
                        holder.installBtn,
                        holder.updateBtn,
                        holder.btnLayout,
                        holder.progressBtn,
                        currentItem.versions!![0].download_url.toString(),
                        currentItem.package_name!!
                    )
                }
                holder.txtItemHeader2.text =
                    "Installed Version: $installedVersion, Latest Version: ${
                        currentItem.versions!![0].version_code
                    }"
            } else {
                holder.installBtn.text = mContext!!.getString(R.string.open)
                holder.updateBtn.visibility = View.GONE
                holder.installBtn.setOnClickListener {
                    if (!FileUtils.openApp(mContext!!, currentItem.package_name!!)) {
                        Toast.makeText(mContext, "App Not Installed Properly", Toast.LENGTH_LONG)
                            .show()
                    }
                }
                holder.txtItemHeader2.text = "Installed Version: $installedVersion"
            }
            // Enable marquee
            holder.txtItemHeader2.isSingleLine = true
            holder.txtItemHeader2.ellipsize = TextUtils.TruncateAt.MARQUEE
            holder.txtItemHeader2.marqueeRepeatLimit = -1
            holder.txtItemHeader2.isSelected = true
            holder.txtItemHeader2.isFocusable = true
            holder.txtItemHeader2.isFocusableInTouchMode = true
            holder.txtItemHeader2.setHorizontallyScrolling(true)
        } else {
            holder.installBtn.text = mContext!!.getString(R.string.install)
            holder.txtItemHeader2.visibility = View.GONE
            holder.updateBtn.visibility = View.GONE
            holder.installBtn.setOnClickListener {
                if (holder.installBtn.text == mContext!!.getString(R.string.open) && !FileUtils.openApp(
                        mContext!!, currentItem.package_name!!
                    )
                ) {
                    Toast.makeText(
                        mContext, "App not installed properly. Reinstalling...", Toast.LENGTH_LONG
                    ).show()
                }
                downloadApk(
                    holder.installBtn,
                    null,
                    holder.btnLayout,
                    holder.progressBtn,
                    currentItem.versions!![0].download_url.toString(),
                    currentItem.package_name!!
                )
            }
        }

//        holder.background.setOnClickListener {
//            if (sharedPrefManaged!!.getBoolean(
//                    Constants.SHARED_MANAGED_CONFIG_SHOW_ALL_VERSIONS, true
//                )
//            ) {
//                mContext?.let { it2 -> hideKeyboard(it2) }
//                bottomSheet = BottomSheetFragment(
//                    mContext!!,
//                    currentItem.id,
//                    currentItem.application_name,
//                    currentItem.package_name,
//                    currentItem.versions!![0].icon_url,
//                    sharedPrefManaged!!
//                )
//
//                bottomSheet.show(
//                    (mContext as AppCompatActivity).supportFragmentManager,
//                    "DemoBottomSheetFragment"
//                )
//            } else Log.d(TAG, "All Versions Visible to User: Disabled")
//
//        }
        setFadeAnimation(holder.itemView)

        // Register receiver
        val intentFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addDataScheme("package")
        mContext?.registerReceiver(packageInstalledReceiver, intentFilter)
    }

    private fun setFadeAnimation(view: View) {
        val anim = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 300
        view.startAnimation(anim)
    }

    private fun downloadApk(
        installBtn: TextView,
        updateBtn: TextView?,
        btnLayout: LinearLayout,
        progressBar: ProgressBar,
        url: String,
        packageName: String,
    ) {
        Log.i(TAG, "Downloading Apk: $url")
        val config = PRDownloaderConfig.newBuilder().setDatabaseEnabled(true).setReadTimeout(30000)
            .setConnectTimeout(30000).build()
        PRDownloader.initialize(mContext!!.applicationContext, config)

        PRDownloader.download(
            url, Constants.InternalRootFolder, "${packageName}.apk"
        ).build().setOnStartOrResumeListener {
            btnLayout.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
        }.setOnPauseListener { }.setOnCancelListener { }.setOnProgressListener {
            val progressPercent: Long = it.currentBytes * 100 / it.totalBytes
            progressBar.progress = progressPercent.toInt()
        }.start(object : OnDownloadListener {
            override fun onDownloadComplete() {
                progressBar.progress = 100
                btnLayout.visibility = View.VISIBLE
                installBtn.text = mContext!!.getString(R.string.open)
                updateBtn?.visibility = View.GONE
                progressBar.visibility = View.GONE

                mContext?.let {
                    installApkWithEsperSDK(
                        it, null, File(Constants.InternalRootFolder + "${packageName}.apk").path
                    )
                }
            }

            override fun onError(error: com.downloader.Error?) {
                btnLayout.visibility = View.VISIBLE
                if (updateBtn != null) {
                    updateBtn.visibility = View.VISIBLE
                    installBtn.text = mContext!!.getString(R.string.open)
                } else {
                    installBtn.text = mContext!!.getString(R.string.install)
                }
                progressBar.visibility = View.GONE
                Log.d(TAG, "Download Error: ${error?.connectionException}")
            }
        })
    }

    override fun getItemCount(): Int {
        return appsList?.size ?: 0
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(
        itemView
    ) {
        var txtTitle: TextView = itemView.findViewById(R.id.txt_item_name)
        var txtInfo: TextView = itemView.findViewById(R.id.txt_item_info)
        var imgThumbnail: ImageView = itemView.findViewById(R.id.img_item_thumbnail)
        var btnLayout: LinearLayout = itemView.findViewById(R.id.btnLayout)
        var installBtn: TextView = itemView.findViewById(R.id.installBtn)
        var updateBtn: TextView = itemView.findViewById(R.id.updateBtn)
        var background: CheckableForegroundLinearLayout =
            itemView.findViewById(R.id.item_background)
        var txtItemHeader2: TextView = itemView.findViewById(R.id.txt_item_header2)
        var progressBtn: ProgressBar = itemView.findViewById(R.id.progressBtn)

    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                if (charSequence.toString().isEmpty() || charSequence.toString() == "") {
                    appsList = mItemListOriginal!!
                } else if (charSequence.toString().length < prevCharLength) appsList =
                    mItemPrevList!!
                val filteredList: MutableList<AllApps> = ArrayList()
                for (row in appsList!!) {
                    if (row.application_name!!.lowercase(Locale.getDefault()).contains(
                            charSequence.toString().lowercase(Locale.getDefault())
                        )
                    ) filteredList.add(row)
                }
                mItemListFiltered = filteredList

                prevCharLength = charSequence.toString().length
                val filterResults = FilterResults()
                filterResults.values = mItemListFiltered
                return filterResults
            }

            override fun publishResults(
                charSequence: CharSequence?, filterResults: FilterResults
            ) {
                mItemPrevList =
                    if (mItemListFiltered!!.size <= mItemPrevList!!.size) mItemReadyForPrev
                    else mItemListFiltered

                mItemReadyForPrev = mItemPrevList
                mItemListFiltered = filterResults.values as MutableList<AllApps>?
                appsList = mItemListFiltered!!

                notifyDataSetChanged()
            }
        }
    }

    fun setAppsListItems(appsList: MutableList<AllApps>?) {
        this.appsList!!.clear()
        appsList!!.sortWith(compareBy { it.application_name })
        this.appsList!!.addAll(appsList)
        notifyDataSetChanged()
    }

    fun notifyDataChanged() {
        notifyDataSetChanged()
    }

    private val packageInstalledReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val packageName = intent.data?.encodedSchemeSpecificPart
            if (packageName != null) {
                val app = appsList?.find { it.package_name == packageName }
                app?.let {
                    // Update UI or handle package installed event
                    notifyItemChanged(appsList!!.indexOf(app))
                }
            }
        }
    }

    private fun registerPackageInstalledReceiver() {
        val intentFilter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addDataScheme("package")
        mContext?.registerReceiver(packageInstalledReceiver, intentFilter)
    }

    private fun unregisterPackageInstalledReceiver() {
        try {
            mContext?.unregisterReceiver(packageInstalledReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Receiver not registered: $e")
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        unregisterPackageInstalledReceiver()
    }

    class BottomSheetFragment(
        private val mContext: Context,
        private val appId: String?,
        private val appName: String?,
        private val packageName: String?,
        private val iconUrl: String?,
        private val sharedPrefManaged: SharedPreferences
    ) : SuperBottomSheetFragment() {

        private val TAG: String = "BottomSheetFragment"
        private var callback: ApplicationsAdapter? = null

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
        ): View? {
            super.onCreateView(inflater, container, savedInstanceState)
            return inflater.inflate(R.layout.fragment_bottom_sheet, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            callback = ApplicationsAdapter()
            dialog!!.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

            val sheetTitle: TextView = view.findViewById(R.id.dialog_title)
            sheetTitle.text = getString(R.string.app_details)
            val mAppTitle = view.findViewById<TextView>(R.id.txt_item_name1)
            mAppTitle.text = appName
            val mAppInfo = view.findViewById<TextView>(R.id.txt_item_info1)
            mAppInfo.text = packageName
            val mAppThumbnail = view.findViewById<ImageView>(R.id.img_item_thumbnail1)
            Glide.with(this).load(iconUrl).placeholder(R.mipmap.ic_esper_launcher)
                .optionalCenterCrop().priority(Priority.HIGH).into(mAppThumbnail)
            mEmptyDialogView = view.findViewById(R.id.layout_empty_view_dialog)
            mRecyclerDialogItems = view.findViewById(R.id.dialog_recycler_view)

            val progressDialog: ProgressBar = view.findViewById(R.id.progress_dialog)
            progressDialog.visibility = View.VISIBLE

            val mDetailsAdapter = DetailsAdapter(appName, packageName)
            mRecyclerDialogItems!!.adapter = mDetailsAdapter
            mRecyclerDialogItems!!.layoutManager = LinearLayoutManager(
                mContext, LinearLayoutManager.VERTICAL, false
            )
            appId?.let {
                val cacheSize1 = (5 * 1024 * 1024).toLong()
                val myCache1 = Cache(mContext.cacheDir, cacheSize1)
                val okHttpClient = OkHttpClient.Builder().connectTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(30, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS)
                    .cache(myCache1).addInterceptor { chain ->
                        var request = chain.request()
                        request =
                            if (GeneralUtils.hasActiveInternetConnection(mContext)) request.newBuilder()
                                .header("Cache-Control", "public, max-age=" + 5).build()
                            else request.newBuilder().header(
                                "Cache-Control",
                                "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7
                            ).build()
                        chain.proceed(request)
                    }.build()

                val tenant = GeneralUtils.getTenant(requireContext())
                val enterprise = GeneralUtils.getEnterpriseId(requireContext())
                val token = GeneralUtils.getApiKey(requireContext())

                val getUrl = "$tenant/api/v0/enterprise/$enterprise/application/${appId}/"

                val retrofit = Retrofit.Builder().baseUrl(getUrl)
                    .addConverterFactory(GsonConverterFactory.create()).client(okHttpClient).build()
                val request = retrofit.create(EsperEndpoints::class.java)
                request.getAllVersions(
                    token = "Bearer ${
                        sharedPrefManaged.getString("apiToken", token).toString()
                    }"
                ).enqueue(object : Callback<ApplicationsInfo1> {
                    override fun onResponse(
                        call: Call<ApplicationsInfo1>, response: Response<ApplicationsInfo1>
                    ) {
                        if (response.isSuccessful) {
                            if (response.body() != null) {
                                mDetailsAdapter.setAppsListItems(response.body()!!.results)
                                progressDialog.visibility = View.GONE
                            }
                        } else {
                            progressDialog.visibility = View.GONE
                            when (response.code()) {
                                400 -> Toast.makeText(
                                    mContext, "Data Not Found. Try Later.", Toast.LENGTH_SHORT
                                ).show()

                                401 -> Toast.makeText(
                                    mContext,
                                    "Authorization Failure. Try Later.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                404 -> Toast.makeText(
                                    mContext, "Data Not Found. Try Later", Toast.LENGTH_SHORT
                                ).show()

                                500 -> Toast.makeText(
                                    mContext,
                                    "Server Broken. Please Try Again Later",
                                    Toast.LENGTH_SHORT
                                ).show()

                                else -> {
                                    Toast.makeText(
                                        mContext, R.string.network_issue, Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call<ApplicationsInfo1>, t: Throwable) {
                        progressDialog.visibility = View.GONE
                        Toast.makeText(
                            mContext, "Couldn't load Data. Error!", Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            val btn1: ImageView = view.findViewById(R.id.search_btn)
            val searchView: SimpleSearchView = view.findViewById(R.id.searchView)
            searchView.enableVoiceSearch(true)
            searchView.setBackIconDrawable(null)
            btn1.setOnClickListener {
                searchView.showSearch()
            }

            searchView.setOnQueryTextListener(object : SimpleSearchView.OnQueryTextListener {
                private var searcheddialog: Boolean = false
                override fun onQueryTextChange(newText: String): Boolean {
                    Log.d(TAG, "Changed$newText")
                    searcheddialog = true
                    mDetailsAdapter.filter.filter(newText)
                    return false
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    Log.d(TAG, "Submitted$query")
                    searcheddialog = true
                    return false
                }

                override fun onQueryTextCleared(): Boolean {
                    Log.d(TAG, "Cleared")
                    searcheddialog = false
                    mDetailsAdapter.filter.filter("")
                    return false
                }
            })
        }

        override fun getCornerRadius() =
            requireContext().resources.getDimension(R.dimen.demo_sheet_rounded_corner)
    }
}