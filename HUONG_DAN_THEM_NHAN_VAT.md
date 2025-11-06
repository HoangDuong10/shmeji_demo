# Hướng Dẫn Thêm Nhân Vật Mới

## Cách thêm nhân vật mới vào hệ thống

### 1. Chuẩn bị ảnh sprite

Đầu tiên, bạn cần chuẩn bị các ảnh sprite cho nhân vật mới và đặt vào thư mục `app/src/main/res/drawable/`:

**Các loại ảnh cần thiết:**
- `[tên]_idle_1.png`, `[tên]_idle_2.png` - Ảnh đứng yên
- `[tên]_walking_1.png`, `[tên]_walking_2.png` - Ảnh đi bộ
- `[tên]_hover_1.png`, `[tên]_hover_2.png`, `[tên]_hover_3.png` - Ảnh khi chạm
- `[tên]_falling_1.png`, `[tên]_falling_2.png` - Ảnh rơi
- `[tên]_impact_1.png`, `[tên]_impact_2.png`, `[tên]_impact_3.png` - Ảnh chạm đất
- `[tên]_dash_1.png`, `[tên]_dash_2.png`, `[tên]_dash_3.png` - Ảnh nhảy nhanh
- `[tên]_climb_1.png`, `[tên]_climb_2.png`, `[tên]_climb_3.png` - Ảnh leo tường
- `[tên]_custom_1.png` đến `[tên]_custom_X.png` - Ảnh animation đặc biệt

### 2. Thêm nhân vật vào CharacterRepository

Mở file `app/src/main/java/com/example/demoshemij/CharacterData.kt` và thêm nhân vật mới vào danh sách `availableCharacters`:

```kotlin
val availableCharacters = listOf(
    vampireCharacter,
    shimejiCharacter,
    
    // THÊM NHÂN VẬT MỚI Ở ĐÂY
    CharacterData(
        id = "ten_nhan_vat", // ID duy nhất
        name = "Tên Hiển Thị", // Tên hiển thị trong UI
        previewImage = R.drawable.ten_nhan_vat_idle_1, // Ảnh preview
        spriteAnimations = SpriteAnimations(
            idleImages = listOf(R.drawable.ten_nhan_vat_idle_1, R.drawable.ten_nhan_vat_idle_2),
            walkingImages = listOf(R.drawable.ten_nhan_vat_walking_1, R.drawable.ten_nhan_vat_walking_2),
            touchImages = listOf(R.drawable.ten_nhan_vat_hover_1, R.drawable.ten_nhan_vat_hover_2, R.drawable.ten_nhan_vat_hover_3),
            fallImages = listOf(R.drawable.ten_nhan_vat_falling_1, R.drawable.ten_nhan_vat_falling_2),
            bottomImages = listOf(R.drawable.ten_nhan_vat_impact_1, R.drawable.ten_nhan_vat_impact_2, R.drawable.ten_nhan_vat_impact_3),
            dashImages = listOf(R.drawable.ten_nhan_vat_dash_1, R.drawable.ten_nhan_vat_dash_2, R.drawable.ten_nhan_vat_dash_3),
            climbImages = listOf(R.drawable.ten_nhan_vat_climb_1, R.drawable.ten_nhan_vat_climb_2, R.drawable.ten_nhan_vat_climb_3),
            customImages = listOf(
                R.drawable.ten_nhan_vat_custom_1, R.drawable.ten_nhan_vat_custom_2,
                // ... thêm các ảnh custom khác
            )
        ),
        animationTimings = AnimationTimings(
            idleDelay = 500L,        // Thời gian giữa các frame idle (ms)
            walkingDelay = 250L,     // Thời gian giữa các frame walking (ms)
            touchDelay = 100L,       // Thời gian giữa các frame touch (ms)
            fallDelay = 250L,        // Thời gian giữa các frame fall (ms)
            bottomDelay = 500L,      // Thời gian giữa các frame impact (ms)
            dashStartDelay = 160L,   // Thời gian hiển thị dash_1 (ms)
            dashLoopDelay = 333L,    // Thời gian lặp dash_2 ↔ dash_3 (ms)
            climbDelay = 333L,       // Thời gian giữa các frame climb (ms)
            customDelay = 200L       // Thời gian giữa các frame custom (ms)
        )
    )
)
```

