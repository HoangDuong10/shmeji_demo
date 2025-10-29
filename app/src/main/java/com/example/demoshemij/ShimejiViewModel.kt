package com.example.demoshemij

import androidx.lifecycle.ViewModel
import com.example.demoshemij.domain.SpriteFlip
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ShimejiViewModel : ViewModel() {

    private val _spriteState = MutableStateFlow(SpriteState1.Idle)
    val spriteState: StateFlow<SpriteState1> = _spriteState

    private val _spriteFlip = MutableStateFlow<SpriteFlip?>(null)
    val spriteFlip: StateFlow<SpriteFlip?> = _spriteFlip

    fun updateSpriteState(newState: SpriteState1) {
        _spriteState.value = newState
    }

    fun updateFlip(newFlip: SpriteFlip?) {
        _spriteFlip.value = newFlip
    }
}
