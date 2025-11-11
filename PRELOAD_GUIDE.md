# 📥 Preload Images Guide

## Tổng quan

Preload tất cả ảnh khi app start, cache vào disk, sau đó dùng offline.

## Lợi ích

### ✅ Advantages
1. **Smooth animation**: Không bị lag khi chuyển frame
2. **Offline support**: Dùng được khi không có mạng
3. **Predictable**: Biết trước tất cả ảnh đã sẵn sàng
4. **Better UX**: Loading một lần, dùng mãi mãi

### ❌ Considerations
1. **Initial loading time**: Phải đợi download lần đầu
2. **Storage**: Chiếm dung lượng cache
3. **Network**: Tốn bandwidth lần đầu

## Cách sử dụng

### Option 1: Tự động preload khi app start

#### Trong MainActivity:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoShemijTheme {
                PreloadWrapper {
                    // Your main content
                    MainScreen()
                }
            }
        }
    }
}
```

### Option 2: Manual preload

#### Trong ViewModel hoặc Activity:
```kotlin
lifecycleScope.launch {
    // Preload tất cả
    val success = PreloadManager.preloadAllImages(context)
    if (success) {
        // Ready to use
    }
}
```

### Option 3: Preload từng character

```kotlin
lifecycleScope.launch {
    // Chỉ preload goku
    PreloadManager.preloadCharacter(context, "goku")
    
    // Chỉ preload vampire
    PreloadManager.preloadCharacter(context, "vampire")
}
```

## UI với Progress

### PreloadScreen tự động:
```kotlin
@Composable
fun MyApp() {
    var showPreload by remember { mutableStateOf(true) }
    
    if (showPreload) {
        PreloadScreen(
            onComplete = { showPreload = false }
        )
    } else {
        MainScreen()
    }
}
```

### Custom progress UI:
```kotlin
@Composable
fun CustomPreload() {
    val progress by PreloadManager.progress.collectAsState()
    
    Column {
        Text("Loading: ${progress.loaded}/${progress.total}")
        LinearProgressIndicator(progress = progress.progress)
        
        if (progress.isComplete) {
            Button(onClick = { /* Continue */ }) {
                Text("Start")
            }
        }
    }
}
```

## Progress State

### PreloadProgress data:
```kotlin
data class PreloadProgress(
    val total: Int,           // Tổng số ảnh
    val loaded: Int,          // Đã load thành công
    val failed: Int,          // Load thất bại
    val currentImage: String, // Ảnh đang load
    val isComplete: Boolean   // Đã xong chưa
)
```

### Computed properties:
```kotlin
progress.progress    // 0.0 - 1.0 (tổng progress)
progress.successRate // 0.0 - 1.0 (tỷ lệ thành công)
```

## Flow

### Lần đầu (chưa có cache):
```
1. App start
2. Show PreloadScreen
3. Load JSON từ URL (~1-3s)
4. Collect tất cả image URLs
5. Download từng ảnh (~30-60s cho 50 ảnh)
6. Save to disk cache
7. Show progress: 1/50, 2/50, ...
8. Complete → Hide PreloadScreen
9. Show MainScreen
10. Animations smooth (dùng cache)
```

### Lần sau (đã có cache):
```
1. App start
2. Check cache → Có rồi
3. Skip preload
4. Show MainScreen ngay
5. Animations smooth (dùng cache)
```

## Performance

### Ước tính thời gian:

| Số ảnh | Kích thước | Tốc độ mạng | Thời gian |
|--------|------------|-------------|-----------|
| 20 | 50KB/ảnh | 4G | ~5-10s |
| 50 | 50KB/ảnh | 4G | ~15-30s |
| 100 | 50KB/ảnh | 4G | ~30-60s |
| 20 | 50KB/ảnh | WiFi | ~2-5s |
| 50 | 50KB/ảnh | WiFi | ~5-15s |

### Tối ưu:
- Compress ảnh: PNG → WebP (giảm 30-50%)
- Resize ảnh: 500px thay vì 1000px
- Parallel download: Download nhiều ảnh cùng lúc

## API

### PreloadManager methods:

```kotlin
// Preload tất cả
suspend fun preloadAllImages(context: Context): Boolean

