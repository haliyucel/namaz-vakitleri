package com.namazvakti.app.alarm

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.namazvakti.app.MainActivity
import com.namazvakti.app.R
import com.namazvakti.app.domain.KerahatType
import com.namazvakti.app.model.PrayerType

enum class CountdownNotificationStyle(
    val pillBackgroundRes: Int,
    val accentColor: Int
) {
    PRAYER(
        pillBackgroundRes = R.drawable.bg_notif_pill_emerald,
        accentColor = Color.rgb(14, 107, 78)
    ),
    KERAHAT(
        pillBackgroundRes = R.drawable.bg_notif_pill_kerahat,
        accentColor = Color.rgb(198, 93, 7)
    )
}

data class CountdownTiming(
    val remainingMillis: Long,
    val chronometerBaseElapsed: Long,
    val timeoutAfterMillis: Long
) {
    companion object {
        private const val MIN_VALID_REMAINING_MILLIS = 1_000L

        /**
         * Pure helper for computing native Android Chronometer countdown parameters.
         * Returns null when the target instant has already passed so stale/negative timers never appear.
         */
        fun compute(
            nowWallMillis: Long,
            targetWallMillis: Long,
            nowElapsedRealtimeMillis: Long
        ): CountdownTiming? {
            val remaining = targetWallMillis - nowWallMillis
            if (remaining <= MIN_VALID_REMAINING_MILLIS) return null
            return CountdownTiming(
                remainingMillis = remaining,
                chronometerBaseElapsed = nowElapsedRealtimeMillis + remaining,
                timeoutAfterMillis = remaining
            )
        }
    }
}

/**
 * Posts zero-battery live-ticking countdown notifications using Android's native Chronometer
 * inside DecoratedCustomViewStyle + automatic OS dismissal via setTimeoutAfter(remainingMs).
 */
object CountdownNotificationHelper {

    const val FRIDAY_REMINDER_ID = 3500
    const val TEST_PRAYER_COUNTDOWN_ID = 7777
    const val TEST_KERAHAT_COUNTDOWN_ID = 7778

    fun prayerWarningId(prayer: PrayerType): Int = 3100 + prayer.ordinal

    fun kerahatId(kerahat: KerahatType): Int = 3400 + kerahat.ordinal

    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Posts a notification that chimes once and displays a live ticking countdown
     * until [targetWallMillis], automatically disappearing at 00:00.
     * Returns false if permission is missing or the target time has already passed.
     */
    fun postCountdownNotification(
        context: Context,
        id: Int,
        title: String,
        subtitle: String,
        detail: String,
        targetWallMillis: Long,
        style: CountdownNotificationStyle
    ): Boolean {
        if (!canPostNotifications(context)) return false
        val timing = CountdownTiming.compute(
            nowWallMillis = System.currentTimeMillis(),
            targetWallMillis = targetWallMillis,
            nowElapsedRealtimeMillis = SystemClock.elapsedRealtime()
        ) ?: return false

        val openIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val collapsedViews = RemoteViews(context.packageName, R.layout.notification_countdown).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_subtitle, subtitle)
            setInt(R.id.notif_countdown, "setBackgroundResource", style.pillBackgroundRes)
            setChronometerCountDown(R.id.notif_countdown, true)
            setChronometer(R.id.notif_countdown, timing.chronometerBaseElapsed, null, true)
        }

        val expandedViews = RemoteViews(context.packageName, R.layout.notification_countdown_big).apply {
            setTextViewText(R.id.notif_title, title)
            setTextViewText(R.id.notif_subtitle, subtitle)
            setTextViewText(R.id.notif_detail, detail)
            setInt(R.id.notif_countdown, "setBackgroundResource", style.pillBackgroundRes)
            setChronometerCountDown(R.id.notif_countdown, true)
            setChronometer(R.id.notif_countdown, timing.chronometerBaseElapsed, null, true)
        }

        val notif = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_WARNINGS)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setColor(style.accentColor)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setCustomHeadsUpContentView(collapsedViews)
            .setShowWhen(false)
            .setTimeoutAfter(timing.timeoutAfterMillis)
            .setSound(NotificationChannels.warningSoundUri(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(openIntent)
            .build()

        val nm = NotificationManagerCompat.from(context)
        return runCatching {
            // Cancel prior notification with same ID first so replacing 15m -> 5m always replays the chime cleanly
            nm.cancel(id)
            nm.notify(id, notif)
            true
        }.getOrDefault(false)
    }

    fun cancelPrayerCountdown(context: Context, prayer: PrayerType) {
        val nm = NotificationManagerCompat.from(context)
        runCatching {
            nm.cancel(prayerWarningId(prayer))
            // Legacy 5-min warning ID cleanup in case an older notification was active before update
            nm.cancel(3200 + prayer.ordinal)
            when (prayer) {
                PrayerType.OGLE -> {
                    nm.cancel(FRIDAY_REMINDER_ID)
                    nm.cancel(kerahatId(KerahatType.OGLE))
                }
                PrayerType.AKSAM -> {
                    nm.cancel(kerahatId(KerahatType.AKSAM))
                }
                else -> Unit
            }
        }
    }

    fun cancelKerahatCountdown(context: Context, kerahat: KerahatType) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(kerahatId(kerahat))
        }
    }
}
