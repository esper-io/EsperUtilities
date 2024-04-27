package io.esper.android.files.app

import android.os.AsyncTask
import android.os.Build
import android.webkit.WebView
import com.jakewharton.threetenabp.AndroidThreeTen
import jcifs.context.SingletonContext
import io.esper.android.files.BuildConfig
import io.esper.android.files.coil.initializeCoil
import io.esper.android.files.filejob.fileJobNotificationTemplate
import io.esper.android.files.ftpserver.ftpServerServiceNotificationTemplate
import io.esper.android.files.hiddenapi.HiddenApi
import io.esper.android.files.provider.FileSystemProviders
import io.esper.android.files.settings.Settings
import io.esper.android.files.storage.FtpServerAuthenticator
import io.esper.android.files.storage.SftpServerAuthenticator
import io.esper.android.files.storage.SmbServerAuthenticator
import io.esper.android.files.storage.StorageVolumeListLiveData
import io.esper.android.files.storage.WebDavServerAuthenticator
import io.esper.android.files.theme.custom.CustomThemeHelper
import io.esper.android.files.theme.night.NightModeHelper
import java.util.Properties
import io.esper.android.files.provider.ftp.client.Client as FtpClient
import io.esper.android.files.provider.sftp.client.Client as SftpClient
import io.esper.android.files.provider.smb.client.Client as SmbClient
import io.esper.android.files.provider.webdav.client.Client as WebDavClient

val appInitializers = listOf(
    ::disableHiddenApiChecks,
    ::initializeThreeTen,
    ::initializeWebViewDebugging,
    ::initializeCoil,
    ::initializeFileSystemProviders,
    ::upgradeApp,
    ::initializeLiveDataObjects,
    ::initializeCustomTheme,
    ::initializeNightMode,
    ::createNotificationChannels,
    ::uploadServiceConfig
)

private fun disableHiddenApiChecks() {
    HiddenApi.disableHiddenApiChecks()
}

private fun initializeThreeTen() {
    AndroidThreeTen.init(application)
}

private fun initializeWebViewDebugging() {
    if (BuildConfig.DEBUG) {
        WebView.setWebContentsDebuggingEnabled(true)
    }
}

private fun initializeFileSystemProviders() {
    FileSystemProviders.install()
    FileSystemProviders.overflowWatchEvents = true
    // SingletonContext.init() calls NameServiceClientImpl.initCache() which connects to network.
    AsyncTask.THREAD_POOL_EXECUTOR.execute {
        SingletonContext.init(Properties().apply {
            setProperty("jcifs.netbios.cachePolicy", "0")
            setProperty("jcifs.smb.client.maxVersion", "SMB1")
        })
    }
    FtpClient.authenticator = FtpServerAuthenticator
    SftpClient.authenticator = SftpServerAuthenticator
    SmbClient.authenticator = SmbServerAuthenticator
    WebDavClient.authenticator = WebDavServerAuthenticator
}

private fun initializeLiveDataObjects() {
    // Force initialization of LiveData objects so that it won't happen on a background thread.
    StorageVolumeListLiveData.value
    Settings.initializeFileListDefaultDirectory(application.applicationContext).value
}

private fun initializeCustomTheme() {
    CustomThemeHelper.initialize(application)
}

private fun initializeNightMode() {
    NightModeHelper.initialize(application)
}

private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        notificationManager.createNotificationChannels(listOf(
            backgroundActivityStartNotificationTemplate.channelTemplate,
            fileJobNotificationTemplate.channelTemplate,
            ftpServerServiceNotificationTemplate.channelTemplate,
        ).map { it.create(application) })
    }
}


private fun uploadServiceConfig() {
    net.gotev.uploadservice.UploadServiceConfig.initialize(
        context = application, defaultNotificationChannel = "file_job", debug = BuildConfig.DEBUG
    )
}
