package com.namazvakti.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.namazvakti.app.MainActivity
import com.namazvakti.app.R
import com.namazvakti.app.alarm.AutoSilentManager
import com.namazvakti.app.alarm.NotificationChannels
import com.namazvakti.app.alarm.PrayerAlarmReceiver
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Compliant mediaPlayback Foreground Service that performs AudioManager
 * Silent/Normal mode transitions so Android 17 (API 37) Background Audio
 * Hardening does not silently drop setRingerMode/setStreamVolume calls.
 */
class AutoSilentService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        pendingRunnable?.let { handler.removeCallbacks(it) }

        when (action) {
            ACTION_ENTER_SILENT -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Namaz"
                val durationSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, 20L * 60L)
                    .coerceAtLeast(5L)
                val restoreAtMillis = System.currentTimeMillis() + durationSeconds * 1000L

                promoteToForeground(
                    buildOngoingSilentNotification(prayerName, restoreAtMillis)
                )

                // Wait briefly after startForeground so ActivityManager/AppOps updates UID foreground state,
                // then immediately detach notification & stop service so FGS does NOT stay alive for 20 minutes!
                val task = Runnable {
                    val ok = AutoSilentManager.applySilentModeNow(
                        context = this,
                        prayerName = prayerName,
                        durationSeconds = durationSeconds,
                        postStandaloneNotification = false
                    )
                    Log.i(TAG, "applySilentModeNow($prayerName, ${durationSeconds}s) -> ok=$ok")
                    if (!ok) {
                        // Retry once in case AMS UID proc-state propagation lagged slightly
                        handler.postDelayed({
                            val retryOk = AutoSilentManager.applySilentModeNow(
                                context = this,
                                prayerName = prayerName,
                                durationSeconds = durationSeconds,
                                postStandaloneNotification = false
                            )
                            Log.i(TAG, "applySilentModeNow retry -> ok=$retryOk")
                            finishServiceKeepingNotification()
                        }, 450L)
                    } else {
                        handler.postDelayed({ finishServiceKeepingNotification() }, 300L)
                    }
                }
                pendingRunnable = task
                handler.postDelayed(task, 250L)
                return START_NOT_STICKY
            }

            ACTION_RESTORE_SOUND -> {
                val restoreAlwaysLoud = intent.getBooleanExtra(EXTRA_RESTORE_ALWAYS_LOUD, true)
                val isManual = intent.getBooleanExtra(EXTRA_IS_MANUAL, false)

                promoteToForeground(buildRestoringNotification())

                val task = Runnable {
                    val ok = AutoSilentManager.applyRestoreSoundNow(
                        context = this,
                        restoreAlwaysLoud = restoreAlwaysLoud,
                        isManual = isManual
                    )
                    Log.i(TAG, "applyRestoreSoundNow(alwaysLoud=$restoreAlwaysLoud, manual=$isManual) -> ok=$ok")
                    if (!ok) {
                        handler.postDelayed({
                            val retryOk = AutoSilentManager.applyRestoreSoundNow(
                                context = this,
                                restoreAlwaysLoud = restoreAlwaysLoud,
                                isManual = isManual
                            )
                            Log.i(TAG, "applyRestoreSoundNow retry -> ok=$retryOk")
                            finishService()
                        }, 450L)
                    } else {
                        handler.postDelayed({ finishService() }, 300L)
                    }
                }
                pendingRunnable = task
                handler.postDelayed(task, 250L)
                return START_NOT_STICKY
            }

            else -> {
                finishService()
                return START_NOT_STICKY
            }
        }
    }

    private fun promoteToForeground(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildOngoingSilentNotification(prayerName: String, restoreAtMillis: Long) =
        NotificationCompat.Builder(this, NotificationChannels.CHANNEL_AUTO_SILENT)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle("Sessiz Mod Aktif ($prayerName)")
            .setContentText(
                "Telefon ${
                    Instant.ofEpochMilli(restoreAtMillis)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                } saatinde tekrar sesli moda geçecek."
            )
            .setWhen(restoreAtMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    4201,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                0,
                "Şimdi Sesliye Al",
                PendingIntent.getForegroundService(
                    this,
                    4202,
                    Intent(this, AutoSilentService::class.java).apply {
                        action = ACTION_RESTORE_SOUND
                        putExtra(EXTRA_RESTORE_ALWAYS_LOUD, true)
                        putExtra(EXTRA_IS_MANUAL, true)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun buildRestoringNotification() =
        NotificationCompat.Builder(this, NotificationChannels.CHANNEL_AUTO_SILENT)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle("Sesli Moda Geçiliyor")
            .setContentText("Telefon sesi geri açılıyor...")
            .setSilent(true)
            .build()

    private fun finishServiceKeepingNotification() {
        pendingRunnable?.let { handler.removeCallbacks(it) }
        pendingRunnable = null
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun finishService() {
        pendingRunnable?.let { handler.removeCallbacks(it) }
        pendingRunnable = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        pendingRunnable?.let { handler.removeCallbacks(it) }
        pendingRunnable = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AutoSilentService"
        const val NOTIFICATION_ID = 4201

        const val ACTION_ENTER_SILENT = "com.namazvakti.app.SERVICE_ENTER_SILENT"
        const val ACTION_RESTORE_SOUND = "com.namazvakti.app.SERVICE_RESTORE_SOUND"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_DURATION_SECONDS = "duration_seconds"
        const val EXTRA_RESTORE_ALWAYS_LOUD = "restore_always_loud"
        const val EXTRA_IS_MANUAL = "is_manual"

        fun startEnterSilent(context: Context, prayerName: String, durationSeconds: Long): Boolean {
            val intent = Intent(context.applicationContext, AutoSilentService::class.java).apply {
                action = ACTION_ENTER_SILENT
                putExtra(EXTRA_PRAYER_NAME, prayerName)
                putExtra(EXTRA_DURATION_SECONDS, durationSeconds)
            }
            return runCatching {
                ContextCompat.startForegroundService(context.applicationContext, intent)
                true
            }.onFailure { err ->
                Log.w(TAG, "startEnterSilent FGS failed, trying direct fallback", err)
                AutoSilentManager.applySilentModeNow(
                    context = context,
                    prayerName = prayerName,
                    durationSeconds = durationSeconds,
                    postStandaloneNotification = true
                )
            }.getOrDefault(false)
        }

        fun startRestoreSound(context: Context, restoreAlwaysLoud: Boolean, isManual: Boolean): Boolean {
            val intent = Intent(context.applicationContext, AutoSilentService::class.java).apply {
                action = ACTION_RESTORE_SOUND
                putExtra(EXTRA_RESTORE_ALWAYS_LOUD, restoreAlwaysLoud)
                putExtra(EXTRA_IS_MANUAL, isManual)
            }
            return runCatching {
                ContextCompat.startForegroundService(context.applicationContext, intent)
                true
            }.onFailure { err ->
                Log.w(TAG, "startRestoreSound FGS failed, trying direct fallback", err)
                AutoSilentManager.applyRestoreSoundNow(
                    context = context,
                    restoreAlwaysLoud = restoreAlwaysLoud,
                    isManual = isManual
                )
            }.getOrDefault(false)
        }
    }
}
