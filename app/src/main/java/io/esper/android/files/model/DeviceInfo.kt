package io.esper.android.files.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class DeviceInfo {
    @SerializedName("count")
    @Expose
    var count: String? = null

    @SerializedName("results")
    @Expose
    val results: List<Data>? = null
}

class Data(
        @SerializedName("id")
        @Expose
        val id: String? = null,

        @SerializedName("device_name")
        @Expose
        var device_name: String? = null,

        @SerializedName("alias_name")
        @Expose
        val alias_name: String? = null
)