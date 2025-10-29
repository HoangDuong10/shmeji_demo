package com.example.demoshemij

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.demoshemij.domain.SpriteSpec
import com.example.demoshemij.domain.SpriteFlip
import com.example.demoshemij.domain.SpriteManager
import com.example.demoshemij.domain.SpriteSheet
import com.example.demoshemij.domain.SpriteState
import com.example.demoshemij.domain.rememberSpriteState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

class FloatingSpriteService : LifecycleService(), SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager

    // Danh sách các sprite (view + params)
    private data class SpriteInstance(
        val view: ComposeView,
        val params: WindowManager.LayoutParams,
        var isDragging: Boolean = false,
        var initialTouchX: Float = 0f,
        var initialTouchY: Float = 0f,
        var initialX: Int = 0,
        var initialY: Int = 0,
        var moveJob: Job? = null,
        val flipUpdater: (SpriteFlip?) -> Unit,
        val stateUpdater: (Int?) -> Unit
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
        // Không tạo sprite ở đây nữa
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == "ADD_SPRITE") {
            addNewSprite()
        } else if (intent?.action == "STOP_ALL") {
            stopAllSprites()
        }
        return START_STICKY
    }
    private fun stopAllSprites() {
        spriteList.forEach { instance ->
            instance.moveJob?.cancel()
            if (instance.view.isAttachedToWindow) {
                windowManager.removeView(instance.view)
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

//    companion object {
//        var currentFlipUpdater: ((SpriteFlip?) -> Unit)? = null
//        var currentState:((Int?) -> Unit)? = null
//    }

    private fun addNewSprite() {
        val flipState = mutableStateOf<SpriteFlip?>(null)
        val spriteState = mutableStateOf<Int?>(null)

        val newView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@FloatingSpriteService)
            setViewTreeSavedStateRegistryOwner(this@FloatingSpriteService)

            setContent {
                setContent {
                    SpriteContent(
                        spriteFlip = flipState,
                        selectedRow = spriteState,
                        onFlipUpdate = { },
                        onStateUpdate = {  }
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(newView, params)

        val instance = SpriteInstance(
            view = newView,
            params = params,
            flipUpdater = { flip ->
                flipState.value = flip
            },
            stateUpdater = { state ->
                spriteState.value = state
            }
        )

        startSpriteAnimation(instance)

        newView.setOnTouchListener { _, event ->
            handleTouch(instance, event)
        }

        spriteList.add(instance)
    }

    private fun handleTouch(instance: SpriteInstance, event: MotionEvent): Boolean {
        val params = instance.params
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                instance.isDragging = true
                instance.moveJob?.cancel()
                instance.initialX = params.x
                instance.initialY = params.y
                instance.initialTouchX = event.rawX
                instance.initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (instance.isDragging) {
                    val (screenWidth, screenHeight) = getScreenSize(this)
                    val spriteWidth = instance.view.width
                    val spriteHeight = instance.view.height

// Tính toán vị trí mới
                    var newX = (instance.initialX + (event.rawX - instance.initialTouchX)).toInt()
                    var newY = (instance.initialY + (event.rawY - instance.initialTouchY)).toInt()

// Giới hạn để không vượt quá màn hình
                    newX = newX.coerceIn(0, screenWidth - spriteWidth)
                    newY = newY.coerceIn(0, screenHeight - spriteHeight)

// Gán lại params
                    params.x = newX
                    params.y = newY

                    windowManager.updateViewLayout(instance.view, params)

                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                instance.isDragging = false
                lifecycleScope.launch {
                    fallDown(instance)  // Truyền instance
                    startSpriteAnimation(instance)
                }
                return true
            }
        }
        return false
    }

    @Composable
    fun SpriteContent(
        spriteFlip: State<SpriteFlip?>,
        selectedRow: State<Int?>,
        onFlipUpdate: (SpriteFlip?) -> Unit,
        onStateUpdate: (Int?) -> Unit
    ) {
        val spriteState = rememberSpriteState(
            totalFrames = 9,
            framesPerRow = 3,
            animationSpeed = 100
        )

        val spriteSpec = SpriteSpec(
            screenWidth = 360f,
            default = SpriteSheet(
                frameWidth = 253,
                frameHeight = 303,
                imageRes = R.drawable.sprite_normal
            )
        )

        SpriteManager.setSpriteState(spriteState)

        LaunchedEffect(Unit) {
            spriteState.start()
            SpriteManager.stopSignal.collect { shouldStop ->
                if (shouldStop) spriteState.stop()
            }
        }

//        LaunchedEffect(spriteFlip.value, selectedRow.value) {
//            onFlipUpdate(spriteFlip.value)
//            onStateUpdate(selectedRow.value)
//        }

        MovingSprite(
            spriteState = spriteState,
            spriteSpec = spriteSpec,
            spriteFlip = spriteFlip.value,
            selectedRow = selectedRow.value
        )
    }

    private fun startSpriteAnimation(instance: SpriteInstance) {
        instance.moveJob?.cancel()
        instance.moveJob = lifecycleScope.launch {
            animateSpriteWindow(instance)
        }
    }
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
            // Dừng nếu sprite này đang bị kéo
            if (instance.isDragging) return

            val fraction = (step + 1).toFloat() / steps
            val value = start + (distance * fraction).toInt()
            if (axis == "x") params.x = value else params.y = value

            try {
                windowManager.updateViewLayout(instance.view, params)
            } catch (e: Exception) {
                return
            }

            // Cập nhật flip đúng cho sprite này
            val flip = when {
                params.x <= 0 -> SpriteFlip.Horizontal // Cạnh trái
                params.x >= screenWidth - spriteWidth -> null // Cạnh phải
                params.y <= 0 -> SpriteFlip.Both // Cạnh trên
                params.y >= screenHeight - spriteHeight -> SpriteFlip.Horizontal // Cạnh dưới
                else -> if (isMovingRight) null else SpriteFlip.Horizontal
            }

            // Gọi updater của sprite này
//            val flipUpdater = instance.view.getTag(R.id.sprite_flip_updater) as? (SpriteFlip?) -> Unit
            instance.flipUpdater.invoke(flip)

            delay(delayPerStep.toLong())
        }

        // Hoàn thành
        onComplete()
    }

    private fun animateSpriteWindow(instance: SpriteInstance,) {
        instance.view.post {
            instance.moveJob?.cancel()
            instance.moveJob = lifecycleScope.launch {
                val (screenWidth, screenHeight) = getScreenSize(this@FloatingSpriteService)
                val spriteWidth =    instance.view.width
                val spriteHeight =    instance.view.height
                val margin = 0
                var isMovingRight = true // Theo dõi hướng di chuyển ngang
                instance.stateUpdater.invoke(null)
                while (!   instance.isDragging) {
                    // Thêm biến kiểm tra vị trí góc để dễ debug và xử lý
                    val isAtTop =    instance.params.y <= margin
                    val isAtBottom =    instance.params.y >= screenHeight - spriteHeight - margin
                    val isAtLeft =   instance. params.x <= 0
                    val isAtRight =    instance.params.x >= screenWidth - spriteWidth - margin

                    // Chọn tỷ lệ dựa trên vị trí (cạnh trái, cạnh phải, hoặc ở giữa)
                    val action = Random.nextInt(100)

                    when {
                        // Cạnh trái: lên 10%, xuống 85%, nhảy 5%
                        isAtLeft -> {  // Sử dụng isAtLeft thay vì params.x <= 0 để nhất quán
                            when {
                                action < 10 -> { // 0-9: 10% - Di chuyển lên
                                    Log.d("duonghx", "canh trai len: ${   instance.params.x}, ${   instance.params.y}")
                                    val maxUpDistance =    instance.params.y - margin // Khoảng cách tối đa có thể đi lên
                                    if (maxUpDistance > 180) {
                                        val targetY =    instance.params.y - Random.nextInt(180, min(200, maxUpDistance))
                                        animateParamTo(   instance, "y", targetY, 2000, isMovingRight)
                                    } else if (maxUpDistance > 0) {
                                        val targetY = margin
                                        animateParamTo(  instance, "y", targetY, 2000, isMovingRight)
                                    } else {
                                        // Đã ở top (góc trên trái), buộc di chuyển ngang sang phải trên cạnh trên
                                        val targetX = screenWidth - spriteWidth - margin
                                        animateParamTo(instance, "x", targetX, 2000, true)  // isMovingRight = true, không ! vì đổi từ up sang right
                                    }
                                }
                                action < 95 -> { // 10-94: 85% - Di chuyển xuống
                                    Log.d("duonghx", "canh trai xuong: ${instance.params.x}, ${instance.params.y}")
                                    val maxDownDistance = screenHeight - spriteHeight - margin - instance.params.y
                                    if (maxDownDistance > 180) {
                                        val targetY = instance.params.y + Random.nextInt(180, min(200, maxDownDistance))
                                        animateParamTo(instance, "y", targetY, 2000, isMovingRight)
                                    } else if (maxDownDistance > 0) {
                                        val targetY = screenHeight - spriteHeight - margin
                                        animateParamTo(instance, "y", targetY, 2000, isMovingRight)
                                    } else {
                                        // Đã ở bottom (góc dưới trái), buộc di chuyển ngang sang phải trên cạnh dưới
                                        val targetX = screenWidth - spriteWidth - margin
                                        animateParamTo(instance, "x", targetX, 2000, true)  // isMovingRight = true, không ! vì đổi từ down sang right
                                    }
                                }
                                else -> { // 95-99: 5% - Nhảy sang cạnh phải
                                    Log.d("duonghx", "canh trai nhay: ${instance.params.x}, ${instance.params.y}")
                                    val targetX = screenWidth - spriteWidth - margin
                                    animateParamTo(instance, "x", targetX, 2000, isMovingRight) {
                                        isMovingRight = !isMovingRight // Giữ nguyên: Đổi hướng sau nhảy
                                    }
                                }
                            }
                        }
                        // Cạnh phải: lên 85%, xuống 10%, nhảy 5%
                        isAtRight -> {  // Sử dụng isAtRight
                            when {
                                action < 85 -> { // 0-84: 85% - Di chuyển lên
                                    Log.d("duonghx", "canh phai len: ${instance.params.x}, ${instance.params.y}")
                                    val maxUpDistance = instance.params.y - margin
                                    if (maxUpDistance > 180) {
                                        val targetY = instance.params.y - Random.nextInt(180, min(200, maxUpDistance))
                                        animateParamTo(instance, "y", targetY, 2000, isMovingRight)
                                    } else if (maxUpDistance > 0) {
                                        val targetY = margin
                                        animateParamTo(instance, "y", targetY, 2000, isMovingRight)
                                    } else {
                                        // Đã ở top (góc trên phải), buộc di chuyển ngang sang trái trên cạnh trên
                                        val targetX = 0
                                        animateParamTo(instance, "x", targetX, 2000, false)  // isMovingRight = false, không ! vì đổi từ up sang left
                                    }
                                }
                                action < 95 -> { // 85-94: 10% - Di chuyển xuống
                                    Log.d("duonghx", "canh phai xuong: ${instance.params.x}, ${instance.params.y}")
                                    val maxDownDistance = screenHeight - spriteHeight - margin - instance.params.y
                                    if (maxDownDistance > 180) {
                                        val targetY = instance.params.y + Random.nextInt(180, min(200, maxDownDistance))
                                        animateParamTo(instance, "y", targetY, 2000, isMovingRight)
                                    } else if (maxDownDistance > 0) {
                                        val targetY = screenHeight - spriteHeight - margin
                                        animateParamTo(instance, "y", targetY, 2000, isMovingRight)
                                    } else {
                                        // Đã ở bottom (góc dưới phải), buộc di chuyển ngang sang trái trên cạnh dưới
                                        val targetX = 0
                                        animateParamTo(instance, "x", targetX, 2000, false)  // isMovingRight = false, không ! vì đổi từ down sang left
                                    }
                                }
                                else -> { // 95-99: 5% - Nhảy sang cạnh trái
                                    Log.d("duonghx", "canh phai nhay: ${instance.params.x}, ${instance.params.y}")
                                    val targetX = 0
                                    animateParamTo(instance, "x", targetX, 200, isMovingRight) {
                                        isMovingRight = !isMovingRight // Giữ nguyên: Đổi hướng sau nhảy
                                    }
                                }
                            }
                        }
                        // Ở giữa: lên 10%, xuống 85%, nhảy 5%
                        else -> {
                            when {
                                action < 10 -> { // 0-9: 10% - Di chuyển lên
                                    Log.d("duonghx", "giua len: ${instance.params.x}, ${instance.params.y}")
                                    val maxUpDistance = instance.params.y - margin
                                    if (maxUpDistance > 180) {
                                        val targetY = instance.params.y - Random.nextInt(180, min(200, maxUpDistance))
                                        animateParamTo(instance, "y", targetY, 2000, isMovingRight)
                                    } else if (maxUpDistance > 0) {
                                        val targetY = margin
                                        animateParamTo(instance, "y", targetY, 2000, isMovingRight)
                                    } else {
                                        // Đã ở top (hiếm ở giữa, nhưng xử lý), di chuyển ngang theo isMovingRight
                                        val targetX = if (isMovingRight) screenWidth - spriteWidth - margin else 0
                                        animateParamTo(instance, "x", targetX, 2000, isMovingRight) {
                                            isMovingRight = !isMovingRight // Đổi hướng sau khi đến
                                        }
                                    }
                                }
                                action < 95 -> { // 10-94: 85% - Di chuyển xuống
                                    Log.d("duonghx", "giua xuong: ${instance.params.x}, ${instance.params.y}")
                                    val maxDownDistance = screenHeight - spriteHeight - margin - instance.params.y
                                    if (maxDownDistance > 180) {
                                        val targetY = instance.params.y + Random.nextInt(180, min(200, maxDownDistance))
                                        animateParamTo(instance, "y", targetY, 2000, isMovingRight)
                                    } else if (maxDownDistance > 0) {
                                        val targetY = screenHeight - spriteHeight - margin
                                        animateParamTo(instance, "y", targetY, 2000, isMovingRight)
                                    } else {
                                        // Đã ở bottom, di chuyển ngang theo isMovingRight
                                        val targetX = if (isMovingRight) screenWidth - spriteWidth - margin else 0
                                        animateParamTo(instance, "x", targetX, 2000, isMovingRight) {
                                            isMovingRight = !isMovingRight // Đổi hướng sau khi đến
                                        }
                                    }
                                }
                                else -> { // 95-99: 5% - Nhảy sang cạnh đối diện
                                    Log.d("duonghx", "giua nhay: ${instance.params.x}, ${instance.params.y}")
                                    val targetX = if (isMovingRight) screenWidth - spriteWidth - margin else 0
                                    animateParamTo(instance, "x", targetX, 2000, isMovingRight) {
                                        isMovingRight = !isMovingRight // Đổi hướng sau nhảy
                                    }
                                }
                            }
                        }
                    }
                    // Thêm độ trễ nhỏ giữa các hành động để tránh di chuyển quá nhanh
                    delay(100)
                }
            }
        }
    }
    // 👉 Hàm rơi xuống (gravity effect)
    private suspend fun fallDown(instance: SpriteInstance) {
        val (_, screenHeight) = getScreenSize(this@FloatingSpriteService)
        val spriteHeight = instance.view.height
        val groundY = screenHeight - spriteHeight
instance.stateUpdater.invoke(2)
        // Chỉ rơi nếu chưa chạm đất
        while (instance.params.y < groundY) {
            // Nếu người dùng kéo lại → dừng ngay
            if (instance.isDragging) return

            instance.params.y += 20
            windowManager.updateViewLayout(instance.view, instance.params)
            delay(10)
        }

        // Đảm bảo chạm đất chính xác
        instance.params.y = groundY
        windowManager.updateViewLayout(instance.view, instance.params)
    }


    @Suppress("DEPRECATION")
    private fun getScreenSize(context: Context): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = context.getSystemService(WindowManager::class.java).currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
            )
            val width = windowMetrics.bounds.width() - insets.left - insets.right
            val height = windowMetrics.bounds.height() - insets.top - insets.bottom
            width to height
        } else {
            val displayMetrics = context.resources.displayMetrics
            displayMetrics.widthPixels to displayMetrics.heightPixels
        }
    }

}
