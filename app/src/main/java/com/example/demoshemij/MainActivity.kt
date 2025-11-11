package com.example.demoshemij

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
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
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoShemijTheme {
                // ✅ Thêm PreloadWrapper để preload ảnh từ URL trước
                com.example.demoshemij.ui.PreloadWrapper {
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
        var isGokuRunning by remember { mutableStateOf(false) }
        var isVampireRunning by remember { mutableStateOf(false) }

        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Quản lý nhân vật",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ===== GOKU =====
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🐵 Son Goku",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isGokuRunning) "Đang chạy" else "Đã tắt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isGokuRunning) Color(0xFF4CAF50) else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row (
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (checkOverlayPermission()) {
                                    startFloatingService("START_GOKU")
                                    isGokuRunning = true
                                }
                            },
                            enabled = !isGokuRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Bật Goku")
                        }
                        
                        Button(
                            onClick = {
                                startFloatingService("STOP_GOKU")
                                isGokuRunning = false
                            },
                            enabled = isGokuRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Tắt Goku")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ===== VAMPIRE =====
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🧛 Vampire",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isVampireRunning) "Đang chạy" else "Đã tắt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isVampireRunning) Color(0xFF4CAF50) else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (checkOverlayPermission()) {
                                    startFloatingService("START_VAMPIRE")
                                    isVampireRunning = true
                                }
                            },
                            enabled = !isVampireRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9C27B0)
                            )
                        ) {
                            Text("Bật Vampire")
                        }
                        
                        Button(
                            onClick = {
                                startFloatingService("STOP_VAMPIRE")
                                isVampireRunning = false
                            },
                            enabled = isVampireRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Tắt Vampire")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // NÚT TẮT TẤT CẢ
            Button(
                onClick = {
                    onStopAll()
                    isGokuRunning = false
                    isVampireRunning = false
                },
                enabled = isGokuRunning || isVampireRunning,
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
    spriteFlip: SpriteFlip1?,
    onCustomAnimationFinished: () -> Unit,
    onImpactFinish : () -> Unit
) {
    var currentFrame by remember { mutableStateOf(0) }

    val idleImages = remember { listOf(R.drawable.vampire_idle_1, R.drawable.vampire_idle_2) }
    val touchImages = remember { listOf(R.drawable.vampire_hover_1, R.drawable.vampire_hover_2, R.drawable.vampire_hover_3) }
    val fallImages = remember { listOf(R.drawable.vampire_falling_1, R.drawable.vampire_falling_2) }
    val bottomImages = remember { listOf(R.drawable.vampire_impact_1, R.drawable.vampire_impact_2, R.drawable.vampire_impact_3) }
    val dashImages = remember {
        listOf(
            R.drawable.vampire_dash_1,
            R.drawable.vampire_dash_2,
            R.drawable.vampire_dash_3,
            R.drawable.vampire_dash_4,
            R.drawable.vampire_dash_5,
            R.drawable.vampire_dash_6,
            R.drawable.vampire_dash_7,
            R.drawable.vampire_dash_8,
        )
    }
    val climbImages = remember { listOf(R.drawable.vampire_climb_1, R.drawable.vampire_climb_2, R.drawable.vampire_climb_3) }
    val customImage = remember {
        listOf(
            R.drawable.vampire_custom_1,
            R.drawable.vampire_custom_2,
            R.drawable.vampire_custom_3,
            R.drawable.vampire_custom_4,
            R.drawable.vampire_custom_5,
            R.drawable.vampire_custom_6,
            R.drawable.vampire_custom_7,
            R.drawable.vampire_custom_8,
            R.drawable.vampire_custom_9,
            R.drawable.vampire_custom_10,
            R.drawable.vampire_custom_11,
            R.drawable.vampire_custom_12,
            R.drawable.vampire_custom_13,
            R.drawable.vampire_custom_14,
            R.drawable.vampire_custom_15,
            R.drawable.vampire_custom_16,
            R.drawable.vampire_custom_17,
        )
    }
    val walkingImages = remember { listOf(R.drawable.vampire_walking_1, R.drawable.vampire_walking_2) }
    var width by  remember { mutableStateOf(0) }
    val idleDelay = 1000/3L
    val touchDelay = 250L
    val bottomDelay = 500L
    val fallDelay = 250L
    val dashStartDelay = 160L
    val dashLoopDelay = 333L

    LaunchedEffect(spriteState) {
        currentFrame = 0
        when (spriteState) {
            SpriteState1.Idle -> {
                width = 160
                animateFrames(idleImages, idleDelay) { currentFrame = it }

            }
            SpriteState1.Touch -> {
                width = 100
                animateFrames(touchImages, touchDelay) { currentFrame = it }

            }
            SpriteState1.Bottom -> animateImpact(bottomImages, setFrame = {currentFrame = it}, onFinished = onImpactFinish)
            SpriteState1.FALL -> animateFrames(fallImages, fallDelay) { currentFrame = it }
            SpriteState1.CLIMB -> animateFrames(climbImages, 1000/3L) { currentFrame = it }
            SpriteState1.DASH ->   animateDash1(dashImages,setFrame = {currentFrame = it} )
            SpriteState1.CUSTOM -> {
                width = 300
                animateCustom11(customImage,  onFinished = onCustomAnimationFinished,setFrame = {currentFrame = it} )
            }
            SpriteState1.WALKING -> {
                width = 50
                animateFrames(walkingImages, idleDelay) { currentFrame = it }
            }
        }
    }

    val imageRes = when (spriteState) {
        SpriteState1.Idle -> idleImages.getOrNull(currentFrame) ?: idleImages.first()
        SpriteState1.Touch -> touchImages.getOrNull(currentFrame) ?: touchImages.first()
        SpriteState1.Bottom -> bottomImages.getOrNull(currentFrame) ?: bottomImages.first()
        SpriteState1.FALL -> fallImages.getOrNull(currentFrame) ?: fallImages.first()
        SpriteState1.DASH -> dashImages.getOrNull(currentFrame) ?: dashImages.first()
        SpriteState1.CLIMB -> climbImages.getOrNull(currentFrame) ?: climbImages.first()
        SpriteState1.CUSTOM -> customImage.getOrNull(currentFrame) ?: customImage.first()
        SpriteState1.WALKING -> walkingImages.getOrNull(currentFrame) ?: walkingImages.first()
    }
    val imageBitmap = ImageBitmap.imageResource(id = walkingImages[0])
    val imageBitmap11 = ImageBitmap.imageResource(id =imageRes).width.toFloat()
    val aspectRatio = imageBitmap.width.toFloat()/10
//    Log.d("ShimejiSprite", "Aspect Ratio: $aspectRatio")
    Log.d("ShimejiSprite", "Aspect Ratio111: ${imageBitmap11 / 10}")
    val animatedScaleX by animateFloatAsState(
        targetValue = when (spriteFlip) {
            SpriteFlip1.LEFT -> 1f
            SpriteFlip1.RIGHT -> -1f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = 300,
            easing = LinearOutSlowInEasing
        ),
        label = "flipAnimation"
    )

    // 🧩 Thu nhỏ 20% so với kích thước gốc
    val scaleFactor = 0.2f
    val widthDp = with(LocalDensity.current) { (imageBitmap.width * scaleFactor).toDp() }
    Box(
        modifier = Modifier.size(150.dp*(755/688f)),
        contentAlignment = Alignment.TopEnd
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier
                .size(150.dp*(766/688f))
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
                        spriteFlip == SpriteFlip1.LEFT && spriteState!= SpriteState1.WALKING -> (0).dp
                        spriteFlip == SpriteFlip1.RIGHT && spriteState!= SpriteState1.WALKING -> (0).dp
                        else -> 0.dp
                    },
                    y = when {
                        // ⚡ Chỉ khi leo trần mới nâng sprite lên -33dp
                        spriteFlip == SpriteFlip1.TOP && spriteState == SpriteState1.CLIMB -> (0).dp
                        else -> 0.dp
                    }
                )
                .graphicsLayer {
                    when {
                        // ⚡ Nếu đang TOUCH hoặc FALL → luôn bình thường

                        // ⚡ Nếu đang leo tường (CLIMB) + hướng TOP → xoay 90°
                        spriteState == SpriteState1.CLIMB && spriteFlip == SpriteFlip1.TOP -> {
                            scaleX = 1f
                            rotationZ = 90f
                        }

                        // ⚡ Các hướng khác giữ nguyên logic cũ
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
//                .background(Color.Red)
            ,
            contentScale = ContentScale.Fit

        )
    }


}

