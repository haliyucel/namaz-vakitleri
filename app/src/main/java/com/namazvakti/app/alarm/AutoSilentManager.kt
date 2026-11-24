package com.namazvakti.app.alarm

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.namazvakti.app.MainActivity
import com.namazvakti.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class AutoSilentStatus(
    val isActive: Boolean = false,
    val prayerName: String = "",
    val restoreAtMillis: Long = 0L
)

/**
 * Silences the phone purely via AudioManager Silent Mode (RINGER_MODE_SILENT)
 * and ring/notification stream muting — WITHOUT turning on Do Not Disturb (Rahatsız Etme).
 */
object AutoSilentManager {
    private const val PREFS_NAME = "namaz_auto_silent_state"
    private const val KEY_ACTIVE = "active"
    private const val KEY_PRAYER_NAME = "prayer_name"
    private const val KEY_RESTORE_AT = "restore_at"
    private const val KEY_PREV_RINGER = "prev_ringer"
    private const val KEY_PREV_RING_VOL = "prev_ring_vol"
    private const val KEY_PREV_NOTIF_VOL = "prev_notif_vol"
    private const val KEY_PREV_SYS_VOL = "prev_sys_vol"

    private const val NOTIFICATION_ID_SILENT = 4201
    private const val NOTIFICATION_ID_RESTORED = 4202

    private val _status = MutableStateFlow(AutoSilentStatus())
    val status: StateFlow<AutoSilentStatus> = _status.asStateFlow()

    fun syncState(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val active = prefs.getBoolean(KEY_ACTIVE, false)
        val restoreAt = prefs.getLong(KEY_RESTORE_AT, 0L)
        val prayer = prefs.getString(KEY_PRAYER_NAME, "") ?: ""
        if (active && restoreAt > System.currentTimeMillis()) {
            _status.value = AutoSilentStatus(true, prayer, restoreAt)
        } else {
            _status.value = AutoSilentStatus(false)
        }
    }

    /** Android requires notification policy access solely to allow AudioManager.RINGER_MODE_SILENT. */
    fun hasSilentModePermission(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return nm?.isNotificationPolicyAccessGranted == true
    }

