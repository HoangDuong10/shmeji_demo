# Fix: Suspend Function Call Errors

## Vấn Đề

Lỗi: `Suspend function 'loadCharacterData' should be called only from a coroutine or another suspend function`

Xảy ra khi gọi `JsonLoader.loadCharacterData()` (suspend function) từ non-suspend context như `remember` block.

## Các File Bị Ảnh Hưởng

### 1. AnimationExample.kt ✅ FIXED
**Vấn đề:**
```kotlin
@Composable
fun rememberAnimationController(...): AnimationController? {
    return remember(...) {
        val characterData = JsonLoader.loadCharacterData(context) // ❌ Suspend call in remember
        ...
    }
}
```

**Giải pháp:**
```kotlin
@Composable
fun rememberAnimationController(...): AnimationController? {
    var controller by remember { mutableStateOf<AnimationController?>(null) }
    
    LaunchedEffect(...) {
        val characterData = JsonLoader.loadCharacterData(context) // ✅ Suspend call in LaunchedEffect
        ...
        controller = ...
    }
    
    return controller
}
```

### 2. AnimationMapper.kt ✅ FIXED
**Vấn đề:**
```kotlin
fun getControllerForState(...): AnimationController? {
    val characterData = JsonLoader.loadCharacterData(context) // ❌ Suspend call in regular function
    ...
}
```

**Giải pháp:**
```kotlin
fun getControllerForState(...): AnimationController? {
    val characterData = JsonLoader.loadCharacterDataSync(context) // ✅ Sync version
    ...
}
```

### 3. OverflowHelper.kt ✅ FIXED
**Vấn đề:**
```kotlin
fun getOverflowConfig(...): OverflowConfig {
    val characterData = JsonLoader.loadCharacterData(context) // ❌ Suspend call in regular function
    ...
}
```

**Giải pháp:**
```kotlin
fun getOverflowConfig(...): OverflowConfig {
    val characterData = JsonLoader.loadCharacterDataSync(context) // ✅ Sync version
    ...
}
```

## Giải Thích

### Khi nào dùng `loadCharacterData()` (suspend)?
- Trong `LaunchedEffect`
- Trong `suspend fun`
- Trong coroutine scope
- Khi cần load fresh data từ URL

### Khi nào dùng `loadCharacterDataSync()` (non-suspend)?
- Trong `remember` block
- Trong regular function
- Khi chỉ cần lấy từ cache
- Khi đã chắc chắn data đã được preload

## Lưu Ý Quan Trọng

### ⚠️ loadCharacterDataSync() chỉ return cache
```kotlin
fun loadCharacterDataSync(context: Context): CharacterList? {
    // Chỉ return cache, không load mới
    return cachedData
}
```

**Điều này có nghĩa:**
- Phải preload data trước (qua PreloadManager)
- Nếu chưa có cache, sẽ return null
- Không tốn thời gian load từ URL

### ✅ Flow đúng:
```
1. App start → PreloadManager.preloadAllImages()
   → Load JSON và cache
   
2. Sprite display → AnimationMapper.getControllerForState()
   → Dùng loadCharacterDataSync() để lấy từ cache
   
3. Fast và không block UI thread
```

## Testing

### Test 1: Verify preload works
```kotlin
// Trong PreloadManager
PreloadManager.preloadAllImages(context)
// → JSON được load và cache

// Sau đó trong AnimationMapper
val data = JsonLoader.loadCharacterDataSync(context)
// → data != null (từ cache)
```

### Test 2: Verify sync version works
```kotlin
// Không có preload
val data = JsonLoader.loadCharacterDataSync(context)
// → data == null (chưa có cache)

// Sau khi preload
PreloadManager.preloadAllImages(context)
val data2 = JsonLoader.loadCharacterDataSync(context)
// → data2 != null (có cache rồi)
```

## Kết Luận

Tất cả lỗi suspend function đã được fix bằng cách:
1. Dùng `LaunchedEffect` cho suspend calls trong Composable
2. Dùng `loadCharacterDataSync()` cho non-suspend context
3. Đảm bảo data được preload trước khi dùng sync version

✅ Code compile thành công
✅ Không còn suspend function errors
✅ Performance tốt (dùng cache)
