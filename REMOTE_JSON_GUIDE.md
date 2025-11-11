# 🌐 Hướng dẫn sử dụng JSON từ URL

## Tổng quan

Hệ thống giờ load JSON từ URL thay vì file local:
- **URL**: `https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/data.json`
- **Fallback**: `app/src/main/assets/data.json` (nếu URL fail)

## Cách hoạt động

### Priority loading:
```
1. Memory cache (nhanh nhất)
   ↓ (nếu không có)
2. URL với disk cache (cache 1 giờ)
   ↓ (nếu fail)
3. Local assets (fallback)
```

### Timeline:
```
Lần đầu:
- Download từ URL: ~1-3s
- Parse JSON: ~100ms
- Save to cache: ~50ms
- Total: ~1-3s

Lần sau (trong 1 giờ):
- Load từ disk cache: ~50ms
- Parse JSON: ~100ms
- Total: ~150ms

Sau 1 giờ:
- Re-download từ URL: ~1-3s
- Update cache
```

## Lợi ích

### ✅ Advantages
1. **Update từ xa**: Thay đổi JSON trên server → App tự động update
2. **Không cần rebuild**: Thêm character, animation mới mà không cần build lại
3. **A/B Testing**: Có thể test nhiều config khác nhau
4. **Giảm kích thước APK**: JSON không nằm trong APK
5. **Centralized**: Một JSON cho tất cả users

### ❌ Considerations
1. **Cần internet lần đầu**: Nếu không có mạng, dùng fallback local
2. **Latency**: Lần đầu chậm hơn ~1-3s
3. **Dependency**: Phụ thuộc vào server availability

## API

### Load JSON
```kotlin
// Trong coroutine
val data = JsonLoader.loadCharacterData(context)
```

### Force reload (bỏ qua cache)
```kotlin
// Trong coroutine
val data = JsonLoader.forceReload(context)
```

### Clear cache
```kotlin
JsonLoader.clearCache(context)
```

### Check cache
```kotlin
val hasCached = JsonLoader.hasCachedData()
```

## Cấu trúc JSON trên server

File: `https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/data.json`

```json
{
  "characters": [
    {
      "goku": {
        "folder": "goku",
        "thumbnail": "https://example.com/goku.png",
        "overflow": {
          "horizontal": 0.43,
          "vertical": 0.43
        },
        "animations": {
          "idle": {
            "thumb": "https://example.com/idle.png",
            "frames": [
              {
                "id": 1,
                "url": "https://example.com/idle_1.png"
              },
              {
                "id": 2,
                "url": "https://example.com/idle_2.png"
              }
            ],
            "logic": [
              {
                "frame": [1, 2],
                "delay": 333,
                "sequence": "infinity"
              }
            ]
          }
        }
      }
    }
  ]
}
```

## Update JSON trên server

### Bước 1: Chỉnh sửa JSON
```bash
# Edit file local
vim data.json

# Hoặc dùng editor online
```

### Bước 2: Upload lên server
```bash
# Ví dụ với DigitalOcean Spaces
s3cmd put data.json s3://wallpaperhd/Shimeji/data.json --acl-public

# Hoặc dùng web interface
```

### Bước 3: Test
```bash
# Kiểm tra URL
curl https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/data.json

# Trong app, force reload
JsonLoader.forceReload(context)
```

### Bước 4: Users tự động update
- Sau 1 giờ, cache hết hạn
- App tự động download JSON mới
- Không cần update app

## Cache Management

### Cache location
```
Memory: RAM (cleared khi app restart)
Disk: /data/data/com.example.demoshemij/cache/json/data.json
```

### Cache duration
```
1 giờ (3600 seconds)
```

### Clear cache
```bash
# Từ adb
adb shell rm /data/data/com.example.demoshemij/cache/json/data.json

# Từ code
JsonLoader.clearCache(context)
```

### Check cache
```bash
# Xem cache file
adb shell ls -la /data/data/com.example.demoshemij/cache/json/

# Xem nội dung
adb shell cat /data/data/com.example.demoshemij/cache/json/data.json
```

## Debug

### Xem log loading
```bash
adb logcat | grep JsonLoader
```

### Output mẫu:
```
D/JsonLoader: Using memory cache
D/JsonLoader: Loading from disk cache (age: 300s)
D/JsonLoader: Downloading JSON from: https://...
D/JsonLoader: JSON downloaded, length: 15234
D/JsonLoader: JSON saved to disk cache
D/JsonLoader: JSON parsed, characters count: 2
E/JsonLoader: Failed to load JSON from URL
W/JsonLoader: Failed to load from URL, falling back to local assets
```

