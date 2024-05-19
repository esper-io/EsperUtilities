@file:Suppress("DEPRECATION")

package io.esper.android.files.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import io.esper.android.files.R
import io.esper.android.files.data.AppDb
import io.esper.appstore.model.AppData
import io.esper.appstore.model.AppData1
import io.esper.appstore.model.InstalledApps
import java.io.File
import java.text.DecimalFormat
import java.util.*

class DetailsAdapter(private var appName: String?, private var packageName: String?) :
    RecyclerView.Adapter<DetailsAdapter.MyViewHolder>(), Filterable {

    private var db: AppDb? = null
    private lateinit var callback: ApplicationsAdapter
    private val TAG: String = "DetailsAdapter"
    private var prevCharLength: Int = 0
    private var mContext: Context? = null
    private var sharedPref: SharedPreferences? = null
    private var mItemAppData: MutableList<AppData>? = ArrayList()
    private var inflater: LayoutInflater? = null
    private var mItemListFilteredDialog: MutableList<AppData>? = ArrayList()
    private var mItemListOriginalDialog: MutableList<AppData>? = ArrayList()
    private var mItemPrevListDialog: MutableList<AppData>? = ArrayList()
    private var mItemReadyForPrevDialog: MutableList<AppData>? = ArrayList()

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): MyViewHolder {
        mContext = parent.context
        inflater = LayoutInflater.from(mContext)
        val view: View = inflater!!.inflate(R.layout.item_file, parent, false)
        mItemListOriginalDialog = mItemAppData
        callback = ApplicationsAdapter()
        db = Room.databaseBuilder(
            mContext!!.applicationContext, AppDb::class.java, "AppDB"
        ).allowMainThreadQueries().fallbackToDestructiveMigration().build()

        return MyViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: MyViewHolder, position: Int
    ) {
        holder.versionName.text = mItemAppData!![position].version_code
        holder.minSDK.text = "Min SDK : ${mItemAppData!![position].min_sdk_version}"
        holder.apkSize.text =
            "${DecimalFormat("#.##").format(mItemAppData!![position].size_in_mb!!.toFloat())} MB"

        val installedVersion: String = try {
            Log.d(TAG, db!!.appDao().getInstalledOnlyVersion(packageName!!).version_name.toString())
            if (db!!.appDao().getInstalledOnlyVersion(packageName!!).version_name == null) ""
            else db!!.appDao().getInstalledOnlyVersion(packageName!!).version_name.toString()
        } catch (e: NullPointerException) {
            ""
        }

        when {
            versionCompare(
                mItemAppData!![position].version_code!!,
                installedVersion
            ) < 0 -> holder.versionBtn2.visibility = View.GONE

            versionCompare(mItemAppData!![position].version_code!!, installedVersion) > 0 -> {
                holder.versionBtn2.text = mContext!!.getString(R.string.install)
                holder.versionBtn2.visibility = View.VISIBLE
            }

            else -> {
                holder.versionBtn2.text = mContext!!.getString(R.string.open)
                holder.versionBtn2.visibility = View.VISIBLE
            }
        }

        holder.versionBtn2.setOnClickListener {
            if (holder.versionBtn2.text == "Open") {
                try {
                    val i: Intent =
                        mContext!!.packageManager.getLaunchIntentForPackage(packageName!!)!!
                    mContext!!.startActivity(i)
                } catch (e: Exception) {
//                        holder.installBtn.text = "Install"
                    Toast.makeText(mContext, "App Not Installed Properly", Toast.LENGTH_LONG).show()
                    e.message?.let { it1 -> Log.e(TAG, it1) }
                }
            } else {
//                downloadApk(
//                    mItemAppData!![position].app_file.toString(),
//                    holder.versionBtn2,
//                    holder.progressBtn,
//                    mItemAppData!![position].version_code!!,
//                    "${appName}.apk"
//                )
            }
        }
    }

    private fun versionCompare(v1: String, v2: String): Int {
        // vnum stores each numeric part of version
        var vnum1 = 0
        var vnum2 = 0

        // loop until both String are processed
        var i = 0
        var j = 0
        while (i < v1.length || j < v2.length) {

            // Storing numeric part of
            // version 1 in vnum1
            while (i < v1.length && v1[i] != '.') {
                vnum1 = (vnum1 * 10 + (v1[i] - '0'))
                i++
            }

            // storing numeric part
            // of version 2 in vnum2
            while (j < v2.length && v2[j] != '.') {
                vnum2 = (vnum2 * 10 + (v2[j] - '0'))
                j++
            }
            if (vnum1 > vnum2) return 1
            if (vnum2 > vnum1) return -1

            // if equal, reset variables and
            // go for next numeric part
            vnum2 = 0
            vnum1 = vnum2
            i++
            j++
        }
        return 0
    }

    override fun getItemCount(): Int {
        return mItemAppData!!.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var versionName: TextView = itemView.findViewById<View>(R.id.txt_item_name2) as TextView
        var minSDK: TextView = itemView.findViewById<View>(R.id.txt_item_name3) as TextView
        var apkSize: TextView = itemView.findViewById<View>(R.id.txt_item_name4) as TextView
        var versionBtn2: TextView = itemView.findViewById<View>(R.id.versionBtn2) as TextView
        var progressBtn: ProgressBar = itemView.findViewById(R.id.progressBtn)
    }

    fun getRootDirPath(context: Context): String? {
        return if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            val file = ContextCompat.getExternalFilesDirs(
                context.applicationContext, null
            )[0]
            file.absolutePath
        } else {
            context.applicationContext.filesDir.absolutePath
        }
    }

    private fun getExtension(fileName: String): String {
        val arrayOfFilename = fileName.toCharArray()
        for (i in arrayOfFilename.size - 1 downTo 1) {
            if (arrayOfFilename[i] == '.') {
                return fileName.substring(i + 1, fileName.length)
            }
        }
        return ""
    }

    private fun getMimeType(file: File): String? {
        var mimeType: String? = ""
        val extension: String = getExtension(file.name)
        if (MimeTypeMap.getSingleton().hasExtension(extension)) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        return mimeType
    }

    private fun installApk(
        file: File,
        packageName: String,
        versionCode: String,
        existingView: TextView,
        newView: ProgressBar
    ) {

        try {
            val type = getMimeType(file)
            val intent = Intent(Intent.ACTION_VIEW)
            var data = Uri.fromFile(file)
            if (file.name.endsWith(
                    ".apk", false
                ) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            ) {
                data = FileProvider.getUriForFile(
                    mContext!!, mContext!!.packageName + ".provider", file
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            intent.setDataAndType(data, type)
            mContext!!.startActivity(intent)
            delayedPackageChecker1(packageName, versionCode, existingView, newView)
        } catch (e: Exception) {
            Toast.makeText(
                mContext,
                "No Application Available to Open this File. Please Contact your Administrator.",
                Toast.LENGTH_LONG
            ).show()
        } finally {
        }
    }

    private fun delayedPackageChecker1(
        packageName: String, versionCode: String, existingView: TextView, newView: ProgressBar
    ) {
        val handler = Handler()
        var count = 0

        val runnable: Runnable = object : Runnable {
            override fun run() {
                // need to do tasks on the UI thread
                if (!delayedPackageChecker(packageName, versionCode, existingView, newView)) {
                    if (count++ < 20) {
                        handler.postDelayed(this, 3000)
                    }
                }
//                else {
//                    if (mContext is MainActivity) {
//                        Handler().postDelayed({ (mContext as MainActivity).getInstalledAppsList(true) }, 30000)
//                    }
//                }
            }
        }

        handler.post(runnable)
    }

    private fun delayedPackageChecker(
        packageName: String, versionCode: String, existingView: TextView, newView: ProgressBar
    ): Boolean {
        return if (isPackageInstalled(packageName, mContext!!.packageManager)) {
            existingView.visibility = View.VISIBLE
            existingView.text = mContext!!.getString(R.string.open)
            newView.visibility = View.GONE

            val id: String = try {
                db!!.appDao().getInstalledOnlyVersion(packageName).id
            } catch (e: NullPointerException) {
                ""
            }
            if (id != "") db!!.appDao().update(InstalledApps(id, packageName, versionCode))
            else db!!.appDao().insertInstalled(
                AppData1(
                    db!!.appDao().getIdFromRoot(packageName).id,
                    "",
                    "",
                    packageName,
                    "",
                    "",
                    versionCode,
                    ""
                )
            )

            with(sharedPref!!.edit()) {
                putBoolean("isRefreshReq", true)
                apply()
            }
            notifyDataSetChanged()
            true
        } else {

            existingView.visibility = View.VISIBLE
            existingView.text = mContext!!.getString(R.string.install)
            newView.visibility = View.GONE
            notifyDataSetChanged()
            false
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                if (charSequence.toString().isEmpty() || charSequence.toString() == "") {
                    mItemAppData = mItemListOriginalDialog!!
                } else if (charSequence.toString().length < prevCharLength) mItemAppData =
                    mItemPrevListDialog!!
                val filteredList: MutableList<AppData> = ArrayList()
                for (row in mItemAppData!!) {
                    if (row.version_code!!.toLowerCase(Locale.getDefault()).contains(
                                charSequence.toString().toLowerCase(Locale.getDefault())
                            )
                    ) filteredList.add(row)
                }
                mItemListFilteredDialog = filteredList

                prevCharLength = charSequence.toString().length
                val filterResults = FilterResults()
                filterResults.values = mItemListFilteredDialog
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(
                charSequence: CharSequence?, filterResults: FilterResults
            ) {
                mItemPrevListDialog =
                    if (mItemListFilteredDialog!!.size <= mItemPrevListDialog!!.size) mItemReadyForPrevDialog
                    else mItemListFilteredDialog

                mItemReadyForPrevDialog = mItemPrevListDialog
                mItemListFilteredDialog = filterResults.values as MutableList<AppData>?
                mItemAppData = mItemListFilteredDialog!!

                notifyDataSetChanged()
            }
        }
    }

    fun setAppsListItems(mItemAppData: MutableList<AppData>?) {
        this.mItemAppData = mItemAppData
        notifyDataSetChanged()
    }
}