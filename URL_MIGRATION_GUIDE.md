# Hướng Dẫn Migration: Load Dữ Liệu Từ URL

## Tổng Quan

App đã được cập nhật để load JSON và ảnh từ URL thay vì local assets/drawables.

## Cấu Hình URL

### JSON URL
```kotlin
const val JSON_URL = "https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/data_test.json"
```

### Base Image URL
```kotlin
const val BASE_IMAGE_URL = "https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/"
```

### Format URL Ảnh
Ảnh sẽ được load theo format:
```
{BASE_IMAGE_URL}{folder}/{filename}
```

Ví dụ:
```
https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/vampire/climb1.png
https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/goku/idle1.png
```

## Cấu Trúc JSON

JSON phải có cấu trúc như sau:

```json
{
  "characters": [
    {
      "vampire": {
        "folder": "vampire",
        "thumbnail": "vampire.png",
        "overflow": {
          "horizontal": 0.333,
          "vertical": 0.333
        },
        "animations": {
          "idle": {
            "thumb": "idle.png",
            "frames": [
              {"id": 1, "url": "idle1.png"},
              {"id": 2, "url": "idle2.png"}
            ],
            "logic": [
              {
                "frame": [1, 2],
                "delay": 333,
                "sequence": 0
              }
            ]
          }
        }
      }
    }
  ]
}
```

### Giải Thích Các Trường

- **folder**: Tên thư mục chứa ảnh của nhân vật trên server
- **thumbnail**: Ảnh thumbnail của nhân vật
- **overflow**: Cấu hình độ tràn sprite ra ngoài màn hình
  - `horizontal`: Tỷ lệ tràn ngang (0.333 = 1/3 chiều rộng sprite)
  - `vertical`: Tỷ lệ tràn dọc (0.333 = 1/3 chiều cao sprite)
- **animations**: Danh sách animations
  - **thumb**: Ảnh thumbnail của animation
  - **frames**: Danh sách frames
    - `id`: ID của frame (bắt đầu từ 1)
    - `url`: Tên file ảnh (không bao gồm path)
  - **logic**: Logic animation
    - `frame`: Mảng các frame ID sẽ chạy
    - `delay`: Thời gian delay giữa các frame (ms)
    - `sequence`: Số lần lặp (0 hoặc "infinity" = lặp vô hạn)

## Các Thay Đổi Chính

### 1. JsonLoader
- Load JSON từ URL với caching
- Fallback về local assets nếu URL fail
- Memory cache và disk cache

### 2. ImageLoader
- Load ảnh từ URL với caching
- Hỗ trợ cả URL và drawable local
- Memory cache và disk cache

### 3. PreloadManager
- Preload tất cả ảnh khi app khởi động
- Hiển thị progress bar
- Cache để dùng offline

### 4. JsonAnimatedSprite
- Load ảnh từ URL thay vì drawable
- Tự động build full URL từ folder + filename
- Hiển thị placeholder khi đang load

## Flow Hoạt Động

1. **App Start**
   - Hiển thị PreloadScreen
   - Load JSON từ URL (hoặc cache)
   - Preload tất cả ảnh từ JSON
   - Hiển thị progress

2. **Khi Sprite Hiển Thị**
   - AnimationController đọc logic từ JSON
   - JsonAnimatedSprite load ảnh từ URL
   - Ảnh được cache trong memory và disk
   - Lần sau load nhanh hơn từ cache

3. **Offline Mode**
   - Nếu có cache, app vẫn hoạt động offline
   - JSON và ảnh đã được cache trong disk

## Cache Management

### JSON Cache
- Location: `{cacheDir}/json/data.json`
- Valid: 1 giờ
- Clear: `JsonLoader.clearCache(context)`

### Image Cache
- Location: `{cacheDir}/images/{md5_hash}`
- Valid: Vĩnh viễn (cho đến khi clear)
- Clear: `ImageLoader.clearCache(context)`

### Force Reload
```kotlin
// Force reload JSON
JsonLoader.forceReload(context)

// Reset preload
PreloadManager.reset()
```

## Testing

### Test Load JSON
```kotlin
val data = JsonLoader.loadCharacterData(context)
Log.d("Test", "Characters: ${data?.characters?.size}")
```

### Test Load Image
```kotlin
val imageUrl = "https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/vampire/idle1.png"
val bitmap = ImageLoader.loadImageFromUrl(context, imageUrl)
Log.d("Test", "Image loaded: ${bitmap != null}")
```

### Test Preload
```kotlin
PreloadManager.preloadAllImages(context)
```

## Troubleshooting

### Ảnh không hiển thị
1. Kiểm tra URL có đúng không
2. Kiểm tra INTERNET permission trong AndroidManifest
3. Kiểm tra network connection
4. Xem logs để debug

### JSON không load được
1. Kiểm tra JSON URL có accessible không
2. Kiểm tra format JSON có đúng không
3. Xem logs trong JsonLoader
4. Thử fallback về local assets

### App chậm khi start
1. Preload đang load nhiều ảnh
2. Có thể skip preload bằng nút "Skip"
3. Lần sau sẽ nhanh hơn nhờ cache

## Lưu Ý

1. **INTERNET Permission**: Đã được thêm vào AndroidManifest
2. **Caching**: Ảnh và JSON được cache tự động
3. **Offline**: App vẫn hoạt động offline nếu đã có cache
4. **Performance**: Lần đầu chậm, lần sau nhanh nhờ cache
5. **Error Handling**: Có fallback về local assets nếu URL fail

## Cấu Hình Tùy Chỉnh

### Thay đổi URL
Sửa trong `JsonLoader.kt`:
```kotlin
const val JSON_URL = "your_json_url_here"
const val BASE_IMAGE_URL = "your_base_image_url_here"
```

### Thay đổi Cache Duration
Sửa trong `JsonLoader.kt`:
```kotlin
// Cache valid trong 1 giờ
if (cacheAge < 3600000) { // Thay đổi số này
```

### Disable Preload
Xóa `PreloadWrapper` trong `MainActivity.kt`:
```kotlin
// Thay vì
PreloadWrapper {
    Scaffold { ... }
}

// Dùng
Scaffold { ... }
```

## Kết Luận

App đã được migrate hoàn toàn sang load dữ liệu từ URL. Tất cả ảnh và JSON đều được cache để tối ưu performance và hỗ trợ offline mode.
