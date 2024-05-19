@file:Suppress("unused")

package io.esper.appstore.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


class ApplicationsInfo(
        @SerializedName("count")
        @Expose
        val count: Int? = null,

        @SerializedName("next")
        @Expose
        val next: String? = null,

        @SerializedName("previous")
        @Expose
        val previous: String? = null,

        @SerializedName("results")
        @Expose
        val results: MutableList<AllApps>? = null
)

class ApplicationsInfo1(
        @SerializedName("count")
        @Expose
        val count: Int? = null,

        @SerializedName("next")
        @Expose
        val next: String? = null,

        @SerializedName("previous")
        @Expose
        val previous: String? = null,

        @SerializedName("results")
        @Expose
        val results: MutableList<AppData>? = null
)

class ApplicationsInfo2(
        @SerializedName("count")
        @Expose
        val count: Int? = null,

        @SerializedName("next")
        @Expose
        val next: String? = null,

        @SerializedName("previous")
        @Expose
        val previous: String? = null,

        @SerializedName("results")
        @Expose
        val results: MutableList<AppData1>? = null
)

@Entity(tableName = "AllApps", primaryKeys = ["id"])
class AllApps(

        @ColumnInfo(name = "application_name")
        @SerializedName("application_name")
        @Expose
        var application_name: String? = null,

        @ColumnInfo(name = "package_name")
        @SerializedName("package_name")
        @Expose
        var package_name: String? = null,

        @ColumnInfo(name = "developer")
        @SerializedName("developer")
        @Expose
        var developer: String? = null,

        @ColumnInfo(name = "category")
        @SerializedName("category")
        @Expose
        var category: String? = null,

        @ColumnInfo(name = "content_rating")
        @SerializedName("content_rating")
        @Expose
        var content_rating: String? = null,

        @ColumnInfo(name = "compatibility")
        @SerializedName("compatibility")
        @Expose
        var compatibility: String? = null,

        @ColumnInfo(name = "created_on")
        @SerializedName("created_on")
        @Expose
        var created_on: String? = null,

        @ColumnInfo(name = "updated_on")
        @SerializedName("updated_on")
        @Expose
        var updated_on: String? = null,

        @ColumnInfo(name = "is_active")
        @SerializedName("is_active")
        @Expose
        var is_active: String? = null,

        @ColumnInfo(name = "is_hidden")
        @SerializedName("is_hidden")
        @Expose
        var is_hidden: String? = null,

        @ColumnInfo(name = "enterprise")
        @SerializedName("enterprise")
        @Expose
        var enterprise: String? = null,

        @SerializedName("versions")
        @Expose
        @ColumnInfo(name = "versions")
        var versions: MutableList<AppData>? = null,

        @SerializedName("id")
        @Expose
        var id: String
)

@Entity(tableName = "AppData", primaryKeys = ["id"])
class AppData(
        @ColumnInfo(name = "id")
        @SerializedName("id")
        @Expose
        val id: String,

        @SerializedName("app_file")
        @Expose
        val app_file: String? = null,

        @SerializedName("size_in_mb")
        @Expose
        val size_in_mb: String? = null,

        @SerializedName("version_code")
        @Expose
        var version_code: String? = null,

        @SerializedName("build_number")
        @Expose
        val build_number: String? = null,

        @SerializedName("hash_string")
        @Expose
        val hash_string: String? = null,

        @SerializedName("min_sdk_version")
        @Expose
        val min_sdk_version: String? = null,

        @SerializedName("target_sdk_version")
        @Expose
        val target_sdk_version: String? = null,

        @SerializedName("download_url")
        @Expose
        val download_url: String? = null,

        @SerializedName("icon_url")
        @Expose
        val icon_url: String? = null,

        @SerializedName("release_name")
        @Expose
        val release_name: String? = null
)

@Entity(tableName = "AppData1", primaryKeys = ["id"])
class AppData1(
        @ColumnInfo(name = "id")
        @SerializedName("id")
        @Expose
        val id: String,

        @ColumnInfo(name = "app_name")
        @SerializedName("app_name")
        @Expose
        val app_name: String? = null,

        @ColumnInfo(name = "app_type")
        @SerializedName("app_type")
        @Expose
        val app_type: String? = null,

        @ColumnInfo(name = "package_name")
        @SerializedName("package_name")
        @Expose
        var package_name: String? = null,

        @ColumnInfo(name = "state")
        @SerializedName("state")
        @Expose
        val state: String? = null,

        @ColumnInfo(name = "version_code")
        @SerializedName("version_code")
        @Expose
        val version_code: String? = null,

        @ColumnInfo(name = "version_name")
        @SerializedName("version_name")
        @Expose
        val version_name: String? = null,

        @ColumnInfo(name = "whitelisted")
        @SerializedName("whitelisted")
        @Expose
        val whitelisted: String? = null,
)

class InstalledApps(
        @SerializedName("id")
        @Expose
        var id: String,

        @SerializedName("package_name")
        @Expose
        var package_name: String? = null,

        @SerializedName("version_name")
        @Expose
        var version_name: String? = null
)