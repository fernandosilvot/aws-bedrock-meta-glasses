package com.meta.wearable.dat.externalsampleapps.cameraaccess.nova

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SearchResult(
    val query: String,
    val answer: String?,
    val results: List<SearchItem>
)

data class SearchItem(
    val title: String,
    val url: String,
    val content: String
)

class TavilySearchService(private val apiKey: String) {
    companion object {
        private const val TAG = "TavilySearch"
        private const val API_URL = "https://api.tavily.com/search"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    suspend fun search(query: String, maxResults: Int = 3): SearchResult? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching Tavily for: $query")
            
            val requestBody = JSONObject().apply {
                put("api_key", apiKey)
                put("query", query)
                put("max_results", maxResults)
                put("search_depth", "basic")
                put("include_answer", true)
                put("include_images", false)
            }
            
            val request = Request.Builder()
                .url(API_URL)
                .header("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "API error: ${response.code}")
                return@withContext null
            }
            
            val body = response.body?.string()
            if (body == null) {
                Log.e(TAG, "Empty response")
                return@withContext null
            }
            
            Log.d(TAG, "Response received: ${body.take(200)}...")
            parseResponse(body, query)
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            null
        }
    }
    
    private fun parseResponse(json: String, query: String): SearchResult {
        val jsonObj = JSONObject(json)
        
        val resultsArray = jsonObj.optJSONArray("results")
        val results = if (resultsArray != null) {
            (0 until resultsArray.length()).map { i ->
                val item = resultsArray.getJSONObject(i)
                SearchItem(
                    title = item.optString("title", ""),
                    url = item.optString("url", ""),
                    content = item.optString("content", "")
                )
            }
        } else emptyList()
        
        return SearchResult(
            query = query,
            answer = jsonObj.optString("answer", null),
            results = results
        )
    }
    
    fun shouldSearch(transcript: String): Boolean {
        val searchKeywords = listOf(
            "busca", "search", "investiga", "qué es", "what is",
            "quién es", "who is", "información sobre", "information about",
            "dime sobre", "tell me about"
        )
        return searchKeywords.any { transcript.lowercase().contains(it) }
    }
}