## Error Handling

### Scenario 1: URL không accessible
```
1. Try load từ URL → Fail
2. Log error
3. Fallback về local assets
4. App vẫn hoạt động bình thường
```

### Scenario 2: JSON format sai
```
1. Download thành công
2. Parse fail
3. Log error
4. Fallback về local assets
```

### Scenario 3: Không có internet
```
1. Try load từ URL → Timeout
2. Check disk cache → Load từ cache
3. Nếu không có cache → Fallback về local assets
```

## Testing

### Test 1: Load lần đầu
```bash
# Clear cache
adb shell rm -rf /data/data/com.example.demoshemij/cache/json/

# Restart app
adb shell am force-stop com.example.demoshemij
adb shell am start -n com.example.demoshemij/.MainActivity

# Xem log
adb logcat | grep JsonLoader
# Expected: "Downloading JSON from: https://..."
```

### Test 2: Load từ cache
```bash
# Restart app (cache còn)
adb shell am force-stop com.example.demoshemij
adb shell am start -n com.example.demoshemij/.MainActivity

# Xem log
adb logcat | grep JsonLoader
# Expected: "Loading from disk cache"
```

### Test 3: Fallback
```bash
# Disable internet
adb shell svc wifi disable
adb shell svc data disable

# Clear cache
adb shell rm -rf /data/data/com.example.demoshemij/cache/json/

# Restart app
adb shell am force-stop com.example.demoshemij
adb shell am start -n com.example.demoshemij/.MainActivity

# Xem log
adb logcat | grep JsonLoader
# Expected: "Failed to load from URL, falling back to local assets"
```

### Test 4: Force reload
```kotlin
// Trong app, thêm button
Button(onClick = {
    lifecycleScope.launch {
        JsonLoader.forceReload(context)
        // Reload UI
    }
}) {
    Text("Force Reload JSON")
}
```

## Best Practices

### 1. Validate JSON trước khi upload
```bash
# Dùng jsonlint
jsonlint data.json

# Hoặc online
# https://jsonlint.com
```

### 2. Versioning
```json
{
  "version": "1.0.0",
  "lastUpdated": "2024-01-15T10:30:00Z",
  "characters": [...]
}
```

### 3. Backup
```bash
# Luôn giữ backup local
cp data.json data.json.backup

# Hoặc version control
git add data.json
git commit -m "Update animations"
```

### 4. Monitoring
```kotlin
// Log version khi load
val data = JsonLoader.loadCharacterData(context)
Log.d("App", "JSON version: ${data?.version}")
```

## Migration từ local sang remote

### Bước 1: Upload JSON lên server
```bash
# Upload file
s3cmd put app/src/main/assets/data.json s3://bucket/data.json --acl-public
```

### Bước 2: Verify URL
```bash
curl https://your-url.com/data.json
```

### Bước 3: Update code (đã làm)
```kotlin
// JsonLoader.kt
private const val JSON_URL = "https://your-url.com/data.json"
```

### Bước 4: Test
```bash
./gradlew clean build
./gradlew installDebug
adb logcat | grep JsonLoader
```

### Bước 5: Keep local fallback
```
Giữ file app/src/main/assets/data.json
Để fallback khi không có internet
```

## Troubleshooting

### JSON không load được
```bash
# 1. Check URL
curl -I https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/data.json

# 2. Check internet
adb shell ping -c 3 8.8.8.8

# 3. Check log
adb logcat | grep -E "JsonLoader|Exception"

# 4. Clear cache và retry
adb shell rm -rf /data/data/com.example.demoshemij/cache/json/
```

### Cache không update
```kotlin
// Force reload
JsonLoader.forceReload(context)

// Hoặc clear cache
JsonLoader.clearCache(context)
```

### Timeout
```kotlin
// Tăng timeout trong JsonLoader.kt
connection.connectTimeout = 30000  // 30s
connection.readTimeout = 30000     // 30s
```

## Files đã cập nhật

1. ✅ `JsonLoader.kt` - Load từ URL với caching
2. ✅ `AnimationMapper.kt` - Suspend function
3. ✅ `OverflowHelper.kt` - Suspend function
4. ✅ `JsonAnimatedSprite.kt` - Load controller trong coroutine

---

**Giờ JSON được load từ server! 🌐**
