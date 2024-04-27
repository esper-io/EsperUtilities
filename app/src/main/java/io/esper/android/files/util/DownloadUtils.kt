package io.esper.android.files.util

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import io.esper.android.files.R
import io.esper.android.files.ui.AutoGoneTextView
import io.esper.android.files.util.Constants.DownloadUtilsTag
import io.esper.android.files.util.Constants.FileUtilsTag
import io.karn.notify.Notify
import net.gotev.uploadservice.UploadServiceConfig.defaultNotificationChannel
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.data.UploadNotificationAction
import net.gotev.uploadservice.data.UploadNotificationConfig
import net.gotev.uploadservice.data.UploadNotificationStatusConfig
import net.gotev.uploadservice.exceptions.UploadError
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.extensions.getCancelUploadIntent
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import net.gotev.uploadservice.placeholders.Placeholder
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import net.lingala.zip4j.ZipFile
import java.io.File
import kotlin.math.min

object DownloadUtils {
    @SuppressLint("SetTextI18n")
    fun downloadContent(
        mContext: Context,
        sharedPrefManaged: SharedPreferences?,
        url: String,
        downloadImg: ImageView,
        downloadTxt: AutoGoneTextView,
        fileName: String,
        id: Int,
        downloadBtn: LinearLayout
    ) {
        var prevProgress = 0
        initFileDownloader(mContext)
        val downloadFolder = GeneralUtils.getInternalStoragePath(sharedPrefManaged!!)
        Log.d(DownloadUtilsTag, "Download Folder: $downloadFolder")
        PRDownloader.download(url, downloadFolder, fileName).build().setOnStartOrResumeListener {
            Toast.makeText(
                mContext, "Downloading $fileName", Toast.LENGTH_SHORT
            ).show()
            downloadBtn.isEnabled = false
            downloadImg.visibility = View.GONE
            downloadTxt.visibility = View.VISIBLE
            downloadTxt.text = "0%"
            downloadStartNotification(mContext, fileName, id)
        }.setOnPauseListener { }.setOnCancelListener { }.setOnProgressListener {
            val progressPercent: Long = it.currentBytes * 100 / it.totalBytes
            if (progressPercent.toInt() > prevProgress) {
                prevProgress = progressPercent.toInt()
                downloadTxt.text = "$prevProgress%"
            }
        }.start(object : OnDownloadListener {
            override fun onDownloadComplete() {
                Log.d(DownloadUtilsTag, "Download Completed $fileName")
                removeDownloadStartNotification(mContext, id)
                downloadCompleteNotification(mContext, fileName, id)
                downloadImg.setImageResource(R.drawable.ic_complete)
                downloadImg.visibility = View.VISIBLE
                downloadTxt.visibility = View.GONE
            }

            override fun onError(error: com.downloader.Error?) {
                Log.d(
                    DownloadUtilsTag,
                    "Download Error : Response Code :${error!!.responseCode} and Message :${error.serverErrorMessage}"
                )
                if (error.isServerError) Log.d(
                    DownloadUtilsTag, "Download Error : Server issue: ${error.isServerError}"
                )
                if (error.isConnectionError) Log.d(
                    DownloadUtilsTag,
                    "Download Error : Connection issue: ${error.isConnectionError}"
                )
                removeDownloadStartNotification(mContext, id)
                downloadImg.setImageResource(R.drawable.ic_cloud_download)
            }
        })
    }

    private fun initFileDownloader(mContext: Context) {
        val config = PRDownloaderConfig.newBuilder().setDatabaseEnabled(true).setReadTimeout(30000)
            .setConnectTimeout(30000).build()
        PRDownloader.initialize(mContext.applicationContext, config)
    }

    private fun downloadStartNotification(mContext: Context, fileName: String, id: Int) {
        Notify.with(mContext).asBigText {
            title = "Downloading $fileName"
            expandedText = "We'll notify you once the download is complete"
            bigText = ""
        }.progress {
            showProgress = true
        }.show()
    }

    fun removeDownloadStartNotification(mContext: Context, id: Int) {
        Notify.cancelNotification(mContext, id)
    }

    fun downloadCompleteNotification(mContext: Context, fileName: String, id: Int) {
        Notify.cancelNotification(mContext, id)
        val newFileName: String =
            if (fileName.length > 15) fileName.substring(0, min(fileName.length, 10)) + "..."
            else fileName

        Notify.with(mContext).content {
            title = "Download Complete"
            text = "$newFileName can be viewed in Files App"
        }.show()
    }

