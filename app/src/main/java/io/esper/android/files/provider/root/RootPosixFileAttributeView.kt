package io.esper.android.files.provider.root

import io.esper.android.files.provider.common.PosixFileAttributeView
import io.esper.android.files.provider.remote.RemoteInterface
import io.esper.android.files.provider.remote.RemotePosixFileAttributeView

open class RootPosixFileAttributeView(
    attributeView: PosixFileAttributeView
) : RemotePosixFileAttributeView(
    RemoteInterface { RootFileService.getRemotePosixFileAttributeViewInterface(attributeView) }
) {
    override fun name(): String {
        throw AssertionError()
    }
}
