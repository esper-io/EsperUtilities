package io.esper.android.files.provider.remote;

import io.esper.android.files.provider.remote.IRemoteFileSystem;
import io.esper.android.files.provider.remote.IRemoteFileSystemProvider;
import io.esper.android.files.provider.remote.IRemotePosixFileAttributeView;
import io.esper.android.files.provider.remote.IRemotePosixFileStore;
import io.esper.android.files.provider.remote.ParcelableObject;

interface IRemoteFileService {
    IRemoteFileSystemProvider getRemoteFileSystemProviderInterface(String scheme);

    IRemoteFileSystem getRemoteFileSystemInterface(in ParcelableObject fileSystem);

    IRemotePosixFileStore getRemotePosixFileStoreInterface(in ParcelableObject fileStore);

    IRemotePosixFileAttributeView getRemotePosixFileAttributeViewInterface(
        in ParcelableObject attributeView
    );

    void setArchivePasswords(in ParcelableObject fileSystem, in List<String> passwords);

    void refreshArchiveFileSystem(in ParcelableObject fileSystem);
}