    @Suppress("NAME_SHADOWING")
    fun upload(
        filePath: String,
        fileName: String,
        context: Context,
        lifecycleOwner: LifecycleOwner,
        deleteFile: Boolean = false
    ) {
        Toast.makeText(context, "$fileName Upload Starting", Toast.LENGTH_SHORT).show()
        val sharedPrefManaged = context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        val tenant = sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_TENANT, null)
        val enterpriseId =
            sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_ENTERPRISE_ID, null)
        val apiKey = sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_API_KEY, null)

        val url = "$tenant/api/v0/enterprise/$enterpriseId/content/upload/"

        MultipartUploadRequest(context, url).addFileToUpload(
            filePath = filePath, parameterName = "key"
        ).setBearerAuth(apiKey!!).setNotificationConfig { context, uploadId ->
            val title = "Esper File Upload (${fileName})"
            UploadNotificationConfig(
                notificationChannelId = defaultNotificationChannel!!,
                isRingToneEnabled = true,
                progress = UploadNotificationStatusConfig(
                    title = title,
                    message = "Uploading at ${Placeholder.UploadRate} (${Placeholder.Progress})",
                    actions = arrayListOf(
                        UploadNotificationAction(
                            icon = android.R.drawable.ic_menu_close_clear_cancel,
                            title = "Cancel",
                            intent = context.getCancelUploadIntent(uploadId)
                        )
                    )
                ),
                success = UploadNotificationStatusConfig(
                    title = title,
                    message = "Upload completed successfully in ${Placeholder.ElapsedTime}"
                ),
                error = UploadNotificationStatusConfig(
                    title = title, message = "Error during upload"
                ),
                cancelled = UploadNotificationStatusConfig(
                    title = title, message = "Upload cancelled"
                )
            )
        }.subscribe(context = context,
            lifecycleOwner = lifecycleOwner,
            delegate = object : RequestObserverDelegate {
                override fun onProgress(context: Context, uploadInfo: UploadInfo) {

                }

                override fun onSuccess(
                    context: Context, uploadInfo: UploadInfo, serverResponse: ServerResponse
                ) {
                    Toast.makeText(context, "File Upload Successful", Toast.LENGTH_SHORT).show()
                    if (deleteFile) {
                        FileUtils.deleteFile(filePath)
                    }
                }

                override fun onError(
                    context: Context, uploadInfo: UploadInfo, exception: Throwable
                ) {
                    when (exception) {
                        is UserCancelledUploadException -> {
                            Toast.makeText(
                                context, "$fileName Upload Cancelled", Toast.LENGTH_SHORT
                            ).show()
                            Log.e(DownloadUtilsTag, "Error, user cancelled upload: $uploadInfo.")
                        }

                        is UploadError -> {
                            Log.e(
                                DownloadUtilsTag,
                                "Error, upload error: ${exception.serverResponse.code}"
                            )
                            if (exception.serverResponse.code == 400) {
                                Toast.makeText(
                                    context,
                                    "File Upload Failed: File with same name may exist in the server. Please rename the file and try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "File Upload Failed", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                        else -> {
                            Log.e(DownloadUtilsTag, "Error: $uploadInfo", exception)
                            Toast.makeText(context, "File Upload Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    if (deleteFile) {
                        FileUtils.deleteFile(filePath)
                    }
                }

                override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
                    if (deleteFile) {
                        FileUtils.deleteFile(filePath)
                    }
                }

                override fun onCompletedWhileNotObserving() {
                    if (deleteFile) {
                        FileUtils.deleteFile(filePath)
                    }
                }
            })
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StaticFieldLeak")
    class Compress(
        private var _ctx: Context,
        private var _toZipFolder: String,
        private var _zipFileName: String,
        private var _viewLifecycleOwner: LifecycleOwner,
        private var _sharedPrefManaged: SharedPreferences
    ) : AsyncTask<Void, Int, Boolean>() {

        private var progressDialog: ProgressDialog? = null

        override fun onPreExecute() {
            progressDialog = ProgressDialog(_ctx)
            progressDialog!!.setTitle("Zipping")
            progressDialog!!.setMessage("Please wait...")
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            progressDialog!!.setCancelable(false)
            progressDialog!!.show()
        }

        override fun onProgressUpdate(vararg values: Int?) {
        }

        override fun onPostExecute(result: Boolean?) {
            progressDialog!!.dismiss()
            val zipFilePath = GeneralUtils.getInternalStoragePath(_sharedPrefManaged)!!
            upload(
                zipFilePath + _zipFileName, _zipFileName, _ctx, _viewLifecycleOwner, true
            )
        }

        override fun doInBackground(vararg p0: Void?): Boolean {
            return try {
                val zipFilePath = GeneralUtils.getInternalStoragePath(_sharedPrefManaged)!!
                ZipFile(zipFilePath + _zipFileName).addFolder(File(_toZipFolder))
                true
            } catch (ioe: Exception) {
                Log.d(FileUtilsTag, ioe.toString())
                false
            } finally {

            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StaticFieldLeak")
    class CompressMultipleFiles(
        private var _ctx: Context,
        private var _filePathsToZip: ArrayList<String>,
        private var _zipFileName: String,
        private var _viewLifecycleOwner: LifecycleOwner,
        private var _sharedPrefManaged: SharedPreferences,
        private var upload: Boolean
    ) : AsyncTask<Void, Int, Boolean>() {

        private var progressDialog: ProgressDialog? = null

        override fun onPreExecute() {
            progressDialog = ProgressDialog(_ctx)
            progressDialog!!.setTitle("Zipping")
            progressDialog!!.setMessage("Please wait...")
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            progressDialog!!.setCancelable(false)
            progressDialog!!.show()
        }

        override fun onProgressUpdate(vararg values: Int?) {
        }

        override fun onPostExecute(result: Boolean?) {
            progressDialog!!.dismiss()
            val zipFilePath = GeneralUtils.getInternalStoragePath(_sharedPrefManaged)
            if (upload) {
                upload(
                    zipFilePath + _zipFileName, _zipFileName, _ctx, _viewLifecycleOwner, true
                )
            }
        }

        override fun doInBackground(vararg p0: Void?): Boolean {
            return try {
                val zipFilePath = GeneralUtils.getInternalStoragePath(_sharedPrefManaged)
                for (i in _filePathsToZip.indices) {
                    try {
                        if (File(_filePathsToZip[i]).isDirectory) {
                            ZipFile(zipFilePath + _zipFileName).addFolder(File(_filePathsToZip[i]))
                        } else {
                            ZipFile(zipFilePath + _zipFileName).addFile(File(_filePathsToZip[i]))
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e(FileUtilsTag, e.message.toString())
                    } finally {

                    }
                }
                true
            } catch (ioe: Exception) {
                Log.d(FileUtilsTag, ioe.toString())
                false
            } finally {

            }
        }
    }
}