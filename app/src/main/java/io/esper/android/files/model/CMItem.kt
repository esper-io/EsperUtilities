@file:Suppress("unused")

package io.esper.android.files.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


class CMItem(
    @SerializedName("count") @Expose val count: Int? = null,

    @SerializedName("next") @Expose val next: String? = null,

    @SerializedName("previous") @Expose val previous: String? = null,

    @SerializedName("results") @Expose val results: MutableList<AllContent>? = null
)

@Entity(tableName = "AllContent", primaryKeys = ["id"])
class AllContent(

    @ColumnInfo(name = "name") @SerializedName("name") @Expose var name: String? = null,

    @ColumnInfo(name = "download_url") @SerializedName("download_url") @Expose var download_url: String? = null,

    @ColumnInfo(name = "size") @SerializedName("size") @Expose var size: String? = null,

    @ColumnInfo(name = "kind") @SerializedName("kind") @Expose var kind: String? = null,
//        @SerializedName("tags")
//        @Expose
//        @ColumnInfo(name = "tags")
//        var tags: MutableList<AppData>? = null,

    @SerializedName("id") @Expose var id: String
)