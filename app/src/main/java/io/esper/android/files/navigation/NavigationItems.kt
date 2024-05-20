package io.esper.android.files.navigation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.storage.StorageVolume
import androidx.annotation.DrawableRes
import androidx.annotation.Size
import androidx.annotation.StringRes
import io.esper.android.files.R
import io.esper.android.files.about.AboutActivity
import io.esper.android.files.app.application
import io.esper.android.files.compat.getDescriptionCompat
import io.esper.android.files.compat.isPrimaryCompat
import io.esper.android.files.compat.pathCompat
import io.esper.android.files.file.JavaFile
import io.esper.android.files.file.asFileSize
import io.esper.android.files.filelist.AppStoreActivity
import io.esper.android.files.filelist.DlcActivity
import io.esper.android.files.ftpserver.FtpServerActivity
import io.esper.android.files.settings.Settings
import io.esper.android.files.settings.SettingsActivity
import io.esper.android.files.settings.StandardDirectoryListActivity
import io.esper.android.files.storage.AddStorageDialogActivity
import io.esper.android.files.storage.FileSystemRoot
import io.esper.android.files.storage.Storage
import io.esper.android.files.storage.StorageVolumeListLiveData
import io.esper.android.files.util.GeneralUtils
import io.esper.android.files.util.createIntent
import io.esper.android.files.util.isMounted
import io.esper.android.files.util.putArgs
import io.esper.android.files.util.supportsExternalStorageManager
import io.esper.android.files.util.valueCompat
import java8.nio.file.Path
import java8.nio.file.Paths

val navigationItems: List<NavigationItem?>
    get() = mutableListOf<NavigationItem?>().apply {
        if (GeneralUtils.getDeviceName(application) != null && GeneralUtils.showDeviceDetails(
                application
            )
        ) {
            addAll(deviceDetails)
            add(null)
        }
        addAll(storageItems)
        if (Environment::class.supportsExternalStorageManager()) {
            // Starting with R, we can get read/write access to non-primary storage volumes with
            // MANAGE_EXTERNAL_STORAGE. However before R, we only have read-only access to them
            // and need to use the Storage Access Framework instead, so hide them in this case
            // to avoid confusion.
            addAll(storageVolumeItems)
        }
        if (GeneralUtils.isAddingStorageAllowed(application)) {
            add(AddStorageItem())
        }
        add(null)
        addAll(menuItems)
    }
val navigationBottomListItems: List<NavigationItem?>
    get() = mutableListOf<NavigationItem?>().apply {
        addAll(settingsAndAboutItems)
    }

private val storageItems: List<NavigationItem>
    @Size(min = 0) get() = Settings.STORAGES.valueCompat.filter { it.isVisible }.map {
        if (it.path != null) PathStorageItem(it) else IntentStorageItem(it)
    }

private abstract class PathItem(val path: Path) : NavigationItem() {
    override fun isChecked(listener: Listener): Boolean = listener.currentPath == path

    override fun onClick(listener: Listener) {
        if (this is NavigationRoot) {
            listener.navigateToRoot(path)
        } else {
            listener.navigateTo(path)
        }
        listener.closeNavigationDrawer()
    }
}

private class PathStorageItem(
    private val storage: Storage
) : PathItem(storage.path!!), NavigationRoot {
    init {
        require(storage.isVisible)
    }

    override val id: Long
        get() = storage.id

    override val iconRes: Int
        @DrawableRes get() = storage.iconRes

    override fun getTitle(context: Context): String = storage.getName(context)

    override fun getSubtitle(context: Context): String? =
        storage.linuxPath?.let { getStorageSubtitle(it, context) }

    override fun onLongClick(listener: Listener): Boolean {
        listener.launchIntent(storage.createEditIntent())
        return true
    }

    override fun getName(context: Context): String = getTitle(context)
}

