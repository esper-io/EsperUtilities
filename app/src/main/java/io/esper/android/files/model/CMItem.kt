package io.esper.android.files.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

// Using data class for CMItem
data class CMItem(
    @SerializedName("count") val count: Int? = null,

    @SerializedName("next") val next: String? = null,

    @SerializedName("previous") val previous: String? = null,

    @SerializedName("results") val results: List<AllContent>? = null  // Changed MutableList to List
)

@Entity(tableName = "AllContent")
data class AllContent(
    @PrimaryKey @SerializedName("id") val id: String,

    @SerializedName("name") val name: String? = null,

    @SerializedName("download_url") val downloadUrl: String? = null,  // Adjusted naming to camelCase

    @SerializedName("size") val size: Long? = null,  // Changed type to Long

    @SerializedName("kind") val kind: String? = null,

    @SerializedName("tags") val tags: List<String>? = null
)