// Preload một character
suspend fun preloadCharacter(context: Context, characterName: String): Boolean

// Check đã preload chưa
fun isPreloaded(): Boolean

// Reset progress
fun reset()

// Observe progress
val progress: StateFlow<PreloadProgress>
```

## Debug

### Xem log:
```bash
adb logcat | grep PreloadManager
```

### Output:
```
D/PreloadManager: Starting preload...
D/PreloadManager: Total images to preload: 50
D/PreloadManager: ✅ Loaded: https://.../idle1.png
D/PreloadManager: ✅ Loaded: https://.../idle2.png
D/PreloadManager: ❌ Failed: https://.../missing.png
D/PreloadManager: Preload complete! Loaded: 48, Failed: 2
```

### Check cache:
```bash
# Xem cache directory
adb shell ls -la /data/data/com.example.demoshemij/cache/images/

# Xem kích thước
adb shell du -sh /data/data/com.example.demoshemij/cache/images/
```

## Advanced

### Parallel download (faster):

```kotlin
suspend fun preloadAllImagesParallel(context: Context): Boolean {
    return withContext(Dispatchers.IO) {
        val urls = collectAllUrls()
        
        // Download 5 ảnh cùng lúc
        urls.chunked(5).forEach { chunk ->
            chunk.map { url ->
                async {
                    ImageLoader.loadImageFromUrl(context, url)
                }
            }.awaitAll()
        }
        
        true
    }
}
```

### Selective preload:

```kotlin
// Chỉ preload animations quan trọng
suspend fun preloadEssentialAnimations(context: Context) {
    val essentialAnimations = listOf("idle", "walking", "falling")
    
    characterData.animations
        .filter { it.key in essentialAnimations }
        .forEach { (name, animation) ->
            animation.frames.forEach { frame ->
                ImageLoader.loadImageFromUrl(context, frame.url)
            }
        }
}
```

### Background preload:

```kotlin
// Preload trong background, không block UI
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        lifecycleScope.launch {
            PreloadManager.preloadAllImages(this@MyApplication)
        }
    }
}
```

## Best Practices

### 1. Show progress
```kotlin
// ✅ GOOD: User biết đang load
PreloadScreen(onComplete = { ... })

// ❌ BAD: User không biết gì, tưởng app bị treo
// (silent loading)
```

### 2. Allow skip
```kotlin
// ✅ GOOD: User có thể skip nếu không muốn đợi
TextButton(onClick = onComplete) {
    Text("Skip")
}
```

### 3. Cache validation
```kotlin
// Check cache trước khi preload
if (!PreloadManager.isPreloaded()) {
    PreloadManager.preloadAllImages(context)
}
```

### 4. Error handling
```kotlin
val success = PreloadManager.preloadAllImages(context)
if (!success) {
    // Show error, allow retry
    showRetryDialog()
}
```

## Troubleshooting

### Preload chậm
- Check network speed
- Reduce image size
- Use parallel download
- Preload chỉ essential animations

### Preload fail
- Check URLs
- Check internet connection
- Check storage space
- See logs for specific errors

### Cache không work
- Check cache directory exists
- Check storage permission
- Clear cache và retry

## Files

1. ✅ `PreloadManager.kt` - Core preload logic
2. ✅ `PreloadScreen.kt` - UI với progress
3. ✅ `ImageLoader.kt` - Load và cache images

## Example Integration

### MainActivity.kt:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DemoShemijTheme {
                PreloadWrapper {
                    Scaffold { padding ->
                        MainScreen(Modifier.padding(padding))
                    }
                }
            }
        }
    }
}
```

---

**Preload một lần, dùng mãi mãi! 📥**
