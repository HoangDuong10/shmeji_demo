# 🖼️ Hướng dẫn sử dụng ảnh từ URL

## Tổng quan

Hệ thống giờ hỗ trợ load ảnh từ 2 nguồn:
1. **URL** - Link ảnh trên internet (http:// hoặc https://)
2. **Drawable** - Tên drawable resource local (vampire_idle_1)

## Cách sử dụng

### 1. Sử dụng URL trong JSON

```json
{
  "animations": {
    "idle": {
      "frames": [
        {
          "id": 1,
          "url": "https://example.com/images/idle_1.png"
        },
        {
          "id": 2,
          "url": "https://example.com/images/idle_2.png"
        }
      ]
    }
  }
}
```

### 2. Sử dụng Drawable local

```json
{
  "animations": {
    "idle": {
      "frames": [
        {
          "id": 1,
          "url": "vampire_idle_1"
        },
        {
          "id": 2,
          "url": "vampire_idle_2"
        }
      ]
    }
  }
}
```

### 3. Mix cả hai

```json
{
  "animations": {
    "idle": {
      "frames": [
        {
          "id": 1,
          "url": "https://example.com/images/idle_1.png"
        },
        {
          "id": 2,
          "url": "vampire_idle_2"
        }
      ]
    }
  }
}
```

## Tính năng ImageLoader

### 1. Tự động phát hiện
- Nếu URL bắt đầu bằng `http://` hoặc `https://` → Load từ internet
- Nếu không → Load từ drawable resources

### 2. Caching
- **Memory cache**: Ảnh được cache trong RAM
- **Disk cache**: Ảnh được lưu vào `cache/images/` với tên MD5
- Lần load tiếp theo sẽ nhanh hơn

### 3. Error handling
- Nếu load URL thất bại → Log error, không crash app
- Nếu drawable không tồn tại → Log error, không crash app
- Timeout: 10 giây cho connect và read

## Ví dụ thực tế

### Ví dụ 1: Sử dụng GitHub raw URL

```json
{
  "animations": {
    "idle": {
      "frames": [
        {
          "id": 1,
          "url": "https://raw.githubusercontent.com/username/repo/main/assets/idle_1.png"
        },
        {
          "id": 2,
          "url": "https://raw.githubusercontent.com/username/repo/main/assets/idle_2.png"
        }
      ]
    }
  }
}
```

### Ví dụ 2: Sử dụng CDN

```json
{
  "animations": {
    "idle": {
      "frames": [
        {
          "id": 1,
          "url": "https://cdn.example.com/sprites/goku/idle_1.png"
        },
        {
          "id": 2,
          "url": "https://cdn.example.com/sprites/goku/idle_2.png"
        }
      ]
    }
  }
}
```

### Ví dụ 3: Mix local và remote

```json
{
  "animations": {
    "idle": {
      "frames": [
        {
          "id": 1,
          "url": "vampire_idle_1"
        },
        {
          "id": 2,
          "url": "vampire_idle_2"
        }
      ]
    },
    "custom": {
      "frames": [
        {
          "id": 1,
          "url": "https://example.com/custom/frame_1.png"
        },
        {
          "id": 2,
          "url": "https://example.com/custom/frame_2.png"
        }
      ]
    }
  }
}
```

## API ImageLoader

### Load ảnh trong Composable

```kotlin
@Composable
fun MyComponent() {
    val imageBitmap = rememberImageBitmap("https://example.com/image.png")
    
    if (imageBitmap != null) {
        Image(bitmap = imageBitmap, contentDescription = null)
    } else {
        // Loading hoặc error
        CircularProgressIndicator()
    }
}
```

### Load ảnh trong suspend function

```kotlin
suspend fun loadMyImage(context: Context) {
    val bitmap = ImageLoader.loadImage(context, "https://example.com/image.png")
    // Sử dụng bitmap
}
```

### Clear cache

```kotlin
// Clear tất cả cache
ImageLoader.clearCache(context)
```

## Debug

### Xem log loading

```bash
adb logcat | grep ImageLoader
```

Output:
```
D/ImageLoader: Loading from disk cache: https://example.com/image.png
D/ImageLoader: Downloading image: https://example.com/image.png
D/ImageLoader: Image loaded successfully: https://example.com/image.png
E/ImageLoader: Failed to load image from URL: https://example.com/bad.png
```

### Kiểm tra cache

```bash
# Xem cache directory
adb shell ls -la /data/data/com.example.demoshemij/cache/images/

# Xem kích thước cache
adb shell du -sh /data/data/com.example.demoshemij/cache/images/
```

## Performance

### Lần đầu load (từ URL)
- Download: ~1-3 giây (tùy kích thước ảnh và tốc độ mạng)
- Decode: ~100-300ms
- Tổng: ~1-3 giây

### Lần sau load (từ cache)
- Disk read: ~50-100ms
- Decode: ~100-300ms
- Tổng: ~150-400ms

### Load từ drawable
- Decode: ~50-100ms
- Tổng: ~50-100ms

## Best Practices

### 1. Tối ưu kích thước ảnh
```
Khuyến nghị:
- Width: 200-500px
- Height: 200-500px
- Format: PNG với transparency
- Size: < 100KB mỗi frame
```

### 2. Sử dụng CDN
```
✅ GOOD: https://cdn.example.com/sprites/frame.png
❌ BAD: https://slow-server.com/large-image.png
```

### 3. Preload ảnh quan trọng
```kotlin
// Load trước các ảnh sẽ dùng
LaunchedEffect(Unit) {
    ImageLoader.loadImage(context, "https://example.com/idle_1.png")
    ImageLoader.loadImage(context, "https://example.com/idle_2.png")
}
```

### 4. Handle loading state
```kotlin
var isLoading by remember { mutableStateOf(true) }

LaunchedEffect(frameUrl) {
    isLoading = true
    imageBitmap = ImageLoader.loadImage(context, frameUrl)
    isLoading = false
}

if (isLoading) {
    CircularProgressIndicator()
} else if (imageBitmap != null) {
    Image(bitmap = imageBitmap!!, ...)
}
```

## Troubleshooting

### Ảnh không load được

**1. Kiểm tra URL**
```bash
# Test URL trong browser hoặc curl
curl -I https://example.com/image.png
```

**2. Kiểm tra permission**
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET"/>
```

**3. Kiểm tra network**
```kotlin
// Thêm cleartext traffic nếu dùng HTTP (không khuyến nghị)
// AndroidManifest.xml
android:usesCleartextTraffic="true"
```

**4. Xem log**
```bash
adb logcat | grep -E "ImageLoader|JsonAnimatedSprite"
```

### Cache quá lớn

```kotlin
// Clear cache định kỳ
ImageLoader.clearCache(context)

// Hoặc clear cache khi app start
override fun onCreate() {
    super.onCreate()
    ImageLoader.clearCache(this)
}
```

### Timeout

Tăng timeout trong `ImageLoader.kt`:
```kotlin
connection.connectTimeout = 30000  // 30 giây
connection.readTimeout = 30000     // 30 giây
```

## Migration từ drawable sang URL

### Bước 1: Upload ảnh lên server/CDN
```bash
# Ví dụ: Upload lên GitHub
git add assets/*.png
git commit -m "Add sprite images"
git push
```

### Bước 2: Lấy URL
```
https://raw.githubusercontent.com/username/repo/main/assets/idle_1.png
```

### Bước 3: Cập nhật JSON
```json
{
  "url": "https://raw.githubusercontent.com/username/repo/main/assets/idle_1.png"
}
```

### Bước 4: Test
```bash
./gradlew clean build
./gradlew installDebug
adb logcat | grep ImageLoader
```

## Lợi ích

### Sử dụng URL
- ✅ Không cần rebuild app khi thay đổi ảnh
- ✅ Giảm kích thước APK
- ✅ Dễ dàng update ảnh từ xa
- ✅ Có thể A/B test các ảnh khác nhau
- ❌ Cần internet lần đầu
- ❌ Chậm hơn drawable local

### Sử dụng Drawable
- ✅ Nhanh, không cần internet
- ✅ Luôn available
- ❌ Tăng kích thước APK
- ❌ Cần rebuild để thay đổi

## Kết luận

Hệ thống giờ linh hoạt hơn, bạn có thể:
1. Dùng drawable cho ảnh quan trọng, cần load nhanh
2. Dùng URL cho ảnh có thể thay đổi, không quan trọng
3. Mix cả hai tùy nhu cầu

---

**Happy coding! 🚀**
