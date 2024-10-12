package io.esper.android.service

import android.content.Context
import android.util.Log
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.exceptions.UploadError
import net.gotev.uploadservice.exceptions.UserCancelledUploadException
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import java.io.File

class GlobalUploadObserver(private val service: LogCollectionService) : RequestObserverDelegate {

    private val TAG = "GlobalUploadObserver"

    override fun onProgress(context: Context, uploadInfo: UploadInfo) {
        if (uploadInfo.uploadId.startsWith("service-")) {
            Log.i(TAG, "Progress: $uploadInfo")
        }
    }

    override fun onSuccess(
        context: Context,
        uploadInfo: UploadInfo,
        serverResponse: ServerResponse
    ) {
        handleSuccessOrError(uploadInfo, "Success: $serverResponse")
    }

    override fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable) {
        val errorMessage = when (exception) {
            is InterruptedException -> "Error, upload cancelled: $uploadInfo"
            is UserCancelledUploadException -> "Error, user cancelled upload: $uploadInfo"
            is UploadError -> "Error, upload error: ${exception.serverResponse}"
            else -> "Error: $uploadInfo"
        }
        Log.e(TAG, errorMessage, exception)
        handleSuccessOrError(uploadInfo, errorMessage)
    }

    private fun handleSuccessOrError(uploadInfo: UploadInfo, logMessage: String) {
        if (uploadInfo.uploadId.startsWith("service-")) {
            Log.i(TAG, logMessage)
            val filePath = uploadInfo.uploadId.removePrefix("service-")
            deleteFile(filePath)
            service.stopService()
        }
    }

    private fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (file.exists() && file.delete()) {
            Log.i(TAG, "File deleted successfully: $filePath")
        } else {
            Log.e(TAG, "Failed to delete file: $filePath")
        }
    }

    override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
        if (uploadInfo.uploadId.startsWith("service-")) {
            Log.i(TAG, "Completed: $uploadInfo")
        }
    }

    override fun onCompletedWhileNotObserving() {
        Log.i(TAG, "Completed while not observing")
    }
}
