package arfoxcode.joaohouto.assistivemenutool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class ServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Settings.canDrawOverlays(context)) return
        if (!isAccessibilityEnabled(context)) return
        try {
            val svc = Intent(context, FloatingButtonService::class.java).apply { action = FloatingButtonService.ACTION_RESTART }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(svc) else context.startService(svc)
        } catch (_: Exception) {}
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains(context.packageName, ignoreCase = true)
    }
}
