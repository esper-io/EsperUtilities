package api

import io.esper.android.files.model.CMItem
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
}