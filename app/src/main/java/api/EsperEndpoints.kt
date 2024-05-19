package api

import io.esper.android.files.model.CMItem
import io.esper.android.files.model.DeviceInfo
import io.esper.appstore.model.ApplicationsInfo
import io.esper.appstore.model.ApplicationsInfo1
import io.esper.appstore.model.ApplicationsInfo2
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface EsperEndpoints {

    @GET("content")
    fun getAllContent(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Call<CMItem>

    @GET("application")
    fun getAllApplications(
        @Header("Authorization") token: String,
        @Query("is_hidden") is_hidden: Boolean,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Call<ApplicationsInfo>

    @GET("version")
    fun getAllVersions(@Header("Authorization") token: String): Call<ApplicationsInfo1>

    @GET("app")
    fun getInstalledAppsList(
        @Header("Authorization") token: String,
        @Query("app_type") app_type: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Call<ApplicationsInfo2>
}