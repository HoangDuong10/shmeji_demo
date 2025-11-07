package com.example.demoshemij

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class FloatingSpriteService : LifecycleService(), SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager

    // ✅ Không tạo scope riêng, dùng lifecycleScope có sẵn
    // private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob()) // ❌ XÓA

     data class SpriteInstance(
        val view: ComposeView,
        val params: WindowManager.LayoutParams,
        val controller: SpriteController,
        val characterData: CharacterData, // ✅ Thêm thông tin nhân vật
        var isDragging: Boolean = false,
        var initialTouchX: Float = 0f,
        var initialTouchY: Float = 0f,
        var initialX: Int = 0,
        var initialY: Int = 0,
        var moveJob: Job? = null,  // ✅ Job để quản lý animation
        var hasPlayedCustom: Boolean = false  // ✅ Track xem đã chạy CUSTOM lần đầu chưa
    )

    private val spriteList = mutableListOf<SpriteInstance>()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        savedStateRegistryController.performRestore(null)
        startForegroundService()
        
        // Debug: Check drawables
        com.example.demoshemij.util.DrawableChecker.checkAllDrawables(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            "ADD_SPRITE" -> addNewSprite()
            "STOP_ALL" -> stopAllSprites()
        }
        return START_STICKY
    }

    // ✅ Cancel đúng cách tất cả jobs trước khi remove view
    private fun stopAllSprites() {
        spriteList.forEach { instance ->
            instance.moveJob?.cancel()
            instance.moveJob = null

            try {
                if (instance.view.isAttachedToWindow) {
                    windowManager.removeView(instance.view)
                }
            } catch (e: IllegalArgumentException) {
                Log.e("FloatingSprite", "View already removed", e)
            }
        }
        spriteList.clear()
        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForegroundService() {
        val channelId = "floating_sprite_channel"
        val channel = NotificationChannel(
            channelId,
            "Floating Sprite Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Floating Sprite Running")
            .setContentText("Sprite is floating on screen")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addNewSprite() {
        val (screenWidth, screenHeight) = getScreenSize(this@FloatingSpriteService)
        val controller = SpriteController()
        // ✅ Lấy nhân vật hiện tại được chọn khi tạo sprite
        val selectedCharacter = CharacterRepository.getCurrentCharacter()
        var instance: SpriteInstance? = null

        val newView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@FloatingSpriteService)
            setViewTreeSavedStateRegistryOwner(this@FloatingSpriteService)

            setContent {
                val spriteState by controller.state.collectAsStateWithLifecycle()
                val spriteFlip by controller.flip.collectAsStateWithLifecycle()

                SpriteContent(
                    spriteState = spriteState,
                    spriteFlip = spriteFlip,
                    characterData = selectedCharacter,
                    onCustomAnimationFinished = {
                        // ✅ Khi CUSTOM xong → WALKING
                        Log.d("FloatingService", "onCustomAnimationFinished called!")
                        instance?.controller?.setState(SpriteState1.WALKING)
                        instance?.let { resumeSpriteAnimation(it) }
                    },
                    instance = instance,
                    isInitial = true
                )
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth + newView.width) / 2
            y = 0
        }

        windowManager.addView(newView, params)

        instance = SpriteInstance(
            view = newView,
            params = params,
            controller = controller,
            characterData = selectedCharacter // ✅ Lưu nhân vật cho sprite này
        )

        startSpriteAnimation(instance)
        newView.setOnTouchListener { _, event -> handleTouch(instance, event) }
        spriteList.add(instance)
    }

    private fun resumeSpriteAnimation(instance: SpriteInstance) {
        // Kiểm tra xem có đang dragging không
        if (instance.isDragging) return

        // Cancel job cũ nếu có
        instance.moveJob?.cancel()
        instance.moveJob = null

        // Tạo job mới để tiếp tục animation
        instance.moveJob = lifecycleScope.launch {
            try {
                val (_, screenHeight) = getScreenSize(this@FloatingSpriteService)
                val spriteHeight = instance.view.height
                val currentY = instance.params.y
                val groundY = screenHeight - spriteHeight

                // Nếu đang trên cao, rơi xuống trước
                if (currentY < groundY - 20) {
                    fallDown(instance, false)
                } else {
                    // Nếu đã ở mặt đất, tiếp tục di chuyển bình thường
                    animateSpriteWindow(instance)
                }
            } catch (e: CancellationException) {
                Log.d("FloatingSprite", "Resume animation cancelled")
                throw e
            } catch (e: Exception) {
                Log.e("FloatingSprite", "Resume animation error", e)
            }
        }}

    private var touchDownTime = 0L
    private val touchSlop = 10

    private fun handleTouch(instance: SpriteInstance, event: MotionEvent): Boolean {
        val params = instance.params

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (instance.controller.getState() == SpriteState1.CLIMB) {
                    instance.isDragging = true
                }

                // ✅ Cancel job an toàn
                instance.moveJob?.cancel()
                instance.moveJob = null

                instance.initialX = params.x
                instance.initialY = params.y
                instance.initialTouchX = event.rawX
                instance.initialTouchY = event.rawY
                touchDownTime = System.currentTimeMillis()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - instance.initialTouchX
                val dy = event.rawY - instance.initialTouchY
                val distance = sqrt(dx * dx + dy * dy)
                val duration = System.currentTimeMillis() - touchDownTime
                if (distance < touchSlop) {
                    return@handleTouch true
                }
                Log.d("SpriteTouch111", "Touch duration1111: $distance ${instance.controller.getState()}")
                if (distance > 1 || instance.controller.getState() == SpriteState1.WALKING ) {
                    instance.isDragging = true
                    instance.controller.setState(SpriteState1.Touch)

                    val (screenWidth, screenHeight) = getScreenSize(this)
                    val spriteWidth = instance.view.width
                    val spriteHeight = instance.view.height

                    var newX = (instance.initialX + dx).toInt()
                    var newY = (instance.initialY + dy).toInt()

                    // ✅ CHO PHÉP TRÀN RA NGOÀI 1/3 NHÂN VẬT
                    newX = newX.coerceIn(-spriteWidth / 3, screenWidth - spriteWidth * 2 / 3)
                    newY = newY.coerceIn(-spriteHeight / 3, screenHeight - spriteHeight)

                    params.x = newX
                    params.y = newY

                    try {
                        windowManager.updateViewLayout(instance.view, params)
                    } catch (e: IllegalArgumentException) {
                        Log.e("FloatingSprite", "Failed to update view", e)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                // ✅ Dùng lifecycleScope thay vì scope riêng
                lifecycleScope.launch {
                    val duration = System.currentTimeMillis() - touchDownTime
                    val wasDragging = instance.isDragging
                    instance.isDragging = false

                    val (screenWidth, screenHeight) = getScreenSize(this@FloatingSpriteService)
                    val spriteHeight = instance.view.height
                    val currentY = instance.params.y
                    val groundY = screenHeight - spriteHeight

                    Log.d("SpriteTouch111", "Touch duration: $duration ms, Was dragging: $wasDragging va ${instance.controller.getState()}")
                    when {
                        !wasDragging && duration < 2000 &&
                                instance.controller.getState() == SpriteState1.WALKING -> {
                            Log.d("SpriteTouch111", "Click detected!")
                            instance.controller.setState(SpriteState1.CUSTOM)
                        }

//                        currentY < groundY -> {
//                            Log.d("SpriteTouch", "Fall down triggered")
//                            fallDown(instance, false)
//                        }

                        else -> {
                            fallDown(instance, false)
                            Log.d("SpriteTouch", "Already on ground")
                        }
                    }
                }
                return true
            }
        }
        return false
    }

    @Composable
    fun SpriteContent(
        spriteState: SpriteState1,
        spriteFlip: SpriteFlip1?,
        characterData: CharacterData,
        onCustomAnimationFinished: () -> Unit,
        isInitial : Boolean,
        instance : SpriteInstance? = null
    ) {
        // ✅ SỬ DỤNG COMPONENT MỚI TỪ JSON
        com.example.demoshemij.component.JsonAnimatedSprite(
            spriteState = spriteState,
            spriteFlip = spriteFlip,
            onCustomAnimationFinished = { 
                // ✅ Khi CUSTOM xong → WALKING
                onCustomAnimationFinished() 
            },
            onImpactFinish = {
                // ✅ Khi IMPACT xong
                val shouldPlayCustom = instance?.hasPlayedCustom == false
                Log.d("FloatingService", "onImpactFinish called! hasPlayedCustom=${instance?.hasPlayedCustom}, shouldPlayCustom=$shouldPlayCustom")
                
                instance?.moveJob?.cancel()
                instance?.moveJob = lifecycleScope.launch {
                    if (shouldPlayCustom) {
                        // ✅ Lần đầu tiên: IMPACT → CUSTOM
                        Log.d("FloatingService", "First time - Setting state to CUSTOM")
                        instance?.hasPlayedCustom = true  // Đánh dấu đã chạy CUSTOM
                        instance?.controller?.setState(SpriteState1.CUSTOM)
                    } else {
                        // ✅ Các lần sau: IMPACT → WALKING → di chuyển
                        Log.d("FloatingService", "Not first time - Setting state to WALKING")
                        instance?.controller?.setState(SpriteState1.WALKING)
                        delay(100) // Đợi animation WALKING bắt đầu
                        if (instance?.isDragging == false) {
                            animateSpriteWindow(instance)
                        }
                    }
                }
            },
            characterName = "goku"
        )
    }

    private fun startSpriteAnimation(instance: SpriteInstance) {
        // ✅ Cancel job cũ trước khi tạo mới
        instance.moveJob?.cancel()
        instance.moveJob = null

        // ✅ Dùng lifecycleScope và xử lý exception
        instance.moveJob = lifecycleScope.launch {
            try {
                fallDown(instance, true)
            } catch (e: CancellationException) {
                Log.d("FloatingSprite", "Animation cancelled")
                throw e // Re-throw để coroutine biết đã bị cancel
            } catch (e: Exception) {
                Log.e("FloatingSprite", "Animation error", e)
            }
        }
    }

    // ✅ Thêm suspend và xử lý cancellation
    private suspend fun animateParamTo(
        instance: SpriteInstance,
        axis: String,
        target: Int,
        durationMillis: Int,
        isMovingRight: Boolean,
        onComplete: () -> Unit = {}
    ) {
        val params = instance.params
        val start = if (axis == "x") params.x else params.y
        val distance = target - start
        val steps = 60
        val delayPerStep = durationMillis / steps

        val (screenWidth, screenHeight) = getScreenSize(this@FloatingSpriteService)
        val spriteWidth = instance.view.width
        val spriteHeight = instance.view.height

        repeat(steps) { step ->
            yield()

            if (instance.controller.getState() == SpriteState1.CUSTOM || instance.isDragging || instance.controller.getState() == SpriteState1.Idle) return

            val fraction = (step + 1).toFloat() / steps
            val value = start + (distance * fraction).toInt()
            if (axis == "x") params.x = value else params.y = value

            try {
                windowManager.updateViewLayout(instance.view, params)
            } catch (e: IllegalArgumentException) {
                Log.e("FloatingSprite", "Failed to update view", e)
                return
            }

            // ✅ CẬP NHẬT ĐIỀU KIỆN BIÊN VỚI OVERFLOW 1/3
            val isAtTop = params.y <= -spriteHeight / 3  // Cho phép tràn trên 1/3
            val isAtBottom = params.y >= screenHeight - spriteHeight
            val isAtLeft = params.x <= -spriteWidth / 3  // Cho phép tràn 1/3
            val isAtRight = params.x >= screenWidth - spriteWidth * 2 / 3  // Cho phép tràn 1/3

            when {
                isAtLeft || isAtRight -> instance.controller.setState(SpriteState1.CLIMB)
                isAtBottom -> instance.controller.setState(SpriteState1.WALKING)
            }

            val flip = when {
                params.y <= -spriteHeight / 3 -> SpriteFlip1.TOP  // ✅ Cập nhật điều kiện
                params.x <= -spriteWidth / 3 ||  // ✅ Cập nhật điều kiện
                        axis == "x" && (target < start && instance.controller.getState() == SpriteState1.WALKING) ||
                        (target > start && instance.controller.getState() == SpriteState1.DASH) -> SpriteFlip1.LEFT

                params.x >= screenWidth - spriteWidth * 2 / 3 ||  // ✅ Cập nhật điều kiện
                        axis == "x" && (target > start && instance.controller.getState() == SpriteState1.WALKING) ||
                        (target < start && instance.controller.getState() == SpriteState1.DASH) -> SpriteFlip1.RIGHT

                else -> if (isMovingRight) null else SpriteFlip1.RIGHT
            }

            instance.controller.setFlip(flip)
            delay(delayPerStep.toLong())
        }

        onComplete()
    }


    private suspend fun animateSpriteWindow(instance: SpriteInstance) {
        val (screenWidth, screenHeight) = getScreenSize(this@FloatingSpriteService)
        val spriteWidth = instance.view.width
        val spriteHeight = instance.view.height
        val margin = 0

        while (coroutineContext.isActive && !instance.isDragging) {
            val isAtTop = instance.params.y <= -spriteHeight / 3 + margin  // ✅ Cho phép tràn trên 1/3
            val isAtBottom = instance.params.y >= screenHeight - spriteHeight - margin
            val isAtLeft = instance.params.x <= -spriteWidth / 3  // ✅ Cho phép tràn 1/3
            val isAtRight = instance.params.x >= screenWidth - spriteWidth * 2 / 3  // ✅ Cho phép tràn 1/3

            val action = Random.nextInt(100)

            when {
                isAtLeft -> {
                    when {
                        action < 10 -> { // Di chuyển lên
                            val maxUpDistance = instance.params.y - (-spriteHeight / 3 + margin)  // ✅ Cập nhật biên trên
                            if (maxUpDistance > 180) {
                                val targetY = instance.params.y - Random.nextInt(180, min(200, maxUpDistance))
                                animateParamTo(instance, "y", targetY, 1000, false)
                            } else if (maxUpDistance > 0) {
                                val targetY = -spriteHeight / 3 + margin  // ✅ Cập nhật target
                                animateParamTo(instance, "y", targetY, 2000, false)
                            } else {
                                val targetX = screenWidth/3
                                animateParamTo(instance, "x", targetX, 1000, true)
                            }
                        }
                        action < 95 -> { // Di chuyển xuống
                            val maxDownDistance = screenHeight - spriteHeight - margin - instance.params.y
                            if (maxDownDistance > 180) {
                                val targetY = instance.params.y + Random.nextInt(180, min(200, maxDownDistance))
                                animateParamTo(instance, "y", targetY, 1000, false)
                            } else if (maxDownDistance > 0) {
                                val targetY = screenHeight - spriteHeight - margin
                                animateParamTo(instance, "y", targetY, 2000, false)
                            } else {
                                val targetX = screenWidth/3
                                animateParamTo(instance, "x", targetX, 1000, true)
                            }
                        }
                        else -> { // Nhảy sang phải
                            if (instance.params.y <= -spriteHeight / 3 + spriteHeight) {  // ✅ Cập nhật điều kiện
                                Log.d("duonghx", "đang ở gần đỉnh, không cho nhảy sang phải")
                            } else {
                                instance.controller.setState(SpriteState1.DASH)
                                val targetX = screenWidth - spriteWidth * 2 / 3 - margin  // ✅ Cập nhật target
                                animateParamTo(instance, "x", targetX, 700, true)
                            }
                        }
                    }
                }

                isAtRight -> {
                    when {
                        action < 85 -> { // Di chuyển lên
                            val maxUpDistance = instance.params.y - (-spriteHeight / 3 + margin)  // ✅ Cập nhật biên trên
                            if (maxUpDistance > 180) {
                                val targetY = instance.params.y - Random.nextInt(180, min(200, maxUpDistance))
                                animateParamTo(instance, "y", targetY, 1000, false)
                            } else if (maxUpDistance > 0) {
                                val targetY = -spriteHeight / 3 + margin  // ✅ Cập nhật target
                                animateParamTo(instance, "y", targetY, 2000, false)
                            } else {
                                val targetX = screenWidth/3
                                animateParamTo(instance, "x", targetX, 1000, false)
                            }
                        }
                        action < 95 -> { // Di chuyển xuống
                            val maxDownDistance = screenHeight - spriteHeight - margin - instance.params.y
                            if (maxDownDistance > 180) {
                                val targetY = instance.params.y + Random.nextInt(180, min(200, maxDownDistance))
                                animateParamTo(instance, "y", targetY, 1000, false)
                            } else if (maxDownDistance > 0) {
                                val targetY = screenHeight - spriteHeight - margin
                                animateParamTo(instance, "y", targetY, 2000, false)
                            } else {
                                val targetX = screenWidth/3
                                animateParamTo(instance, "x", targetX, 1000, false)
                            }
                        }
                        else -> { // Nhảy sang trái
                            if (instance.params.y <= -spriteHeight / 3 + spriteHeight) {  // ✅ Cập nhật điều kiện
                                Log.d("duonghx", "đang ở gần đỉnh, không cho nhảy sang trái")
                            } else {
                                instance.controller.setState(SpriteState1.DASH)
                                val targetX = -spriteWidth / 3  // ✅ Cập nhật target
                                animateParamTo(instance, "x", targetX, 700, false)
                            }
                        }
                    }
                }

                isAtBottom -> {
                    when {
                        action < 60 -> { // Đi sang phải
                            delay(300)
                            if (Random.nextInt(100) < 20) {
                                instance.controller.setState(SpriteState1.Idle)
                                delay(Random.nextLong(2000, 3000))
                                instance.controller.setState(SpriteState1.WALKING)
                            }
                            val maxRightDistance = (screenWidth - spriteWidth * 2 / 3) - margin - instance.params.x  // ✅ Cập nhật
                            if (maxRightDistance > 180) {
                                val targetX = instance.params.x + Random.nextInt(180, min(200, maxRightDistance))
                                animateParamTo(instance, "x", targetX, 200, true)
                            } else if (maxRightDistance > 0) {
                                val targetX = screenWidth - spriteWidth * 2 / 3 - margin  // ✅ Cập nhật
                                animateParamTo(instance, "x", targetX, 200, true)
                            } else {
                                val targetY = -spriteHeight / 3 + margin  // ✅ Cập nhật target lên trên
                                animateParamTo(instance, "y", targetY, 200, false)
                            }
                        }
                        else -> { // Đi sang trái
                            if (Random.nextInt(100) < 20) {
                                instance.controller.setState(SpriteState1.Idle)
                                delay(Random.nextLong(2000, 3000))
                                instance.controller.setState(SpriteState1.WALKING)
                            }
                            val maxLeftDistance = instance.params.x - (-spriteWidth / 3) - margin  // ✅ Cập nhật
                            if (maxLeftDistance > 180) {
                                val targetX = instance.params.x - Random.nextInt(180, min(200, maxLeftDistance))
                                animateParamTo(instance, "x", targetX, 200, false)
                            } else if (maxLeftDistance > 0) {
                                val targetX = -spriteWidth / 3 + margin  // ✅ Cập nhật
                                animateParamTo(instance, "x", targetX, 200, false)
                            } else {
                                val targetY = -spriteHeight / 3 + margin  // ✅ Cập nhật target lên trên
                                animateParamTo(instance, "y", targetY, 200, true)
                            }
                        }
                    }
                } else -> {
                Log.d("FloatingSprite", "Sprite in middle")
                delay(100)
                when {
                    action < 60 -> { // Đi sang phải
                        delay(300)
                        if(instance.controller.getState() == SpriteState1.WALKING) {
                            if (Random.nextInt(100) < 20) {
                                instance.controller.setState(SpriteState1.Idle)
                                delay(Random.nextLong(2000, 3000))
                                instance.controller.setState(SpriteState1.WALKING)
                            }
                        }
                        val maxRightDistance = (screenWidth - spriteWidth * 2 / 3) - margin - instance.params.x  // ✅ Cập nhật
                        if (maxRightDistance > 180) {
                            val targetX = instance.params.x + Random.nextInt(180, min(200, maxRightDistance))
                            animateParamTo(instance, "x", targetX, 200, true)
                        } else if (maxRightDistance > 0) {
                            val targetX = screenWidth - spriteWidth * 2 / 3 - margin  // ✅ Cập nhật
                            animateParamTo(instance, "x", targetX, 200, true)
                        } else {
                            val targetY = -spriteHeight / 3 + margin  // ✅ Cập nhật
                            animateParamTo(instance, "y", targetY, 200, false)
                        }
                    }
                    else -> { // Đi sang trái
                        if(instance.controller.getState() == SpriteState1.WALKING) {
                            if (Random.nextInt(100) < 20) {
                                instance.controller.setState(SpriteState1.Idle)
                                delay(Random.nextLong(2000, 3000))
                                instance.controller.setState(SpriteState1.WALKING)
                            }
                        }
                        val maxLeftDistance = instance.params.x - (-spriteWidth / 3) - margin  // ✅ Cập nhật
                        if (maxLeftDistance > 180) {
                            val targetX = instance.params.x - Random.nextInt(180, min(200, maxLeftDistance))
                            animateParamTo(instance, "x", targetX, 200, false)
                        } else if (maxLeftDistance > 0) {
                            val targetX = -spriteWidth / 3 + margin  // ✅ Cập nhật
                            animateParamTo(instance, "x", targetX, 200, false)
                        } else {
                            val targetY = -spriteHeight / 3 + margin  // ✅ Cập nhật
                            animateParamTo(instance, "y", targetY, 200, true)
                        }
                    }
                }
            }
            }
        }
    }

    // ✅ Sửa fallDown để xử lý coroutine đúng cách
    private fun fallDown(instance: SpriteInstance,isInitial: Boolean) {
        instance.view.post {
            // ✅ Cancel job cũ
            instance.moveJob?.cancel()
            instance.moveJob = null

            // ✅ Tạo job mới trong lifecycleScope
            instance.moveJob = lifecycleScope.launch {
                try {
                    val (_, screenHeight) = getScreenSize(this@FloatingSpriteService)
                    val spriteHeight = instance.view.height
                    val groundY = screenHeight - spriteHeight

                    instance.controller.setState(SpriteState1.FALL)

                    while (isActive && instance.params.y < groundY && !instance.isDragging) {
                        instance.params.y += 20

                        try {
                            windowManager.updateViewLayout(instance.view, instance.params)
                        } catch (e: IllegalArgumentException) {
                            Log.e("FloatingSprite", "Failed to update view during fall", e)
                            return@launch
                        }

                        delay(10)
                    }
                    Log.d("ccccc","${groundY}")
                    Log.d("cccc123c","${instance.params.y}")


                    if (!isActive || instance.isDragging) return@launch

                    Log.d("FloatingService", "Setting state to IMPACT (Bottom), isInitial=$isInitial")
                    instance.controller.setState(SpriteState1.Bottom)
                    Log.d("FloatingService", "State set to Bottom, waiting for onImpactFinish callback...")
//                    if(isInitial){
//                        instance.controller.setState(SpriteState1.CUSTOM)
//                    }else{
//                        if (isActive && !instance.isDragging) {
//                            animateSpriteWindow(instance)
//                        }
//                    }
                    Log.d("bbbb","${instance.params.y}")
                    Log.d("bbbb111","${instance.params.y}")
                } catch (e: CancellationException) {
                    Log.d("FloatingSprite", "Fall animation cancelled")
                    throw e
                } catch (e: Exception) {
                    Log.e("FloatingSprite", "Fall animation error", e)
                }
            }
        }
    }

    private suspend fun awaitViewMeasured(view: View): Int = suspendCancellableCoroutine { cont ->
        if (view.height > 0) {
            cont.resume(view.height)
            return@suspendCancellableCoroutine
        }

        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (view.height > 0) {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    cont.resume(view.height)
                }
            }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)

        cont.invokeOnCancellation {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    @Suppress("DEPRECATION")
    private fun getScreenSize(context: Context): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = context.getSystemService(WindowManager::class.java).currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
            )
            val width = windowMetrics.bounds.width()
            val height = windowMetrics.bounds.height() - insets.bottom
            width to height
        } else {
            val displayMetrics = context.resources.displayMetrics
            displayMetrics.widthPixels to displayMetrics.heightPixels
        }
    }

    // ✅ Cleanup khi service destroy
    override fun onDestroy() {
        stopAllSprites()
        super.onDestroy()
    }
}

// ✅ SpriteController giữ nguyên
class SpriteController {
    private val _state = MutableStateFlow(SpriteState1.Idle)
    val state: StateFlow<SpriteState1> = _state.asStateFlow()

    private val _flip = MutableStateFlow<SpriteFlip1?>(null)
    val flip: StateFlow<SpriteFlip1?> = _flip.asStateFlow()

    fun setState(newState: SpriteState1) {
        _state.value = newState
    }

    fun setFlip(flip: SpriteFlip1?) {
        _flip.value = flip
    }

    fun getState(): SpriteState1 = _state.value

    fun getFlip(): SpriteFlip1? = _flip.value
}