private suspend fun animateFrames(images: List<Int>, frameDelay: Long, setFrame: (Int) -> Unit = {}) {
    var current = 0
    while (true) {
        if (!isActive) return
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
        if (!isActive) return
        delay(dashLoopDelay)
        current = if (current == 1) 2 else 1
        setFrame(current)
    }
}

private suspend fun animateCustom(
    images: List<Int>,
    setFrame: (Int) -> Unit,
    onFinished: () -> Unit
) {
    val sequence = listOf(0, 1, 2, 3, 2, 3, 4, 5, 6, 5, 6, 5, 6, 5, 6)

    for (i in sequence.indices) {
        if (!isActive) return
        setFrame(sequence[i])

        // Nếu là frame 5 -> 6 (index 6 -> 7) thì delay 1/12s, còn lại 1/6s
        val delayTime = if (i == 6) 83L else 166L

        delay(delayTime)
    }
    onFinished()
}

private suspend fun animateCustom11(
    images: List<Int>,
    setFrame: (Int) -> Unit,
    onFinished: () -> Unit
) {
    val sequence = listOf(0, 1, 2, 3, 4, 5,6,7,8,9,10,11,12,13,14,15,16)

    for (i in sequence.indices) {
        if (!isActive) return
        setFrame(sequence[i])

        // Nếu là frame 5 -> 6 (index 6 -> 7) thì delay 1/12s, còn lại 1/6s

        delay(250L)
    }
    onFinished()
}

