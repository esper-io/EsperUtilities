package io.esper.android.files.provider.root

import io.esper.android.files.provider.common.PosixFileStore
import io.esper.android.files.provider.remote.RemoteInterface
import io.esper.android.files.provider.remote.RemotePosixFileStore

class RootPosixFileStore(fileStore: PosixFileStore) : RemotePosixFileStore(
    RemoteInterface { RootFileService.getRemotePosixFileStoreInterface(fileStore) }
)