### 3. Tùy chỉnh timing animation

Bạn có thể tùy chỉnh tốc độ animation cho từng nhân vật bằng cách thay đổi các giá trị trong `AnimationTimings`:

- **Giá trị nhỏ hơn** = Animation nhanh hơn
- **Giá trị lớn hơn** = Animation chậm hơn

**Ví dụ:**
```kotlin
animationTimings = AnimationTimings(
    idleDelay = 800L,    // Chậm hơn (800ms thay vì 500ms)
    walkingDelay = 150L, // Nhanh hơn (150ms thay vì 250ms)
    dashStartDelay = 100L, // Nhanh hơn
    // ... các timing khác
)
```

### 4. Sử dụng ảnh có sẵn (tạm thời)

Nếu chưa có đủ ảnh sprite, bạn có thể tạm thời sử dụng một ảnh cho tất cả trạng thái:

```kotlin
spriteAnimations = SpriteAnimations(
    idleImages = listOf(R.drawable.nhan_vat_chinh),
    walkingImages = listOf(R.drawable.nhan_vat_chinh),
    touchImages = listOf(R.drawable.nhan_vat_chinh),
    fallImages = listOf(R.drawable.nhan_vat_chinh),
    bottomImages = listOf(R.drawable.nhan_vat_chinh),
    dashImages = listOf(R.drawable.nhan_vat_chinh),
    climbImages = listOf(R.drawable.nhan_vat_chinh),
    customImages = listOf(R.drawable.nhan_vat_chinh)
)
```

### 5. Test nhân vật mới

1. Build và chạy app
2. Nhấn nút "Chọn Nhân Vật"
3. Chọn nhân vật mới từ danh sách
4. Nhấn "Áp dụng nhân vật"
5. Nhấn "Thêm Sprite" để test

## Ví dụ hoàn chỉnh

Đây là ví dụ thêm nhân vật "Pikachu":

```kotlin
CharacterData(
    id = "pikachu",
    name = "Pikachu",
    previewImage = R.drawable.pikachu_idle_1,
    spriteAnimations = SpriteAnimations(
        idleImages = listOf(R.drawable.pikachu_idle_1, R.drawable.pikachu_idle_2),
        walkingImages = listOf(R.drawable.pikachu_walk_1, R.drawable.pikachu_walk_2),
        touchImages = listOf(R.drawable.pikachu_touch_1),
        fallImages = listOf(R.drawable.pikachu_fall_1),
        bottomImages = listOf(R.drawable.pikachu_impact_1),
        dashImages = listOf(R.drawable.pikachu_dash_1, R.drawable.pikachu_dash_2),
        climbImages = listOf(R.drawable.pikachu_climb_1),
        customImages = listOf(R.drawable.pikachu_thunder_1, R.drawable.pikachu_thunder_2)
    ),
    animationTimings = AnimationTimings(
        idleDelay = 600L,     // Pikachu đứng yên chậm hơn
        walkingDelay = 200L,  // Đi bộ nhanh
        customDelay = 150L    // Thunder attack nhanh
    )
)
```

## Lưu ý

- **ID phải duy nhất**: Không được trùng với nhân vật khác
- **Tên file ảnh**: Nên đặt theo quy tắc `[tên]_[trạng thái]_[số].png`
- **Thứ tự ảnh**: Đảm bảo thứ tự ảnh trong list đúng với animation mong muốn
- **Kích thước ảnh**: Nên có kích thước tương đương nhau để hiển thị đẹp

Sau khi thêm xong, nhân vật mới sẽ xuất hiện trong danh sách chọn nhân vật!