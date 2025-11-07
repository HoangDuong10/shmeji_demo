# 🔧 Fix Applied - Ảnh không hiển thị

## Vấn đề
Ảnh không hiển thị, chỉ thấy icon lỗi (ic_menu_report_image)

## Nguyên nhân
**Character name không khớp giữa JSON và code:**
- Trong `data.json`: `"vampire": { ... }`
- Trong code: `characterName = "goku"`

→ AnimationMapper không tìm thấy character "goku" trong JSON!

## Giải pháp đã áp dụng

### 1. Sửa data.json
```json
// ❌ TRƯỚC
{
  "characters": [
    {
      "vampire": {  // ← SAI
        "folder": "goku",
        ...
      }
    }
  ]
}

// ✅ SAU
{
  "characters": [
    {
      "goku": {  // ← ĐÚNG
        "folder": "goku",
        ...
      }
    }
  ]
}
```

### 2. Thêm debug logs
Đã thêm log chi tiết vào:
- `JsonLoader.kt` - Kiểm tra JSON loading
- `AnimationMapper.kt` - Kiểm tra character/animation lookup
- `JsonAnimatedSprite.kt` - Kiểm tra frame rendering
- `DrawableChecker.kt` - Kiểm tra tất cả drawable resources

### 3. Thêm DrawableChecker
Tự động kiểm tra tất cả 37 drawable resources khi service khởi động.

## Test lại

### 1. Rebuild project
```bash
./gradlew clean build
./gradlew installDebug
```

### 2. Xem log
```bash
adb logcat -c
adb logcat | grep -E "DrawableChecker|JsonLoader|AnimationMapper|JsonAnimatedSprite"
```

### 3. Kết quả mong đợi

```
D/DrawableChecker: ✅ Found: vampire_idle_1 (ID: 2131165312)
D/DrawableChecker: ✅ Found: vampire_idle_2 (ID: 2131165313)
...
D/DrawableChecker: Found: 37 / 37

D/JsonLoader: JSON loaded, length: 8543
D/JsonLoader: JSON parsed, characters count: 1

D/AnimationMapper: Getting controller for state: Idle -> animation: idle
D/AnimationMapper: Character 'goku' found: true
D/AnimationMapper: Animation 'idle' found: true, frames: 2

D/JsonAnimatedSprite: Looking for drawable: vampire_idle_1 -> ID: 2131165312
D/JsonAnimatedSprite: State: Idle, Frame ID: 1, URL: vampire_idle_1, Drawable: 2131165312

D/AnimationController: Frame: 1, Delay: 333ms, Repeat: 1/∞
D/AnimationController: Frame: 2, Delay: 333ms, Repeat: 1/∞
```

## Nếu vẫn có vấn đề

### Kiểm tra drawable resources
Nếu log hiển thị:
```
D/DrawableChecker: ❌ Missing: vampire_idle_1
```

→ File drawable không tồn tại hoặc tên sai

**Giải pháp:**
1. Kiểm tra `app/src/main/res/drawable/vampire_idle_1.png`
2. Rebuild project
3. Invalidate Caches: `File → Invalidate Caches / Restart`

### Kiểm tra JSON syntax
Nếu log hiển thị:
```
E/JsonLoader: Failed to parse JSON
```

→ JSON có lỗi syntax

**Giải pháp:**
1. Copy nội dung `data.json`
2. Paste vào https://jsonlint.com
3. Fix lỗi syntax
4. Rebuild

### Fallback về code cũ
Nếu cần quay lại code cũ tạm thời:

```kotlin
// FloatingService.kt - SpriteContent
@Composable
fun SpriteContent(...) {
    // Dùng lại ShimejiSprite cũ
    ShimejiSprite(
        spriteState = spriteState,
        spriteFlip = spriteFlip,
        onCustomAnimationFinished = { onCustomAnimationFinished() },
        onImpactFinish = { ... }
    )
}
```

## Files đã sửa

1. ✅ `app/src/main/assets/data.json` - Sửa character name
2. ✅ `JsonLoader.kt` - Thêm debug logs
3. ✅ `AnimationMapper.kt` - Thêm debug logs
4. ✅ `JsonAnimatedSprite.kt` - Thêm debug logs
5. ✅ `DrawableChecker.kt` - NEW - Kiểm tra drawables
6. ✅ `FloatingService.kt` - Gọi DrawableChecker

## Tài liệu debug

- `DEBUG_STEPS.md` - Hướng dẫn debug chi tiết
- `debug_animation.sh` - Script xem log tự động

---

**Bây giờ build lại và test! 🚀**
