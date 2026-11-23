package com.namazvakti.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.namazvakti.app.data.PrayerTimesRepository
import com.namazvakti.app.model.AppSettings
import com.namazvakti.app.model.PrayerType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PrayerAlarmScheduler {

    private const val REQ_RESTORE_SOUND = 9901
    private const val REQ_DAILY_RESCHEDULE = 9902
    private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return am.canScheduleExactAlarms()
    }

    fun rescheduleAll(
        context: Context,
        settings: AppSettings,
        prayerTimesRepository: PrayerTimesRepository
    ) {
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val zone = runCatching { ZoneId.of(settings.location.timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val today = LocalDate.now(zone)
        val nowMillis = System.currentTimeMillis()

        // Cancel all existing 2-day prayer alarms cleanly before scheduling fresh ones
        for (dayOffset in 0..1) {
            for (prayer in PrayerType.entries) {
                cancelAlarm(appCtx, am, requestCode(dayOffset, prayer, 1), PrayerAlarmReceiver.ACTION_WARN_15)
                cancelAlarm(appCtx, am, requestCode(dayOffset, prayer, 2), PrayerAlarmReceiver.ACTION_WARN_5)
                cancelAlarm(appCtx, am, requestCode(dayOffset, prayer, 3), PrayerAlarmReceiver.ACTION_PRAYER_TIME)
            }
        }

        // Schedule today's remaining + tomorrow's prayers
        for (dayOffset in 0..1) {
            val targetDate = today.plusDays(dayOffset.toLong())
            val prayerDay = prayerTimesRepository.getPrayerDayImmediate(
                date = targetDate,
                location = settings.location,
                method = settings.calculationMethod
            )

            for (prayer in PrayerType.entries) {
                val prayerZdt = prayerDay.zonedDateTime(prayer)
                val prayerMillis = prayerZdt.toInstant().toEpochMilli()
                val timeStr = prayerZdt.toLocalTime().format(TIME_FMT)

                val alertEnabled = prayer in settings.alertEnabledPrayers

                // 1. 15-minute first warning
                if (settings.warn15MinEnabled && alertEnabled) {
                    val trigger15 = prayerMillis - 15L * 60_000L
                    if (trigger15 > nowMillis + 2_000L) {
                        scheduleExact(
                            context = appCtx,
                            am = am,
                            reqCode = requestCode(dayOffset, prayer, 1),
                            action = PrayerAlarmReceiver.ACTION_WARN_15,
                            prayer = prayer,
                            prayerTimeText = timeStr,
                            triggerAtMillis = trigger15
                        )
                    }
                }

                // 2. 5-minute second warning
                if (settings.warn5MinEnabled && alertEnabled) {
                    val trigger5 = prayerMillis - 5L * 60_000L
                    if (trigger5 > nowMillis + 2_000L) {
                        scheduleExact(
                            context = appCtx,
                            am = am,
                            reqCode = requestCode(dayOffset, prayer, 2),
                            action = PrayerAlarmReceiver.ACTION_WARN_5,
                            prayer = prayer,
                            prayerTimeText = timeStr,
                            triggerAtMillis = trigger5
                        )
                    }
                }

                // 3. Prayer time arrival (Adhan + Auto-Silent + Arrival notification)
                val needsArrival =
                    (settings.adhanEnabled && prayer.hasAdhan && prayer in settings.adhanEnabledPrayers) ||
                        (settings.autoSilentEnabled && prayer in settings.autoSilentPrayers) ||
                        alertEnabled

                if (needsArrival && prayerMillis > nowMillis + 2_000L) {
                    scheduleExact(
                        context = appCtx,
                        am = am,
                        reqCode = requestCode(dayOffset, prayer, 3),
                        action = PrayerAlarmReceiver.ACTION_PRAYER_TIME,
                        prayer = prayer,
                        prayerTimeText = timeStr,
                        triggerAtMillis = prayerMillis
                    )
                }
            }
        }

        // Daily rollover reschedule at 00:05 tomorrow
        val nextMidnight = today.plusDays(1).atTime(0, 5).atZone(zone).toInstant().toEpochMilli()
        val dailyPi = PendingIntent.getBroadcast(
            appCtx,
            REQ_DAILY_RESCHEDULE,
            Intent(appCtx, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_RESCHEDULE_DAILY
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarmCompat(appCtx, am, nextMidnight, dailyPi)
    }

    fun scheduleRestoreSound(context: Context, triggerAtMillis: Long) {
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            appCtx,
            REQ_RESTORE_SOUND,
            Intent(appCtx, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_RESTORE_SOUND
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarmCompat(appCtx, am, triggerAtMillis, pi)
    }

    fun cancelRestoreSound(context: Context) {
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            appCtx,
            REQ_RESTORE_SOUND,
            Intent(appCtx, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_RESTORE_SOUND
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun scheduleExact(
        context: Context,
        am: AlarmManager,
        reqCode: Int,
        action: String,
        prayer: PrayerType,
        prayerTimeText: String,
        triggerAtMillis: Long
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TYPE, prayer.name)
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME_TEXT, prayerTimeText)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setAlarmCompat(context, am, triggerAtMillis, pi)
    }

    private fun cancelAlarm(context: Context, am: AlarmManager, reqCode: Int, action: String) {
        val pi = PendingIntent.getBroadcast(
            context,
            reqCode,
            Intent(context, PrayerAlarmReceiver::class.java).apply { this.action = action },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun setAlarmCompat(context: Context, am: AlarmManager, triggerAtMillis: Long, pi: PendingIntent) {
        if (canScheduleExactAlarms(context)) {
            runCatching {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }.onFailure {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun requestCode(dayOffset: Int, prayer: PrayerType, eventKind: Int): Int =
        1000 + dayOffset * 100 + prayer.ordinal * 10 + eventKind
}
