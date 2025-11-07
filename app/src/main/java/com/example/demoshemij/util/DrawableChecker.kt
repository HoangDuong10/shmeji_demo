package com.example.demoshemij.util

import android.content.Context
import android.util.Log

/**
 * Utility để kiểm tra drawable resources
 */
object DrawableChecker {
    
    /**
     * Kiểm tra tất cả drawable resources cần thiết
     */
    fun checkAllDrawables(context: Context) {
        val requiredDrawables = listOf(
            // Idle
            "vampire_idle_1", "vampire_idle_2",
            // Hover
            "vampire_hover_1", "vampire_hover_2", "vampire_hover_3",
            // Walking
            "vampire_walking_1", "vampire_walking_2",
            // Falling
            "vampire_falling_1", "vampire_falling_2",
            // Climb
            "vampire_climb_1", "vampire_climb_2", "vampire_climb_3",
            // Dash
            "vampire_dash_1", "vampire_dash_2", "vampire_dash_3", "vampire_dash_4",
            "vampire_dash_5", "vampire_dash_6", "vampire_dash_7", "vampire_dash_8",
            // Custom
            "vampire_custom_1", "vampire_custom_2", "vampire_custom_3", "vampire_custom_4",
            "vampire_custom_5", "vampire_custom_6", "vampire_custom_7", "vampire_custom_8",
            "vampire_custom_9", "vampire_custom_10", "vampire_custom_11", "vampire_custom_12",
            "vampire_custom_13", "vampire_custom_14", "vampire_custom_15", "vampire_custom_16",
            "vampire_custom_17"
        )
        
        Log.d("DrawableChecker", "=== Checking ${requiredDrawables.size} drawables ===")
        
        var foundCount = 0
        var missingCount = 0
        val missingList = mutableListOf<String>()
        
        requiredDrawables.forEach { name ->
            val resId = context.resources.getIdentifier(
                name,
                "drawable",
                context.packageName
            )
            
            if (resId != 0) {
                foundCount++
                Log.d("DrawableChecker", "✅ Found: $name (ID: $resId)")
            } else {
                missingCount++
                missingList.add(name)
                Log.e("DrawableChecker", "❌ Missing: $name")
            }
        }
        
        Log.d("DrawableChecker", "=== Summary ===")
        Log.d("DrawableChecker", "Found: $foundCount / ${requiredDrawables.size}")
        Log.d("DrawableChecker", "Missing: $missingCount")
        
        if (missingList.isNotEmpty()) {
            Log.e("DrawableChecker", "Missing drawables: ${missingList.joinToString(", ")}")
        }
    }
}
