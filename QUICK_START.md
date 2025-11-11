# Quick Start Guide - Load Từ URL

## Tóm Tắt Nhanh

App đã được cập nhật để load JSON và ảnh từ URL thay vì local. Đây là những gì bạn cần biết:

## URLs

### JSON
```
https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/data_test.json
```

### Images
```
https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/{folder}/{filename}
```

Ví dụ:
- `https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/vampire/idle1.png`
- `https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/goku/walking2.png`

## Cách Hoạt Động

1. **App Start**: Hiển thị preload screen, download JSON và tất cả ảnh
2. **Caching**: Tất cả được cache trong disk để dùng offline
3. **Lần Sau**: Load nhanh từ cache

## Thay Đổi URLs

Nếu muốn đổi URL, sửa trong `JsonLoader.kt`:

```kotlin
const val JSON_URL = "your_json_url_here"
const val BASE_IMAGE_URL = "your_base_image_url_here"
```

## JSON Format

```json
{
  "characters": [
    {
      "vampire": {
        "folder": "vampire",
        "thumbnail": "vampire.png",
        "animations": {
          "idle": {
            "thumb": "idle.png",
            "frames": [
              {"id": 1, "url": "idle1.png"},
              {"id": 2, "url": "idle2.png"}
            ],
            "logic": [
              {"frame": [1, 2], "delay": 333, "sequence": 0}
            ]
          }
        }
      }
    }
  ]
}
```

## Test Nhanh

1. Build và run app
2. Xem preload screen (progress 0-100%)
3. Bật vampire hoặc goku
4. Kiểm tra animations chạy đúng
5. Tắt internet, restart app → vẫn hoạt động (cache)

## Troubleshooting

### Ảnh không hiển thị?
- Check internet connection
- Check logs: `adb logcat | grep ImageLoader`
- Try clear cache: `adb shell pm clear com.example.demoshemij`

### JSON không load?
- Check URL trong browser
- Check logs: `adb logcat | grep JsonLoader`
- Verify JSON format

### App chậm?
- Lần đầu sẽ chậm (download)
- Lần sau nhanh (cache)
- Có thể skip preload

## Files Quan Trọng

- `JsonLoader.kt` - Load JSON từ URL
- `ImageLoader.kt` - Load ảnh từ URL
- `PreloadManager.kt` - Preload system
- `JsonAnimatedSprite.kt` - Display sprite từ URL

## Documentation

- `URL_MIGRATION_GUIDE.md` - Chi tiết đầy đủ
- `TEST_INSTRUCTIONS.md` - Hướng dẫn test
- `MIGRATION_SUMMARY.md` - Tóm tắt thay đổi

## Done!

Vậy là xong! App giờ load tất cả từ URL với caching. Chỉ cần upload JSON và ảnh lên server là có thể update content mà không cần rebuild app.
