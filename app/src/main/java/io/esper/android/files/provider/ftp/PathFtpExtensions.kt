package io.esper.android.files.provider.ftp

import java8.nio.file.Path
import io.esper.android.files.provider.ftp.client.Authority

fun Authority.createFtpRootPath(): Path =
    FtpFileSystemProvider.getOrNewFileSystem(this).rootDirectory
