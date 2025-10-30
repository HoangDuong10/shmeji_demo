package com.example.demoshemij

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.demoshemij.ui.theme.DemoShemijTheme
import com.stevdza_san.sprite.component.SpriteView
import com.example.demoshemij.domain.SpriteSheet
import com.example.demoshemij.domain.SpriteSpec
import com.example.demoshemij.domain.SpriteFlip
import com.example.demoshemij.domain.SpriteManager
import com.example.demoshemij.domain.SpriteState
import com.example.demoshemij.domain.rememberSpriteState
import com.stevdza_san.sprite.util.getScreenWidth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoShemijTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onAddSprite = { startFloatingService("ADD_SPRITE") },
                        onStopAll = { startFloatingService("STOP_ALL") },
                        checkOverlayPermission = { checkAndRequestOverlayPermission() }
                    )
                }
            }
        }
    }

    // Kiểm tra và xin quyền overlay
    private fun checkAndRequestOverlayPermission(): Boolean {
        return if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${packageName}")
            )
            startActivity(intent)
            false
        } else {
            true
        }
    }

    // Gửi lệnh đến service
    private fun startFloatingService(action: String) {
        if (action == "ADD_SPRITE" && !Settings.canDrawOverlays(this)) return

        val serviceIntent = Intent(this, FloatingSpriteService::class.java).apply {
            this.action = action
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    @Composable
    fun MainScreen(
        modifier: Modifier = Modifier,
        onAddSprite: () -> Unit,
        onStopAll: () -> Unit,
        checkOverlayPermission: () -> Boolean
    ) {
        var isServiceRunning by remember { mutableStateOf(false) }

        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isServiceRunning) "Có sprite đang chạy!" else "Chưa có sprite",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // NÚT THÊM SPRITE
            Button(
                onClick = {
                    if (checkOverlayPermission()) {
                        onAddSprite()
                        isServiceRunning = true
                    }
                },
                enabled = true // Luôn bật, vì thêm bao nhiêu cũng được
            ) {
                Text("Thêm Sprite")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // NÚT TẮT TẤT CẢ
            Button(
                onClick = {
                    onStopAll()
                    isServiceRunning = false
                },
                enabled = isServiceRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Tắt Tất Cả")
            }
        }
    }
}
@Composable
fun MovingSprite(
    spriteState: SpriteState,
    spriteSpec: SpriteSpec,
    spriteFlip : SpriteFlip?= null,
    modifier: Modifier = Modifier,
    stop :() -> Unit = {},
    selectedRow : Int? = null
) {
    SpriteView(
        modifier = modifier,
        spriteState = spriteState,
        spriteSpec = spriteSpec,
        spriteFlip = spriteFlip,
        selectedRow = selectedRow
    )

}

