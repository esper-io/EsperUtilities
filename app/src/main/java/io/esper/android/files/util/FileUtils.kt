package io.esper.android.files.util

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.topjohnwu.superuser.internal.UiThreadHandler
import io.esper.android.files.app.application
import io.esper.android.files.file.FileItem
import io.esper.android.files.file.fileProviderUri
import io.esper.android.files.filejob.FileJobService
import io.esper.android.files.model.Item
import io.esper.android.files.provider.archive.isArchivePath
import io.esper.android.files.provider.linux.isLinuxPath
import io.esper.android.files.util.Constants.FileUtilsTag
import io.esper.android.files.util.UploadDownloadUtils.uploadFile
import io.esper.devicesdk.EsperDeviceSDK
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.util.Date

object FileUtils {
    fun moveScreenshotDirectoryContents(
        context: Context?, sharedPrefStorage: SharedPreferences, mOriginalScreenshotPath: String
    ) {
        sharedPrefStorage.edit()
            .putString(Constants.ORIGINAL_SCREENSHOT_STORAGE_VALUE, mOriginalScreenshotPath).apply()
        if (!File(Constants.EsperScreenshotFolder).exists()) {
            GeneralUtils.createDir(Constants.EsperScreenshotFolder)
        }
        for (i in getDirectoryContents(File(mOriginalScreenshotPath), context)) {
            moveFile(File(i.path), File(Constants.EsperScreenshotFolder))
        }
    }

    fun moveScreenshotDirectoryContentsBack(context: Context?, mUpdatedScreenshotPath: String) {
        for (i in getDirectoryContents(File(Constants.EsperScreenshotFolder), context)) {
            moveFile(File(i.path), File(mUpdatedScreenshotPath))
        }
        if (File(Constants.EsperScreenshotFolder).exists()) {
            GeneralUtils.deleteDir(Constants.EsperScreenshotFolder)
        }
    }

    private fun moveFile(file: File, dir: File) {
        val newFile = File(dir, file.name)
        var outputChannel: FileChannel? = null
        var inputChannel: FileChannel? = null
        try {
            outputChannel = FileOutputStream(newFile).channel
            inputChannel = FileInputStream(file).channel
            inputChannel.transferTo(0, inputChannel.size(), outputChannel)
            inputChannel.close()
            file.delete()
        } catch (_: java.lang.Exception) {
        } finally {
            inputChannel?.close()
            outputChannel?.close()
        }
    }

