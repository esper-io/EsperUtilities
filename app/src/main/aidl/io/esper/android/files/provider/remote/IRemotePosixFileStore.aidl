package io.esper.android.files.provider.remote;

import io.esper.android.files.provider.remote.ParcelableException;

interface IRemotePosixFileStore {
    void setReadOnly(boolean readOnly, out ParcelableException exception);

    long getTotalSpace(out ParcelableException exception);

    long getUsableSpace(out ParcelableException exception);

    long getUnallocatedSpace(out ParcelableException exception);
}
