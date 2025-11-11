package com.example.demoshemij.util

import android.content.Context
import android.util.Log
import com.example.demoshemij.domain.OverflowConfig

/**
 * Helper để quản lý overflow (độ tràn) của sprite ra ngoài màn hình
 * Overflow được định nghĩa theo tỷ lệ % của kích thước sprite
 */
object OverflowHelper {
    
    /**
     * Lấy overflow config từ JSON cho character cụ thể
     * ✅ Dùng sync version vì được gọi từ non-suspend context
     */
     fun getOverflowConfig(context: Context, characterName: String): OverflowConfig {
        return try {
            // ✅ Dùng sync version - chỉ lấy từ cache
            val characterData = JsonLoader.loadCharacterDataSync(context)
            val character = characterData?.characters?.firstOrNull()?.get(characterName)
            
            // Lấy overflow từ JSON nếu có
            val overflowConfig = character?.overflow
            
            if (overflowConfig != null) {
                Log.d("OverflowHelper", "Character '$characterName' overflow: H=${overflowConfig.horizontal}, V=${overflowConfig.vertical}")
                overflowConfig
            } else {
                Log.d("OverflowHelper", "Character '$characterName' using default overflow: 0.333")
                OverflowConfig()  // Mặc định 1/3
            }
        } catch (e: Exception) {
            Log.e("OverflowHelper", "Failed to load overflow config", e)
            OverflowConfig()  // Fallback về mặc định
        }
    }
    
    /**
     * Lấy overflow config đồng bộ (dùng cache)
     * Chỉ dùng khi đã load JSON trước đó
     */
//    fun getOverflowConfigSync(context: Context, characterName: String): OverflowConfig {
//        return if (JsonLoader.hasCachedData()) {
//            kotlinx.coroutines.runBlocking {
//                getOverflowConfig(context, characterName)
//            }
//        } else {
//            Log.w("OverflowHelper", "No cached data, using default overflow")
//            OverflowConfig()
//        }
//    }
    
    /**
     * Tính toán giới hạn X dựa trên overflow config
     * @param spriteWidth Chiều rộng sprite
     * @param screenWidth Chiều rộng màn hình
     * @param config Overflow config
     * @return Pair(minX, maxX)
     */
    fun calculateXBounds(
        spriteWidth: Int,
        screenWidth: Int,
        config: OverflowConfig
    ): Pair<Int, Int> {
        val overflowPixels = (spriteWidth * config.horizontal).toInt()
        val minX = -overflowPixels
        val maxX = screenWidth - spriteWidth + overflowPixels
        return Pair(minX, maxX)
    }
    
    /**
     * Tính toán giới hạn Y dựa trên overflow config
     * @param spriteHeight Chiều cao sprite
     * @param screenHeight Chiều cao màn hình
     * @param config Overflow config
     * @return Pair(minY, maxY)
     */
    fun calculateYBounds(
        spriteHeight: Int,
        screenHeight: Int,
        config: OverflowConfig
    ): Pair<Int, Int> {
        val overflowPixels = (spriteHeight * config.vertical).toInt()
        val minY = -overflowPixels
        val maxY = screenHeight - spriteHeight
        return Pair(minY, maxY)
    }
    
    /**
     * Kiểm tra sprite có ở biên trái không
     */
    fun isAtLeft(x: Int, spriteWidth: Int, config: OverflowConfig): Boolean {
        val overflowPixels = (spriteWidth * config.horizontal).toInt()
        return x <= -overflowPixels + 10  // +10 để có margin nhỏ
    }
    
    /**
     * Kiểm tra sprite có ở biên phải không
     */
    fun isAtRight(x: Int, spriteWidth: Int, screenWidth: Int, config: OverflowConfig): Boolean {
        val overflowPixels = (spriteWidth * config.horizontal).toInt()
        return x >= screenWidth - spriteWidth + overflowPixels - 10  // -10 để có margin nhỏ
    }
    
    /**
     * Kiểm tra sprite có ở biên trên không
     */
    fun isAtTop(y: Int, spriteHeight: Int, config: OverflowConfig): Boolean {
        val overflowPixels = (spriteHeight * config.vertical).toInt()
        return y <= -overflowPixels + 10
    }
    
    /**
     * Kiểm tra sprite có ở biên dưới không
     */
    fun isAtBottom(y: Int, spriteHeight: Int, screenHeight: Int): Boolean {
        return y >= screenHeight - spriteHeight - 10
    }
}
