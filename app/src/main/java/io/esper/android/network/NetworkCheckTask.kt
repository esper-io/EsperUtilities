package io.esper.android.network

import android.util.Log
import io.esper.android.network.model.UrlAndPortItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NetworkCheckTask(
    private val adapter: NetworkResultAdapter,
    private val items: MutableList<UrlAndPortItem>,
    private val itemsToCheck: List<UrlAndPortItem>
) {
    private val TAG = "NetworkCheckTask"

    var job: Job? = null
    private val successCount = AtomicInteger(0)
    private val failureCount = AtomicInteger(0)

    fun start() {
        job = CoroutineScope(Dispatchers.IO).launch {
            itemsToCheck.map { item ->
                async {
                    val isAccessible = if (item.url.startsWith("http")) {
                        try {
                            checkUrl(item.url)
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
                        items.add(UrlAndPortItem(item.url, item.port, isAccessible))
                        adapter.notifyItemInserted(items.size - 1)
                    }
                }
            }.awaitAll()
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private fun checkUrl(urlString: String): Boolean {
        val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).build()

        return try {
            val request = Request.Builder().url(urlString).head().build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful // True if code in [200..300)
            }
        } catch (e: IOException) {
            Log.e(TAG, "checkUrl: Error checking URL: $urlString", e)
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

