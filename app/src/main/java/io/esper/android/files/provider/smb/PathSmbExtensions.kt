package io.esper.android.files.provider.smb

import java8.nio.file.Path
import io.esper.android.files.provider.smb.client.Authority

fun Authority.createSmbRootPath(): Path =
    SmbFileSystemProvider.getOrNewFileSystem(this).rootDirectory
