# Hướng Dẫn Test App Sau Migration

## Checklist Test

### 1. Test Preload Screen
- [ ] App khởi động hiển thị màn hình preload
- [ ] Progress bar chạy từ 0% → 100%
- [ ] Hiển thị text "Loading JSON..." → "Loading image X/Y"
- [ ] Có nút "Skip" để bỏ qua
- [ ] Sau khi complete, tự động chuyển sang màn hình chính

### 2. Test Load JSON
- [ ] JSON được load từ URL: `https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/data_test.json`
- [ ] Kiểm tra logs: `JsonLoader: JSON downloaded, length: XXX`
- [ ] Nếu URL fail, fallback về local assets
- [ ] JSON được cache trong disk

### 3. Test Load Images
- [ ] Ảnh được load từ URL với format: `{BASE_URL}{folder}/{filename}`
- [ ] Ví dụ: `https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/vampire/idle1.png`
- [ ] Kiểm tra logs: `ImageLoader: Downloading image: XXX`
- [ ] Ảnh được cache trong disk
- [ ] Lần load thứ 2 nhanh hơn (từ cache)

### 4. Test Character Animations
- [ ] Bật Vampire → sprite hiển thị đúng
- [ ] Bật Goku → sprite hiển thị đúng
- [ ] Animations chạy mượt mà
- [ ] Không có placeholder màu xám (tức là ảnh đã load)

### 5. Test Offline Mode
- [ ] Tắt internet
- [ ] Restart app
- [ ] App vẫn hoạt động bình thường (dùng cache)
- [ ] Sprites vẫn hiển thị đúng

### 6. Test Cache
- [ ] Kiểm tra thư mục cache: `{app_cache_dir}/images/`
- [ ] Kiểm tra JSON cache: `{app_cache_dir}/json/data.json`
- [ ] Clear cache và test lại

## Logs Cần Kiểm Tra

### JsonLoader Logs
```
JsonLoader: Downloading JSON from: https://...
JsonLoader: JSON downloaded, length: 12345
JsonLoader: JSON parsed, characters count: 2
JsonLoader: JSON saved to disk cache
```

### ImageLoader Logs
```
ImageLoader: Downloading image: https://...
ImageLoader: Image loaded successfully: https://...
ImageLoader: Loading from disk cache: https://...
```

### PreloadManager Logs
```
PreloadManager: Total images to preload: 50
PreloadManager: Preloaded: https://...
PreloadManager: Preload completed successfully
```

### JsonAnimatedSprite Logs
```
JsonAnimatedSprite: [vampire] Loading image from: https://...
JsonAnimatedSprite: [vampire] State: WALKING, Frame ID: 1, URL: walking1.png
```

## Test Commands

### Xem Logs
```bash
adb logcat | grep -E "JsonLoader|ImageLoader|PreloadManager|JsonAnimatedSprite"
```

### Clear Cache
```bash
adb shell pm clear com.example.demoshemij
```

### Check Cache Size
```bash
adb shell du -sh /data/data/com.example.demoshemij/cache
```

## Expected Results

### Lần Đầu (No Cache)
- Preload: ~10-30 giây (tùy network)
- Load JSON: ~1-2 giây
- Load mỗi ảnh: ~0.5-1 giây
- Total: ~15-40 giây

### Lần Sau (With Cache)
- Preload: ~1-2 giây (chỉ check cache)
- Load JSON: instant (từ memory cache)
- Load ảnh: instant (từ disk cache)
- Total: ~1-3 giây

## Troubleshooting

### Ảnh không hiển thị
1. Check logs xem có error không
2. Check URL có đúng không
3. Check internet connection
4. Try clear cache và reload

### JSON không load
1. Check URL có accessible không (test trong browser)
2. Check JSON format có đúng không
3. Check logs trong JsonLoader
4. Verify fallback về local assets

### App crash khi start
1. Check AndroidManifest có INTERNET permission không
2. Check logs để xem error
3. Try disable preload (comment PreloadWrapper)

### Preload quá lâu
1. Check network speed
2. Check số lượng ảnh cần load
3. Có thể skip preload
4. Lần sau sẽ nhanh hơn nhờ cache

## Performance Metrics

### Target Performance
- First load: < 30s
- Cached load: < 3s
- Animation FPS: 60fps
- Memory usage: < 100MB

### Monitoring
```bash
# CPU usage
adb shell top | grep demoshemij

# Memory usage
adb shell dumpsys meminfo com.example.demoshemij

# Network usage
adb shell dumpsys netstats | grep demoshemij
```

## Success Criteria

✅ App khởi động thành công
✅ JSON load từ URL
✅ Tất cả ảnh load từ URL
✅ Animations chạy mượt
✅ Cache hoạt động đúng
✅ Offline mode hoạt động
✅ Không có crash hoặc ANR
✅ Performance đạt target

## Next Steps

Sau khi test thành công:
1. Deploy JSON lên server production
2. Upload tất cả ảnh lên server
3. Update URL trong code
4. Test lại với production URLs
5. Release app
