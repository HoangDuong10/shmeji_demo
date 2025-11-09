package com.example.demoshemij.component

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
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
    
    // ✅ Xử lý animation kết thúc cho CUSTOM - KEY RIÊNG
    LaunchedEffect(key1 = "custom_$characterName", key2 = spriteState) {
        if (spriteState == SpriteState1.CUSTOM) {
            Log.d("JsonAnimatedSprite", "[$characterName] CUSTOM animation started")
            // Tính tổng thời gian animation
            val characterData = JsonLoader.loadCharacterData(context)
            val animationData = characterData?.characters?.firstOrNull()
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
            val characterData = JsonLoader.loadCharacterData(context)
            val animationData = characterData?.characters?.firstOrNull()
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
    
    // ✅ Lấy drawable resource - PHÂN BIỆT THEO CHARACTER
    val imageRes = remember(frameUrl, characterName) {
        val resId = frameUrl?.let { 
            val id = AnimationMapper.getDrawableId(context, it, characterName)
            Log.d("JsonAnimatedSprite", "[$characterName] Looking for drawable: $it -> ID: $id")
            id
        } ?: run {
            Log.e("JsonAnimatedSprite", "[$characterName] frameUrl is null! Controller: $controller, State: $spriteState")
            android.R.drawable.ic_menu_report_image
        }
        resId
    }
    
    // Animation scale cho flip
    val animatedScaleX = when (spriteFlip) {
        SpriteFlip1.LEFT -> 1f
        SpriteFlip1.RIGHT -> -1f
        else -> 1f
    }

        Image(
            bitmap = ImageBitmap.imageResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier
                .size(150.dp * (755 / 688f))
                .aspectRatio(755/688f)
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
                .background(Color.Red),
        )
    
    // ✅ Log để debug - KEY RIÊNG
    LaunchedEffect(key1 = "debug_$characterName", key2 = currentFrameId, key3 = frameUrl) {
        Log.d("JsonAnimatedSprite", "[$characterName] State: $spriteState, Frame ID: $currentFrameId, URL: $frameUrl, Drawable: $imageRes")
    }
}
