package io.esper.android.files.storage

import android.content.Context
import android.content.Intent
import android.os.storage.StorageVolume
import androidx.annotation.DrawableRes
import java8.nio.file.Path
import java8.nio.file.Paths
import kotlinx.parcelize.Parcelize
import io.esper.android.files.R
import io.esper.android.files.compat.getDescriptionCompat
import io.esper.android.files.compat.isPrimaryCompat
import io.esper.android.files.util.Constants
import io.esper.android.files.util.createIntent
import io.esper.android.files.util.putArgs
import io.esper.android.files.util.valueCompat

sealed class DeviceStorage() : Storage() {
    override val description: String
        get() = linuxPath

    override val path: Path
        get() = Paths.get(linuxPath)

    abstract override val linuxPath: String

    override fun createEditIntent(): Intent =
        EditDeviceStorageDialogActivity::class.createIntent()
            .putArgs(EditDeviceStorageDialogFragment.Args(this))

    fun copy_(
        customName: String? = this.customName,
        isVisible: Boolean = this.isVisible
    ): DeviceStorage =
        when (this) {
            is FileSystemRoot -> copy(customName, isVisible)
            is PrimaryStorageVolume -> copy(customName, isVisible)
        }
}

@Parcelize
data class FileSystemRoot(
    override val customName: String?,
    override val isVisible: Boolean
) : DeviceStorage() {
    override val id: Long
        get() = "FileSystemRoot".hashCode().toLong()

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.device_icon_white_24dp

    override fun getDefaultName(context: Context): String =
        context.getString(R.string.storage_file_system_root_title)

    override val linuxPath: String
        get() = LINUX_PATH

    companion object {
        const val LINUX_PATH = "/"
    }
}

@Parcelize
data class PrimaryStorageVolume(
    override val customName: String?,
    override val isVisible: Boolean,
    val rootPath: String?,
) : DeviceStorage() {
    override val id: Long
        get() = "PrimaryStorageVolume".hashCode().toLong()

    override val iconRes: Int
        @DrawableRes
        get() = R.drawable.sd_card_icon_white_24dp

    override fun getDefaultName(context: Context): String =
        storageVolume.getDescriptionCompat(context)

    override val linuxPath: String
        get() = rootPath ?: Constants.InternalRootFolder

    private val storageVolume: StorageVolume
        get() = StorageVolumeListLiveData.valueCompat.find { it.isPrimaryCompat }!!
}
