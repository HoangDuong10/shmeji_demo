# Các bước chuyển đổi sang Animation System mới

## 1. Sync Gradle Dependencies

Sau khi đã thêm Gson vào `app/build.gradle.kts`, chạy:
```bash
./gradlew build
```

Hoặc trong Android Studio: **File → Sync Project with Gradle Files**

## 2. Di chuyển file data.json

Đảm bảo file `data.json` nằm đúng vị trí:
```
app/src/main/assets/data.json
```

Hiện tại file đang ở: `app/src/assets/data.json` (SAI)

**Cần di chuyển sang**: `app/src/main/assets/data.json` (ĐÚNG)

## 3. Test Animation System

### Option A: Sử dụng AnimationTestScreen

Thêm vào MainActivity hoặc navigation:
```kotlin
AnimationTestScreen()
```

### Option B: Tích hợp vào code hiện tại

Thay thế `SpriteState` bằng `AnimationController`:

**Trước:**
```kotlin
val spriteState = rememberSpriteState(
    totalFrames = 6,
    framesPerRow = 3,
    animationSpeed = 333L
)
spriteState.start()
```

**Sau:**
```kotlin
val controller = rememberAnimationController(
    context = LocalContext.current,
    characterName = "goku",
    animationName = "idle"
)

LaunchedEffect(controller) {
    controller?.start()
}

val currentFrameId by controller.currentFrameId.collectAsState()
```

## 4. Cập nhật SpriteView (nếu cần)

Nếu bạn muốn sử dụng `AnimationController` với `SpriteView` hiện tại, cần tạo adapter:

```kotlin
@Composable
fun AnimatedSpriteView(
    controller: AnimationController,
    spriteSpec: SpriteSpec,
    modifier: Modifier = Modifier
) {
    val currentFrameId by controller.currentFrameId.collectAsState()
    
    // Convert frame ID to sprite sheet position
    // Cần logic để map frame ID sang row/column trong sprite sheet
    
    // Sau đó dùng SpriteView như bình thường
}
```

## 5. Kiểm tra Log

Khi chạy animation, bạn sẽ thấy log như:
```
D/AnimationController: Frame: 1, Delay: 333ms, Repeat: 1/∞
D/AnimationController: Frame: 2, Delay: 333ms, Repeat: 1/∞
```

## 6. Các file đã tạo

✅ `AnimationController.kt` - Controller chính
✅ `JsonLoader.kt` - Load JSON data
✅ `AnimationExample.kt` - Ví dụ và helpers
✅ `AnimationTestScreen.kt` - Screen để test
✅ `ANIMATION_GUIDE.md` - Hướng dẫn chi tiết

## 7. Lưu ý quan trọng

### Về cấu trúc JSON:
- `delay` tính bằng milliseconds
- `sequence: "infinity"` = lặp vô hạn
- `sequence: 1` = chạy 1 lần rồi chuyển bước tiếp theo

### Về logic animation:
- Các bước trong `logic` chạy tuần tự
- Khi gặp `"infinity"`, sẽ lặp mãi ở bước đó
- Nếu không có `"infinity"`, sẽ lặp lại từ đầu

### Ví dụ CUSTOM animation:
```json
"logic": [
  { "frame": [1,2,3,4,3,4], "delay": 167, "sequence": 1 },
  { "frame": [5], "delay": 83, "sequence": 1 },
  { "frame": [6,7], "delay": 167, "sequence": "infinity" }
]
```

Chạy như sau:
1. 1→2→3→4→3→4 (167ms, 1 lần)
2. 5 (83ms, 1 lần)
3. 6→7→6→7... (167ms, vô hạn)

## 8. Troubleshooting

### Lỗi: "data.json not found"
→ Kiểm tra file nằm trong `app/src/main/assets/`

### Lỗi: "Gson not found"
→ Chạy Gradle sync lại

### Animation không chạy
→ Kiểm tra log để xem controller có start không
→ Đảm bảo `controller?.start()` được gọi trong `LaunchedEffect`

### Frame không đúng
→ Kiểm tra `frame` array trong JSON có đúng ID không
→ Kiểm tra `frames` có đủ frame với ID tương ứng không
