package io.esper.android.network

import io.esper.android.network.model.ResultItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

class NetworkCheckTask(
    private val adapter: NetworkResultAdapter,
    private val items: MutableList<ResultItem>,
    private val itemsToCheck: List<ResultItem>
) {
    var job: Job? = null
    private val successCount = AtomicInteger(0)
    private val failureCount = AtomicInteger(0)

    fun start() {
        job = CoroutineScope(Dispatchers.IO).launch {
            itemsToCheck.map { item ->
                async {
                    val isAccessible = if (item.url.startsWith("http")) {
                        try {
                            checkUrl(item.url, item.port)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            false
                        }
                    } else {
                        try {
                            checkPort(item.url, item.port)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            false
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (isAccessible) {
                            successCount.incrementAndGet()
                        } else {
                            failureCount.incrementAndGet()
                        }
                        items.add(ResultItem(item.url, item.port, isAccessible))
                        adapter.notifyItemInserted(items.size - 1)
                    }
                }
            }.awaitAll()
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private fun checkUrl(urlString: String, port: Int): Boolean {
        return try {
            val url = URL(urlString)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "HEAD"
            urlConnection.connectTimeout = 5000
            urlConnection.connect()
            urlConnection.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: IOException) {
            false
        }
    }

    private fun checkPort(host: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 5000)
                true
            }
        } catch (e: IOException) {
            false
        }
    }

    val successCountValue: Int
        get() = successCount.get()

    val failureCountValue: Int
        get() = failureCount.get()
}