@Composable
fun ShimejiSprite(
    spriteState: SpriteState1,
    spriteFlip: SpriteFlip1?
) {
    var currentFrame by remember { mutableStateOf(0) }

    val idleImages = remember { listOf(R.drawable.idle_1, R.drawable.idle_2) }
    val touchImages = remember { listOf(R.drawable.hover_1, R.drawable.hover_2, R.drawable.hover_3) }
    val fallImages = remember { listOf(R.drawable.falling_1, R.drawable.faling_2) }
    val bottomImages = remember { listOf(R.drawable.impact_2, R.drawable.impact_3, R.drawable.impact_4) }
    val dashImages = remember { listOf(R.drawable.dash_1, R.drawable.dash_2, R.drawable.dash_3) }
    val climbImages = remember { listOf(R.drawable.climb_1, R.drawable.climb_2, R.drawable.climb_3) }
    val customImage = remember { listOf(R.drawable.custom_1, R.drawable.custom_2, R.drawable.custom_3, R.drawable.custom_4, R.drawable.custom_5, R.drawable.custom_6, R.drawable.custom_7) }
    val walkingImages = remember { listOf(R.drawable.walking_1, R.drawable.walking_2) }
    val idleDelay = 300L
    val touchDelay = 250L
    val bottomDelay = 500L
    val fallDelay = 250L
    val dashStartDelay = 160L
    val dashLoopDelay = 333L

    LaunchedEffect(spriteState) {
        currentFrame = 0
        when (spriteState) {
            SpriteState1.Idle -> animateFrames(idleImages, idleDelay) { currentFrame = it }
            SpriteState1.Touch -> animateFrames(touchImages, touchDelay) { currentFrame = it }
            SpriteState1.Bottom -> animateFrames(bottomImages, bottomDelay) { currentFrame = it }
            SpriteState1.FALL -> animateFrames(fallImages, fallDelay) { currentFrame = it }
            SpriteState1.CLIMB -> animateFrames(climbImages, dashLoopDelay) { currentFrame = it }
            SpriteState1.DASH -> animateDash(dashImages, dashStartDelay, dashLoopDelay) { currentFrame = it }
            SpriteState1.CUSTOM -> animateFrames(customImage, fallDelay) { currentFrame = it }
            SpriteState1.WALKING -> animateFrames(walkingImages, fallDelay) { currentFrame = it }
        }
    }

    val imageRes = when (spriteState) {
        SpriteState1.Idle -> idleImages.getOrNull(currentFrame) ?: idleImages.first()
        SpriteState1.Touch -> touchImages.getOrNull(currentFrame) ?: touchImages.first()
        SpriteState1.Bottom -> bottomImages.getOrNull(currentFrame) ?: bottomImages.first()
        SpriteState1.FALL -> fallImages.getOrNull(currentFrame) ?: fallImages.first()
        SpriteState1.DASH -> dashImages.getOrNull(currentFrame) ?: dashImages.first()
        SpriteState1.CLIMB -> climbImages.getOrNull(currentFrame) ?: dashImages.first()
        SpriteState1.CUSTOM -> customImage.getOrNull(currentFrame) ?: dashImages.first()
        SpriteState1.WALKING -> walkingImages.getOrNull(currentFrame) ?: dashImages.first()
    }

    Box(
        modifier = Modifier.graphicsLayer {
            clip = false // 👈 Cho phép phần tử con vẽ tràn ra ngoài
        }
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .offset(
                    x = when {
                        spriteFlip == SpriteFlip1.TOP ||
                                spriteState == SpriteState1.Touch ||
                                spriteState == SpriteState1.FALL ||
                                spriteState == SpriteState1.Bottom ||
                                spriteState == SpriteState1.DASH
                            -> 0.dp
                        spriteFlip == SpriteFlip1.LEFT && spriteState!= SpriteState1.WALKING -> (-32).dp
                        spriteFlip == SpriteFlip1.RIGHT && spriteState!= SpriteState1.WALKING -> (32).dp
                        else -> 0.dp
                    },
                    y = when (spriteFlip) {
                        SpriteFlip1.TOP -> (-32).dp
                        else -> 0.dp
                    },
                )
                .graphicsLayer {
                    when (spriteFlip) {
                        SpriteFlip1.LEFT -> {
                            scaleX = 1f
                            rotationZ = 0f
                        }
                        SpriteFlip1.RIGHT -> {
                            scaleX = -1f
                            rotationZ = 0f
                        }
                        SpriteFlip1.TOP -> {
                            scaleX = 1f
                            rotationZ = 90f
                        }
                        else -> {
                            scaleX = 1f
                            rotationZ = 0f
                        }
                    }
                }
        )
    }


}

private suspend fun animateFrames(images: List<Int>, frameDelay: Long, setFrame: (Int) -> Unit = {}) {
    var current = 0
    while (true) {
        delay(frameDelay)
        current = (current + 1) % images.size
        setFrame(current)
    }
}

private suspend fun animateDash(
    images: List<Int>,
    dashStartDelay: Long,
    dashLoopDelay: Long,
    setFrame: (Int) -> Unit
) {
    // Bước 1: dash_1 → dash_2 (chạy 1 lần)
    setFrame(0)
    delay(dashStartDelay)
    setFrame(1)

    // Bước 2: lặp 2 ↔ 3 vô hạn
    var current = 1
    while (true) {
        delay(dashLoopDelay)
        current = if (current == 1) 2 else 1
        setFrame(current)
    }
}


// 🧩 Enum mô tả trạng thái Shimeji
enum class SpriteState1 {
    Idle, Touch, Bottom,FALL,DASH,CLIMB,CUSTOM,WALKING
}

// 🧭 Enum mô tả hướng lật
enum class SpriteFlip1 {
    LEFT, TOP,RIGHT,BOTTOM
}
//object SpriteController {
//    val flipState = MutableStateFlow<SpriteFlip?>(null)
//    val spriteState = MutableStateFlow(SpriteState1.Idle)
//}
//


