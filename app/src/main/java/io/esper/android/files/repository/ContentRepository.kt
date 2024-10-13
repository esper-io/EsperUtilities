package io.esper.android.files.repository

import android.app.Application
import androidx.lifecycle.LiveData
import api.EsperEndpoints
import io.esper.android.files.data.ContentDao
import io.esper.android.files.model.AllContent
import io.esper.android.files.util.GeneralUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ContentRepository(
    private val application: Application, private val contentDao: ContentDao
) {

    val allContent: LiveData<List<AllContent>> = contentDao.getAllContentLive()

    // Fetch content from the network and update the database
    suspend fun fetchContentFromNetwork() {
        withContext(Dispatchers.IO) {
            try {
                var offset = 0
                var hasMore = true

                while (hasMore) {
                    val results = fetchContentPage(offset)
                    results?.let {
                        contentDao.insertAll(it)
                        offset += it.size
                        hasMore = it.isNotEmpty() && it.size == 50
                    } ?: run {
                        hasMore = false
                    }
                }
            } catch (e: Exception) {
                // Handle exceptions (e.g., log error, show message)
            }
        }
    }

    private suspend fun fetchContentPage(offset: Int): List<AllContent>? {
        return withContext(Dispatchers.IO) {
            try {
                val tenant = GeneralUtils.getTenant(application)
                val enterprise = GeneralUtils.getEnterpriseId(application)
                val token = GeneralUtils.getApiKey(application)
                val getUrl = "$tenant/api/v0/enterprise/$enterprise/"

                val okHttpClient = OkHttpClient.Builder().connectTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(30, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).build()

                val retrofit = Retrofit.Builder().baseUrl(getUrl)
                    .addConverterFactory(GsonConverterFactory.create()).client(okHttpClient).build()

                val request = retrofit.create(EsperEndpoints::class.java)

                val response = request.getAllContent(
                    token = "Bearer $token", limit = 50, offset = offset
                )

                response.results
            } catch (e: Exception) {
                null
            }
        }
    }
}
