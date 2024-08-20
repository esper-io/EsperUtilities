package io.esper.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import io.esper.android.files.R
import io.esper.android.files.filelist.FileListActivity
import io.esper.android.files.util.Constants
import io.esper.android.files.util.GeneralUtils
import io.esper.android.files.util.UploadDownloadUtils
import net.gotev.uploadservice.observer.request.GlobalRequestObserver
import java.io.File

class LogCollectionService : LifecycleService() {
    private val CHANNEL_ID = "LogCollectionServiceChannel"

    private lateinit var zipFileName: String
    private lateinit var globalUploadObserver: GlobalUploadObserver

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceWithNotification()

        // Initialize the global upload observer
        globalUploadObserver = GlobalUploadObserver(this)
        GlobalRequestObserver(application, globalUploadObserver)
    }

    fun stopService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Log Collection Service Channel", NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val notificationIntent = Intent(this, FileListActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("Log Collection Service")
                .setContentText("Collecting and uploading logs")
                .setSmallIcon(R.drawable.notification_icon).setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW).build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            "io.esper.android.files.ACTION_START_LOG_COLLECTION" -> {
                val logPath = intent.getStringExtra("log_path")
                if (!logPath.isNullOrEmpty()) {
                    Log.i(
                        Constants.LogCollectionServiceTag,
                        "Log path is $logPath, proceeding with log collection."
                    )
                    zipFileName =
                        "${GeneralUtils.getDeviceName(this)}-${GeneralUtils.getCurrentDateTime()}.zip"
                    Handler(Looper.getMainLooper()).postDelayed({
                        processLogPath(logPath)
                    }, 10000) // 10 seconds delay
                } else {
                    Log.i(
                        Constants.LogCollectionServiceTag,
                        "Log path is missing, skipping log collection."
                    )
                }
            }

            else -> {
                Log.e(
                    Constants.LogCollectionServiceTag, "Unknown action received: ${intent?.action}"
                )
            }
        }
        return START_NOT_STICKY
    }

    private fun processLogPath(logPath: String) {
        val logFile = File(logPath)
        when {
            logFile.isDirectory && logFile.listFiles()?.isNotEmpty() == true -> {
                // Directory is not empty, compress it
                Log.i(Constants.LogCollectionServiceTag, "Compressing directory: $logPath")
                UploadDownloadUtils.Compress(
                    context = this,
                    toZipFolder = logPath,
                    zipFileName = zipFileName,
                    viewLifecycleOwner = this,
                    sharedPrefManaged = getSharedPreferences(
                        Constants.SHARED_MANAGED_CONFIG_VALUES, MODE_PRIVATE
                    ),
                    fromService = true
                ).execute()
            }

            logFile.isFile -> {
                // File is not empty, compress it
                Log.i(Constants.LogCollectionServiceTag, "Compressing file: $logPath")
                val filePathsToZip: ArrayList<String> = ArrayList()
                filePathsToZip.add(logPath)
                UploadDownloadUtils.CompressMultipleFiles(
                    context = this,
                    filePathsToZip = filePathsToZip,
                    zipFileName = zipFileName,
                    viewLifecycleOwner = this,
                    sharedPrefManaged = getSharedPreferences(
                        Constants.SHARED_MANAGED_CONFIG_VALUES, MODE_PRIVATE
                    ),
                    upload = true,
                    fromService = true
                ).execute()
            }

            else -> {
                // Log path is neither a file nor a directory
                Log.e(Constants.LogCollectionServiceTag, "Invalid log path: $logPath")
                stopService()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}

/**
 * Usage/Trigger:
 *
 * {
 *     "command_type": "DEVICE",
 *     "command": "UPDATE_DEVICE_CONFIG",
 *     "command_args": {
 *         "custom_settings_config": {
 *             "scripts": [
 *                 {
 *                     "action": "LAUNCH",
 *                     "launchType": "SERVICE",
 *                     "actionParams": {
 *                         "intentAction": "io.esper.android.files.ACTION_START_LOG_COLLECTION",
 *                         "componentName": "io.esper.android.files/io.esper.android.service.LogCollectionService",
 *                         "flags": 268435456,
 *                         "serviceType": "FOREGROUND",
 *                         "extras": {
 *                             "log_path": "/storage/emulated/0/esperfiles/qwe123qwe.png"
 *                         }
 *                     }
 *                 }
 *             ]
 *         }
 *     },
 *     "devices": [
 *         "50055d6a-92ab-4c41-b49b-cc8b9fe2cc79"
 *     ],
 *     "device_type": "all"
 * }
 */