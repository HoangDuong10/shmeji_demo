package com.example.demoshemij.domain

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.demoshemij.util.JsonLoader

/**
 * Ví dụ sử dụng AnimationController với data từ JSON
 * 
 * Cách sử dụng:
 * 
 * 1. Load data từ JSON:
 *    val characterData = JsonLoader.loadCharacterData(context)
 *    val gokuData = characterData?.characters?.firstOrNull()?.get("goku")
 *    val idleAnimation = gokuData?.animations?.get("idle")
 * 
 * 2. Tạo AnimationController:
 *    val controller = remember { AnimationController(idleAnimation!!) }
 * 
 * 3. Bắt đầu animation:
 *    LaunchedEffect(Unit) {
 *        controller.start()
 *    }
 * 
 * 4. Lấy frame hiện tại:
 *    val currentFrameId by controller.currentFrameId.collectAsState()
 *    val frameUrl = controller.getCurrentFrameUrl()
 * 
 * 5. Cleanup khi không dùng:
 *    DisposableEffect(Unit) {
 *        onDispose { controller.cleanup() }
 *    }
 */

@Composable
fun rememberAnimationController(
    context: Context,
    characterName: String,
    animationName: String
): AnimationController? {
    return remember(characterName, animationName) {
        val characterData = JsonLoader.loadCharacterData(context)
        val character = characterData?.characters?.firstOrNull()?.get(characterName)
        val animationData = character?.animations?.get(animationName)
        
        animationData?.let { AnimationController(it) }
    }
}

/**
 * Ví dụ cụ thể cho các animation:
 * 
 * IDLE Animation:
 * - Frame 1 -> 2 với delay 333ms, lặp vô hạn
 * 
 * CUSTOM Animation (phức tạp):
 * - Bước 1: Frame 1->2->3->4->3->4 với delay 167ms, lặp 1 lần
 * - Bước 2: Frame 5 với delay 83ms, lặp 1 lần  
 * - Bước 3: Frame 6->7 với delay 167ms, lặp vô hạn
 * 
 * DASH Animation:
 * - Bước 1: Frame 1 với delay 167ms, lặp 1 lần
 * - Bước 2: Frame 2->3 với delay 333ms, lặp vô hạn
 */
