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
import com.namazvakti.app.domain.KerahatType
import com.namazvakti.app.model.PrayerType
import com.namazvakti.app.service.AdhanPlaybackService
import com.namazvakti.app.widget.PrayerWidgetProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

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
                val targetMillis = intent.getLongExtra(EXTRA_TARGET_TIME_MILLIS, 0L)
                if (targetMillis > 0L) {
                    CountdownNotificationHelper.postCountdownNotification(
                        context = context,
                        id = CountdownNotificationHelper.prayerWarningId(prayer),
                        title = "${prayer.displayName} Vaktine Kalan",
                        subtitle = "Saat $timeText • 15 dk uyarısı",
                        detail = "${prayer.displayName} vakti saat $timeText. Hazırlık için 1. uyarı; vakit girene kadar geri sayar.",
                        targetWallMillis = targetMillis,
                        style = CountdownNotificationStyle.PRAYER
                    )
                } else {
                    postWarningNotification(
                        context = context,
                        id = CountdownNotificationHelper.prayerWarningId(prayer),
                        title = "${prayer.displayName} Vaktine 15 Dakika Kaldı",
                        message = "${prayer.displayName} vakti saat $timeText. Hazırlık için 1. uyarı."
                    )
                }
            }

            ACTION_WARN_5 -> {
                val prayer = PrayerType.fromNameOrNull(intent.getStringExtra(EXTRA_PRAYER_TYPE)) ?: return
                val timeText = intent.getStringExtra(EXTRA_PRAYER_TIME_TEXT) ?: ""
                val targetMillis = intent.getLongExtra(EXTRA_TARGET_TIME_MILLIS, 0L)
                if (targetMillis > 0L) {
                    CountdownNotificationHelper.postCountdownNotification(
                        context = context,
                        id = CountdownNotificationHelper.prayerWarningId(prayer),
                        title = "${prayer.displayName} Vaktine Kalan",
                        subtitle = "Saat $timeText • 5 dk son uyarı!",
                        detail = "${prayer.displayName} vakti saat $timeText. 2. ve son uyarı; vakit girene kadar geri sayar.",
                        targetWallMillis = targetMillis,
                        style = CountdownNotificationStyle.PRAYER
                    )
                } else {
                    postWarningNotification(
                        context = context,
                        id = CountdownNotificationHelper.prayerWarningId(prayer),
                        title = "${prayer.displayName} Vaktine 5 Dakika Kaldı!",
                        message = "${prayer.displayName} vakti saat $timeText. 2. ve son uyarı."
                    )
                }
            }

            ACTION_KERAHAT_START -> {
                val kerahatName = intent.getStringExtra(EXTRA_KERAHAT_TYPE) ?: return
                val kerahat = KerahatType.entries.firstOrNull { it.name == kerahatName } ?: return
                val rangeText = intent.getStringExtra(EXTRA_PRAYER_TIME_TEXT) ?: ""
                val endText = intent.getStringExtra(EXTRA_KERAHAT_END_TEXT) ?: ""
                val targetMillis = intent.getLongExtra(EXTRA_TARGET_TIME_MILLIS, 0L)
                if (targetMillis > 0L) {
                    val subtitle = if (endText.isNotEmpty()) {
                        "Çıkış: $endText ($rangeText)"
                    } else {
                        rangeText
                    }
                    CountdownNotificationHelper.postCountdownNotification(
                        context = context,
                        id = CountdownNotificationHelper.kerahatId(kerahat),
                        title = "${kerahat.title} Çıkmasına Kalan",
                        subtitle = subtitle,
                        detail = "${kerahat.fiqhNote} ($rangeText)",
                        targetWallMillis = targetMillis,
                        style = CountdownNotificationStyle.KERAHAT
                    )
                } else {
                    postWarningNotification(
                        context = context,
                        id = CountdownNotificationHelper.kerahatId(kerahat),
                        title = "${kerahat.title} Başladı ($rangeText)",
                        message = kerahat.fiqhNote
                    )
                }
            }

            ACTION_KERAHAT_END -> {
                val kerahatName = intent.getStringExtra(EXTRA_KERAHAT_TYPE)
                val kerahat = KerahatType.entries.firstOrNull { it.name == kerahatName }
                if (kerahat != null) {
                    CountdownNotificationHelper.cancelKerahatCountdown(context, kerahat)
                }
            }

            ACTION_FRIDAY_REMINDER -> {
                val timeText = intent.getStringExtra(EXTRA_PRAYER_TIME_TEXT) ?: ""
                val leadMins = intent.getIntExtra(EXTRA_FRIDAY_LEAD_MINUTES, settings.fridayReminderMinutesBefore)
                val targetMillis = intent.getLongExtra(EXTRA_TARGET_TIME_MILLIS, 0L)
                if (targetMillis > 0L) {
                    CountdownNotificationHelper.postCountdownNotification(
                        context = context,
                        id = CountdownNotificationHelper.FRIDAY_REMINDER_ID,
                        title = "Cuma Namazına Kalan",
                        subtitle = "Öğle vakti $timeText • Hayırlı Cumalar",
                        detail = "${settings.location.displayTitle} için Cuma (Öğle) vakti saat $timeText ($leadMins dk hazırlık hatırlatıcısı).",
                        targetWallMillis = targetMillis,
                        style = CountdownNotificationStyle.PRAYER
                    )
                } else {
                    postWarningNotification(
                        context = context,
                        id = CountdownNotificationHelper.FRIDAY_REMINDER_ID,
                        title = "Hayırlı Cumalar • Cuma Namazına $leadMins dk Kaldı",
                        message = "${settings.location.displayTitle} için Cuma (Öğle) vakti saat $timeText. Hazırlık hatırlatıcısı."
                    )
                }
            }

            ACTION_PRAYER_TIME -> {
                val prayer = PrayerType.fromNameOrNull(intent.getStringExtra(EXTRA_PRAYER_TYPE)) ?: return
                val timeText = intent.getStringExtra(EXTRA_PRAYER_TIME_TEXT) ?: ""

                // Clear pre-prayer & ending kerahat countdown notifications immediately on prayer arrival
                CountdownNotificationHelper.cancelPrayerCountdown(context, prayer)

                // 1. Auto-silent at prayer arrival using this prayer's configured duration (Cuma override on Fridays)
                val zone = runCatching { ZoneId.of(settings.location.timezoneId) }
                    .getOrDefault(ZoneId.systemDefault())
                val dayOfWeek = LocalDate.now(zone).dayOfWeek
                val silentMinutes = settings.silentDurationFor(prayer, dayOfWeek)
                val silentLabel = if (prayer == PrayerType.OGLE && dayOfWeek == DayOfWeek.FRIDAY) {
                    "Cuma"
                } else {
                    prayer.displayName
                }

                val silenced = if (settings.autoSilentEnabled && prayer in settings.autoSilentPrayers) {
                    AutoSilentManager.enterSilentMode(
                        context = context,
                        prayerName = silentLabel,
                        durationSeconds = silentMinutes * 60L
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
                        " Telefon $silentMinutes dk sessize alındı."
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

            ACTION_TEST_ENTER_SILENT -> {
                val durationSec = intent.getLongExtra(EXTRA_TEST_DURATION_SECONDS, 15L).coerceIn(5L, 120L)
                AutoSilentManager.enterSilentMode(
                    context = context,
                    prayerName = "Test",
                    durationSeconds = durationSec
                )
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

        // Update Home Screen Widget only when prayer/kerahat/date/time actually changed
        if (action in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                ACTION_RESCHEDULE_DAILY,
                ACTION_PRAYER_TIME,
                ACTION_KERAHAT_START,
                ACTION_KERAHAT_END
            )
        ) {
            PrayerWidgetProvider.updateAll(context)
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
        const val ACTION_KERAHAT_START = "com.namazvakti.app.ACTION_KERAHAT_START"
        const val ACTION_KERAHAT_END = "com.namazvakti.app.ACTION_KERAHAT_END"
        const val ACTION_FRIDAY_REMINDER = "com.namazvakti.app.ACTION_FRIDAY_REMINDER"
        const val ACTION_PRAYER_TIME = "com.namazvakti.app.ACTION_PRAYER_TIME"
        const val ACTION_TEST_ENTER_SILENT = "com.namazvakti.app.ACTION_TEST_ENTER_SILENT"
        const val ACTION_RESTORE_SOUND = "com.namazvakti.app.ACTION_RESTORE_SOUND"
        const val ACTION_MANUAL_RESTORE_SOUND = "com.namazvakti.app.ACTION_MANUAL_RESTORE_SOUND"
        const val ACTION_RESCHEDULE_DAILY = "com.namazvakti.app.ACTION_RESCHEDULE_DAILY"

        const val EXTRA_PRAYER_TYPE = "prayer_type"
        const val EXTRA_KERAHAT_TYPE = "kerahat_type"
        const val EXTRA_PRAYER_TIME_TEXT = "prayer_time_text"
        const val EXTRA_KERAHAT_END_TEXT = "kerahat_end_text"
        const val EXTRA_TARGET_TIME_MILLIS = "target_time_millis"
        const val EXTRA_FRIDAY_LEAD_MINUTES = "friday_lead_minutes"
        const val EXTRA_TEST_DURATION_SECONDS = "test_duration_seconds"
    }
}
