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

class NetworkCheckTask(
    private val adapter: NetworkResultAdapter, private val items: MutableList<ResultItem>
) {
    var job: Job? = null
    var successCount = 0
    var failureCount = 0

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
                    if (isAccessible) {
                        successCount++
                    } else {
                        failureCount++
                    }
                    ResultItem(item.url, item.port, isAccessible).also {
                        withContext(Dispatchers.Main) {
                            items.add(it)
                            adapter.notifyItemInserted(items.size - 1)
                        }
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

    private val itemsToCheck = listOf(
        ResultItem("*.amazonaws.com", 443, false),
        ResultItem("mqtt.shoonyacloud.com", 1883, false),
        ResultItem("services.shoonyacloud.com", 443, false),
        ResultItem("turn.shoonyacloud.com", 3478, false),
        ResultItem("turn.shoonyacloud.com", 5349, false),
        ResultItem("authn2.esper.cloud", 443, false),
        ResultItem("id.esper.cloud", 443, false),
        ResultItem("ping.esper.cloud", 443, false),
        ResultItem("*.esper.cloud", 443, false),
        ResultItem("*.esper.io", 443, false),
        ResultItem("shoonya-firebase.firebaseio.com", 443, false),
        ResultItem("*.crashlytics.com", 443, false),
        ResultItem("crashlyticsreports-pa.googleapis.com", 443, false),
        ResultItem("firebasecrashlyticssymbols.googleapis.com", 443, false),
        ResultItem("*.firebaseio.com", 443, false),
        ResultItem("*.googleapis.com", 443, false),
        ResultItem("*.googleapis.com", 5228, false),
        ResultItem("*.googleapis.com", 5229, false),
        ResultItem("*.googleapis.com", 5230, false),
        ResultItem("8.8.8.8", 443, false),
        ResultItem("mqtt-telemetry-prod.esper.cloud", 1883, false),
        ResultItem("dpcdownloads.esper.cloud", 443, false),
        ResultItem("firebaseinstallations.googleapis.com", 443, false),
        ResultItem("fcm.googleapis.com", 443, false),
        ResultItem("time.google.com", 123, false),
        ResultItem("ip-api.com", 80, false),
        ResultItem("eea-services.esper.cloud", 443, false),
        ResultItem("ota.esper.io", 443, false)
    ).flatMap { expandWildcard(it) }

    private fun expandWildcard(item: ResultItem): List<ResultItem> {
        val expandedItems = mutableListOf<ResultItem>()
        if (item.url.contains("*")) {
            val baseUrl = item.url.replace("*", "")
            val subdomains = when (baseUrl) {
                ".amazonaws.com" -> listOf("example")
                ".shoonyacloud.com" -> listOf("services", "mqtt", "turn")
                ".esper.cloud" -> listOf("downloads", "mqtt-telemetry-prod", "dpcdownloads", "eea-services", "authn2", "id", "ping", "mqtt", "statserv", "onboarding", "eea-sentry")
                ".esper.io" -> listOf("ota", "downloads")
                else -> listOf("www")
            }
            for (subdomain in subdomains) {
                expandedItems.add(
                    ResultItem("https://$subdomain$baseUrl", item.port, item.isAccessible)
                )
            }
        } else {
            expandedItems.add(item)
        }
        return expandedItems
    }
}
