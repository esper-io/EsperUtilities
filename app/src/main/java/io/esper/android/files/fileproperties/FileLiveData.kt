package io.esper.android.files.fileproperties

import android.os.AsyncTask
import java8.nio.file.Path
import io.esper.android.files.file.FileItem
import io.esper.android.files.file.loadFileItem
import io.esper.android.files.util.Failure
import io.esper.android.files.util.Loading
import io.esper.android.files.util.Stateful
import io.esper.android.files.util.Success
import io.esper.android.files.util.valueCompat

class FileLiveData private constructor(
    path: Path,
    file: FileItem?
) : PathObserverLiveData<Stateful<FileItem>>(path) {
    constructor(path: Path) : this(path, null)

    constructor(file: FileItem) : this(file.path, file)

    init {
        if (file != null) {
            value = Success(file)
        } else {
            loadValue()
        }
        observe()
    }

    override fun loadValue() {
        value = Loading(value?.value)
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val value = try {
                val file = path.loadFileItem()
                Success(file)
            } catch (e: Exception) {
                Failure(valueCompat.value, e)
            }
            postValue(value)
        }
    }
}
