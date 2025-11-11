## 🔍 Debug Steps - Ảnh không hiển thị

### Bước 1: Build và chạy app
```bash
./gradlew clean build
./gradlew installDebug
```

### Bước 2: Xem log khi app khởi động
```bash
adb logcat -c  # Clear log
adb logcat | grep -E "DrawableChecker|JsonLoader|AnimationMapper|JsonAnimatedSprite"
```

### Bước 3: Kiểm tra các vấn đề thường gặp

#### A. Kiểm tra drawable resources
Log sẽ hiển thị:
```
D/DrawableChecker: ✅ Found: vampire_idle_1 (ID: 2131165312)
D/DrawableChecker: ❌ Missing: vampire_idle_2
```

**Nếu thiếu drawable:**
- Kiểm tra file có tồn tại trong `app/src/main/res/drawable/`
- Rebuild project: `Build → Clean Project` → `Build → Rebuild Project`

#### B. Kiểm tra JSON loading
Log sẽ hiển thị:
```
D/JsonLoader: Loading data.json from assets...
D/JsonLoader: JSON loaded, length: 8543
D/JsonLoader: JSON parsed, characters count: 1
```

**Nếu JSON không load được:**
- Kiểm tra file `app/src/main/assets/data.json` có tồn tại
- Kiểm tra JSON syntax có đúng không (dùng jsonlint.com)

#### C. Kiểm tra AnimationMapper
Log sẽ hiển thị:
```
D/AnimationMapper: Getting controller for state: Idle -> animation: idle
D/AnimationMapper: Character data loaded: true
D/AnimationMapper: Character 'goku' found: true
D/AnimationMapper: Animation 'idle' found: true, frames: 2
```

**Nếu không tìm thấy animation:**
- Kiểm tra tên character trong JSON: `"goku"`
- Kiểm tra tên animation trong JSON: `"idle"`, `"hover"`, etc.

#### D. Kiểm tra frame URL
Log sẽ hiển thị:
```
D/JsonAnimatedSprite: Looking for drawable: vampire_idle_1 -> ID: 2131165312
D/JsonAnimatedSprite: State: Idle, Frame ID: 1, URL: vampire_idle_1, Drawable: 2131165312
```

**Nếu Drawable ID = 0 hoặc = 17301543 (ic_menu_report_image):**
- Tên trong JSON không match với drawable resource
- Kiểm tra `data.json`: `"url": "vampire_idle_1"` (không có .png)
- Kiểm tra drawable file: `vampire_idle_1.png` hoặc `vampire_idle_1.xml`

### Bước 4: Các lỗi thường gặp

#### Lỗi 1: FileNotFoundException - data.json
```
E/JsonLoader: Failed to load JSON
java.io.FileNotFoundException: data.json
```

**Giải pháp:**
```bash
# Kiểm tra file có tồn tại
ls -la app/src/main/assets/data.json

# Nếu không có, copy lại
cp app/src/assets/data.json app/src/main/assets/data.json
```

#### Lỗi 2: JsonSyntaxException
```
E/JsonLoader: Failed to parse JSON
com.google.gson.JsonSyntaxException
```

**Giải pháp:**
- Kiểm tra JSON syntax tại jsonlint.com
- Đảm bảo không có trailing comma
- Đảm bảo tất cả string dùng double quotes

#### Lỗi 3: Resources$NotFoundException
```
E/JsonAnimatedSprite: frameUrl is null! Controller: null, State: Idle
```

**Giải pháp:**
- Controller không được tạo → JSON không load được
- Xem log JsonLoader và AnimationMapper

#### Lỗi 4: Drawable ID = 0
```
D/JsonAnimatedSprite: Looking for drawable: vampire_idle_1 -> ID: 0
```

**Giải pháp:**
- Drawable không tồn tại
- Tên không match (case-sensitive)
- Rebuild project

### Bước 5: Test thủ công

#### Test JSON loading
Thêm vào MainActivity:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Test JSON
    val data = JsonLoader.loadCharacterData(this)
    Log.d("TEST", "Characters: ${data?.characters?.size}")
    Log.d("TEST", "Goku animations: ${data?.characters?.firstOrNull()?.get("goku")?.animations?.keys}")
}
```

#### Test drawable loading
```kotlin
val resId = resources.getIdentifier("vampire_idle_1", "drawable", packageName)
Log.d("TEST", "vampire_idle_1 ID: $resId")
```

### Bước 6: Quick fix

Nếu vẫn không hoạt động, thử fallback về code cũ tạm thời:

```kotlin
// Trong FloatingService.kt - SpriteContent
@Composable
fun SpriteContent(...) {
    // Tạm thời dùng lại code cũ
    ShimejiSprite(
        spriteState = spriteState,
        spriteFlip = spriteFlip,
        onCustomAnimationFinished = { onCustomAnimationFinished() },
        onImpactFinish = { ... }
    )
}
```

### Bước 7: Liên hệ debug

Gửi log đầy đủ:
```bash
adb logcat -d > logcat.txt
```

Kiểm tra:
1. DrawableChecker output
2. JsonLoader output
3. AnimationMapper output
4. JsonAnimatedSprite output
5. Bất kỳ Exception nào

### Script tự động

Chạy script debug:
```bash
chmod +x debug_animation.sh
./debug_animation.sh
```
