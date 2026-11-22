package arfoxcode.joaohouto.assistivemenutool

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class AssistiveMenuAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo?.also {
            it.eventTypes          = 0
            it.feedbackType        = AccessibilityServiceInfo.FEEDBACK_GENERIC
            it.notificationTimeout = 0
            it.flags               = 0
            it.packageNames        = null
        }
        if (!FloatingButtonService.isRunning) tryStartFloatingService()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        handler.postDelayed({
            if (FloatingButtonService.isRunning)
                FloatingButtonService.notifyAccessibilityDisconnected()
        }, 500)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    fun execute(action: MenuAction) {
        when (action) {
            MenuAction.HOME       -> performGlobalAction(GLOBAL_ACTION_HOME)
            MenuAction.BACK       -> performGlobalAction(GLOBAL_ACTION_BACK)
            MenuAction.RECENTS    -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            MenuAction.LOCK       -> { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) }
            MenuAction.SCREENSHOT -> { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) }
            MenuAction.NOTIFICATIONS  -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            MenuAction.QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            else -> Unit
        }
    }

    private fun tryStartFloatingService() {
        try {
            val svc = Intent(this, FloatingButtonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc) else startService(svc)
        } catch (_: Exception) {}
    }

    companion object {
        var instance: AssistiveMenuAccessibilityService? = null
            private set
        val isEnabled get() = instance != null
    }
}