private class IntentStorageItem(
    private val storage: Storage
) : NavigationItem() {
    init {
        require(storage.isVisible)
    }

    override val id: Long
        get() = storage.id

    override val iconRes: Int
        @DrawableRes get() = storage.iconRes

    override fun getTitle(context: Context): String = storage.getName(context)

    override fun onClick(listener: Listener) {
        listener.launchIntent(storage.createIntent()!!)
        listener.closeNavigationDrawer()
    }

    override fun onLongClick(listener: Listener): Boolean {
        listener.launchIntent(storage.createEditIntent())
        return true
    }
}

private val storageVolumeItems: List<NavigationItem>
    @Size(min = 0) get() = StorageVolumeListLiveData.valueCompat.filter { !it.isPrimaryCompat && it.isMounted }
        .map { StorageVolumeItem(it) }

private class StorageVolumeItem(
    private val storageVolume: StorageVolume
) : PathItem(Paths.get(storageVolume.pathCompat)), NavigationRoot {
    override val id: Long
        get() = storageVolume.hashCode().toLong()

    override val iconRes: Int
        @DrawableRes get() = R.drawable.sd_card_icon_white_24dp

    override fun getTitle(context: Context): String = storageVolume.getDescriptionCompat(context)

    override fun getSubtitle(context: Context): String? =
        getStorageSubtitle(storageVolume.pathCompat, context)

    override fun getName(context: Context): String = getTitle(context)
}

private fun getStorageSubtitle(linuxPath: String, context: Context): String? {
    var totalSpace = JavaFile.getTotalSpace(linuxPath)
    val freeSpace: Long
    when {
        totalSpace != 0L -> freeSpace = JavaFile.getFreeSpace(linuxPath)
        linuxPath == FileSystemRoot.LINUX_PATH -> {
            // Root directory may not be an actual partition on legacy Android versions (can be
            // a ramdisk instead). On modern Android the system partition will be mounted as
            // root instead so let's try with the system partition again.
            // @see https://source.android.com/devices/bootloader/system-as-root
            val systemPath = Environment.getRootDirectory().path
            totalSpace = JavaFile.getTotalSpace(systemPath)
            freeSpace = JavaFile.getFreeSpace(systemPath)
        }

        else -> freeSpace = 0
    }
    if (totalSpace == 0L) {
        return null
    }
    val freeSpaceString = freeSpace.asFileSize().formatHumanReadable(context)
    val totalSpaceString = totalSpace.asFileSize().formatHumanReadable(context)
    return context.getString(
        R.string.navigation_storage_subtitle_format, freeSpaceString, totalSpaceString
    )
}

private class AddStorageItem : NavigationItem() {
    override val id: Long = R.string.navigation_add_storage.toLong()

    @DrawableRes
    override val iconRes: Int = R.drawable.add_icon_white_24dp

    override fun getTitle(context: Context): String =
        context.getString(R.string.navigation_add_storage)

    override fun onClick(listener: Listener) {
        listener.launchIntent(AddStorageDialogActivity::class.createIntent())
    }
}

private val deviceDetails: List<NavigationItem>
    @Size(min = 0) get() = listOf(deviceDetailsItem())

fun deviceDetailsItem(): NavigationItem {
    var clickCount = 0
    val requiredClicks = 10 // Adjust this value as needed
    val delayMillis = 3000L // Adjust this value as needed (e.g., 3000 milliseconds = 3 seconds)

    val handler = Handler()
    val resetClickCountRunnable = Runnable {
        // Reset the click count after the delay
        clickCount = 0
    }

    return object : NavigationItem() {
        override val id: Long = R.string.navigation_device_details.toLong()

        @DrawableRes
        override val iconRes: Int = R.drawable.device_icon_white_24dp

        override fun getTitle(context: Context): String {
            return GeneralUtils.getDeviceName(context)
                ?: context.getString(R.string.navigation_device_details_title_unknown)
        }

        override fun getSubtitle(context: Context): String {
            return GeneralUtils.getDeviceSerial(context)
                ?: context.getString(R.string.navigation_device_details_title_unknown)
        }

        override fun onClick(listener: Listener) {
            clickCount++
            handler.removeCallbacks(resetClickCountRunnable)
            if (clickCount >= requiredClicks) {
                // Todo - add required action here

                clickCount = 0
            } else {
                handler.postDelayed(resetClickCountRunnable, delayMillis)
            }
        }
    }
}

