package com.example.demoshemij.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

/**
 * Helper để load ảnh từ URL hoặc local drawable
 */
object ImageLoader {
    
    private val imageCache = mutableMapOf<String, ImageBitmap>()
    
    /**
     * Load ảnh từ URL với caching
     */
    suspend fun loadImageFromUrl(context: Context, url: String): ImageBitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Kiểm tra cache trong memory
                imageCache[url]?.let { return@withContext it }
                
                // Kiểm tra cache trong disk
                val cacheFile = getCacheFile(context, url)
                if (cacheFile.exists()) {
                    Log.d("ImageLoader", "Loading from disk cache: $url")
                    val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                    val imageBitmap = bitmap.asImageBitmap()
                    imageCache[url] = imageBitmap
                    return@withContext imageBitmap
                }
                
                // Download từ URL
                Log.d("ImageLoader", "Downloading image: $url")
                val connection = URL(url).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()
                
                val inputStream = connection.getInputStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (bitmap != null) {
                    // Save to disk cache
                    saveToDiskCache(cacheFile, bitmap)
                    
                    // Save to memory cache
                    val imageBitmap = bitmap.asImageBitmap()
                    imageCache[url] = imageBitmap
                    
                    Log.d("ImageLoader", "Image loaded successfully: $url")
                    imageBitmap
                } else {
                    Log.e("ImageLoader", "Failed to decode bitmap: $url")
                    null
                }
            } catch (e: Exception) {
                Log.e("ImageLoader", "Failed to load image from URL: $url", e)
                null
            }
        }
    }
    
    /**
     * Load ảnh từ drawable resource
     */
    fun loadImageFromDrawable(context: Context, resourceName: String): ImageBitmap? {
        return try {
            val resId = context.resources.getIdentifier(
                resourceName,
                "drawable",
                context.packageName
            )
            
            if (resId != 0) {
                val bitmap = BitmapFactory.decodeResource(context.resources, resId)
                bitmap?.asImageBitmap()
            } else {
                Log.e("ImageLoader", "Drawable not found: $resourceName")
                null
            }
        } catch (e: Exception) {
            Log.e("ImageLoader", "Failed to load drawable: $resourceName", e)
            null
        }
    }
    
    /**
     * Xác định xem URL là link hay tên drawable
     */
    fun isUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }
    
    /**
     * Load ảnh tự động (URL hoặc drawable)
     */
    suspend fun loadImage(context: Context, source: String): ImageBitmap? {
        return if (isUrl(source)) {
            loadImageFromUrl(context, source)
        } else {
            loadImageFromDrawable(context, source)
        }
    }
    
    /**
     * Lấy file cache cho URL
     */
    private fun getCacheFile(context: Context, url: String): File {
        val cacheDir = File(context.cacheDir, "images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        val fileName = url.toMD5()
        return File(cacheDir, fileName)
    }
    
    /**
     * Lưu bitmap vào disk cache
     */
    private fun saveToDiskCache(file: File, bitmap: Bitmap) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            Log.e("ImageLoader", "Failed to save to disk cache", e)
        }
    }
    
    /**
     * Convert string sang MD5 hash để làm tên file
     */
    private fun String.toMD5(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Lấy ảnh từ memory cache (sync, instant)
     * Return null nếu chưa có trong memory
     */
    fun getFromMemoryCache(url: String): ImageBitmap? {
        return imageCache[url]
    }
    
    /**
     * Check xem ảnh có trong cache không (memory hoặc disk)
     */
    fun isImageCached(context: Context, url: String): Boolean {
        // Check memory cache
        if (imageCache.containsKey(url)) return true
        
        // Check disk cache
        val cacheFile = getCacheFile(context, url)
        return cacheFile.exists()
    }
    
    /**
     * Clear cache
     */
    fun clearCache(context: Context) {
        imageCache.clear()
        val cacheDir = File(context.cacheDir, "images")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
        Log.d("ImageLoader", "Cache cleared")
    }
}

/**
 * Composable để load ảnh với state management
 */
@Composable
fun rememberImageBitmap(source: String): ImageBitmap? {
    val context = LocalContext.current
    var imageBitmap by remember(source) { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(source) {
        imageBitmap = ImageLoader.loadImage(context, source)
    }
    
    return imageBitmap
}
