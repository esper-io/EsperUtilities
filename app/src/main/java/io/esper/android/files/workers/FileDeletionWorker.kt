package io.esper.android.files.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File

class FileDeletionWorker(
    context: Context, workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val filePath = inputData.getString("FILE_PATH") ?: return Result.failure()
        val file = File(filePath)
        return if (file.exists() && file.delete()) {
            Log.d("FileDeletionWorker", "File deleted: $filePath")
            Result.success()
        } else {
            Log.d("FileDeletionWorker", "Failed to delete file: $filePath")
            Result.failure()
        }
    }
}