private val standardDirectoryItems: List<NavigationItem>
    @Size(min = 0) get() = StandardDirectoriesLiveData.valueCompat.filter { it.isEnabled }
        .map { StandardDirectoryItem(it) }

private class StandardDirectoryItem(
    private val standardDirectory: StandardDirectory
) : PathItem(Paths.get(getExternalStorageDirectory(standardDirectory.relativePath))) {
    init {
        require(standardDirectory.isEnabled)
    }

    override val id: Long
        get() = standardDirectory.id

    override val iconRes: Int
        @DrawableRes get() = standardDirectory.iconRes

    override fun getTitle(context: Context): String = standardDirectory.getTitle(context)

    override fun onLongClick(listener: Listener): Boolean {
        listener.launchIntent(StandardDirectoryListActivity::class.createIntent())
        return true
    }
}

val standardDirectories: List<StandardDirectory>
    get() {
        val settingsMap = Settings.STANDARD_DIRECTORY_SETTINGS.valueCompat.associateBy { it.id }
        return defaultStandardDirectories.map {
            val settings = settingsMap[it.key]
            if (settings != null) it.withSettings(settings) else it
        }
    }

private const val relativePathSeparator = ":"

private val defaultStandardDirectories: List<StandardDirectory>
    // HACK: Show QQ, TIM and WeChat standard directories based on whether the directory exists.
    get() = DEFAULT_STANDARD_DIRECTORIES.mapNotNull {
        when (it.iconRes) {
            R.drawable.qq_icon_white_24dp, R.drawable.tim_icon_white_24dp, R.drawable.wechat_icon_white_24dp -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Direct access to Android/data is blocked since Android 11.
                    null
                } else {
                    for (relativePath in it.relativePath.split(relativePathSeparator)) {
                        val path = getExternalStorageDirectory(relativePath)
                        if (JavaFile.isDirectory(path)) {
                            return@mapNotNull it.copy(relativePath = relativePath)
                        }
                    }
                    null
                }
            }

            else -> it
        }
    }

// @see android.os.Environment#STANDARD_DIRECTORIES
private val DEFAULT_STANDARD_DIRECTORIES = listOf(
    StandardDirectory(
        R.drawable.alarm_icon_white_24dp,
        R.string.navigation_standard_directory_alarms,
        Environment.DIRECTORY_ALARMS,
        false
    ), StandardDirectory(
        R.drawable.camera_icon_white_24dp,
        R.string.navigation_standard_directory_dcim,
        Environment.DIRECTORY_DCIM,
        true
    ), StandardDirectory(
        R.drawable.document_icon_white_24dp,
        R.string.navigation_standard_directory_documents,
        Environment.DIRECTORY_DOCUMENTS,
        false
    ), StandardDirectory(
        R.drawable.download_icon_white_24dp,
        R.string.navigation_standard_directory_downloads,
        Environment.DIRECTORY_DOWNLOADS,
        true
    ), StandardDirectory(
        R.drawable.video_icon_white_24dp,
        R.string.navigation_standard_directory_movies,
        Environment.DIRECTORY_MOVIES,
        true
    ), StandardDirectory(
        R.drawable.audio_icon_white_24dp,
        R.string.navigation_standard_directory_music,
        Environment.DIRECTORY_MUSIC,
        true
    ), StandardDirectory(
        R.drawable.notification_icon_white_24dp,
        R.string.navigation_standard_directory_notifications,
        Environment.DIRECTORY_NOTIFICATIONS,
        false
    ), StandardDirectory(
        R.drawable.image_icon_white_24dp,
        R.string.navigation_standard_directory_pictures,
        Environment.DIRECTORY_PICTURES,
        true
    ), StandardDirectory(
        R.drawable.podcast_icon_white_24dp,
        R.string.navigation_standard_directory_podcasts,
        Environment.DIRECTORY_PODCASTS,
        false
    ), StandardDirectory(
        R.drawable.ringtone_icon_white_24dp,
        R.string.navigation_standard_directory_ringtones,
        Environment.DIRECTORY_RINGTONES,
        false
    ), StandardDirectory(
        R.drawable.qq_icon_white_24dp, R.string.navigation_standard_directory_qq, listOf(
            "Android/data/com.tencent.mobileqq/Tencent/QQfile_recv", "Tencent/QQfile_recv"
        ).joinToString(relativePathSeparator), true
    ), StandardDirectory(
        R.drawable.tim_icon_white_24dp, R.string.navigation_standard_directory_tim, listOf(
            "Android/data/com.tencent.tim/Tencent/TIMfile_recv", "Tencent/TIMfile_recv"
        ).joinToString(relativePathSeparator), true
    ), StandardDirectory(
        R.drawable.wechat_icon_white_24dp, R.string.navigation_standard_directory_wechat, listOf(
            "Android/data/com.tencent.mm/MicroMsg/Download", "Tencent/MicroMsg/Download"
        ).joinToString(relativePathSeparator), true
    )
)

