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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoShemijTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onStartService = { startFloatingService() },
                        onStopService = { stopFloatingService() },
                        checkOverlayPermission = { checkAndRequestOverlayPermission() },
                        onStop = {}
                    )
//                    MovingSpriteAroundScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    private fun checkAndRequestOverlayPermission(): Boolean {
        return if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${this.packageName}")
            )
            startActivity(intent)
            false
        } else {
            true
        }
    }

    private fun startFloatingService() {
        if (Settings.canDrawOverlays(this)) {
            val serviceIntent = Intent(this, FloatingSpriteService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun stopFloatingService() {
        val serviceIntent = Intent(this, FloatingSpriteService::class.java)
        stopService(serviceIntent)
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    checkOverlayPermission: () -> Boolean,
    onStop : () -> Unit = { }
) {
    var isServiceRunning by remember { mutableStateOf(false) }
    var isAnimationRunning by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isServiceRunning) "Sprite đang chạy!" else "Sprite đã dừng",
            modifier = Modifier.padding(16.dp)
        )
        Button(
            onClick = {
                if (!isServiceRunning) {
                    if (checkOverlayPermission()) {
                        onStartService()
                        isServiceRunning = true
                    } else {
                        // Hiển thị thông báo yêu cầu quyền nếu cần
//                        Text("Vui lòng cấp quyền overlay!")
                    }
                }
            },
            enabled = !isServiceRunning
        ) {
            Text("Bật Sprite")
        }
        Button(
            onClick = {
//                if (isServiceRunning) {
//                    onStopService()
//                    isServiceRunning = false
//                }
                SpriteManager.stopAnimation()
            },
            enabled = isServiceRunning
        ) {
            Text("Tắt Sprite")
        }
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DemoShemijTheme {
        Greeting("Android")
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