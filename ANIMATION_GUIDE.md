# Hướng dẫn sử dụng Animation System từ JSON

## Tổng quan

Hệ thống animation mới cho phép bạn cấu hình animation từ file `data.json` thay vì fix cứng trong code. Hệ thống hỗ trợ:

- ✅ Nhiều bước animation với delay khác nhau
- ✅ Lặp vô hạn (`"infinity"`) hoặc số lần cụ thể
- ✅ Chuỗi frame phức tạp (ví dụ: 1->2->3->4->3->4)

## Cấu trúc JSON

```json
{
  "animations": {
    "idle": {
      "frames": [
        { "id": 1, "url": "idle1.png" },
        { "id": 2, "url": "idle2.png" }
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

### Giải thích các trường:

- **frames**: Danh sách các frame với `id` và `url` ảnh
- **logic**: Mảng các bước animation, thực hiện tuần tự
  - **frame**: Mảng ID frame sẽ chạy (ví dụ: `[1, 2, 3, 4, 3, 4]`)
  - **delay**: Thời gian delay giữa các frame (milliseconds)
  - **sequence**: Số lần lặp (`1`, `2`, ...) hoặc `"infinity"` để lặp vô hạn

## Ví dụ Animation

### 1. IDLE - Animation đơn giản
```json
"logic": [
  {
    "frame": [1, 2],
    "delay": 333,
    "sequence": "infinity"
  }
]
```
**Kết quả**: Frame 1 → 2 → 1 → 2... (lặp vô hạn, mỗi frame 333ms)

### 2. CUSTOM - Animation phức tạp
```json
"logic": [
  {
    "frame": [1, 2, 3, 4, 3, 4],
    "delay": 167,
    "sequence": 1
  },
  {
    "frame": [5],
    "delay": 83,
    "sequence": 1
  },
  {
    "frame": [6, 7],
    "delay": 167,
    "sequence": "infinity"
  }
]
```
**Kết quả**: 
1. Frame 1→2→3→4→3→4 (167ms mỗi frame, chạy 1 lần)
2. Frame 5 (83ms, chạy 1 lần)
3. Frame 6→7→6→7... (167ms mỗi frame, lặp vô hạn)

### 3. DASH - Animation với intro
```json
"logic": [
  {
    "frame": [1],
    "delay": 167,
    "sequence": 1
  },
  {
    "frame": [2, 3],
    "delay": 333,
    "sequence": "infinity"
  }
]
```
**Kết quả**:
1. Frame 1 (167ms, chạy 1 lần - intro)
2. Frame 2→3→2→3... (333ms mỗi frame, lặp vô hạn)

## Cách sử dụng trong Code

### Bước 1: Load data từ JSON
```kotlin
val characterData = JsonLoader.loadCharacterData(context)
val gokuData = characterData?.characters?.firstOrNull()?.get("goku")
val idleAnimation = gokuData?.animations?.get("idle")
```

### Bước 2: Tạo AnimationController
```kotlin
@Composable
fun MyScreen() {
    val context = LocalContext.current
    val controller = rememberAnimationController(
        context = context,
        characterName = "goku",
        animationName = "idle"
    )
    
    // Hoặc tạo thủ công:
    // val controller = remember { AnimationController(idleAnimation!!) }
}
```

### Bước 3: Bắt đầu animation
```kotlin
LaunchedEffect(controller) {
    controller?.start()
}
```

### Bước 4: Lấy frame hiện tại
```kotlin
val currentFrameId by controller.currentFrameId.collectAsState()
val frameUrl = controller.getCurrentFrameUrl()

// Hiển thị ảnh
Image(
    painter = painterResource(id = getDrawableId(frameUrl)),
    contentDescription = null
)
```

### Bước 5: Cleanup
```kotlin
DisposableEffect(controller) {
    onDispose {
        controller?.cleanup()
    }
}
```

## Chuyển đổi từ hệ thống cũ

### Trước (fix cứng):
```kotlin
val spriteState = rememberSpriteState(
    totalFrames = 6,
    framesPerRow = 3,
    animationSpeed = 333L
)
spriteState.start()
```

### Sau (dùng JSON):
```kotlin
val controller = rememberAnimationController(
    context = context,
    characterName = "goku",
    animationName = "idle"
)
controller?.start()
```

## Lợi ích

1. **Dễ chỉnh sửa**: Thay đổi animation mà không cần rebuild app
2. **Linh hoạt**: Hỗ trợ animation phức tạp với nhiều bước
3. **Tái sử dụng**: Một controller có thể dùng cho nhiều animation
4. **Dễ debug**: Log rõ ràng từng frame và bước

## Files đã tạo

- `AnimationController.kt`: Controller chính để quản lý animation
- `JsonLoader.kt`: Utility để load JSON
- `AnimationExample.kt`: Ví dụ và helper functions
- `Model1.kt`: Data models (đã có sẵn)

## Lưu ý

- File `data.json` phải nằm trong `app/src/assets/`
- Delay tính bằng milliseconds (1000ms = 1 giây)
- Sequence `"infinity"` sẽ lặp mãi ở bước đó, không chuyển sang bước tiếp theo
- Nếu tất cả các bước đều không phải infinity, animation sẽ lặp lại từ đầu
