package com.example.demoshemij.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.demoshemij.domain.AnimationController
import com.example.demoshemij.domain.rememberAnimationController

/**
 * Screen để test animation system mới
 * Hiển thị frame ID hiện tại và cho phép chuyển đổi giữa các animation
 */
@Composable
fun AnimationTestScreen() {
    val context = LocalContext.current
    var selectedAnimation by remember { mutableStateOf("idle") }
    
    // Tạo controller cho animation được chọn
    val controller = rememberAnimationController(
        context = context,
        characterName = "goku",
        animationName = selectedAnimation
    )
    
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Animation Test",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Hiển thị thông tin frame hiện tại
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Animation: $selectedAnimation")
                Text("Current Frame ID: $currentFrameId")
                Text("Frame URL: ${frameUrl ?: "N/A"}")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Buttons để chuyển animation
        Text("Chọn Animation:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        val animations = listOf("idle", "hover", "walking", "falling", "climb", "dash", "custom")
        
        animations.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { animName ->
                    Button(
                        onClick = {
                            controller?.stop()
                            selectedAnimation = animName
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedAnimation != animName
                    ) {
                        Text(animName)
                    }
                }
                // Thêm spacer nếu row có 1 item
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { controller?.start() }) {
                Text("Start")
            }
            Button(onClick = { controller?.stop() }) {
                Text("Stop")
            }
        }
    }
}
