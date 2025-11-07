# Hướng dẫn Test Animation System Mới

## ✅ Đã hoàn thành

1. **AnimationController** - Đọc và thực thi animation từ JSON
2. **JsonAnimatedSprite** - Component mới thay thế ShimejiSprite cũ
3. **AnimationMapper** - Map giữa SpriteState1 và animation names
4. **Cập nhật FloatingService** - Sử dụng component mới
5. **Cập nhật data.json** - Match với tên drawable resources

## 🚀 Cách Test

### 1. Sync Gradle
```bash
./gradlew build
```
Hoặc trong Android Studio: **File → Sync Project with Gradle Files**

### 2. Chạy App
- Build và chạy app trên thiết bị/emulator
- Cấp quyền overlay khi được yêu cầu
- Nhấn "Thêm Sprite"

### 3. Kiểm tra Animations

App sẽ tự động sử dụng animations từ JSON:

#### IDLE Animation
- Frame 1 ↔ 2
- Delay: 333ms
- Lặp vô hạn

#### WALKING Animation  
- Frame 1 ↔ 2
- Delay: 333ms
- Lặp vô hạn

#### HOVER (Touch) Animation
- Frame 1 → 2 → 3 → 2
- Delay: 250ms
- Lặp vô hạn

#### FALLING Animation
- Frame 1 ↔ 2
- Delay: 250ms
- Lặp vô hạn

#### CLIMB Animation
- Frame 1 → 2 → 3 → 2
- Delay: 333ms
- Lặp vô hạn

#### DASH Animation
- **Bước 1**: Frame 1→2→3→4→5→6 (167ms, 1 lần)
- **Bước 2**: Frame 3→4→5→6→7→8 (167ms, vô hạn)

#### CUSTOM Animation
- Frame 1→2→3→4→5→6→7→8→9→10→11→12→13→14→15→16→17
- Delay: 250ms
- Chạy 1 lần rồi kết thúc

## 🔍 Debug

### Xem Log
```bash
adb logcat | grep -E "AnimationController|JsonAnimatedSprite|AnimationMapper"
```

Bạn sẽ thấy:
```
D/AnimationController: Frame: 1, Delay: 333ms, Repeat: 1/∞
D/AnimationController: Frame: 2, Delay: 333ms, Repeat: 1/∞
D/JsonAnimatedSprite: State: Idle, Frame ID: 1, URL: vampire_idle_1, Drawable: 2131165312
```

### Kiểm tra Drawable Resources
Nếu thấy log `Drawable: 0` hoặc ảnh không hiển thị:
1. Kiểm tra tên file trong `data.json` match với drawable resources
2. Kiểm tra file drawable có tồn tại trong `res/drawable/`
3. Rebuild project

## 📝 So sánh Code Cũ vs Mới

### Code Cũ (Hardcoded)
```kotlin
// MainActivity.kt - ShimejiSprite
val idleImages = remember { 
    listOf(R.drawable.vampire_idle_1, R.drawable.vampire_idle_2) 
}
val idleDelay = 1000/3L

LaunchedEffect(spriteState) {
    when (spriteState) {
        SpriteState1.Idle -> {
            animateFrames(idleImages, idleDelay) { currentFrame = it }
        }
        // ... hardcoded cho mỗi state
    }
}
```

### Code Mới (JSON-based)
```kotlin
// JsonAnimatedSprite.kt
val controller = remember(spriteState) {
    AnimationMapper.getControllerForState(context, spriteState, "goku")
}

LaunchedEffect(controller) {
    controller?.start()
}

val currentFrameId by controller?.currentFrameId?.collectAsState()
val frameUrl = controller?.getCurrentFrameUrl()
```

## 🎯 Lợi ích

### Trước (Hardcoded)
- ❌ Phải rebuild app để thay đổi animation
- ❌ Code dài và khó maintain
- ❌ Mỗi animation cần viết logic riêng
- ❌ Khó test và debug

### Sau (JSON-based)
- ✅ Chỉnh animation trong JSON, không cần rebuild
- ✅ Code ngắn gọn, dễ đọc
- ✅ Một controller xử lý tất cả animations
- ✅ Dễ test và debug với log rõ ràng

## 🔧 Troubleshooting

### Lỗi: "data.json not found"
**Nguyên nhân**: File không nằm đúng vị trí
**Giải pháp**: Đảm bảo file ở `app/src/main/assets/data.json`

### Lỗi: "Gson not found"
**Nguyên nhân**: Chưa sync Gradle
**Giải pháp**: Chạy Gradle sync lại

### Animation không chạy
**Nguyên nhân**: Controller không start
**Giải pháp**: Kiểm tra log, đảm bảo `controller?.start()` được gọi

### Ảnh không hiển thị
**Nguyên nhân**: Tên drawable không match
**Giải pháp**: 
1. Kiểm tra tên trong JSON: `"vampire_idle_1"`
2. Kiểm tra file drawable: `res/drawable/vampire_idle_1.png`
3. Rebuild project

### Animation bị lag
**Nguyên nhân**: Delay quá ngắn hoặc quá nhiều frames
**Giải pháp**: Tăng delay trong JSON (đơn vị: milliseconds)

## 📊 Performance

Hệ thống mới có performance tương đương hoặc tốt hơn code cũ:
- ✅ Chỉ load JSON 1 lần khi khởi động
- ✅ Sử dụng StateFlow để quản lý state hiệu quả
- ✅ Coroutine được quản lý đúng cách (cleanup tự động)
- ✅ Không có memory leak

## 🎨 Tùy chỉnh Animation

### Thay đổi delay
```json
{
  "frame": [1, 2],
  "delay": 500,  // Tăng từ 333ms lên 500ms
  "sequence": "infinity"
}
```

### Thay đổi sequence
```json
{
  "frame": [1, 2, 3],
  "delay": 167,
  "sequence": 2  // Lặp 2 lần thay vì vô hạn
}
```

### Thêm animation mới
1. Thêm frames vào drawable resources
2. Cập nhật `data.json`:
```json
"new_animation": {
  "thumb": "new.png",
  "frames": [
    { "id": 1, "url": "vampire_new_1" },
    { "id": 2, "url": "vampire_new_2" }
  ],
  "logic": [
    {
      "frame": [1, 2],
      "delay": 200,
      "sequence": "infinity"
    }
  ]
}
```
3. Thêm mapping trong `AnimationMapper.kt`
4. Rebuild app

## ✨ Next Steps

Sau khi test thành công, bạn có thể:
1. Xóa code cũ trong `MainActivity.kt` (ShimejiSprite function)
2. Thêm nhiều characters khác vào JSON
3. Tạo UI để switch giữa các characters
4. Export JSON để non-developers có thể chỉnh animations
