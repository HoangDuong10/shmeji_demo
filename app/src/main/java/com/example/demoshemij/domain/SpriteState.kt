package com.example.demoshemij.domain

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * Manages the state of a sprite sheet animation by controlling the frame transitions and animation timing.
 *
 * The animation runs within a coroutine scope on the default dispatcher. It loops through the frames
 * at each interval specified by `animationSpeed` and resets when `stop()` is called.
 *
 * @param totalFrames The total number of frames in the sprite sheet. Determines the number of frames
 * that the animation cycles through.
 * @param framesPerRow The number of frames that are included in the sprite sheet, per row.
 * @param animationSpeed Controlling the speed of the animation.
 *
 * @property currentFrame A [StateFlow] that emits the current frame index. Observers can use this
 * property to get the current frame of the animation.
 * @property isRunning A [StateFlow] that emits the running state of the animation, indicating
 * whether the animation is actively running or stopped.
 *
 * @constructor Initializes the `SpriteState` with a specified number of frames and animation speed.
 */
class SpriteState(
    private val totalFrames: Int,
    internal val framesPerRow: Int,
    private val animationSpeed: Long
) {
    private val _currentFrame = MutableStateFlow(value = 0)
    val currentFrame: StateFlow<Int> get() = _currentFrame

    private val _selectedRow = MutableStateFlow<Int?>(null) // null = tất cả frame
    val selectedRow: StateFlow<Int?> = _selectedRow.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val scope = CoroutineScope(
        context = Dispatchers.Default + SupervisorJob()
    )

    init {
        scope.launch {
            _isRunning.collect { running ->
                if (running) {
                    // Animating sprite images
                    while (_isRunning.value) {
                        val selectedRowValue = _selectedRow.value
                        if (selectedRowValue != null) {
                            // Giới hạn frame trong hàng được chọn
                            val startFrame = selectedRowValue * framesPerRow
                            val endFrame = startFrame + framesPerRow - 1
                            _currentFrame.value = if (_currentFrame.value >= endFrame) {
                                startFrame // Quay lại frame đầu tiên của hàng
                            } else {
                                _currentFrame.value + 1 // Tăng frame
                            }
                            Log.d("SpriteState", "SelectedRow: $selectedRowValue, CurrentFrame: ${_currentFrame.value}")
                        } else {
                            // Hiển thị tất cả frame
                            _currentFrame.value = (_currentFrame.value + 1) % totalFrames
                            Log.d("SpriteState", "All Frames, CurrentFrame: ${_currentFrame.value}")
                        }
                        delay(animationSpeed)
                    }
                } else {
                    // Reset the sprite frame to its initial position
                    _currentFrame.value = _selectedRow.value?.let { it * framesPerRow } ?: 0
                    Log.d("SpriteState", "Animation stopped, CurrentFrame: ${_currentFrame.value}")
                }
            }
        }
    }

    /**
     * Starts the sprite animation by setting the `isRunning` state to `true`, causing
     * frames to update based on the specified animation speed.
     */
    fun start() {
        _isRunning.value = true
    }

    /**
     * Sets the row to limit animation to frames in that row. If null, animation runs through all frames.
     * @param row The row index (0-based) to limit animation to, or null for all frames.
     */
    fun setRow(row: Int?) {
        _selectedRow.value = row?.coerceIn(0, totalFrames / framesPerRow - 1)
        // Reset currentFrame to the first frame of the selected row
        if (row != null) {
            _currentFrame.value = row * framesPerRow
            Log.d("SpriteState", "Set row to $row, CurrentFrame: ${_currentFrame.value}")
        }
    }

    /**
     * Stops the sprite animation and resets the current frame to the initial frame of the current row (if selected).
     */
    fun stop() {
        Log.d("SpriteState", "Animation stopped")
        _isRunning.value = false
    }

    /**
     * Cancels the coroutine scope used by this class, releasing any resources and stopping
     * any ongoing animations.
     */
    fun cleanup() {
        scope.cancel()
    }
}

@Composable
fun rememberSpriteState(
    totalFrames: Int,
    framesPerRow: Int,
    animationSpeed: Long = 50L
): SpriteState {
    return remember {
        SpriteState(
            totalFrames,
            framesPerRow,
            animationSpeed
        )
    }
}