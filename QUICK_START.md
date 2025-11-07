# 🚀 Quick Start - Animation System Mới

## ✅ Đã tích hợp xong!

App của bạn đã được chuyển từ **hardcoded animations** sang **JSON-based animations**.

## 📦 Chỉ cần 2 bước

### 1. Sync Gradle
```bash
./gradlew build
```
Hoặc trong Android Studio: **File → Sync Project with Gradle Files**

### 2. Build & Run
- Build app
- Cấp quyền overlay
- Nhấn "Thêm Sprite"
- ✨ Sprite sẽ chạy với animations từ JSON!

## 🎯 Đã thay đổi gì?

### FloatingService.kt
```kotlin
// ❌ CŨ: ShimejiSprite (hardcoded)
ShimejiSprite(
    spriteState = spriteState,
    spriteFlip = spriteFlip,
    onCustomAnimationFinished = { ... }
)

// ✅ MỚI: JsonAnimatedSprite (từ JSON)
JsonAnimatedSprite(
    spriteState = spriteState,
    spriteFlip = spriteFlip,
    characterName = "goku",
    onCustomAnimationFinished = { ... }
)
```

### data.json
```json
{
  "animations": {
    "idle": {
      "frames": [
        { "id": 1, "url": "vampire_idle_1" },
        { "id": 2, "url": "vampire_idle_2" }
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
```

## 🎨 Chỉnh animation

### Thay đổi tốc độ
Mở `app/src/main/assets/data.json`:
```json
{
  "delay": 500  // Thay đổi từ 333 → 500 (chậm hơn)
}
```

### Thay đổi số lần lặp
```json
{
  "sequence": 3  // Lặp 3 lần thay vì "infinity"
}
```

### Thay đổi thứ tự frames
```json
{
  "frame": [1, 2, 3, 2, 1]  // Tùy chỉnh sequence
}
```

## 📊 Animations đã cấu hình

| Animation | Frames | Delay | Logic |
|-----------|--------|-------|-------|
| **idle** | 1→2 | 333ms | Lặp vô hạn |
| **hover** | 1→2→3→2 | 250ms | Lặp vô hạn |
| **walking** | 1→2 | 333ms | Lặp vô hạn |
| **falling** | 1→2 | 250ms | Lặp vô hạn |
| **climb** | 1→2→3→2 | 333ms | Lặp vô hạn |
| **dash** | 1-6 (1x) → 3-8 (∞) | 167ms | Intro + loop |
| **custom** | 1-17 | 250ms | Chạy 1 lần |

## 🔍 Debug

### Xem log
```bash
adb logcat | grep AnimationController
```

Output:
```
D/AnimationController: Frame: 1, Delay: 333ms, Repeat: 1/∞
D/AnimationController: Frame: 2, Delay: 333ms, Repeat: 1/∞
```

## 📁 Files quan trọng

### Core System
- `AnimationController.kt` - Quản lý animation logic
- `JsonAnimatedSprite.kt` - Component hiển thị sprite
- `AnimationMapper.kt` - Map state → animation name

### Configuration
- `app/src/main/assets/data.json` - Animation config
- `app/build.gradle.kts` - Gson dependency

### Integration
- `FloatingService.kt` - Đã tích hợp JsonAnimatedSprite

## 💡 Tips

### Thêm animation mới
1. Thêm drawable resources: `vampire_newaction_1.png`, `vampire_newaction_2.png`
2. Cập nhật `data.json`:
```json
"newaction": {
  "frames": [
    { "id": 1, "url": "vampire_newaction_1" },
    { "id": 2, "url": "vampire_newaction_2" }
  ],
  "logic": [
    { "frame": [1, 2], "delay": 200, "sequence": "infinity" }
  ]
}
```
3. Thêm mapping trong `AnimationMapper.kt`
4. Rebuild

### Test riêng animation
Sử dụng `AnimationTestScreen.kt`:
```kotlin
// Trong MainActivity
setContent {
    AnimationTestScreen()
}
```

## 🎉 Lợi ích

- ✅ Không cần rebuild để thay đổi animation
- ✅ Code ngắn gọn, dễ maintain
- ✅ Dễ thêm animations mới
- ✅ Non-developers có thể chỉnh JSON
- ✅ Log rõ ràng để debug

## 📚 Tài liệu đầy đủ

- `ANIMATION_GUIDE.md` - Hướng dẫn chi tiết về JSON format
- `TESTING_GUIDE.md` - Hướng dẫn test và troubleshooting
- `MIGRATION_STEPS.md` - Chi tiết các bước migration
- `SUMMARY.md` - Tóm tắt toàn bộ thay đổi

## ❓ Troubleshooting

### Ảnh không hiển thị
→ Kiểm tra tên trong JSON match với drawable: `vampire_idle_1`

### Animation không chạy
→ Xem log: `adb logcat | grep AnimationController`

### Gson error
→ Sync Gradle lại

---

**Sẵn sàng để test! 🚀**
