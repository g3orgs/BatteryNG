package de.batteryng.mvp.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.util.TypedValue
import de.batteryng.mvp.MainActivity
import de.batteryng.mvp.AppAppearance
import de.batteryng.mvp.R
import de.batteryng.mvp.measurement.BatteryCurrentReader
import de.batteryng.mvp.measurement.BatteryCurrentReading
import de.batteryng.mvp.measurement.CurrentUnitFormatter
import de.batteryng.mvp.measurement.CurrentUnitPreferences

/** Foreground service for the live battery-current overlay. */
class CurrentOverlayService : Service() {
    private lateinit var reader: BatteryCurrentReader
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: RotatedOverlayContainer? = null
    private var overlayContent: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var overlayValue: TextView? = null
    private var overlayPlug: ImageView? = null
    private var dragStartRawX = 0f
    private var dragStartRawY = 0f
    private var dragStartWindowX = 0
    private var dragStartWindowY = 0
    private var lastPluggedIn: Boolean? = null

    private val refreshTask = object : Runnable {
        override fun run() {
            if (!Settings.canDrawOverlays(this@CurrentOverlayService)) {
                stopSelf()
                return
            }
            val reading = reader.read()
            renderReading(reading)
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    private fun renderReading(reading: BatteryCurrentReading) {
        lastPluggedIn = reading.isPluggedIn
        applyOverlayVisibility()
        val unit = CurrentUnitPreferences.load(this)
        overlayValue?.text = reading.currentMilliAmps?.let {
            CurrentUnitFormatter.format(it, unit)
        } ?: CurrentUnitFormatter.unavailable(unit)
        overlayPlug?.visibility = if (reading.isPluggedIn == true) View.VISIBLE else View.GONE
    }

    override fun onCreate() {
        super.onCreate()
        reader = BatteryCurrentReader(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        activeInstance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: RuntimeException) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (overlayView == null) {
            try {
                addOverlay()
            } catch (_: RuntimeException) {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        updateTouchMode()
        isRunning = true
        handler.removeCallbacks(refreshTask)
        handler.post(refreshTask)
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun addOverlay() {
        val content = LayoutInflater.from(this).inflate(R.layout.current_overlay, null)
        val view = RotatedOverlayContainer(this).apply {
            isClickable = true
            rotationDegrees = OverlayPreferences.load(this@CurrentOverlayService).rotationDegrees
            addView(content, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ))
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> keepInsideDisplay() }
        }
        val windowType = if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                if (appForeground) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val savedPosition = OverlayPreferences.loadPosition(this@CurrentOverlayService)
            x = savedPosition?.first ?: dp(8)
            // The default is below Network Monitor Mini; later drags are persisted.
            y = savedPosition?.second ?: statusBarHeight() + dp(44)
            alpha = 1f
        }
        attachDragHandler(view, params)
        windowManager.addView(view, params)
        overlayView = view
        overlayContent = content
        overlayParams = params
        overlayValue = view.findViewById(R.id.overlayValue)
        overlayPlug = view.findViewById(R.id.overlayPlug)
        applyAppearance()
    }

    private fun attachDragHandler(view: View, params: WindowManager.LayoutParams) {
        view.setOnTouchListener { touchedView, event ->
            if (!appForeground) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartRawX = event.rawX
                    dragStartRawY = event.rawY
                    dragStartWindowX = params.x
                    dragStartWindowY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val screen = resources.displayMetrics
                    val maxX = (screen.widthPixels - touchedView.width).coerceAtLeast(0)
                    val maxY = (screen.heightPixels - touchedView.height).coerceAtLeast(0)
                    params.x = (dragStartWindowX + event.rawX - dragStartRawX).toInt()
                        .coerceIn(0, maxX)
                    params.y = (dragStartWindowY + event.rawY - dragStartRawY).toInt()
                        .coerceIn(0, maxY)
                    updateOverlayLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    OverlayPreferences.savePosition(this, params.x, params.y)
                    touchedView.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateOverlayLayout(view: View, params: WindowManager.LayoutParams) {
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: RuntimeException) {
            // A revoked overlay permission can remove the window during a drag.
        }
    }

    private fun keepInsideDisplay() {
        val view = overlayView ?: return
        val params = overlayParams ?: return
        val screen = resources.displayMetrics
        val nextX = params.x.coerceIn(0, (screen.widthPixels - view.width).coerceAtLeast(0))
        val nextY = params.y.coerceIn(0, (screen.heightPixels - view.height).coerceAtLeast(0))
        if (params.x != nextX || params.y != nextY) {
            params.x = nextX
            params.y = nextY
            updateOverlayLayout(view, params)
            OverlayPreferences.savePosition(this, nextX, nextY)
        }
    }

    private fun updateTouchMode() {
        val view = overlayView ?: return
        val params = overlayParams ?: return
        if (appForeground && view.visibility == View.VISIBLE) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            OverlayPreferences.savePosition(this, params.x, params.y)
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        params.alpha = 1f
        updateOverlayLayout(view, params)
    }

    private fun applyOverlayVisibility(showOnlyWhilePlugged: Boolean =
        OverlayPreferences.load(this).showOnlyWhilePlugged
    ) {
        val view = overlayView ?: return
        val visibility = if (!showOnlyWhilePlugged || lastPluggedIn == true) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        if (view.visibility != visibility) {
            view.visibility = visibility
            updateTouchMode()
            if (visibility == View.VISIBLE) view.post { keepInsideDisplay() }
        }
    }

    private fun applyAppearance() {
        val view = overlayView ?: return
        val style = OverlayPreferences.load(this)
        view.rotationDegrees = style.rotationDegrees
        overlayValue?.apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSizeSp.toFloat())
            setTextColor(style.textColor)
        }
        overlayPlug?.imageTintList = ColorStateList.valueOf(style.textColor)
        val rgb = style.backgroundColor
        val alpha = (255 * style.backgroundOpacityPercent / 100.0f).toInt()
        overlayContent?.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb)))
        }
        renderReading(reader.read())
        overlayParams?.let { updateOverlayLayout(view, it) }
        view.post { keepInsideDisplay() }
    }

    private fun statusBarHeight(): Int {
        @Suppress("DiscouragedApi")
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id != 0) resources.getDimensionPixelSize(id) else dp(24)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun localizedString(id: Int): String = AppAppearance.wrap(this).getString(id)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                localizedString(R.string.overlay_notification_title),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = localizedString(R.string.overlay_notification_description)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CurrentOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(R.drawable.ic_overlay_notification)
            .setContentTitle(localizedString(R.string.overlay_notification_title))
            .setContentText(localizedString(R.string.overlay_notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                localizedString(R.string.overlay_notification_stop),
                stopIntent,
            )
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshTask)
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: RuntimeException) {
                // The system may already have removed the window.
            }
        }
        overlayView = null
        overlayContent = null
        overlayParams = null
        isRunning = false
        if (activeInstance === this) activeInstance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "batteryng_overlay"
        private const val NOTIFICATION_ID = 101
        private const val REFRESH_INTERVAL_MS = 2_000L
        private const val ACTION_START = "de.batteryng.mvp.overlay.START"
        private const val ACTION_STOP = "de.batteryng.mvp.overlay.STOP"

        @Volatile
        private var activeInstance: CurrentOverlayService? = null

        @Volatile
        private var appForeground: Boolean = false

        @Volatile
        var isRunning: Boolean = false
            private set

        /** MainActivity calls this in onResume/onPause to permit dragging only there. */
        fun setAppForeground(foreground: Boolean) {
            appForeground = foreground
            activeInstance?.let { service ->
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    service.updateTouchMode()
                } else {
                    service.handler.post { service.updateTouchMode() }
                }
            }
        }

        /** Reapply saved font and color preferences without restarting the service. */
        fun refreshAppearance() {
            activeInstance?.let { service ->
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    service.applyAppearance()
                } else {
                    service.handler.post { service.applyAppearance() }
                }
            }
        }

        /** Returns false if overlay access is absent or the service could not start. */
        fun start(context: Context): Boolean {
            if (!Settings.canDrawOverlays(context)) return false
            val intent = Intent(context, CurrentOverlayService::class.java).setAction(ACTION_START)
            return try {
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (_: RuntimeException) {
                false
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CurrentOverlayService::class.java))
        }
    }
}
