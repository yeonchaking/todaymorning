package com.yeon.todaymorning.mission

// 1.0 릴리즈: 플로팅 위젯 비활성화 — 파일 전체 주석처리 (2026-07-12).
// P2 재활성화 시: 아래 블록 주석 해제 + Manifest SYSTEM_ALERT_WINDOW 권한 복원
// + MissionService/TimeAttackScreen 의 "플로팅 위젯 비활성화" 주석들 해제.
// 재도입 시 "우리 앱이 전면일 때 오버레이 숨기기"(TODO P2) 함께 처리할 것.
/*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.yeon.todaymorning.ui.MainActivity
import kotlin.math.abs

/**
 * 미션 진행 중 다른 앱 위에 떠 있는 작은 플로팅 위젯.
 *
 * - WindowManager + TYPE_APPLICATION_OVERLAY 로 시스템 오버레이를 그린다(Compose 가 아니라
 *   전통 View — 오버레이용 LifecycleOwner 세팅이 필요 없어 간단·안정적).
 * - 남은시간 + 다음 버스를 보여주고, 드래그로 이동, 탭하면 앱(타임어택)으로 진입한다.
 * - SYSTEM_ALERT_WINDOW 권한이 없으면 [show] 는 조용히 아무것도 하지 않는다.
 *
 * View 조작은 반드시 메인 스레드에서 호출해야 한다(호출부가 보장).
 */
class MissionOverlay(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var root: LinearLayout? = null
    private var timeView: TextView? = null
    private var busView: TextView? = null

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = dp(12)
        y = dp(120)
    }

    val isShown: Boolean get() = root != null

    fun canDraw(): Boolean = Settings.canDrawOverlays(context)

    /** 오버레이를 화면에 추가. 이미 떠 있거나 권한이 없으면 아무것도 안 함. */
    fun show() {
        if (root != null || !canDraw()) return

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(0xF21565C0.toInt())  // 반투명 블루
            }
        }
        val time = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 22f
            text = "--:--"
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val bus = TextView(context).apply {
            setTextColor(0xFFE3F2FD.toInt())
            textSize = 12.5f
            text = "도착 정보…"
            setLineSpacing(dp(2).toFloat(), 1f)  // 두 줄(버스 2대) 간격
        }
        card.addView(time)
        card.addView(bus)

        attachTouch(card)

        runCatching { wm.addView(card, params) }
            .onSuccess { root = card; timeView = time; busView = bus }
    }

    /** 표시 텍스트 + 불투명도 갱신(메인 스레드). busText 는 줄바꿈(\n) 포함 가능. opacity %(30~100). */
    fun update(timeText: String, busText: String, opacity: Int) {
        timeView?.text = timeText
        busView?.text = busText
        root?.alpha = (opacity.coerceIn(30, 100)) / 100f
    }

    fun hide() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null; timeView = null; busView = null
    }

    /** 드래그 이동 + (거의 안 움직였으면) 탭 → 앱 열기. */
    private fun attachTouch(view: View) {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        var downX = 0f; var downY = 0f
        var startX = 0; var startY = 0
        var dragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = params.x; startY = params.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX); val dy = (event.rawY - downY)
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) dragging = true
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        runCatching { wm.updateViewLayout(root, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) openApp()
                    true
                }
                else -> false
            }
        }
    }

    private fun openApp() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_FROM_ALARM, true)
        }
        runCatching { context.startActivity(intent) }
    }

    private fun dp(v: Int): Int = (v * context.resources.displayMetrics.density).toInt()
}
*/
