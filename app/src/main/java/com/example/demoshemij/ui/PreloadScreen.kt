package com.example.demoshemij.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.demoshemij.util.PreloadManager
import kotlinx.coroutines.launch

/**
 * Màn hình preload hiển thị progress khi load ảnh
 */
@Composable
fun PreloadScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isLoading by PreloadManager.isLoading.collectAsState()
    val progress by PreloadManager.progress.collectAsState()
    val currentTask by PreloadManager.currentTask.collectAsState()
    
    // Tự động bắt đầu preload khi màn hình hiển thị
    LaunchedEffect(Unit) {
        scope.launch {
            PreloadManager.preloadAllImages(context)
        }
    }
    
    // Tự động chuyển màn hình khi hoàn thành
    LaunchedEffect(progress) {
        if (progress >= 1f && !isLoading) {
            kotlinx.coroutines.delay(500) // Đợi 0.5s để user thấy "Complete!"
            onComplete()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Loading Resources...",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))

//            LinearProgressIndicator(
//                progress = progress,
//                modifier = Modifier
//                    .width(300.dp)
//                    .height(8.dp)
//            )
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF9E9E9E)) // Nền xám
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(4.dp)) // Bo tròn cả 4 góc luôn
                        .background(Color(0xFF4CAF50))
                )
            }
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (currentTask.isNotEmpty()) {
                Text(
                    text = currentTask,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Nút skip (optional)
            TextButton(
                onClick = { onComplete() },
                enabled = !isLoading
            ) {
                Text("Skip")
            }
        }
    }
}

/**
 * Wrapper component để wrap content với preload screen
 */
@Composable
fun PreloadWrapper(
    content: @Composable () -> Unit
) {
    var showPreload by remember { mutableStateOf(true) }
    
    if (showPreload) {
        PreloadScreen(
            onComplete = { showPreload = false }
        )
    } else {
//        content()
    }
}
