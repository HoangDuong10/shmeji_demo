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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ShimejiSprite() {
    val spriteState by SpriteController.spriteState.collectAsState()
    val spriteFlip by SpriteController.flipState.collectAsState()

    var currentFrame by remember { mutableStateOf(0) }

    val idleImages = remember { listOf(R.drawable.idle_1, R.drawable.idle_2) }
    val touchImages = remember { listOf(R.drawable.hover_1, R.drawable.hover_2, R.drawable.hover_3) }
    val fallImages = remember { listOf(R.drawable.falling_1, R.drawable.faling_2) } // ✅ fix tên
    val bottomImages = remember { listOf(R.drawable.impact_2, R.drawable.impact_3, R.drawable.impact_4) }

    val idleDelay = 300L
    val touchDelay = 250L
    val bottomDelay = 500L
    val fallDelay = 250L

    // 👇 Đảm bảo mỗi state tạo 1 coroutine mới, coroutine cũ bị hủy
    LaunchedEffect(key1 = spriteState) {
        currentFrame = 0
        val (images, defaultDelay) = when (spriteState) {
            SpriteState1.Idle -> idleImages to idleDelay
            SpriteState1.Touch -> touchImages to touchDelay
            SpriteState1.Bottom -> bottomImages to bottomDelay
            SpriteState1.FALL -> fallImages to fallDelay
        }

        while (true) {
            // ⏱️ Chọn delay theo frame
            val frameDelay = when (spriteState) {
                SpriteState1.Bottom -> when (currentFrame) {
                    0 -> 500L // impact_2 → impact_3
                    1 -> 250L // impact_3 → impact_4
                    else -> defaultDelay
                }
                else -> defaultDelay
            }

            delay(frameDelay)
            currentFrame = (currentFrame + 1) % images.size
        }
    }


    // 👇 An toàn hơn, dùng when tương ứng với từng state
    val imageRes = when (spriteState) {
        SpriteState1.Idle -> idleImages.getOrNull(currentFrame) ?: idleImages.first()
        SpriteState1.Touch -> touchImages.getOrNull(currentFrame) ?: touchImages.first()
        SpriteState1.Bottom -> bottomImages.getOrNull(currentFrame) ?: bottomImages.first()
        SpriteState1.FALL -> fallImages.getOrNull(currentFrame) ?: fallImages.first()
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = Modifier
            .size(100.dp)
            .graphicsLayer {
//                scaleX = if (spriteFlip == SpriteFlip.Horizontal) -1f else 1f
            }
    )
}


// 🧩 Enum mô tả trạng thái Shimeji
enum class SpriteState1 {
    Idle, Touch, Bottom,FALL
}

// 🧭 Enum mô tả hướng lật
enum class SpriteFlip1 {
    Horizontal, Vertical
}
object SpriteController {
    val flipState = MutableStateFlow<SpriteFlip?>(null)
    val spriteState = MutableStateFlow(SpriteState1.Idle)
}



