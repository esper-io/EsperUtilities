package io.esper.android.files.provider.remote;

import io.esper.android.files.provider.remote.ParcelableException;

interface IRemoteFileSystem {
    void close(out ParcelableException exception);
}