    fun getDirectoryContents(directory: File, context: Context?): List<File> {
        val fileList = mutableListOf<File>()
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile) {
                        fileList.add(file)
                    } else if (file.isDirectory && context != null) {
                        // If it's a directory, recursively fetch its contents
                        fileList.addAll(getDirectoryContents(file, context))
                    }
                }
            }
        }
        return fileList
    }

    fun startScreenShotMove(context: Context) {
        val sharedPrefManaged = context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        if (sharedPrefManaged.getBoolean(Constants.SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS, false)) {
            if (loadDirectoryContents(context, Constants.InternalScreenshotFolderDCIM)) {
                moveScreenshotDirectoryContents(
                    context, sharedPrefManaged, Constants.InternalScreenshotFolderDCIM
                )
            } else if (loadDirectoryContents(
                    context, Constants.InternalScreenshotFolderPictures
                )
            ) {
                moveScreenshotDirectoryContents(
                    context, sharedPrefManaged, Constants.InternalScreenshotFolderPictures
                )
            }
        } else {
            moveScreenshotDirectoryContentsBack(
                context, sharedPrefManaged.getString(
                    Constants.ORIGINAL_SCREENSHOT_STORAGE_VALUE, null
                ).toString()
            )
        }
    }

    fun loadDirectoryContents(context: Context?, mScreenshotPath: String): Boolean {
        getDirectoryContents(File(mScreenshotPath), context).size
        return getDirectoryContents(File(mScreenshotPath), context).isNotEmpty()
    }

    fun createEsperFolder() {
        GeneralUtils.getInternalStoragePath(application)?.let { GeneralUtils.createDirectory(it) }
    }

    fun deleteFile(filePath: String): Boolean {
        val file = File(filePath)
        return file.delete()
    }


    fun populateItemList(directoryPath: String): MutableList<Item> {
        val directory = File(directoryPath)
        val mItemList: MutableList<Item> = mutableListOf()

        // Recursive function to traverse directories and populate items
        fun populateItemsRecursively(directory: File) {
            if (directory.exists() && directory.isDirectory) {
                val files = directory.listFiles()
                files?.forEach { file ->
                    val item = Item(
                        name = file.name,
                        data = file.absolutePath,
                        date = Date(file.lastModified()).toString(), // You can customize the date format as needed
                        path = file.parent,
                        image = null, // Set the image as needed
                        emptySubFolder = false, // Assuming no empty subfolders
                        isDirectory = file.isDirectory,
                        size = file.length().toString()
                    )
                    mItemList.add(item)
                    if (file.isDirectory) {
                        populateItemsRecursively(file) // Recursively call for subdirectories
                    }
                }
            }
        }

        populateItemsRecursively(directory) // Start recursive traversal from the root directory
        return mItemList
    }

    fun getFilePath(mContext: Context, fileName: String): String {
        // Get the external files directory
        val filesDir = GeneralUtils.getInternalStoragePath(mContext)?.let { File(it) }

        // Check if the external files directory exists
        if (filesDir != null) {
            // Recursively search for the file in the external files directory and its subfolders
            val filePath = searchFileInDirectory(filesDir, fileName)

            // Return the file path if found, otherwise return an empty string
            return if (filePath != null) {
                // Mask the internal storage path in the file path
                val maskedPath = filePath.replace(filesDir.path, "Internal Storage")
                maskedPath
            } else {
                ""
            }
        }

        // Return an empty string if the external files directory is null
        return ""
    }


    // Recursive function to search for a file in a directory and its subdirectories
    private fun searchFileInDirectory(directory: File, fileName: String): String? {
        // List all files and subdirectories in the current directory
        val files = directory.listFiles() ?: return null
        // Iterate through the files and subdirectories
        for (file in files) {
            // Check if the current file is the desired file
            if (file.name == fileName) {
                // Return the absolute path of the file if found
                return file.absolutePath
            }
            // If the current file is a directory, recursively search in it
            if (file.isDirectory) {
                val filePath = searchFileInDirectory(file, fileName)
                if (filePath != null) {
                    return filePath
                }
            }
        }
        // Return null if the file is not found in the current directory or its subdirectories
        return null
    }

    fun getPackageNameFromApk(context: Context, apkFilePath: String): String? {
        val packageManager = context.packageManager
        try {
            val packageInfo = packageManager.getPackageArchiveInfo(apkFilePath, 0)
            if (packageInfo != null) {
                val packageName = packageInfo.packageName
                return packageName
            }
        } catch (e: Exception) {
            Log.e("Error", "Error getting package name from APK: ${e.message}")
        }
        return null
    }

    fun installApkWithPackageInstaller(context: Context, file: FileItem) {
        val path = file.path
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!path.isArchivePath) path.fileProviderUri else null
        } else {
            // PackageInstaller only supports file URI before N.
            if (path.isLinuxPath) Uri.fromFile(path.toFile()) else null
        }
        if (uri != null) {
            context.startActivitySafe(uri.createInstallPackageIntent())
        } else {
            FileJobService.installApk(path, context)
        }
    }

    fun installApkWithEsperSDK(context: Context, file: FileItem? = null, path: String? = null) {
        val pathName = file?.path?.toString() ?: path
        GeneralUtils.isEsperDeviceSDKActivated(context) { activated ->
            if (activated) {
                Log.d(
                    Constants.FileListFragmentTag,
                    "installApkWithEsperSDK: Esper Device SDK is activated"
                )
                val packageName = pathName?.let { getPackageNameFromApk(context, it) }
                packageName?.let { it1 ->
                    GeneralUtils.getEsperSDK(context)
                        .installApp(it1, pathName, object : EsperDeviceSDK.Callback<Boolean> {
                            override fun onResponse(p0: Boolean?) {
                                UiThreadHandler.handler.post {
                                    Toast.makeText(
                                        context, "App installed successfully", Toast.LENGTH_SHORT
                                    ).show()
                                }
                                if (path != null) {
                                    deleteFile(path)
                                }
                            }

                            override fun onFailure(t: Throwable) {
                                if (file != null) {
                                    UiThreadHandler.handler.post {
                                        installApkWithPackageInstaller(context, file)
                                    }
                                }
                            }
                        })
                }
            } else {
                Log.d(
                    Constants.FileListFragmentTag,
                    "installApkWithEsperSDK: Esper Device SDK is not activated, using package installer"
                )
                if (file != null) {
                    installApkWithPackageInstaller(context, file)
                }
            }
        }
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            packageInfo.applicationInfo.enabled && packageInfo.packageName == packageName
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getVersionName(context: Context, packageName: String): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun openApp(mContext: Context, packageName: String): Boolean {
        try {
            val i: Intent? = mContext.packageManager.getLaunchIntentForPackage(packageName)
            mContext.startActivity(i)
            return true
        } catch (e: Exception) {
            Toast.makeText(mContext, "App not available!", Toast.LENGTH_LONG).show()
            Log.e("Error", "Error opening app: ${e.message}")
            return false
        }
    }

    class CompressTask(
        private val context: Context,
        private val pathsToZip: List<String>,
        private val zipFileName: String,
        private val viewLifecycleOwner: LifecycleOwner,
        private val upload: Boolean = true,
        private val fromService: Boolean = false
    ) : AsyncTask<Void, Int, Boolean>() {

        private var progressDialog: ProgressDialog? = null

        override fun onPreExecute() {
            if (!fromService) {
                progressDialog = ProgressDialog(context).apply {
                    setTitle("Zipping")
                    setMessage("Please wait...")
                    setProgressStyle(ProgressDialog.STYLE_SPINNER)
                    setCancelable(false)
                    show()
                }
            }
        }

        override fun onProgressUpdate(vararg values: Int?) {
            // Progress update logic, if needed
        }

        override fun onPostExecute(result: Boolean?) {
            progressDialog?.dismiss()

            val zipFilePath = GeneralUtils.getInternalStoragePath(context)
            if (result == true && zipFilePath != null) {
                if (upload && GeneralUtils.hasActiveInternetConnection(context)) {
                    uploadFile(
                        zipFilePath + zipFileName, zipFileName, context, viewLifecycleOwner, true, fromService
                    )
                } else {
                    Toast.makeText(context, "No Internet Connection, Aborting!", Toast.LENGTH_SHORT).show()
                    deleteFile(zipFilePath + zipFileName)
                }
            } else {
                Toast.makeText(context, "Compression failed!", Toast.LENGTH_SHORT).show()
            }
        }

        override fun doInBackground(vararg p0: Void?): Boolean {
            return try {
                val zipFilePath = GeneralUtils.getInternalStoragePath(context) ?: return false
                val zipFile = ZipFile(zipFilePath + zipFileName)

                for (path in pathsToZip) {
                    val file = File(path)
                    if (file.isDirectory) {
                        zipFile.addFolder(file)
                    } else {
                        zipFile.addFile(file)
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(FileUtilsTag, e.toString())
                false
            }
        }
    }

}