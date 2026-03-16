package com.seocho.ppd.obe.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.seocho.ppd.obe.MainActivity
import com.seocho.ppd.obe.R

/**
 * 부팅 시 Full-Screen Intent로 Activity를 시작하는 ForegroundService.
 * Android 10+ 백그라운드 Activity 시작 제한을 우회한다.
 * BootReceiver → ForegroundService → Full-Screen Intent → MainActivity
 */
class BootForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "부팅 자동 시작",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("서초구 운전자앱")
            .setContentText("앱을 시작하는 중...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "boot_fullscreen_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
