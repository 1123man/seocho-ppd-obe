package com.seocho.ppd.obe.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.seocho.ppd.obe.MainActivity

/**
 * 기기 부팅 완료 시 앱을 자동 실행하는 BroadcastReceiver.
 *
 * SYSTEM_ALERT_WINDOW 권한이 있으면 직접 Activity 시작 가능 (백그라운드 제한 우회).
 * 없으면 ForegroundService + Full-Screen Intent로 시도.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bootActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
        if (intent.action in bootActions) {
            if (Settings.canDrawOverlays(context)) {
                // SYSTEM_ALERT_WINDOW 권한이 있으면 직접 Activity 시작 가능
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(launchIntent)
            } else {
                // 권한 없으면 ForegroundService + Full-Screen Intent로 시도
                val serviceIntent = Intent(context, BootForegroundService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
