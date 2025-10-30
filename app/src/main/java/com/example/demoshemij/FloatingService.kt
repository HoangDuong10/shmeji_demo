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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

class FloatingSpriteService : LifecycleService(), SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager

    // Danh sách các sprite (view + params)
    private data class SpriteInstance(
        val view: ComposeView,
        val params: WindowManager.LayoutParams,
        val controller: SpriteController,          // <-- mới
        var isDragging: Boolean = false,
        var initialTouchX: Float = 0f,
        var initialTouchY: Float = 0f,
        var initialX: Int = 0,
        var initialY: Int = 0,
        var moveJob: Job? = null
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
        val controller = SpriteController()               // <-- riêng cho sprite này

        val newView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@FloatingSpriteService)
            setViewTreeSavedStateRegistryOwner(this@FloatingSpriteService)

            setContent {
                val spriteState by controller.state.collectAsStateWithLifecycle()
                val spriteFlip  by controller.flip.collectAsStateWithLifecycle()

                SpriteContent(spriteState, spriteFlip)
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
        }
//        newView.systemUiVisibility = (
//                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                )
        windowManager.addView(newView, params)

        val instance = SpriteInstance(
            view = newView,
            params = params,
            controller = controller
        )

        startSpriteAnimation(instance)
        newView.setOnTouchListener { _, event -> handleTouch(instance, event) }
        spriteList.add(instance)
    }
    private var touchDownTime = 0L

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

                touchDownTime = System.currentTimeMillis()

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (instance.isDragging) {
                    val (screenWidth, screenHeight) = getScreenSize(this)
                    val spriteWidth = instance.view.width
                    val spriteHeight = instance.view.height
                    instance.controller.setState(SpriteState1.Touch)

                    var newX = (instance.initialX + (event.rawX - instance.initialTouchX)).toInt()
                    var newY = (instance.initialY + (event.rawY - instance.initialTouchY)).toInt()
                    newX = newX.coerceIn(0, screenWidth - spriteWidth)
                    newY = newY.coerceIn(0, screenHeight - spriteHeight)
                    params.x = newX
                    params.y = newY
                    windowManager.updateViewLayout(instance.view, params)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val duration = System.currentTimeMillis() - touchDownTime

                instance.isDragging = false

                when {
                    duration < 200 && instance.controller.getState() == SpriteState1.WALKING -> {
                        // 🩵 Sự kiện click
                        Log.d("SpriteTouch", "Click detected!")
                        instance.controller.setState(SpriteState1.CUSTOM)
                    }
                    else -> {
                        // Bình thường — thả sau kéo
                        lifecycleScope.launch {
                            fallDown(instance)
                            delay(750L)
                            startSpriteAnimation(instance)
                        }
                    }
                }

                return true
            }
        }
        return false
    }

    private suspend fun animateSpriteWindow11(instance: SpriteInstance) = withContext(Dispatchers.Main) {
        val (screenWidth, screenHeight) = getScreenSize(this@FloatingSpriteService)
        val spriteWidth = awaitViewMeasured(instance.view)
        val spriteHeight = instance.view.height
        val margin = 0
        var isMovingRight = true

        instance.controller.setState(SpriteState1.Idle)

        while (isActive && !instance.isDragging) {
            val isAtLeft = instance.params.x <= margin
            val isAtRight = instance.params.x >= screenWidth - spriteWidth - margin
            val isAtTop = instance.params.y <= margin
            val isAtBottom = instance.params.y >= screenHeight - spriteHeight - margin

            val action = Random.nextInt(100)

            // Hàm helper để thực hiện và đợi animation
            suspend fun runAnimation(block: suspend () -> Unit) {
                block()
                delay(2200) // Đợi animation xong + nghỉ 200ms
            }

            when {
                // ========== CẠNH TRÁI ==========
                isAtLeft -> {
                    when {
                        action < 10 -> { // Lên
                            val maxUp = instance.params.y - margin
                            val targetY = if (maxUp > 180) {
                                instance.params.y - Random.nextInt(180, minOf(200, maxUp))
                            } else if (maxUp > 0) {
                                margin
                            } else {
                                // Đã ở top → đi sang phải
                                runAnimation {
                                    animateParamTo(instance, "x", screenWidth - spriteWidth - margin, 2000, true)
                                }
                                continue
                            }
                            runAnimation { animateParamTo(instance, "y", targetY, 2000, isMovingRight) }
                        }
                        action < 95 -> { // Xuống
                            val maxDown = screenHeight - spriteHeight - margin - instance.params.y
                            val targetY = if (maxDown > 180) {
                                instance.params.y + Random.nextInt(180, minOf(200, maxDown))
                            } else if (maxDown > 0) {
                                screenHeight - spriteHeight - margin
                            } else {
                                // Đã ở bottom → đi sang phải
                                runAnimation {
                                    animateParamTo(instance, "x", screenWidth - spriteWidth - margin, 2000, true)
                                }
                                continue
                            }
                            runAnimation { animateParamTo(instance, "y", targetY, 2000, isMovingRight) }
                        }
                        else -> { // Nhảy sang phải
                            runAnimation {
                                animateParamTo(instance, "x", screenWidth - spriteWidth - margin, 2000, isMovingRight) {
                                    isMovingRight = !isMovingRight
                                }
                            }
                        }
                    }
                }

                // ========== CẠNH PHẢI ==========
                isAtRight -> {
                    when {
                        action < 85 -> { // Lên
                            val maxUp = instance.params.y - margin
                            val targetY = if (maxUp > 180) {
                                instance.params.y - Random.nextInt(180, minOf(200, maxUp))
                            } else if (maxUp > 0) {
                                margin
                            } else {
                                // Đã ở top → đi sang trái
                                runAnimation {
                                    animateParamTo(instance, "x", 0, 2000, false)
                                }
                                continue
                            }
                            runAnimation { animateParamTo(instance, "y", targetY, 2000, isMovingRight) }
                        }
                        action < 95 -> { // Xuống
                            val maxDown = screenHeight - spriteHeight - margin - instance.params.y
                            val targetY = if (maxDown > 180) {
                                instance.params.y + Random.nextInt(180, minOf(200, maxDown))
                            } else if (maxDown > 0) {
                                screenHeight - spriteHeight - margin
                            } else {
                                // Đã ở bottom → đi sang trái
                                runAnimation {
                                    animateParamTo(instance, "x", 0, 2000, false)
                                }
                                continue
                            }
                            runAnimation { animateParamTo(instance, "y", targetY, 2000, isMovingRight) }
                        }
                        else -> { // Nhảy sang trái
                            runAnimation {
                                animateParamTo(instance, "x", 0, 2000, isMovingRight) {
                                    isMovingRight = !isMovingRight
                                }
                            }
                        }
                    }
                }

                // ========== Ở GIỮA ==========
                else -> {
                    when {
                        action < 10 -> { // Lên
                            val maxUp = instance.params.y - margin
                            val targetY = if (maxUp > 180) {
                                instance.params.y - Random.nextInt(180, minOf(200, maxUp))
                            } else if (maxUp > 0) {
                                margin
                            } else {
                                val targetX = if (isMovingRight) screenWidth - spriteWidth - margin else 0
                                runAnimation {
                                    animateParamTo(instance, "x", targetX, 2000, isMovingRight) {
                                        isMovingRight = !isMovingRight
                                    }
                                }
                                continue
                            }
                            runAnimation { animateParamTo(instance, "y", targetY, 2000, isMovingRight) }
                        }
                        action < 95 -> { // Xuống
                            val maxDown = screenHeight - spriteHeight - margin - instance.params.y
                            val targetY = if (maxDown > 180) {
                                instance.params.y + Random.nextInt(180, minOf(200, maxDown))
                            } else if (maxDown > 0) {
                                screenHeight - spriteHeight - margin
                            } else {
                                val targetX = if (isMovingRight) screenWidth - spriteWidth - margin else 0
                                runAnimation {
                                    animateParamTo(instance, "x", targetX, 2000, isMovingRight) {
                                        isMovingRight = !isMovingRight
                                    }
                                }
                                continue
                            }
                            runAnimation { animateParamTo(instance, "y", targetY, 2000, isMovingRight) }
                        }
                        else -> { // Nhảy ngang
                            val targetX = if (isMovingRight) screenWidth - spriteWidth - margin else 0
                            runAnimation {
                                animateParamTo(instance, "x", targetX, 2000, isMovingRight) {
                                    isMovingRight = !isMovingRight
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    @Composable
    fun SpriteContent(
        spriteState: SpriteState1,
        spriteFlip: SpriteFlip1?
    ) {
        ShimejiSprite(
            spriteState = spriteState,
            spriteFlip = spriteFlip
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
        val isMovingRight11 = axis == "x" && target > start
        val isMovingDown = axis == "y" && target > start
        val (screenWidth, screenHeight) = getScreenSize(this@FloatingSpriteService)
        Log.d("animateParamTo", "animateParamTo: $screenWidth, $screenHeight")
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
//            val flip = when {
//                axis == "x" && target > start -> SpriteFlip1.RIGHT
//                axis == "x" && target < start -> SpriteFlip1.LEFT
//////                axis == "y" && target > start -> SpriteFlip1.BOTTOM
//                params.y <= 0 -> SpriteFlip1.TOP
//                else -> SpriteFlip1.RIGHT
//            }

            // Cập nhật flip đúng cho sprite này
            val flip = when {
                params.y <= 0 -> SpriteFlip1.TOP
                params.x <= 0 ||  axis == "x" && (target < start && instance.controller.getState() == SpriteState1.WALKING) || (target > start && instance.controller.getState() == SpriteState1.DASH) -> SpriteFlip1.LEFT // Cạnh trái
                params.x >= screenWidth - spriteWidth  || axis == "x" && (target > start && instance.controller.getState() == SpriteState1.WALKING) || (target < start && instance.controller.getState() == SpriteState1.DASH) -> SpriteFlip1.RIGHT // Cạnh phải // Cạnh trên
                else -> if (isMovingRight) null else SpriteFlip1.RIGHT
            }
            Log.d("animateParamTo", "Đang di chuyển theo hướng: $flip")
            if (params.x <= 0 || params.x >= screenWidth - spriteWidth) {
                instance.controller.setState(SpriteState1.CLIMB)
            }else if(params.y >= screenHeight - spriteHeight){
                instance.controller.setState(SpriteState1.WALKING)
            }

            // Gọi updater của sprite này
//            val flipUpdater = instance.view.getTag(R.id.sprite_flip_updater) as? (SpriteFlip?) -> Unit
            instance.controller.setFlip(flip)

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
                while (!   instance.isDragging) {

                    // Thêm biến kiểm tra vị trí góc để dễ debug và xử lý
                    val isAtTop =    instance.params.y <= margin
                    val isAtBottom =    instance.params.y >= screenHeight - spriteHeight - margin
                    val isAtLeft =   instance. params.x <= 0
                    val isAtRight =    instance.params.x >= screenWidth - spriteWidth - margin
                    Log.d("duonghx11111", "aaaa ${instance.params.y} + ${instance.params.x}")
                    // Chọn tỷ lệ dựa trên vị trí (cạnh trái, cạnh phải, hoặc ở giữa)
                    val action = Random.nextInt(100)

                    when {
                        // Cạnh trái: lên 10%, xuống 85%, nhảy 5%
                        isAtLeft -> {  // Sử dụng isAtLeft thay vì params.x <= 0 để nhất quán
                            Log.d("duonghx11111", "canh trai")
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
                                    instance.controller.setState(SpriteState1.DASH)
                                    Log.d("duonghx", "canh trai nhay: ${instance.params.x}, ${instance.params.y}")
                                    val targetX = screenWidth - spriteWidth - margin
                                    animateParamTo(instance, "x", targetX, 700, isMovingRight)
                                }
                            }
                        }
                        // Cạnh phải: lên 85%, xuống 10%, nhảy 5%
                        isAtRight -> {  // Sử dụng isAtRight
                            Log.d("duonghx11111", "canh phai")
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
                                    instance.controller.setState(SpriteState1.DASH)
                                    Log.d("duonghx", "canh phai nhay: ${instance.params.x}, ${instance.params.y}")
                                    val targetX = 0
                                    animateParamTo(instance, "x", targetX, 700, isMovingRight)
                                }
                            }
                        }
                        else -> {
                            when {
                                action < 60 -> { // 0-59: 60% - Đi sang phải
//                                    instance.controller.setFlip(SpriteFlip1.LEFT)
                                    Log.d("aaaaa",",60")
                                    val maxRightDistance = screenWidth - spriteWidth - margin - instance.params.x
                                    if (maxRightDistance > 180) {
                                        val targetX = instance.params.x + Random.nextInt(180, min(200, maxRightDistance))
                                        animateParamTo(instance, "x", targetX, 2000, true) // true = sang phải
                                    } else if (maxRightDistance > 0) {
                                        val targetX = screenWidth - spriteWidth - margin
                                        animateParamTo(instance, "x", targetX, 2000, true)
                                    } else {
                                        // Đã ở góc phải dưới → buộc đi lên
                                        val targetY = margin
                                        animateParamTo(instance, "y", targetY, 2000, false)
                                    }
                                }

                                action < 100 -> { // 60-99: 40% - Đi sang trái
                                    Log.d("aaaaa","40")
//                                    instance.controller.setFlip(SpriteFlip1.RIGHT)
                                    val maxLeftDistance = instance.params.x - margin
                                    if (maxLeftDistance > 180) {
                                        val targetX = instance.params.x - Random.nextInt(180, min(200, maxLeftDistance))
                                        animateParamTo(instance, "x", targetX, 2000, false) // false = sang trái
                                    } else if (maxLeftDistance > 0) {
                                        val targetX = margin
                                        animateParamTo(instance, "x", targetX, 2000, false)
                                    } else {
                                        // Đã ở góc trái dưới → buộc đi lên
                                        val targetY = margin
                                        animateParamTo(instance, "y", targetY, 2000, true)
                                    }
                                }
                            }

                        }
                    }
                    // Thêm độ trễ nhỏ giữa các hành động để tránh di chuyển quá nhanh
//                    delay(100)
                }
            }
        }
    }
    // 👉 Hàm rơi xuống (gravity effect)
    private suspend fun fallDown(instance: SpriteInstance) {
        val (_, screenHeight) = getScreenSize(this@FloatingSpriteService)
        val spriteHeight = instance.view.height
        val groundY = screenHeight - spriteHeight

        instance.controller.setState(SpriteState1.FALL)

        while (instance.params.y < groundY) {
            if (instance.isDragging) {
                return
            }
            instance.params.y += 20
            windowManager.updateViewLayout(instance.view, instance.params)
            delay(10)
        }

        instance.params.y = groundY
//        windowManager.updateViewLayout(instance.view, instance.params)

        instance.controller.setState(SpriteState1.Bottom)
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
            val width = windowMetrics.bounds.width() - insets.left - insets.right
//            val height = windowMetrics.bounds.height() - insets.top - insets.bottom
            val height = windowMetrics.bounds.height()- insets.bottom
            width to height
        } else {
            val displayMetrics = context.resources.displayMetrics
            displayMetrics.widthPixels to displayMetrics.heightPixels
        }
    }

}
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

    fun getState() : SpriteState1 {
        return _state.value
    }
}
