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
    
    // ✅ Placeholder character khi chưa load được JSON
    private val placeholderCharacter = CharacterData(
        id = "placeholder",
        name = "Loading...",
        previewImage = android.R.drawable.ic_menu_report_image,
        spriteAnimations = SpriteAnimations(
            idleImages = listOf(android.R.drawable.ic_menu_report_image),
            walkingImages = listOf(android.R.drawable.ic_menu_report_image),
            touchImages = listOf(android.R.drawable.ic_menu_report_image),
            fallImages = listOf(android.R.drawable.ic_menu_report_image),
            bottomImages = listOf(android.R.drawable.ic_menu_report_image),
            dashImages = listOf(android.R.drawable.ic_menu_report_image),
            climbImages = listOf(android.R.drawable.ic_menu_report_image),
            customImages = listOf(android.R.drawable.ic_menu_report_image)
        ),
        animationTimings = AnimationTimings()
    )
    
    // ✅ Danh sách nhân vật sẽ được load từ JSON
    private val _availableCharacters = MutableStateFlow<List<CharacterData>>(emptyList())
    val availableCharacters: StateFlow<List<CharacterData>> = _availableCharacters.asStateFlow()
    
    // Nhân vật hiện tại được chọn (mặc định là placeholder)
    private val _currentCharacter = MutableStateFlow(placeholderCharacter)
    val currentCharacter: StateFlow<CharacterData> = _currentCharacter.asStateFlow()
    
    /**
     * Load danh sách nhân vật từ JSON
     */
    suspend fun loadCharactersFromJson(context: android.content.Context) {
        val jsonData = com.example.demoshemij.util.JsonLoader.loadCharacterData(context)
        
        if (jsonData != null) {
            val characters = mutableListOf<CharacterData>()
            
            jsonData.characters.forEach { characterMap ->
                characterMap.forEach { (id, data) ->
                    // Tạo CharacterData từ JSON
                    val character = CharacterData(
                        id = id,
                        name = id.capitalize(),
                        previewImage = android.R.drawable.ic_menu_report_image, // Sẽ load từ URL
                        spriteAnimations = SpriteAnimations(
                            idleImages = emptyList(), // Không cần nữa, dùng URL
                            walkingImages = emptyList(),
                            touchImages = emptyList(),
                            fallImages = emptyList(),
                            bottomImages = emptyList(),
                            dashImages = emptyList(),
                            climbImages = emptyList(),
                            customImages = emptyList()
                        ),
                        animationTimings = AnimationTimings() // Timing từ JSON logic
                    )
                    characters.add(character)
                }
            }
            
            _availableCharacters.value = characters
            
            // Set character đầu tiên làm mặc định
            if (characters.isNotEmpty()) {
                _currentCharacter.value = characters.first()
            }
        }
    }
    
    // Lấy nhân vật hiện tại (sync)
    fun getCurrentCharacter(): CharacterData = _currentCharacter.value
    
    // Thay đổi nhân vật
    fun setCurrentCharacter(characterId: String) {
        _availableCharacters.value.find { it.id == characterId }?.let {
            _currentCharacter.value = it
        }
    }
    
    fun setCurrentCharacter(character: CharacterData) {
        _currentCharacter.value = character
    }
}
