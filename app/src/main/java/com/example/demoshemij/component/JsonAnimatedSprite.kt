package com.example.demoshemij.component

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    
    // Tạo controller cho state hiện tại
    val controller = remember(spriteState) {
        val ctrl = AnimationMapper.getControllerForState(context, spriteState, characterName)
        Log.d("JsonAnimatedSprite", "Created controller for state: $spriteState, controller: $ctrl")
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
    
    // Xử lý animation kết thúc cho CUSTOM
    LaunchedEffect(spriteState) {
        if (spriteState == SpriteState1.CUSTOM) {
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
                delay(totalDuration)
                onCustomAnimationFinished()
            }
        }
    }
    
    // Xử lý impact finish cho Bottom
    LaunchedEffect(spriteState) {
        if (spriteState == SpriteState1.Bottom) {
            Log.d("JsonAnimatedSprite", "State changed to Bottom, calculating impact duration...")
            
            // ✅ Tính tổng thời gian animation impact
            val characterData = JsonLoader.loadCharacterData(context)
            val animationData = characterData?.characters?.firstOrNull()
                ?.get(characterName)?.animations?.get("impact")
            
            Log.d("JsonAnimatedSprite", "Animation data for 'impact': $animationData")
            
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
                
                Log.d("JsonAnimatedSprite", "Logic step: frames=$frames, delay=$delayTime, sequence=$sequence, subtotal=${frames * delayTime * sequence}ms")
            }
            
            Log.d("JsonAnimatedSprite", "Total impact duration calculated: ${totalDuration}ms")
            
            // ✅ Đợi animation impact chạy xong rồi mới gọi callback
            if (totalDuration > 0) {
                Log.d("JsonAnimatedSprite", "Waiting for impact animation: ${totalDuration}ms")
                delay(totalDuration)
            } else {
                // Fallback nếu không tính được thời gian
                Log.w("JsonAnimatedSprite", "Could not calculate impact duration, using fallback 500ms")
                delay(500)
            }
            
            Log.d("JsonAnimatedSprite", "Impact animation finished, calling onImpactFinish()")
            onImpactFinish()
        }
    }
    
    // Lấy drawable resource
    val imageRes = remember(frameUrl) {
        val resId = frameUrl?.let { 
            val id = AnimationMapper.getDrawableId(context, it, "vampire")
            Log.d("JsonAnimatedSprite", "Looking for drawable: $it -> ID: $id")
            id
        } ?: run {
            Log.e("JsonAnimatedSprite", "frameUrl is null! Controller: $controller, State: $spriteState")
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
    
    Box(
        modifier = modifier.size(150.dp * (755 / 688f)),
        contentAlignment = Alignment.TopEnd
    ) {
        Image(
            bitmap = ImageBitmap.imageResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier
                .size(150.dp * (766 / 688f))
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
                },
            contentScale = ContentScale.Fit
        )
    }
    
    // Log để debug
    LaunchedEffect(currentFrameId, frameUrl) {
        Log.d("JsonAnimatedSprite", "State: $spriteState, Frame ID: $currentFrameId, URL: $frameUrl, Drawable: $imageRes")
    }
}