private suspend fun animateImpact(
    images: List<Int>,
    setFrame: (Int) -> Unit,
    onFinished: () -> Unit
) {
    val sequence = listOf(1, 2, 3)

    for (i in sequence.indices) {
        if (!isActive) return

        setFrame(sequence[i])

        val delayTime = when (i) {
            0 -> 333L  // 1 -> 2
            1 -> 250L  // 2 -> 3
            else -> 250L
        }

        delay(delayTime)
    }

    onFinished()
}


private suspend fun animateDash1(
    images: List<Int>,
    setFrame: (Int) -> Unit,
) {
    // Giai đoạn 1: chạy khởi động (0–5)
    val startSequence = listOf(0, 1, 2, 3, 4, 5)
    for (i in startSequence) {
        if (!isActive) return
        setFrame(i)
        delay(1000L / 6) // 1/6s mỗi frame
    }

    // Giai đoạn 2: lặp vô hạn (2–7)
    val loopSequence = listOf(2, 3, 4, 5, 6, 7)
    while (isActive) {
        for (i in loopSequence) {
            if (!isActive) return
            setFrame(i)
            delay(1000L / 6)
        }
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

private suspend fun animateFrames(
    frames: List<Pair<Int,Long>>,
    setFrame: (Int) -> Unit = {}
) {
    var current = 0
    while (true) {
        if (!isActive) return
        val frame = frames[current]
        setFrame(frame.first)
        delay(frame.second)
        current = (current + 1) % frames.size
    }
}