internal fun getExternalStorageDirectory(relativePath: String): String =
    @Suppress("DEPRECATION") Environment.getExternalStoragePublicDirectory(relativePath).path

private val bookmarkDirectoryItems: List<NavigationItem>
    @Size(min = 0) get() = Settings.BOOKMARK_DIRECTORIES.valueCompat.map { BookmarkDirectoryItem(it) }

private class BookmarkDirectoryItem(
    private val bookmarkDirectory: BookmarkDirectory
) : PathItem(bookmarkDirectory.path) {
    // We cannot simply use super.getId() because different bookmark directories may have
    // the same path.
    override val id: Long
        get() = bookmarkDirectory.id

    @DrawableRes
    override val iconRes: Int = R.drawable.directory_icon_white_24dp

    override fun getTitle(context: Context): String = bookmarkDirectory.name

    override fun onLongClick(listener: Listener): Boolean {
        listener.launchIntent(
            EditBookmarkDirectoryDialogActivity::class.createIntent()
                .putArgs(EditBookmarkDirectoryDialogFragment.Args(bookmarkDirectory))
        )
        return true
    }
}

private val menuItems: List<NavigationItem>
    get() {
        val items = mutableListOf<NavigationItem>()
        if (GeneralUtils.isDlcAllowed(application)) {
            items.add(
                IntentMenuItem(
                    R.drawable.dlc_icon,
                    R.string.downloadable_content,
                    DlcActivity::class.createIntent()
                )
            )
        }
        if (GeneralUtils.isEsperAppStoreVisible(application)) {
            items.add(
                IntentMenuItem(
                    R.drawable.esper_app_store_icon,
                    R.string.esper_app_store,
                    AppStoreActivity::class.createIntent()
                )
            )
        }
        if (GeneralUtils.isFtpServerAllowed(application)) {
            items.add(
                IntentMenuItem(
                    R.drawable.ftp_icon_24,
                    R.string.navigation_ftp_server,
                    FtpServerActivity::class.createIntent()
                )
            )
        }

        return items
    }

private val settingsAndAboutItems: List<NavigationItem>
    get() {
        val items = mutableListOf<NavigationItem>()
        items.addAll(
            listOf(
                IntentMenuItem(
                    R.drawable.settings_icon_white_24dp,
                    R.string.navigation_settings,
                    SettingsActivity::class.createIntent()
                ), IntentMenuItem(
                    R.drawable.about_icon_white_24dp,
                    R.string.navigation_about,
                    AboutActivity::class.createIntent()
                )
            )
        )
        return items
    }

private abstract class MenuItem(
    @DrawableRes override val iconRes: Int, @StringRes val titleRes: Int
) : NavigationItem() {
    override fun getTitle(context: Context): String = context.getString(titleRes)
}

private class IntentMenuItem(
    @DrawableRes iconRes: Int, @StringRes titleRes: Int, private val intent: Intent
) : MenuItem(iconRes, titleRes) {
    override val id: Long
        get() = intent.component.hashCode().toLong()

    override fun onClick(listener: Listener) {
        listener.launchIntent(intent)
        listener.closeNavigationDrawer()
    }
}
