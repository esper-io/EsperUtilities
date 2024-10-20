package io.esper.android.files.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import com.downloadservice.filedownloadservice.manager.FileDownloadManager
import io.esper.android.files.R
import io.esper.android.files.model.AllContent
import io.esper.android.files.util.Constants.UploadDownloadUtilsTag
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
import java.io.File
import java.util.UUID

object UploadDownloadUtils {

    fun startDownload(
        mContext: Context, currentItem: AllContent
    ) {
        GeneralUtils.getInternalStoragePath(mContext)?.let { internalStoragePath ->
            val downloadFileName = currentItem.name.toString()
            val targetFile = File(internalStoragePath, downloadFileName)
            if (targetFile.exists()) {
                // If a file with the same name already exists, rename it
                val newFileName = getUniqueFileName(internalStoragePath, downloadFileName)
                FileDownloadManager.initDownload(
                    mContext, currentItem.downloadUrl.toString(), internalStoragePath, newFileName
                )
            } else {
                // If the file doesn't exist, proceed with downloading without renaming
                FileDownloadManager.initDownload(
                    mContext,
                    currentItem.downloadUrl.toString(),
                    internalStoragePath,
                    downloadFileName
                )
            }
        }
    }

    private fun getUniqueFileName(directoryPath: String, fileName: String): String {
        val directory = File(directoryPath)
        val extensionIndex = fileName.lastIndexOf('.')
        val nameWithoutExtension: String
        val extension: String

        if (extensionIndex != -1) {
            nameWithoutExtension = fileName.substring(0, extensionIndex)
            extension = fileName.substring(extensionIndex)
        } else {
            nameWithoutExtension = fileName
            extension = ""
        }

        var newName = fileName
        var counter = 1

        while (File(directory, newName).exists()) {
            newName = if (File(directory, newName).isDirectory) {
                // If the newName is a directory, recursively search for a unique file name
                val subDirectory = File(directory, newName)
                getUniqueFileName(subDirectory.absolutePath, fileName)
            } else {
                if (extension.isNotEmpty()) {
                    val uniqueName = "$nameWithoutExtension($counter)$extension"
                    counter++
                    uniqueName
                } else {
                    "$nameWithoutExtension($counter)"
                }
            }
        }
        return newName
    }

    fun uploadFile(
        filePath: String,
        fileName: String,
        context: Context,
        lifecycleOwner: LifecycleOwner,
        deleteFile: Boolean = false,
        fromService: Boolean = false
    ) {
        if (GeneralUtils.getDeviceName(
                context
            ) == null
        ) {
            Toast.makeText(
                context,
                "Info not available for upload, please set it in the managed config.",
                Toast.LENGTH_SHORT
            ).show()
            if (deleteFile) {
                FileUtils.deleteFile(filePath)
            }
            return
        }
        Toast.makeText(context, "$fileName Upload Starting", Toast.LENGTH_SHORT).show()
        val sharedPrefManaged = context.getSharedPreferences(
            Constants.SHARED_MANAGED_CONFIG_VALUES, Context.MODE_PRIVATE
        )
        val tenant = sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_TENANT, null)
        val enterpriseId =
            sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_ENTERPRISE_ID, null)
        val apiKey = sharedPrefManaged.getString(Constants.SHARED_MANAGED_CONFIG_API_KEY, null)

        val url = "$tenant/api/v0/enterprise/$enterpriseId/content/upload/"

        val uploadId = if (fromService) {
            "service-${filePath}"
        } else {
            UUID.randomUUID().toString()
        }
        MultipartUploadRequest(context, url).addFileToUpload(
            filePath = filePath, parameterName = "key"
        ).setBearerAuth(apiKey!!).setMaxRetries(3).setUploadID(uploadId)
            .setNotificationConfig { _, uploadIdForIntent ->
                val title = "Esper File Upload (${fileName})"
                UploadNotificationConfig(
                    notificationChannelId = defaultNotificationChannel!!,
                    isRingToneEnabled = true,
                    progress = UploadNotificationStatusConfig(
                        title = title,
                        message = "Uploading at ${Placeholder.UploadRate} (${Placeholder.Progress})",
                        actions = arrayListOf(
                            UploadNotificationAction(
                                icon = R.drawable.close_icon_white_24dp,
                                title = "Cancel",
                                intent = context.getCancelUploadIntent(uploadIdForIntent)
                            )
                        )
                    ),
                    success = UploadNotificationStatusConfig(
                        title = title,
                        message = "Upload completed successfully in ${Placeholder.ElapsedTime}"
                    ),
                    error = UploadNotificationStatusConfig(
                        title = title, message = "Error during upload, retry later."
                    ),
                    cancelled = UploadNotificationStatusConfig(
                        title = title, message = "Upload cancelled"
                    )
                )
            }.subscribe(context = context,
                lifecycleOwner = lifecycleOwner,
                delegate = object : RequestObserverDelegate {
                    override fun onProgress(context: Context, uploadInfo: UploadInfo) {
                        // Progress update
                        Log.i(UploadDownloadUtilsTag, "Progress: $uploadInfo")
                    }

                    override fun onSuccess(
                        context: Context, uploadInfo: UploadInfo, serverResponse: ServerResponse
                    ) {
                        Log.i(UploadDownloadUtilsTag, "Success: $serverResponse")
                        Toast.makeText(context, "File Upload Successful", Toast.LENGTH_SHORT).show()
                        if (deleteFile) {
                            FileUtils.deleteFile(filePath)
                        }
                    }

                    override fun onError(
                        context: Context, uploadInfo: UploadInfo, exception: Throwable
                    ) {
                        Log.e(UploadDownloadUtilsTag, "Error: $uploadInfo", exception)
                        handleError(context, fileName, uploadInfo, exception, deleteFile, filePath)
                    }

                    override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
                        Log.i(UploadDownloadUtilsTag, "Completed: $uploadInfo")
                        if (deleteFile) {
                            FileUtils.deleteFile(filePath)
                        }
                    }

                    override fun onCompletedWhileNotObserving() {
                        Log.i(UploadDownloadUtilsTag, "Completed while not observing")
                        if (deleteFile) {
                            FileUtils.deleteFile(filePath)
                        }
                    }
                })
    }

    private fun handleError(
        context: Context,
        fileName: String,
        uploadInfo: UploadInfo,
        exception: Throwable,
        deleteFile: Boolean,
        filePath: String
    ) {
        when (exception) {
            is UserCancelledUploadException -> {
                Toast.makeText(
                    context, "$fileName Upload Cancelled", Toast.LENGTH_SHORT
                ).show()
                Log.e(UploadDownloadUtilsTag, "Error, user cancelled upload: $uploadInfo.")
            }

            is UploadError -> {
                Log.e(
                    UploadDownloadUtilsTag, "Error, upload error: ${exception.serverResponse.code}"
                )
                if (exception.serverResponse.code == 400) {
                    Toast.makeText(
                        context,
                        "File Upload Failed: File with same name may exist in the server. Please rename the file and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, "File Upload Failed", Toast.LENGTH_SHORT).show()
                }
            }

            else -> {
                Log.e(UploadDownloadUtilsTag, "Error: $uploadInfo", exception)
                Toast.makeText(context, "File Upload Failed", Toast.LENGTH_SHORT).show()
            }
        }
        if (deleteFile) {
            FileUtils.deleteFile(filePath)
        }
    }
}