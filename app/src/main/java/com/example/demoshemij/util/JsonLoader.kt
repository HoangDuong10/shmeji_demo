package com.example.demoshemij.util

import android.content.Context
import android.util.Log
import com.example.demoshemij.domain.CharacterList
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

/**
 * Utility để load và parse JSON data từ URL hoặc local
 */
object JsonLoader {
    
    // ✅ URL của JSON trên server
    const val JSON_URL = "https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/data_test.json"
    
    // ✅ Base URL cho images
    const val BASE_IMAGE_URL = "https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/"
    
    // Cache trong memory
    private var cachedData: CharacterList? = null
    
    /**
     * Load character data với priority:
     * 1. Memory cache
     * 2. URL (với disk cache)
     * 3. Local assets (fallback)
     */
    suspend fun loadCharacterData(context: Context): CharacterList? {
        // 1. Kiểm tra memory cache
        cachedData?.let {
            Log.d("JsonLoader", "Using memory cache")
            return it
        }
        
        // 2. Thử load từ URL
        val dataFromUrl = loadFromUrl(context)
        if (dataFromUrl != null) {
            cachedData = dataFromUrl
            return dataFromUrl
        }
        
        // 3. Fallback về local assets
        Log.w("JsonLoader", "Failed to load from URL, falling back to local assets")
        val dataFromAssets = loadFromAssets(context)
        if (dataFromAssets != null) {
            cachedData = dataFromAssets
            return dataFromAssets
        }
        
        return null
    }
    
    /**
     * Load character data đồng bộ (blocking) - chỉ dùng khi đã có cache
     */
    fun loadCharacterDataSync(context: Context): CharacterList? {
        // Chỉ return cache, không load mới
        return cachedData
    }
    
    /**
     * Load JSON từ URL với disk caching
     */
    private suspend fun loadFromUrl(context: Context): CharacterList? {
        return withContext(Dispatchers.IO) {
            try {
                // Kiểm tra disk cache
                val cacheFile = getJsonCacheFile(context)
                if (cacheFile.exists()) {
                    val cacheAge = System.currentTimeMillis() - cacheFile.lastModified()
                    // Cache valid trong 1 giờ
                    if (cacheAge < 3600000) {
                        Log.d("JsonLoader", "Loading from disk cache (age: ${cacheAge / 1000}s)")
                        val jsonString = cacheFile.readText()
                        return@withContext parseJson(jsonString)
                    }
                }
                
                // Download từ URL
                Log.d("JsonLoader", "Downloading JSON from: $JSON_URL")
                val connection = URL(JSON_URL).openConnection()
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()
                
                val jsonString = connection.getInputStream().bufferedReader().use { it.readText() }
                Log.d("JsonLoader", "JSON downloaded, length: ${jsonString.length}")
                
                // Save to disk cache
                saveToDiskCache(cacheFile, jsonString)
                
                // Parse và return
                parseJson(jsonString)
            } catch (e: Exception) {
                Log.e("JsonLoader", "Failed to load JSON from URL", e)
                null
            }
        }
    }
    
    /**
     * Load JSON từ local assets (fallback)
     */
    private fun loadFromAssets(context: Context): CharacterList? {
        return try {
            Log.d("JsonLoader", "Loading data.json from assets...")
            val jsonString = context.assets.open("data.json").bufferedReader().use { it.readText() }
            Log.d("JsonLoader", "JSON loaded from assets, length: ${jsonString.length}")
            parseJson(jsonString)
        } catch (e: IOException) {
            Log.e("JsonLoader", "Failed to load JSON from assets", e)
            null
        }
    }
    
    /**
     * Parse JSON string thành CharacterList
     */
    private fun parseJson(jsonString: String): CharacterList? {
        return try {
            val gson = Gson()
            val data = gson.fromJson(jsonString, CharacterList::class.java)
            Log.d("JsonLoader", "JSON parsed, characters count: ${data?.characters?.size}")
            data
        } catch (e: Exception) {
            Log.e("JsonLoader", "Failed to parse JSON", e)
            null
        }
    }
    
    /**
     * Lấy file cache cho JSON
     */
    private fun getJsonCacheFile(context: Context): File {
        val cacheDir = File(context.cacheDir, "json")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, "data.json")
    }
    
    /**
     * Lưu JSON vào disk cache
     */
    private fun saveToDiskCache(file: File, jsonString: String) {
        try {
            FileOutputStream(file).use { out ->
                out.write(jsonString.toByteArray())
            }
            Log.d("JsonLoader", "JSON saved to disk cache")
        } catch (e: Exception) {
            Log.e("JsonLoader", "Failed to save JSON to disk cache", e)
        }
    }
    
    /**
     * Force reload từ URL (bỏ qua cache)
     */
    suspend fun forceReload(context: Context): CharacterList? {
        cachedData = null
        val cacheFile = getJsonCacheFile(context)
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
        return loadCharacterData(context)
    }
    
    /**
     * Clear tất cả cache
     */
    fun clearCache(context: Context) {
        cachedData = null
        val cacheFile = getJsonCacheFile(context)
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
        Log.d("JsonLoader", "JSON cache cleared")
    }
    
    /**
     * Kiểm tra xem có cache không
     */
    fun hasCachedData(): Boolean {
        return cachedData != null
    }
}
