package com.example.demoshemij.domain

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Controller để quản lý animation dựa trên logic từ JSON
 * Hỗ trợ nhiều bước animation với delay và sequence khác nhau
 */
class AnimationController(
    private val animationData: AnimationData
) {
    private val _currentFrameId = MutableStateFlow(1)
    val currentFrameId: StateFlow<Int> = _currentFrameId.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val scope = CoroutineScope(
        context = Dispatchers.Default + SupervisorJob()
    )

    // Map frame ID sang frame URL để dễ truy xuất
    private val frameMap = animationData.frames.associate { it.id to it.url }

    init {
        scope.launch {
            _isRunning.collect { running ->
                if (running) {
                    runAnimationLogic()
                } else {
                    // Reset về frame đầu tiên khi dừng
                    _currentFrameId.value = animationData.frames.firstOrNull()?.id ?: 1
                }

            }
        }
    }

    /**
     * Thực thi animation logic từ JSON
     * Mỗi logic step có thể có sequence khác nhau (infinity hoặc số lần cụ thể)
     */
    private suspend fun runAnimationLogic() {
        while (_isRunning.value) {
            for (logicStep in animationData.logic) {
                if (!_isRunning.value) break

                val frames = logicStep.frame
                val delayMs = logicStep.delay.toLong()
                val sequence = logicStep.sequence

                // ✅ Xác định số lần lặp - sequence: 0 hoặc "infinity" = lặp vô hạn
                val isInfinite = (sequence is String && sequence == "infinity") || 
                                 (sequence is Int && sequence == 0) ||
                                 (sequence is Double && sequence == 0.0)
                val repeatCount = if (isInfinite) Int.MAX_VALUE else {
                    when (sequence) {
                        is Int -> sequence
                        is Double -> sequence.toInt()
                        else -> 1
                    }
                }

                // Thực hiện lặp theo sequence
                var currentRepeat = 0
                while (currentRepeat < repeatCount && _isRunning.value) {
                    for (frameId in frames) {
                        if (!_isRunning.value) break
                        
                        _currentFrameId.value = frameId
                        Log.d("AnimationController", "Frame: $frameId, Delay: ${delayMs}ms, Repeat: ${currentRepeat + 1}/$repeatCount")
                        delay(delayMs)
                    }
                    currentRepeat++
                }

                // Nếu không phải infinity, chuyển sang logic step tiếp theo
                if (!isInfinite) {
                    continue
                } else {
                    // Nếu là infinity, lặp mãi ở step này
                    break
                }
            }
        }
    }

    /**
     * Bắt đầu animation
     */
    fun start() {
        _isRunning.value = true
        Log.d("AnimationController", "Animation started")
    }

    /**
     * Dừng animation
     */
    fun stop() {
        _isRunning.value = false
        Log.d("AnimationController", "Animation stopped")
    }

    /**
     * Lấy URL của frame hiện tại
     */
    fun getCurrentFrameUrl(): String? {
        return frameMap[_currentFrameId.value]
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
    }
}
