package io.esper.android.files.provider.remote;

import io.esper.android.files.provider.common.ParcelableFileTime;
import io.esper.android.files.provider.common.ParcelablePosixFileMode;
import io.esper.android.files.provider.common.PosixGroup;
import io.esper.android.files.provider.common.PosixUser;
import io.esper.android.files.provider.remote.ParcelableException;
import io.esper.android.files.provider.remote.ParcelableObject;

interface IRemotePosixFileAttributeView {
    ParcelableObject readAttributes(out ParcelableException exception);

    void setTimes(
        in ParcelableFileTime lastModifiedTime,
        in ParcelableFileTime lastAccessTime,
        in ParcelableFileTime createTime,
        out ParcelableException exception
    );

    void setOwner(in PosixUser owner, out ParcelableException exception);

    void setGroup(in PosixGroup group, out ParcelableException exception);

    void setMode(in ParcelablePosixFileMode mode, out ParcelableException exception);

    void setSeLinuxContext(in ParcelableObject context, out ParcelableException exception);

    void restoreSeLinuxContext(out ParcelableException exception);
}
