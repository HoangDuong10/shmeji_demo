package com.example.demoshemij.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * In-memory + Disk Bitmap cache để tránh nhấp nháy
 * - Memory cache: Nhanh nhất, dùng khi app đang chạy
 * - Disk cache: Lưu vĩnh viễn, không cần tải lại từ internet
 */
object BitmapCache {
    
    private val cache = mutableMapOf<String, ImageBitmap>()
    private var isPreloaded = false
    private lateinit var cacheDir: File
    
    // ✅ Progress tracking
    var currentProgress = 0f
        private set
    var currentTask = ""
        private set
    
    /**
     * Khởi tạo cache directory
     */
    fun init(context: Context) {
        cacheDir = File(context.filesDir, "image_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
            Log.d("BitmapCache", "Created cache directory: ${cacheDir.absolutePath}")
        }
    }
    
    /**
     * Tạo tên file từ URL (hash MD5)
     */
    private fun getCacheFileName(url: String): String {
        val md5 = MessageDigest.getInstance("MD5")
        val hash = md5.digest(url.toByteArray())
        return hash.joinToString("") { "%02x".format(it) } + ".png"
    }
    
    /**
     * Lấy file path trong cache
     */
    private fun getCacheFile(url: String): File {
        return File(cacheDir, getCacheFileName(url))
    }
    
    /**
     * Preload tất cả ảnh vào memory + disk
     */
    suspend fun preloadAllImages(context: Context) {
        if (!::cacheDir.isInitialized) {
            init(context)
        }
        
        withContext(Dispatchers.IO) {
            try {
                Log.d("BitmapCache", "Starting preload...")
                
                // 1. Load JSON
                val characterData = JsonLoader.loadCharacterData(context)
                if (characterData == null) {
                    Log.e("BitmapCache", "Failed to load JSON")
                    return@withContext
                }
                
                // 2. Collect tất cả URLs
                val allImageUrls = mutableListOf<String>()
                
                characterData.characters.forEach { characterMap ->
                    characterMap.forEach { (characterId, character) ->
                        val folder = character.folder
                        
                        // Thêm tất cả frames
                        character.animations.forEach { (animName, animData) ->
                            animData.frames.forEach { frame ->
                                val url = "${JsonLoader.BASE_IMAGE_URL}$folder/${frame.url}"
                                allImageUrls.add(url)
                            }
                        }
                    }
                }
                
                Log.d("BitmapCache", "Total images to preload: ${allImageUrls.size}")
                
                // 3. Load từng ảnh vào memory + disk
                allImageUrls.forEachIndexed { index, url ->
                    currentTask = "Loading ${index + 1}/${allImageUrls.size}"
                    
                    try {
                        val bitmap = loadBitmapWithCache(url)
                        if (bitmap != null) {
                            cache[url] = bitmap.asImageBitmap()
                            Log.d("BitmapCache", "Loaded ${index + 1}/${allImageUrls.size}: $url")
                        }
                    } catch (e: Exception) {
                        Log.e("BitmapCache", "Failed to load: $url", e)
                    }
                    
                    // Update progress
                    currentProgress = (index + 1).toFloat() / allImageUrls.size
                }
                
                isPreloaded = true
                Log.d("BitmapCache", "Preload completed! ${cache.size} images in memory, ${countDiskCache()} on disk")
                
            } catch (e: Exception) {
                Log.e("BitmapCache", "Preload failed", e)
            }
        }
    }
    
    /**
     * Load bitmap với disk cache
     * 1. Kiểm tra disk cache trước
     * 2. Nếu không có → tải từ internet và lưu xuống disk
     */
    private fun loadBitmapWithCache(url: String): Bitmap? {
        val cacheFile = getCacheFile(url)
        
        // ✅ Kiểm tra disk cache trước
        if (cacheFile.exists()) {
            Log.d("BitmapCache", "Loading from disk cache: ${cacheFile.name}")
            return BitmapFactory.decodeFile(cacheFile.absolutePath)
        }
        
        // ✅ Không có trong disk → tải từ internet
        Log.d("BitmapCache", "Downloading from internet: $url")
        val bitmap = loadBitmapFromUrl(url)
        
        // ✅ Lưu xuống disk để lần sau không phải tải lại
        if (bitmap != null) {
            saveBitmapToDisk(bitmap, cacheFile)
        }
        
        return bitmap
    }
    
    /**
     * Load bitmap từ URL
     */
    private fun loadBitmapFromUrl(urlString: String): Bitmap? {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            
            inputStream = connection.inputStream
            BitmapFactory.decodeStream(inputStream)
            
        } catch (e: Exception) {
            Log.e("BitmapCache", "Failed to load bitmap from $urlString", e)
            null
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
    }
    
    /**
     * Lưu bitmap xuống disk
     */
    private fun saveBitmapToDisk(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                Log.d("BitmapCache", "Saved to disk: ${file.name}")
            }
        } catch (e: Exception) {
            Log.e("BitmapCache", "Failed to save bitmap to disk", e)
        }
    }
    
    /**
     * Đếm số file trong disk cache
     */
    private fun countDiskCache(): Int {
        return if (::cacheDir.isInitialized && cacheDir.exists()) {
            cacheDir.listFiles()?.size ?: 0
        } else {
            0
        }
    }
    
    /**
     * Lấy bitmap từ cache
     */
    fun get(url: String): ImageBitmap? {
        return cache[url]
    }
    
    /**
     * Kiểm tra xem đã preload chưa
     */
    fun isPreloaded(): Boolean = isPreloaded
    
    /**
     * Clear memory cache
     */
    fun clear() {
        cache.clear()
        isPreloaded = false
    }
    
    /**
     * Clear cả memory và disk cache
     */
    fun clearAll(context: Context) {
        cache.clear()
        isPreloaded = false
        
        if (::cacheDir.isInitialized && cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { it.delete() }
            Log.d("BitmapCache", "Cleared disk cache")
        }
    }
    
    /**
     * Get cache size
     */
    fun size(): Int = cache.size
}
