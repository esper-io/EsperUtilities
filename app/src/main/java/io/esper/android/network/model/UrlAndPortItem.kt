package io.esper.android.network.model

data class UrlAndPortItem(
    val url: String,
    val port: Int,
    var isAccessible: Boolean
)
