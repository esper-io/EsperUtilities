package io.esper.android.files.provider.remote;

import io.esper.android.files.provider.remote.ParcelableException;
import io.esper.android.files.util.RemoteCallback;

interface IRemotePathObservable {
    void addObserver(in RemoteCallback observer);

    void close(out ParcelableException exception);
}
