package io.esper.android.files.fileproperties.video

import android.media.MediaMetadataRetriever
import android.os.AsyncTask
import android.util.Size
import java8.nio.file.Path
import io.esper.android.files.compat.use
import io.esper.android.files.fileproperties.PathObserverLiveData
import io.esper.android.files.fileproperties.date
import io.esper.android.files.fileproperties.extractMetadataNotBlank
import io.esper.android.files.fileproperties.location
import io.esper.android.files.util.Failure
import io.esper.android.files.util.Loading
import io.esper.android.files.util.Stateful
import io.esper.android.files.util.Success
import io.esper.android.files.util.setDataSource
import io.esper.android.files.util.valueCompat
import org.threeten.bp.Duration

class VideoInfoLiveData(path: Path) : PathObserverLiveData<Stateful<VideoInfo>>(path) {
    init {
        loadValue()
        observe()
    }

    override fun loadValue() {
        value = Loading(value?.value)
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            val value = try {
                val videoInfo = MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(path)
                    val title = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_TITLE
                    )
                    val width = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                    )?.toIntOrNull()
                    val height = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                    )?.toIntOrNull()
                    val dimensions = if (width != null && height != null) {
                        Size(width, height)
                    } else {
                        null
                    }
                    val duration = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull()?.let { Duration.ofMillis(it) }
                    val date = retriever.date
                    val location = retriever.location
                    val bitRate = retriever.extractMetadataNotBlank(
                        MediaMetadataRetriever.METADATA_KEY_BITRATE
                    )?.toLongOrNull()
                    VideoInfo(title, dimensions, duration, date, location, bitRate)
                }
                Success(videoInfo)
            } catch (e: Exception) {
                Failure(valueCompat.value, e)
            }
            postValue(value)
        }
    }
}
