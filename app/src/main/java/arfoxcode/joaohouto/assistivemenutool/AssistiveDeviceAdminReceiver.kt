package arfoxcode.joaohouto.assistivemenutool

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserHandle

class AssistiveDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // Admin aktif — restart service jika belum jalan
        if (!FloatingButtonService.isRunning) {
            try {
                val svc = Intent(context, FloatingButtonService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(svc)
                } else {
                    context.startService(svc)
                }
            } catch (_: Exception) {}
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // Admin dicabut — tidak perlu aksi khusus
    }

    override fun onPasswordFailed(context: Context, intent: Intent, user: UserHandle) {
        super.onPasswordFailed(context, intent, user)
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, AssistiveDeviceAdminReceiver::class.java)
        }

        fun isAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(
                android.app.admin.DevicePolicyManager::class.java
            )
            return dpm.isAdminActive(getComponentName(context))
        }

        /** Lock screen via DevicePolicyManager — backup dari accessibility */
        fun lockScreen(context: Context): Boolean {
            return try {
                val dpm = context.getSystemService(
                    android.app.admin.DevicePolicyManager::class.java
                )
                if (dpm.isAdminActive(getComponentName(context))) {
                    dpm.lockNow()
                    true
                } else false
            } catch (_: Exception) { false }
        }
    }
}
