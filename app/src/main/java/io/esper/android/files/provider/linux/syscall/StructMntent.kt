package io.esper.android.files.provider.linux.syscall

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import io.esper.android.files.provider.common.ByteString

@Parcelize
class StructMntent(
    val mnt_fsname: ByteString,
    val mnt_dir: ByteString,
    val mnt_type: ByteString,
    val mnt_opts: ByteString,
    val mnt_freq: Int,
    val mnt_passno: Int
) : Parcelable
