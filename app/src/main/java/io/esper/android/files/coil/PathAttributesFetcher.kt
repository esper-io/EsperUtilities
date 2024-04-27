package io.esper.android.files.coil

import android.content.Context
import android.content.pm.ApplicationInfo
import android.media.MediaMetadataRetriever
import android.os.ParcelFileDescriptor
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import coil.size.Dimension
import java8.nio.file.Path
import java8.nio.file.attribute.BasicFileAttributes
import io.esper.android.files.R
import io.esper.android.files.compat.use
import io.esper.android.files.file.MimeType
import io.esper.android.files.file.asMimeType
import io.esper.android.files.file.isApk
import io.esper.android.files.file.isImage
import io.esper.android.files.file.isMedia
import io.esper.android.files.file.isPdf
import io.esper.android.files.file.isVideo
import io.esper.android.files.file.lastModifiedInstant
import io.esper.android.files.filelist.isRemotePath
import io.esper.android.files.provider.common.AndroidFileTypeDetector
import io.esper.android.files.provider.common.newInputStream
import io.esper.android.files.provider.content.resolver.ResolverException
import io.esper.android.files.provider.document.documentSupportsThumbnail
import io.esper.android.files.provider.document.isDocumentPath
import io.esper.android.files.provider.document.resolver.DocumentResolver
import io.esper.android.files.provider.ftp.isFtpPath
import io.esper.android.files.provider.linux.isLinuxPath
import io.esper.android.files.settings.Settings
import io.esper.android.files.util.getDimensionPixelSize
import io.esper.android.files.util.getPackageArchiveInfoCompat
import io.esper.android.files.util.isGetPackageArchiveInfoCompatible
import io.esper.android.files.util.isMediaMetadataRetrieverCompatible
import io.esper.android.files.util.runWithCancellationSignal
import io.esper.android.files.util.setDataSource
import io.esper.android.files.util.valueCompat
import okio.buffer
import okio.source
import java.io.Closeable
import java.io.IOException
import io.esper.android.files.util.setDataSource as appSetDataSource

class PathAttributesKeyer : Keyer<Pair<Path, BasicFileAttributes>> {
    override fun key(data: Pair<Path, BasicFileAttributes>, options: Options): String {
        val (path, attributes) = data
        return "$path:${attributes.lastModifiedInstant.toEpochMilli()}"
    }
}

class PathAttributesFetcher(
    private val data: Pair<Path, BasicFileAttributes>,
    private val options: Options,
    private val imageLoader: ImageLoader,
    private val appIconFetcherFactory: AppIconFetcher.Factory<Path>,
    private val videoFrameFetcherFactory: VideoFrameFetcher.Factory<Path>,
    private val pdfPageFetcherFactory: PdfPageFetcher.Factory<Path>
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val (path, attributes) = data
        val (width, height) = options.size
        // @see android.provider.MediaStore.ThumbnailConstants.MINI_SIZE
        val isThumbnail = width is Dimension.Pixels && width.px <= 512
            && height is Dimension.Pixels && height.px <= 384
        if (isThumbnail) {
            width as Dimension.Pixels
            height as Dimension.Pixels
            if (path.isDocumentPath && attributes.documentSupportsThumbnail) {
                val thumbnail = runWithCancellationSignal { signal ->
                    try {
                        DocumentResolver.getThumbnail(
                            path as DocumentResolver.Path, width.px, height.px, signal
                        )
                    } catch (e: ResolverException) {
                        e.printStackTrace()
                        null
                    }
                }
                if (thumbnail != null) {
                    return DrawableResult(
                        thumbnail.toDrawable(options.context.resources), true, path.dataSource
                    )
                }
            }
            if (path.isRemotePath) {
                // FTP doesn't support random access and requires one connection per parallel read.
                val shouldReadRemotePath = !path.isFtpPath
                    && Settings.READ_REMOTE_FILES_FOR_THUMBNAIL.valueCompat
                if (!shouldReadRemotePath) {
                    error("Cannot read $path for thumbnail")
                }
            }
        }
        val mimeType = AndroidFileTypeDetector.getMimeType(data.first, data.second).asMimeType()
        when {
            mimeType.isApk && path.isGetPackageArchiveInfoCompatible -> {
                try {
                    return appIconFetcherFactory.create(path, options, imageLoader).fetch()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            mimeType.isImage || mimeType == MimeType.GENERIC -> {
                val inputStream = path.newInputStream()
                return SourceResult(
                    ImageSource(inputStream.source().buffer(), options.context),
                    if (mimeType != MimeType.GENERIC) mimeType.value else null, path.dataSource
                )
            }
            mimeType.isMedia && path.isMediaMetadataRetrieverCompatible -> {
                val embeddedPicture = try {
                    MediaMetadataRetriever().use { retriever ->
                        retriever.setDataSource(path)
                        retriever.embeddedPicture
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                if (embeddedPicture != null) {
                    return SourceResult(
                        ImageSource(
                            embeddedPicture.inputStream().source().buffer(), options.context
                        ), null, path.dataSource
                    )
                }
                if (mimeType.isVideo) {
                    try {
                        return videoFrameFetcherFactory.create(path, options, imageLoader).fetch()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            mimeType.isPdf && (path.isLinuxPath || path.isDocumentPath) -> {
                try {
                    return pdfPageFetcherFactory.create(path, options, imageLoader).fetch()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    class Factory(private val context: Context) : Fetcher.Factory<Pair<Path, BasicFileAttributes>> {
        private val appIconFetcherFactory = object : AppIconFetcher.Factory<Path>(
            // This is used by FileListAdapter.
            context.getDimensionPixelSize(R.dimen.large_icon_size), context
        ) {
            override fun getApplicationInfo(data: Path): Pair<ApplicationInfo, Closeable?> {
                val (packageInfo, closeable) =
                    context.packageManager.getPackageArchiveInfoCompat(data, 0)
                val applicationInfo = packageInfo?.applicationInfo
                if (applicationInfo == null) {
                    closeable?.close()
                    throw IOException("ApplicationInfo is null")
                }
                return applicationInfo to closeable
            }
        }

        private val videoFrameFetcherFactory = object : VideoFrameFetcher.Factory<Path>() {
            override fun MediaMetadataRetriever.setDataSource(data: Path) {
                appSetDataSource(data)
            }
        }

        private val pdfPageFetcherFactory = object : PdfPageFetcher.Factory<Path>() {
            override fun openParcelFileDescriptor(data: Path): ParcelFileDescriptor =
                when {
                    data.isLinuxPath ->
                        ParcelFileDescriptor.open(data.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
                    data.isDocumentPath ->
                        DocumentResolver.openParcelFileDescriptor(data as DocumentResolver.Path, "r")
                    else -> throw IllegalArgumentException(data.toString())
                }
        }

        override fun create(
            data: Pair<Path, BasicFileAttributes>,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher =
            PathAttributesFetcher(
                data, options, imageLoader, appIconFetcherFactory, videoFrameFetcherFactory,
                pdfPageFetcherFactory
            )
    }
}
