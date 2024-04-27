package io.esper.android.files.provider.sftp

import io.esper.android.files.provider.common.PosixFileModeBit
import io.esper.android.files.provider.common.toInt
import net.schmizz.sshj.sftp.FileAttributes

fun Set<PosixFileModeBit>.toSftpAttributes(): FileAttributes =
    FileAttributes.Builder().withPermissions(toInt()).build()
