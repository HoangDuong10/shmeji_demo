package com.example.demoshemij.domain

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SpriteManager {
    private var spriteState: SpriteState? = null
    private val _stopSignal = MutableStateFlow(false)
    val stopSignal: StateFlow<Boolean> = _stopSignal.asStateFlow()

    fun setSpriteState(state: SpriteState) {
        spriteState = state
    }

    fun stopAnimation() {
        _stopSignal.value = true
        spriteState?.stop()
        Log.d("SpriteManager", "Stop animation called")
    }

    fun resetStopSignal() {
        _stopSignal.value = false
    }

    private val _addSpriteFlow = MutableSharedFlow<Unit>()
    val addSpriteFlow = _addSpriteFlow

    suspend fun addNewSprite() {
        _addSpriteFlow.emit(Unit)
    }

//    fun stopAnimation() {
//        // Gửi tín hiệu dừng toàn bộ
//        stopSignal.tryEmit(true)
//    }
}