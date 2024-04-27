package io.esper.android.files.provider.remote

import java8.nio.file.FileSystem
import io.esper.android.files.provider.FileSystemProviders
import io.esper.android.files.provider.archive.archiveRefresh
import io.esper.android.files.provider.archive.archiveSetPasswords

open class RemoteFileServiceInterface : IRemoteFileService.Stub() {
    override fun getRemoteFileSystemProviderInterface(scheme: String): IRemoteFileSystemProvider =
        RemoteFileSystemProviderInterface(FileSystemProviders[scheme])

    override fun getRemoteFileSystemInterface(fileSystem: ParcelableObject): IRemoteFileSystem =
        RemoteFileSystemInterface(fileSystem.value())

    override fun getRemotePosixFileStoreInterface(
        fileStore: ParcelableObject
    ): IRemotePosixFileStore = RemotePosixFileStoreInterface(fileStore.value())

    override fun getRemotePosixFileAttributeViewInterface(
        attributeView: ParcelableObject
    ): IRemotePosixFileAttributeView =
        RemotePosixFileAttributeViewInterface(attributeView.value())

    override fun setArchivePasswords(fileSystem: ParcelableObject, passwords: List<String>) {
        fileSystem.value<FileSystem>().getPath("").archiveSetPasswords(passwords)
    }

    override fun refreshArchiveFileSystem(fileSystem: ParcelableObject) {
        fileSystem.value<FileSystem>().getPath("").archiveRefresh()
    }
}
