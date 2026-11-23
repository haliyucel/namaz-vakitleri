package com.namazvakti.app.alarm

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.namazvakti.app.MainActivity
import com.namazvakti.app.NamazApp
import com.namazvakti.app.R
import com.namazvakti.app.model.PrayerType
import com.namazvakti.app.service.AdhanPlaybackService

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? NamazApp ?: return
        val settings = app.settingsRepository.current()
        val action = intent.action ?: return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_RESCHEDULE_DAILY -> {
                AutoSilentManager.handleBootRecovery(context, settings.restoreAlwaysLoud)
                PrayerAlarmScheduler.rescheduleAll(context, settings, app.prayerTimesRepository)
            }

            ACTION_WARN_15 -> {
                val prayer = PrayerType.fromNameOrNull(intent.getStringExtra(EXTRA_PRAYER_TYPE)) ?: return
                val timeText = intent.getStringExtra(EXTRA_PRAYER_TIME_TEXT) ?: ""
                postWarningNotification(
                    context = context,
                    id = 3100 + prayer.ordinal,
                    title = "${prayer.displayName} Vaktine 15 Dakika Kaldı",
                    message = "${prayer.displayName} vakti saat $timeText. Hazırlık için 1. uyarı."
                )
            }

            ACTION_WARN_5 -> {
                val prayer = PrayerType.fromNameOrNull(intent.getStringExtra(EXTRA_PRAYER_TYPE)) ?: return
                val timeText = intent.getStringExtra(EXTRA_PRAYER_TIME_TEXT) ?: ""
                postWarningNotification(
                    context = context,
                    id = 3200 + prayer.ordinal,
                    title = "${prayer.displayName} Vaktine 5 Dakika Kaldı!",
                    message = "${prayer.displayName} vakti saat $timeText. 2. ve son uyarı."
                )
            }

            ACTION_PRAYER_TIME -> {
                val prayer = PrayerType.fromNameOrNull(intent.getStringExtra(EXTRA_PRAYER_TYPE)) ?: return
                val timeText = intent.getStringExtra(EXTRA_PRAYER_TIME_TEXT) ?: ""

                // 1. Auto-silent at prayer arrival (silences ringtones/notifications, allows Alarm-stream Adhan)
                val silenced = if (settings.autoSilentEnabled && prayer in settings.autoSilentPrayers) {
                    AutoSilentManager.enterSilentMode(
                        context = context,
                        prayerName = prayer.displayName,
                        durationSeconds = settings.autoSilentDurationMinutes * 60L
                    )
                } else false

                // 2. Optional Adhan recitation for this prayer
                val adhanStarted = if (settings.adhanEnabled && prayer.hasAdhan && prayer in settings.adhanEnabledPrayers) {
                    AdhanPlaybackService.start(context, prayer, isPreview = false)
                    true
                } else false

                // 3. If Adhan wasn't started, still notify prayer arrival clearly
                if (!adhanStarted) {
                    val silentSuffix = if (silenced) {
                        " Telefon ${settings.autoSilentDurationMinutes} dk sessize alındı."
                    } else ""
                    postWarningNotification(
                        context = context,
                        id = 3300 + prayer.ordinal,
                        title = "${prayer.displayName} Vakti Girdi ($timeText)",
                        message = "${settings.location.displayTitle} için ${prayer.displayName.lowercase()} vakti.$silentSuffix"
                    )
                }

                // Keep alarms fresh for next days
                PrayerAlarmScheduler.rescheduleAll(context, settings, app.prayerTimesRepository)
            }

            ACTION_RESTORE_SOUND -> {
                AutoSilentManager.restoreSoundMode(
                    context = context,
                    restoreAlwaysLoud = settings.restoreAlwaysLoud,
                    isManual = false
                )
            }

            ACTION_MANUAL_RESTORE_SOUND -> {
                AutoSilentManager.restoreSoundMode(
                    context = context,
                    restoreAlwaysLoud = settings.restoreAlwaysLoud,
                    isManual = true
                )
            }
        }
    }

    private fun postWarningNotification(context: Context, id: Int, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val openIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_WARNINGS)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSound(NotificationChannels.warningSoundUri(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notif)
        }
    }

    companion object {
        const val ACTION_WARN_15 = "com.namazvakti.app.ACTION_WARN_15"
        const val ACTION_WARN_5 = "com.namazvakti.app.ACTION_WARN_5"
        const val ACTION_PRAYER_TIME = "com.namazvakti.app.ACTION_PRAYER_TIME"
        const val ACTION_RESTORE_SOUND = "com.namazvakti.app.ACTION_RESTORE_SOUND"
        const val ACTION_MANUAL_RESTORE_SOUND = "com.namazvakti.app.ACTION_MANUAL_RESTORE_SOUND"
        const val ACTION_RESCHEDULE_DAILY = "com.namazvakti.app.ACTION_RESCHEDULE_DAILY"

        const val EXTRA_PRAYER_TYPE = "prayer_type"
        const val EXTRA_PRAYER_TIME_TEXT = "prayer_time_text"
    }
}
