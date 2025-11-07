package com.example.demoshemij

import androidx.annotation.DrawableRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Data class cho mỗi nhân vật
data class CharacterData(
    val id: String,
    val name: String,
    @DrawableRes val previewImage: Int, // Ảnh preview để hiển thị trong list
    val spriteAnimations: SpriteAnimations,
    val animationTimings: AnimationTimings
)

// Chứa tất cả ảnh animation cho một nhân vật
data class SpriteAnimations(
    val idleImages: List<Int>,
    val walkingImages: List<Int>,
    val touchImages: List<Int>,
    val fallImages: List<Int>,
    val bottomImages: List<Int>,
    val dashImages: List<Int>,
    val climbImages: List<Int>,
    val customImages: List<Int>
)

// Chứa thời gian animation cho mỗi trạng thái
data class AnimationTimings(
    val idleDelay: Long = 500L,
    val walkingDelay: Long = 250L,
    val touchDelay: Long = 100L,
    val fallDelay: Long = 250L,
    val bottomDelay: Long = 500L,
    val dashStartDelay: Long = 160L,
    val dashLoopDelay: Long = 333L,
    val climbDelay: Long = 333L,
    val customDelay: Long = 200L
)

// Repository chứa tất cả nhân vật
object CharacterRepository {
    
    // Nhân vật vampire (mặc định hiện tại)
    private val vampireCharacter = CharacterData(
        id = "vampire",
        name = "Vampire",
        previewImage = R.drawable.vampire_idle_1,
        spriteAnimations = SpriteAnimations(
            idleImages = listOf(R.drawable.vampire_idle_1, R.drawable.vampire_idle_2),
            walkingImages = listOf(R.drawable.vampire_walking_1, R.drawable.vampire_walking_2),
            touchImages = listOf(R.drawable.vampire_hover_1, R.drawable.vampire_hover_2, R.drawable.vampire_hover_3),
            fallImages = listOf(R.drawable.vampire_falling_1, R.drawable.vampire_falling_2),
            bottomImages = listOf(R.drawable.vampire_impact_1, R.drawable.vampire_impact_2, R.drawable.vampire_impact_3),
            dashImages = listOf(
                R.drawable.vampire_dash_1, R.drawable.vampire_dash_2, R.drawable.vampire_dash_3,
                R.drawable.vampire_dash_4, R.drawable.vampire_dash_5, R.drawable.vampire_dash_6,
                R.drawable.vampire_dash_7, R.drawable.vampire_dash_8
            ),
            climbImages = listOf(R.drawable.vampire_climb_1, R.drawable.vampire_climb_2, R.drawable.vampire_climb_3),
            customImages = listOf(
                R.drawable.vampire_custom_1, R.drawable.vampire_custom_2, R.drawable.vampire_custom_3,
                R.drawable.vampire_custom_4, R.drawable.vampire_custom_5, R.drawable.vampire_custom_6,
                R.drawable.vampire_custom_7, R.drawable.vampire_custom_8, R.drawable.vampire_custom_9,
                R.drawable.vampire_custom_10, R.drawable.vampire_custom_11, R.drawable.vampire_custom_12,
                R.drawable.vampire_custom_13, R.drawable.vampire_custom_14, R.drawable.vampire_custom_15,
                R.drawable.vampire_custom_16, R.drawable.vampire_custom_17
            )
        ),
        animationTimings = AnimationTimings(
            idleDelay = 333L, // 1000/3L
            touchDelay = 250L,
            bottomDelay = 500L,
            fallDelay = 250L,
            dashStartDelay = 160L,
            dashLoopDelay = 333L,
            climbDelay = 333L,
            customDelay = 200L
        )
    )
    
    // Nhân vật shimeji cũ
    private val shimejiCharacter = CharacterData(
        id = "shimeji",
        name = "Shimeji",
        previewImage = R.drawable.idle_2,
        spriteAnimations = SpriteAnimations(
            idleImages = listOf(R.drawable.idle_2, R.drawable.idle_2),
            walkingImages = listOf(R.drawable.walking_1, R.drawable.walking_2),
            touchImages = listOf(R.drawable.anh1, R.drawable.anh2, R.drawable.anh3, R.drawable.anh4),
            fallImages = listOf(R.drawable.falling_1, R.drawable.falling_2),
            bottomImages = listOf(R.drawable.impact_2, R.drawable.impact_2, R.drawable.impact_4),
            dashImages = listOf(R.drawable.dash_1, R.drawable.dash_2, R.drawable.dash_2),
            climbImages = listOf(R.drawable.climb_1, R.drawable.climb_2, R.drawable.climb_3),
            customImages = listOf(
                R.drawable.custom_1, R.drawable.custom_2, R.drawable.custom_3,
                R.drawable.custom_4, R.drawable.custom_5, R.drawable.custom_6, R.drawable.custom_7
            )
        ),
        animationTimings = AnimationTimings(
            idleDelay = 500L,
            touchDelay = 100L,
            bottomDelay = 500L,
            fallDelay = 250L,
            dashStartDelay = 160L,
            dashLoopDelay = 333L,
            climbDelay = 333L,
            customDelay = 200L
        )
    )
    
    // Danh sách tất cả nhân vật có sẵn
    val availableCharacters = listOf(
        vampireCharacter,
        shimejiCharacter,
        // Bạn có thể thêm nhân vật mới ở đây
        CharacterData(
            id = "goku",
            name = "Son Goku",
            previewImage = R.drawable.songoku, // Sử dụng ảnh có sẵn làm preview
            spriteAnimations = SpriteAnimations(
                // Tạm thời dùng lại ảnh của shimeji, bạn có thể thay thế
                idleImages = listOf(R.drawable.songoku),
                walkingImages = listOf(R.drawable.songoku),
                touchImages = listOf(R.drawable.songoku),
                fallImages = listOf(R.drawable.songoku),
                bottomImages = listOf(R.drawable.songoku),
                dashImages = listOf(R.drawable.songoku),
                climbImages = listOf(R.drawable.songoku),
                customImages = listOf(R.drawable.songoku)
            ),
            animationTimings = AnimationTimings(
                idleDelay = 800L, // Khác với vampire
                walkingDelay = 200L,
                dashStartDelay = 100L
            )
        )
    )
    
    // Nhân vật hiện tại được chọn (mặc định là vampire)
    private val _currentCharacter = MutableStateFlow(vampireCharacter)
    val currentCharacter: StateFlow<CharacterData> = _currentCharacter.asStateFlow()
    
    // Lấy nhân vật hiện tại (sync)
    fun getCurrentCharacter(): CharacterData = _currentCharacter.value
    
    // Thay đổi nhân vật
    fun setCurrentCharacter(characterId: String) {
        availableCharacters.find { it.id == characterId }?.let {
            _currentCharacter.value = it
        }
    }
    
    fun setCurrentCharacter(character: CharacterData) {
        _currentCharacter.value = character
    }
}
