package com.example.demoshemij.domain

import android.content.Context
import com.example.demoshemij.SpriteState1
import com.example.demoshemij.util.JsonLoader

/**
 * Mapper để chuyển đổi giữa SpriteState1 (enum cũ) và animation names trong JSON
 */
object AnimationMapper {
    
    /**
     * Map từ SpriteState1 sang tên animation trong JSON
     */
    fun mapStateToAnimationName(state: SpriteState1): String {
        return when (state) {
            SpriteState1.Idle -> "idle"
            SpriteState1.Touch -> "hover"
            SpriteState1.WALKING -> "walking"
            SpriteState1.FALL -> "falling"
            SpriteState1.CLIMB -> "climb"
            SpriteState1.DASH -> "dash"
            SpriteState1.CUSTOM -> "custom"
            SpriteState1.Bottom -> "impact"  // ✅ SỬA: Bottom = IMPACT animation
        }
    }
    
    /**
     * Lấy AnimationController cho một state cụ thể
     * ✅ Dùng sync version vì được gọi từ remember block
     */
    fun getControllerForState(
        context: Context,
        state: SpriteState1,
        characterName: String = "goku"
    ): AnimationController? {
        val animationName = mapStateToAnimationName(state)
        android.util.Log.d("AnimationMapper", "Getting controller for state: $state -> animation: $animationName")
        
        // ✅ Dùng sync version - chỉ lấy từ cache
        val characterData = JsonLoader.loadCharacterDataSync(context)
        android.util.Log.d("AnimationMapper", "Character data loaded: ${characterData != null}")
        
        val character = characterData?.characters?.firstOrNull()?.get(characterName)
        android.util.Log.d("AnimationMapper", "Character '$characterName' found: ${character != null}")
        
        val animationData = character?.animations?.get(animationName)
        android.util.Log.d("AnimationMapper", "Animation '$animationName' found: ${animationData != null}, frames: ${animationData?.frames?.size}")
        
        return animationData?.let { AnimationController(it) }
    }
    
    /**
     * Lấy drawable resource ID từ tên file
     * Ví dụ: "vampire_idle_1" -> R.drawable.vampire_idle_1
     */
    fun getDrawableId(context: Context, fileName: String, characterPrefix: String = "vampire"): Int {
        // Tên file đã có format đúng: "vampire_idle_1"
        val resourceName = fileName.lowercase()
        
        val resId = context.resources.getIdentifier(
            resourceName,
            "drawable",
            context.packageName
        )
        
        // Fallback nếu không tìm thấy
        return if (resId != 0) resId else android.R.drawable.ic_menu_report_image
    }
}
