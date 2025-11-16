package com.example.demoshemij.component

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.demoshemij.SpriteFlip1
import com.example.demoshemij.SpriteState1
import com.example.demoshemij.domain.AnimationController
import com.example.demoshemij.domain.AnimationMapper
import com.example.demoshemij.util.JsonLoader
import kotlinx.coroutines.delay

/**
 * Component mới sử dụng AnimationController từ JSON
 * Thay thế cho ShimejiSprite cũ
 */
@Composable
fun JsonAnimatedSprite(
    spriteState: SpriteState1,
    spriteFlip: SpriteFlip1?,
    onCustomAnimationFinished: () -> Unit = {},
    onImpactFinish: () -> Unit = {},
    characterName: String = "goku",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // ✅ Tạo controller cho state hiện tại - PHÂN BIỆT THEO CHARACTER
    val controller = remember(spriteState, characterName) {
        val ctrl = AnimationMapper.getControllerForState(context, spriteState, characterName)
        Log.d("JsonAnimatedSprite", "Created controller for character: $characterName, state: $spriteState, controller: $ctrl")
        ctrl
    }
    
    // Bắt đầu animation khi controller thay đổi
    LaunchedEffect(controller) {
        controller?.start()
    }
    
    // Cleanup khi dispose
    DisposableEffect(controller) {
        onDispose {
            controller?.cleanup()
        }
    }
    
    // Lấy frame hiện tại
    val currentFrameId by controller?.currentFrameId?.collectAsState() ?: remember { mutableStateOf(1) }
    val frameUrl = controller?.getCurrentFrameUrl()
    
    // ✅ Load character data từ JSON
    var characterData by remember(characterName) { mutableStateOf<com.example.demoshemij.domain.CharacterData?>(null) }
    
    LaunchedEffect(characterName) {
        val data = JsonLoader.loadCharacterData(context)
        characterData = data?.characters?.firstOrNull()?.get(characterName)
    }
    
    val fullImageUrl = remember(frameUrl, characterData) {
        frameUrl?.let { 
            characterData?.folder?.let { folder ->
                "${JsonLoader.BASE_IMAGE_URL}$folder/$it"
            }
        }
    }
    
    // ✅ Xử lý animation kết thúc cho CUSTOM - KEY RIÊNG
    LaunchedEffect(key1 = "custom_$characterName", key2 = spriteState) {
        if (spriteState == SpriteState1.CUSTOM) {
            Log.d("JsonAnimatedSprite", "[$characterName] CUSTOM animation started")
            // Tính tổng thời gian animation
            val jsonData = JsonLoader.loadCharacterData(context)
            val animationData = jsonData?.characters?.firstOrNull()
                ?.get(characterName)?.animations?.get("custom")
            
            var totalDuration = 0L
            animationData?.logic?.forEach { logicStep ->
                val frames = logicStep.frame.size
                val delay = logicStep.delay.toLong()
                val sequence = when (val seq = logicStep.sequence) {
                    is String -> if (seq == "infinity") Int.MAX_VALUE else 1
                    is Int -> seq
                    is Double -> seq.toInt()
                    else -> 1
                }
                
                if (sequence != Int.MAX_VALUE) {
                    totalDuration += frames * delay * sequence
                }
            }
            
            if (totalDuration > 0) {
                Log.d("JsonAnimatedSprite", "[$characterName] Waiting for CUSTOM: ${totalDuration}ms")
                delay(totalDuration)
                Log.d("JsonAnimatedSprite", "[$characterName] CUSTOM finished, calling callback")
                onCustomAnimationFinished()
            }
        }
    }
    
    // ✅ Xử lý impact finish cho Bottom - KEY RIÊNG
    LaunchedEffect(key1 = "impact_$characterName", key2 = spriteState) {
        if (spriteState == SpriteState1.Bottom) {
            Log.d("JsonAnimatedSprite", "[$characterName] State changed to Bottom, calculating impact duration...")
            
            // ✅ Tính tổng thời gian animation impact
            val jsonData = JsonLoader.loadCharacterData(context)
            val animationData = jsonData?.characters?.firstOrNull()
                ?.get(characterName)?.animations?.get("impact")
            
            Log.d("JsonAnimatedSprite", "[$characterName] Animation data for 'impact': $animationData")
            
            var totalDuration = 0L
            animationData?.logic?.forEach { logicStep ->
                val frames = logicStep.frame.size
                val delayTime = logicStep.delay.toLong()
                val sequence = when (val seq = logicStep.sequence) {
                    is String -> if (seq == "infinity") Int.MAX_VALUE else 1
                    is Int -> seq
                    is Double -> seq.toInt()
                    else -> 1
                }
                
                if (sequence != Int.MAX_VALUE) {
                    totalDuration += frames * delayTime * sequence
                }
                
                Log.d("JsonAnimatedSprite", "[$characterName] Logic step: frames=$frames, delay=$delayTime, sequence=$sequence, subtotal=${frames * delayTime * sequence}ms")
            }
            
            Log.d("JsonAnimatedSprite", "[$characterName] Total impact duration calculated: ${totalDuration}ms")
            
            // ✅ Đợi animation impact chạy xong rồi mới gọi callback
            if (totalDuration > 0) {
                Log.d("JsonAnimatedSprite", "[$characterName] Waiting for impact animation: ${totalDuration}ms")
                delay(totalDuration)
            } else {
                // Fallback nếu không tính được thời gian
                Log.w("JsonAnimatedSprite", "[$characterName] Could not calculate impact duration, using fallback 500ms")
                delay(500)
            }
            
            Log.d("JsonAnimatedSprite", "[$characterName] Impact animation finished, calling onImpactFinish()")
            onImpactFinish()
        }
    }
    
    // ✅ Lấy bitmap từ BitmapCache (đã preload sẵn trong memory)
    val cachedBitmap = remember(fullImageUrl) {
        fullImageUrl?.let { com.example.demoshemij.util.BitmapCache.get(it) }
    }
    
    // ✅ Hiển thị bitmap từ cache (không nhấp nháy!)
    if (cachedBitmap != null) {
        Image(
            bitmap = cachedBitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(150.dp * (755 / 688f))
                .aspectRatio(755 / 688f)
            .offset(
                x = when {
                    spriteFlip == SpriteFlip1.TOP ||
                            spriteState == SpriteState1.Touch ||
                            spriteState == SpriteState1.FALL ||
                            spriteState == SpriteState1.Bottom ||
                            spriteState == SpriteState1.DASH ||
                            spriteState == SpriteState1.CUSTOM ||
                            spriteState == SpriteState1.Idle
                    -> 0.dp
                    spriteFlip == SpriteFlip1.LEFT && spriteState != SpriteState1.WALKING -> 0.dp
                    spriteFlip == SpriteFlip1.RIGHT && spriteState != SpriteState1.WALKING -> 0.dp
                    else -> 0.dp
                },
                y = when {
                    spriteFlip == SpriteFlip1.TOP && spriteState == SpriteState1.CLIMB -> 0.dp
                    else -> 0.dp
                }
            )
            .graphicsLayer {
                when {
                    spriteState == SpriteState1.CLIMB && spriteFlip == SpriteFlip1.TOP -> {
                        scaleX = 1f
                        rotationZ = 90f
                    }
                    spriteFlip == SpriteFlip1.LEFT -> {
                        scaleX = 1f
                        rotationZ = 0f
                    }
                    spriteFlip == SpriteFlip1.RIGHT -> {
                        scaleX = -1f
                        rotationZ = 0f
                    }
                    else -> {
                        scaleX = 1f
                        rotationZ = 0f
                    }
                }
            }
        )
    } else {
        // ✅ Fallback: Nếu chưa có trong cache, hiển thị placeholder
        Box(
            modifier = Modifier
                .size(150.dp * (755 / 688f))
                .aspectRatio(755 / 688f),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading...", style = MaterialTheme.typography.bodySmall)
        }
        
        // Log warning
        Log.w("JsonAnimatedSprite", "[$characterName] Image not in cache: $fullImageUrl")
    }
    
    // ✅ Log để debug - KEY RIÊNG
    LaunchedEffect(key1 = "debug_$characterName", key2 = currentFrameId, key3 = frameUrl) {
        Log.d("JsonAnimatedSprite", "[$characterName] State: $spriteState, Frame ID: $currentFrameId, URL: $frameUrl, Full URL: $fullImageUrl, Cached: ${cachedBitmap != null}")
    }
}
