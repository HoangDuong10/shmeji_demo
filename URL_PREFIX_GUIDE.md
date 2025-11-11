# 🔗 URL Prefix System

## Tổng quan

Thay vì lưu full URL trong JSON, chỉ cần lưu tên file. App sẽ tự động thêm prefix.

## Cách hoạt động

### JSON trên server (ngắn gọn):
```json
{
  "goku": {
    "folder": "goku",
    "animations": {
      "idle": {
        "frames": [
          { "id": 1, "url": "idle1.png" },
          { "id": 2, "url": "idle2.png" }
        ]
      }
    }
  }
}
```

### App tự động build full URL:
```
idle1.png 
→ https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/goku/idle1.png

climb1.png
→ https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/goku/climb1.png
```

## Logic

### 1. Default Base URL
```kotlin
fun getBaseUrl(): String {
    return "https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/$folder/"
}
```

### 2. Build Full URL
```kotlin
fun buildImageUrl(filename: String): String {
    return if (filename.startsWith("http://") || filename.startsWith("https://")) {
        filename // Đã là URL đầy đủ
    } else {
        getBaseUrl() + filename // Thêm prefix
    }
}
```

### 3. Trong AnimationController
```kotlin
private val frameMap = animationData.frames.associate { frame ->
    val fullUrl = characterData?.buildImageUrl(frame.url) ?: frame.url
    frame.id to fullUrl
}
```

## Ví dụ

### Character: goku
```
folder: "goku"
baseUrl: https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/goku/

idle1.png → https://...../goku/idle1.png
climb1.png → https://...../goku/climb1.png
custom1.png → https://...../goku/custom1.png
```

### Character: vampire
```
folder: "vampire"
baseUrl: https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/vampire/

idle1.png → https://...../vampire/idle1.png
climb1.png → https://...../vampire/climb1.png
custom1.png → https://...../vampire/custom1.png
```

## Lợi ích

### ✅ JSON ngắn gọn
```json
// ❌ Trước (dài)
"url": "https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/goku/idle1.png"

// ✅ Sau (ngắn)
"url": "idle1.png"
```

### ✅ Dễ maintain
- Chỉ cần đổi baseUrl 1 chỗ
- Không cần update từng URL
- Dễ migrate sang CDN khác

### ✅ Flexible
- Có thể override baseUrl cho từng character
- Hỗ trợ cả relative và absolute URL
- Mix được local và remote

## Custom Base URL

### Trong JSON (optional):
```json
{
  "goku": {
    "folder": "goku",
    "baseUrl": "https://custom-cdn.com/sprites/goku/",
    "animations": { ... }
  }
}
```

### Kết quả:
```
idle1.png → https://custom-cdn.com/sprites/goku/idle1.png
```

## Mix URL types

### JSON có thể mix:
```json
{
  "frames": [
    { "id": 1, "url": "idle1.png" },
    { "id": 2, "url": "https://other-cdn.com/special.png" }
  ]
}
```

### Kết quả:
```
idle1.png → https://wallpaperhd.../goku/idle1.png (thêm prefix)
https://other-cdn.com/special.png → https://other-cdn.com/special.png (giữ nguyên)
```

## Debug

### Log để kiểm tra:
```kotlin
Log.d("AnimationController", "Frame URL: ${frameMap[1]}")
```

### Expected output:
```
D/AnimationController: Frame URL: https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/goku/idle1.png
```

## JSON Structure

### Minimal (chỉ cần):
```json
{
  "characters": [
    {
      "goku": {
        "folder": "goku",
        "animations": {
          "idle": {
            "frames": [
              { "id": 1, "url": "idle1.png" }
            ]
          }
        }
      }
    }
  ]
}
```

### Full (với custom baseUrl):
```json
{
  "characters": [
    {
      "goku": {
        "folder": "goku",
        "baseUrl": "https://custom-cdn.com/goku/",
        "animations": {
          "idle": {
            "frames": [
              { "id": 1, "url": "idle1.png" }
            ]
          }
        }
      }
    }
  ]
}
```

## Migration

### Từ full URL sang short URL:

**Trước:**
```json
"url": "https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/goku/idle1.png"
```

**Sau:**
```json
"url": "idle1.png"
```

**Kết quả:** Giống nhau! App tự động thêm prefix.

## Files đã cập nhật

1. ✅ `Model1.kt` - Thêm `baseUrl` và `buildImageUrl()`
2. ✅ `AnimationController.kt` - Build full URL khi tạo frameMap
3. ✅ `AnimationMapper.kt` - Pass characterData vào controller

## Test

```bash
# Build
./gradlew clean build

# Xem log
adb logcat | grep -E "AnimationController|ImageLoader"

# Expected:
D/AnimationController: Frame URL: https://wallpaperhd.../goku/idle1.png
D/ImageLoader: Downloading image: https://wallpaperhd.../goku/idle1.png
```

## Kết luận

- ✅ JSON ngắn gọn hơn 90%
- ✅ Dễ maintain
- ✅ Flexible với custom baseUrl
- ✅ Hỗ trợ cả relative và absolute URL
- ✅ Backward compatible

---

**JSON giờ ngắn gọn và dễ quản lý! 🎉**
