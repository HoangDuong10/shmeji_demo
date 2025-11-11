# 🎬 Impact Animation Update

## Yêu cầu
1. **Ban đầu rơi xuống** → Impact animation → Custom animation
2. **Khi kéo thả** → Impact animation → Walking animation

## Đã thực hiện ✅

### 1. Thêm Impact Animation vào JSON
File: `app/src/main/assets/data.json`

```json
"impact": {
  "thumb": "impact.png",
  "frames": [
    { "id": 1, "url": "vampire_impact_1" },
    { "id": 2, "url": "vampire_impact_2" },
    { "id": 3, "url": "vampire_impact_3" }
  ],
  "logic": [
    {
      "frame": [1, 2, 3],
      "delay": 167,
      "sequence": 1
    }
  ]
}
```

**Logic**: 
- Frame 1 → 2 → 3
- Delay: 167ms mỗi frame
- Chạy 1 lần rồi kết thúc
- Tổng thời gian: 3 frames × 167ms = 501ms

### 2. Cập nhật AnimationMapper
File: `app/src/main/java/com/example/demoshemij/domain/AnimationMapper.kt`

```kotlin
SpriteState1.Bottom -> "impact"  // ✅ Đổi từ "falling" sang "impact"
```

### 3. Cập nhật JsonAnimatedSprite
File: `app/src/main/java/com/example/demoshemij/component/JsonAnimatedSprite.kt`

Thêm logic tự động tính thời gian impact animation và gọi `onImpactFinish()` khi xong.

### 4. Cập nhật DrawableChecker
File: `app/src/main/java/com/example/demoshemij/util/DrawableChecker.kt`

Thêm 3 impact frames vào danh sách kiểm tra (tổng 40 drawables).

## Flow hoạt động

### A. Ban đầu (isInitial = true)
```
1. Sprite xuất hiện ở top
2. FALL animation (rơi xuống)
3. BOTTOM (impact) animation → 501ms
4. onImpactFinish() được gọi
5. FloatingService set state = CUSTOM
6. CUSTOM animation chạy → 4250ms (17 frames × 250ms)
7. onCustomAnimationFinished() được gọi
8. FloatingService set state = WALKING
9. Sprite bắt đầu di chuyển
```

### B. Khi kéo thả (isInitial = false)
```
1. User kéo sprite
2. Touch animation hiển thị
3. User thả sprite
4. FALL animation (rơi xuống)
5. BOTTOM (impact) animation → 501ms
6. onImpactFinish() được gọi
7. FloatingService kiểm tra !isDragging
8. animateSpriteWindow() được gọi
9. Sprite tiếp tục di chuyển với WALKING animation
```

## Code trong FloatingService

Logic hiện tại đã đúng:

```kotlin
@Composable
fun SpriteContent(...) {
    JsonAnimatedSprite(
        spriteState = spriteState,
        spriteFlip = spriteFlip,
        onCustomAnimationFinished = {
            // Sau custom → chuyển sang walking
            instance?.controller?.setState(SpriteState1.WALKING)
            instance?.let { resumeSpriteAnimation(it) }
        },
        onImpactFinish = {
            instance?.moveJob?.cancel()
            instance?.moveJob = lifecycleScope.launch {
                if (isInitial) {
                    // Ban đầu: impact → custom
                    instance?.controller?.setState(SpriteState1.CUSTOM)
                } else {
                    // Kéo thả: impact → walking
                    if (instance?.isDragging == false) {
                        animateSpriteWindow(instance)
                    }
                }
            }
        },
        characterName = "goku"
    )
}
```

## Timeline chi tiết

### Ban đầu
```
0ms     : Sprite xuất hiện
0-500ms : FALL animation (rơi xuống)
500ms   : Chạm đất
500-1001ms : IMPACT animation (3 frames × 167ms)
1001ms  : onImpactFinish() → set CUSTOM
1001-5251ms : CUSTOM animation (17 frames × 250ms)
5251ms  : onCustomAnimationFinished() → set WALKING
5251ms+ : WALKING animation (vô hạn)
```

### Kéo thả
```
0ms     : User bắt đầu kéo
0-Xms   : TOUCH animation
Xms     : User thả
X-Y ms  : FALL animation
Yms     : Chạm đất
Y-Y+501ms : IMPACT animation
Y+501ms : onImpactFinish() → animateSpriteWindow()
Y+501ms+: WALKING animation
```

## Test

### 1. Test ban đầu
```bash
# Chạy app
# Nhấn "Thêm Sprite"
# Quan sát:
# 1. Sprite rơi xuống (FALL)
# 2. Impact 3 frames (IMPACT)
# 3. Custom animation 17 frames (CUSTOM)
# 4. Đi bộ (WALKING)
```

### 2. Test kéo thả
```bash
# Kéo sprite lên cao
# Thả ra
# Quan sát:
# 1. Sprite rơi xuống (FALL)
# 2. Impact 3 frames (IMPACT)
# 3. Đi bộ tiếp (WALKING)
```

### 3. Xem log
```bash
adb logcat | grep -E "AnimationController|JsonAnimatedSprite"
```

Kết quả mong đợi:
```
D/AnimationController: Frame: 1, Delay: 167ms, Repeat: 1/1  # Impact frame 1
D/AnimationController: Frame: 2, Delay: 167ms, Repeat: 1/1  # Impact frame 2
D/AnimationController: Frame: 3, Delay: 167ms, Repeat: 1/1  # Impact frame 3
D/JsonAnimatedSprite: Impact animation finished, calling onImpactFinish
```

## Tùy chỉnh

### Thay đổi tốc độ impact
Trong `data.json`:
```json
{
  "frame": [1, 2, 3],
  "delay": 100,  // Nhanh hơn (từ 167ms → 100ms)
  "sequence": 1
}
```

### Thêm frames impact
```json
{
  "frame": [1, 2, 3, 2, 3],  // Lặp lại frame 2-3
  "delay": 167,
  "sequence": 1
}
```

### Lặp impact nhiều lần
```json
{
  "frame": [1, 2, 3],
  "delay": 167,
  "sequence": 2  // Lặp 2 lần
}
```

## Files đã cập nhật

1. ✅ `app/src/main/assets/data.json` - Thêm impact animation
2. ✅ `AnimationMapper.kt` - Map Bottom → impact
3. ✅ `JsonAnimatedSprite.kt` - Xử lý impact finish
4. ✅ `DrawableChecker.kt` - Thêm impact frames vào check list

## Lưu ý

- Impact animation chỉ chạy 1 lần (sequence: 1)
- Tổng thời gian: 501ms (3 frames × 167ms)
- Sau impact, tự động chuyển sang animation tiếp theo
- Logic trong FloatingService không cần thay đổi

---

**Sẵn sàng test! 🚀**
