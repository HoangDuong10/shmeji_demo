package com.example.demoshemij.util

import android.content.Context
import android.util.Log
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Manager để preload tất cả ảnh từ JSON khi app khởi động
 */
object PreloadManager {
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private val _currentTask = MutableStateFlow("")
    val currentTask: StateFlow<String> = _currentTask.asStateFlow()
    
    private var isPreloaded = false
    
    /**
     * Preload tất cả ảnh từ JSON
     */
//    suspend fun preloadAllImages(context: Context) {
//        if (isPreloaded) {
//            Log.d("PreloadManager", "Already preloaded, skipping...")
//            return
//        }
//
//        withContext(Dispatchers.IO) {
//            try {
//                _isLoading.value = true
//                _progress.value = 0f
//                _currentTask.value = "Loading JSON..."
//
//                // 1. Load JSON
//                val characterData = JsonLoader.loadCharacterData(context)
//                if (characterData == null) {
//                    Log.e("PreloadManager", "Failed to load JSON")
//                    _isLoading.value = false
//                    return@withContext
//                }
//
//                _progress.value = 0.1f
//
//                // 2. Collect tất cả URLs cần load
//                val allImageUrls = mutableListOf<String>()
//
//                characterData.characters.forEach { characterMap ->
//                    characterMap.forEach { (characterId, character) ->
//                        val folder = character.folder
//
//                        // Thêm thumbnail
//                        allImageUrls.add("${JsonLoader.BASE_IMAGE_URL}$folder/${character.thumbnail}")
//
//                        // Thêm tất cả frames từ animations
//                        character.animations.forEach { (animName, animData) ->
//                            // Thêm thumb
//                            allImageUrls.add("${JsonLoader.BASE_IMAGE_URL}$folder/${animData.thumb}")
//
//                            // Thêm frames
//                            animData.frames.forEach { frame ->
//                                allImageUrls.add("${JsonLoader.BASE_IMAGE_URL}$folder/${frame.url}")
//                            }
//                        }
//                    }
//                }
//
//                Log.d("PreloadManager", "Total images to preload: ${allImageUrls.size}")
//                _progress.value = 0.2f
//
//                // 3. Preload từng ảnh với Coil
//                allImageUrls.forEachIndexed { index, url ->
//                    _currentTask.value = "Loading image ${index + 1}/${allImageUrls.size}"
//
//                    try {
//                        // ✅ Dùng Coil để preload
//                        val request = ImageRequest.Builder(context)
//                            .data(url)
//                            .memoryCacheKey(url)
//                            .diskCacheKey(url)
//                            .build()
//
//                        context.imageLoader.execute(request)
//                        Log.d("PreloadManager", "Preloaded: $url")
//                    } catch (e: Exception) {
//                        Log.e("PreloadManager", "Failed to preload: $url", e)
//                    }
//
//                    // Update progress (0.2 -> 1.0)
//                    _progress.value = 0.2f + (0.8f * (index + 1) / allImageUrls.size)
//                }
//
//                _progress.value = 1f
//                _currentTask.value = "Complete!"
//                isPreloaded = true
//
//                Log.d("PreloadManager", "Preload completed successfully")
//
//            } catch (e: Exception) {
//                Log.e("PreloadManager", "Preload failed", e)
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }

    suspend fun preloadAllImages(context: Context) {
        if (isPreloaded) {
            Log.d("PreloadManager", "Already preloaded, skipping...")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _progress.value = 0f
                _currentTask.value = "Loading JSON..."

                // 1. Load JSON
                val characterData = JsonLoader.loadCharacterData(context)
                if (characterData == null) {
                    Log.e("PreloadManager", "Failed to load JSON")
                    _isLoading.value = false
                    return@withContext
                }

                _progress.value = 0.1f

                // 2. Collect tất cả URLs cần load
                val allImageUrls = mutableListOf<String>()

                characterData.characters.forEach { characterMap ->
                    characterMap.forEach { (characterId, character) ->
                        val folder = character.folder

                        // Thêm thumbnail
                        allImageUrls.add("${JsonLoader.BASE_IMAGE_URL}$folder/${character.thumbnail}")

                        // Thêm tất cả frames từ animations
                        character.animations.forEach { (animName, animData) ->
                            // Thêm thumb
                            allImageUrls.add("${JsonLoader.BASE_IMAGE_URL}$folder/${animData.thumb}")

                            // Thêm frames
                            animData.frames.forEach { frame ->
                                allImageUrls.add("${JsonLoader.BASE_IMAGE_URL}$folder/${frame.url}")
                            }
                        }
                    }
                }

                Log.d("PreloadManager", "Total images to preload: ${allImageUrls.size}")
                _progress.value = 0.2f

                // ✅ Lấy cache của Coil
                val imageLoader = context.imageLoader
                val memoryCache = imageLoader.memoryCache
                val diskCache = imageLoader.diskCache

                // 3. Preload từng ảnh với Coil
                allImageUrls.forEachIndexed { index, url ->
                    _currentTask.value = "Loading image ${index + 1}/${allImageUrls.size}"

                    // ✅ Check xem đã có trong cache chưa
                    val isInMemoryCache = memoryCache?.get(MemoryCache.Key(url)) != null
                    val isInDiskCache = diskCache?.get(url) != null
                    Log.d("PreloadManager", "isInMemoryCache : ${isInMemoryCache} +${isInDiskCache}")
                    if (isInMemoryCache) {
                        Log.d("PreloadManager", "In memory, skip: $url")
                    } else {
                        try {
                            // Dù có trong disk cache vẫn load để đưa lên memory
                            val request = ImageRequest.Builder(context)
                                .data(url)
                                .memoryCacheKey(url)
//                                .diskCacheKey(url)
                                .memoryCachePolicy(CachePolicy.ENABLED)
//                                .diskCachePolicy(CachePolicy.READ_ONLY) // chỉ đọc disk cache, không tải lại mạng
                                .build()

                            imageLoader.execute(request)
                            Log.d("PreloadManager", "Loaded to memory: $url")
                        } catch (e: Exception) {
                            Log.e("PreloadManager", "Failed to load: $url", e)
                        }
                    }
                    // Update progress (0.2 -> 1.0)
                    _progress.value = 0.2f + (0.8f * (index + 1) / allImageUrls.size)
                }

                _progress.value = 1f
                _currentTask.value = "Complete!"
                isPreloaded = true

                Log.d("PreloadManager", "Preload completed successfully")

            } catch (e: Exception) {
                Log.e("PreloadManager", "Preload failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Reset preload state (để force reload)
     */
    fun reset() {
        isPreloaded = false
        _progress.value = 0f
        _currentTask.value = ""
    }
    
    /**
     * Kiểm tra xem đã preload chưa
     */
    fun isPreloaded(): Boolean = isPreloaded
}
