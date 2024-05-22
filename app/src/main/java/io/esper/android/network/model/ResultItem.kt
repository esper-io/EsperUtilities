package io.esper.android.network.model

data class ResultItem(
    val url: String,
    val port: Int,
    var isAccessible: Boolean
)