    fun enterSilentMode(
        context: Context,
        prayerName: String,
        durationSeconds: Long
    ): Boolean {
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val hasFullSilentPerm = hasSilentModePermission(appCtx)

        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadyActive = prefs.getBoolean(KEY_ACTIVE, false)

        val prevRinger = if (alreadyActive) {
            prefs.getInt(KEY_PREV_RINGER, AudioManager.RINGER_MODE_NORMAL)
        } else {
            am.ringerMode
        }
        val prevRingVol = if (alreadyActive) {
            prefs.getInt(KEY_PREV_RING_VOL, defaultLoudVolume(am, AudioManager.STREAM_RING))
        } else {
            am.getStreamVolume(AudioManager.STREAM_RING)
        }
        val prevNotifVol = if (alreadyActive) {
            prefs.getInt(KEY_PREV_NOTIF_VOL, defaultLoudVolume(am, AudioManager.STREAM_NOTIFICATION))
        } else {
            am.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        }
        val prevSysVol = if (alreadyActive) {
            prefs.getInt(KEY_PREV_SYS_VOL, defaultLoudVolume(am, AudioManager.STREAM_SYSTEM))
        } else {
            am.getStreamVolume(AudioManager.STREAM_SYSTEM)
        }

        // Put phone into pure Silent Mode (🔕) — NEVER enable Do Not Disturb (setInterruptionFilter)!
        var silentApplied = false
        if (hasFullSilentPerm) {
            silentApplied = runCatching {
                am.ringerMode = AudioManager.RINGER_MODE_SILENT
                true
            }.getOrDefault(false)
        }

        if (!silentApplied) {
            // Fallback without policy permission: mute ring/notification streams & switch to vibrate
            runCatching { am.setStreamVolume(AudioManager.STREAM_RING, 0, 0) }
            runCatching { am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0) }
            runCatching { am.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0) }
            runCatching { am.ringerMode = AudioManager.RINGER_MODE_VIBRATE }
        }

        val restoreAt = System.currentTimeMillis() + (durationSeconds * 1000L)
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_PRAYER_NAME, prayerName)
            .putLong(KEY_RESTORE_AT, restoreAt)
            .putInt(KEY_PREV_RINGER, prevRinger)
            .putInt(KEY_PREV_RING_VOL, prevRingVol)
            .putInt(KEY_PREV_NOTIF_VOL, prevNotifVol)
            .putInt(KEY_PREV_SYS_VOL, prevSysVol)
            .apply()

        _status.value = AutoSilentStatus(
            isActive = true,
            prayerName = prayerName,
            restoreAtMillis = restoreAt
        )

        PrayerAlarmScheduler.scheduleRestoreSound(appCtx, restoreAt)
        showOngoingSilentNotification(appCtx, prayerName, restoreAt)
        return true
    }

    fun restoreSoundMode(
        context: Context,
        restoreAlwaysLoud: Boolean,
        isManual: Boolean = false
    ) {
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val prevRinger = prefs.getInt(KEY_PREV_RINGER, AudioManager.RINGER_MODE_NORMAL)
        val prevRingVol = prefs.getInt(KEY_PREV_RING_VOL, -1)
        val prevNotifVol = prefs.getInt(KEY_PREV_NOTIF_VOL, -1)
        val prevSysVol = prefs.getInt(KEY_PREV_SYS_VOL, -1)

        prefs.edit()
            .putBoolean(KEY_ACTIVE, false)
            .putLong(KEY_RESTORE_AT, 0L)
            .apply()

        _status.value = AutoSilentStatus(isActive = false)
        PrayerAlarmScheduler.cancelRestoreSound(appCtx)
        NotificationManagerCompat.from(appCtx).cancel(NOTIFICATION_ID_SILENT)

        if (am != null) {
            val targetRinger = if (restoreAlwaysLoud) AudioManager.RINGER_MODE_NORMAL else prevRinger
            runCatching { am.ringerMode = targetRinger }

            if (targetRinger == AudioManager.RINGER_MODE_NORMAL) {
                val ringTarget = if (prevRingVol > 0) prevRingVol else defaultLoudVolume(am, AudioManager.STREAM_RING)
                val notifTarget = if (prevNotifVol > 0) prevNotifVol else defaultLoudVolume(am, AudioManager.STREAM_NOTIFICATION)
                val sysTarget = if (prevSysVol > 0) prevSysVol else defaultLoudVolume(am, AudioManager.STREAM_SYSTEM)

                runCatching { am.setStreamVolume(AudioManager.STREAM_RING, ringTarget, 0) }
                runCatching { am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, notifTarget, 0) }
                runCatching { am.setStreamVolume(AudioManager.STREAM_SYSTEM, sysTarget, 0) }
            }
        }

        if (!isManual) {
            showRestoredNotification(appCtx)
        }
    }

    /** Called on reboot: ensures the phone never stays stuck in silent mode. */
    fun handleBootRecovery(context: Context, restoreAlwaysLoud: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val active = prefs.getBoolean(KEY_ACTIVE, false)
        if (!active) return
        val restoreAt = prefs.getLong(KEY_RESTORE_AT, 0L)
        val prayer = prefs.getString(KEY_PRAYER_NAME, "Namaz") ?: "Namaz"
        if (restoreAt <= System.currentTimeMillis()) {
            restoreSoundMode(context, restoreAlwaysLoud, isManual = false)
        } else {
            _status.value = AutoSilentStatus(true, prayer, restoreAt)
            PrayerAlarmScheduler.scheduleRestoreSound(context, restoreAt)
            showOngoingSilentNotification(context, prayer, restoreAt)
        }
    }

    private fun defaultLoudVolume(am: AudioManager, stream: Int): Int {
        val max = am.getStreamMaxVolume(stream).coerceAtLeast(1)
        return (max * 0.7).roundToInt().coerceIn(1, max)
    }

    private fun showOngoingSilentNotification(context: Context, prayerName: String, restoreAtMillis: Long) {
        if (!canPostNotifications(context)) return
        val restoreTimeText = Instant.ofEpochMilli(restoreAtMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))

        val openIntent = PendingIntent.getActivity(
            context,
            4201,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val restoreIntent = PendingIntent.getBroadcast(
            context,
            4202,
            Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_MANUAL_RESTORE_SOUND
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_AUTO_SILENT)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle("Sessiz Mod Aktif ($prayerName)")
            .setContentText("Telefon $restoreTimeText saatinde tekrar sesli moda geçecek.")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, "Şimdi Sesliye Al", restoreIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SILENT, notif)
        }
    }

    private fun showRestoredNotification(context: Context) {
        if (!canPostNotifications(context)) return
        val notif = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_AUTO_SILENT)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle("Sesli Moda Geçildi")
            .setContentText("Namaz süresi tamamlandı, telefon tekrar sesli hale getirildi.")
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_RESTORED, notif)
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
