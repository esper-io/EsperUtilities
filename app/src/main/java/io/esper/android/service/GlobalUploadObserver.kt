package io.esper.android.service

import android.content.Context
import android.util.Log
import io.esper.android.files.util.Constants
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.exceptions.UploadError
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import java.io.File

class GlobalUploadObserver(private val service: LogCollectionService) : RequestObserverDelegate {

    override fun onProgress(context: Context, uploadInfo: UploadInfo) {
        if (uploadInfo.uploadId.startsWith("service-")) {
            Log.i(Constants.GlobalUploadObserverTag, "Progress: $uploadInfo")
        }
    }

    override fun onSuccess(
        context: Context, uploadInfo: UploadInfo, serverResponse: ServerResponse
    ) {
        // Check if the upload ID starts with "service-"
        if (uploadInfo.uploadId.startsWith("service-")) {
            Log.i(Constants.GlobalUploadObserverTag, "Success: $serverResponse")
            // Extract the file path by removing "service-" prefix
            val filePath = uploadInfo.uploadId.removePrefix("service-")
            val file = File(filePath)
            if (file.exists() && file.delete()) {
                Log.i(Constants.GlobalUploadObserverTag, "File deleted successfully: $filePath")
            } else {
                Log.e(Constants.GlobalUploadObserverTag, "Failed to delete file: $filePath")
            }
        }

        // Stop the service after a successful upload
        service.stopService()
    }

    override fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable) {
        if (uploadInfo.uploadId.startsWith("service-")) {
            when (exception) {
                is InterruptedException -> Log.e(
                    Constants.GlobalUploadObserverTag, "Error, upload cancelled: $uploadInfo"
                )

                is UserCancelledUploadException -> Log.e(
                    Constants.GlobalUploadObserverTag, "Error, user cancelled upload: $uploadInfo"
                )

                is UploadError -> Log.e(
                    Constants.GlobalUploadObserverTag,
                    "Error, upload error: ${exception.serverResponse}"
                )

                else -> Log.e(Constants.GlobalUploadObserverTag, "Error: $uploadInfo", exception)
            }

            // Check if the upload ID starts with "service-"
            if (uploadInfo.uploadId.startsWith("service-")) {
                // Extract the file path by removing "service-" prefix
                val filePath = uploadInfo.uploadId.removePrefix("service-")
                val file = File(filePath)
                if (file.exists() && file.delete()) {
                    Log.i(Constants.GlobalUploadObserverTag, "File deleted successfully: $filePath")
                } else {
                    Log.e(Constants.GlobalUploadObserverTag, "Failed to delete file: $filePath")
                }
            }

            // Stop the service even on error
            service.stopService()
        }
    }

    override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
        if (uploadInfo.uploadId.startsWith("service-")) {
            Log.i(Constants.GlobalUploadObserverTag, "Completed: $uploadInfo")
        }
    }

    override fun onCompletedWhileNotObserving() {
        Log.i(Constants.GlobalUploadObserverTag, "Completed while not observing")
    }
}
