package io.esper.android.files.provider.linux

import android.os.Parcel
import android.os.Parcelable
import io.esper.android.files.provider.root.RootPosixFileStore
import io.esper.android.files.provider.root.RootablePosixFileStore
import io.esper.android.files.util.readParcelable

internal class LinuxFileStore private constructor(
    private val path: LinuxPath,
    private val localFileStore: LocalLinuxFileStore
) : RootablePosixFileStore(path, localFileStore, { RootPosixFileStore(it) }) {
    constructor(path: LinuxPath) : this(path, LocalLinuxFileStore(path))

    private constructor(source: Parcel) : this(source.readParcelable()!!, source.readParcelable()!!)

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(path, flags)
        dest.writeParcelable(localFileStore, flags)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<LinuxFileStore> {
            override fun createFromParcel(source: Parcel): LinuxFileStore = LinuxFileStore(source)

            override fun newArray(size: Int): Array<LinuxFileStore?> = arrayOfNulls(size)
        }
    }
}
