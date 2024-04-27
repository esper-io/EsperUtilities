package io.esper.android.files.provider.webdav

import java8.nio.file.Path
import io.esper.android.files.provider.webdav.client.Authority

fun Authority.createWebDavRootPath(): Path =
    WebDavFileSystemProvider.getOrNewFileSystem(this).rootDirectory
