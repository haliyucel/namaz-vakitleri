package com.namazvakti.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.SystemClock
import android.widget.RemoteViews
import com.namazvakti.app.MainActivity
import com.namazvakti.app.NamazApp
import com.namazvakti.app.R
import com.namazvakti.app.domain.KerahatCalculator
import com.namazvakti.app.model.PrayerType
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PrayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_REFRESH) {
            updateAll(context)
        }
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.namazvakti.app.ACTION_WIDGET_REFRESH"
        private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

        fun updateAll(context: Context) {
            val appCtx = context.applicationContext
            val awm = AppWidgetManager.getInstance(appCtx) ?: return
            val ids = awm.getAppWidgetIds(ComponentName(appCtx, PrayerWidgetProvider::class.java))
            if (ids.isEmpty()) return
            for (id in ids) {
                updateSingleWidget(appCtx, awm, id)
            }
        }

        fun requestPin(context: Context): Boolean {
            val awm = AppWidgetManager.getInstance(context) ?: return false
            if (!awm.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, PrayerWidgetProvider::class.java)
            return runCatching {
                awm.requestPinAppWidget(provider, null, null)
            }.getOrDefault(false)
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val app = context.applicationContext as? NamazApp ?: return
            val settings = app.settingsRepository.current()
            val zone = runCatching { ZoneId.of(settings.location.timezoneId) }
                .getOrDefault(ZoneId.systemDefault())
            val nowZdt = ZonedDateTime.now(zone)
            val todayDate = LocalDate.now(zone)

            val today = app.prayerTimesRepository.getPrayerDayImmediate(
                date = todayDate,
                location = settings.location,
                method = settings.calculationMethod
            )
            val tomorrow = app.prayerTimesRepository.getPrayerDayImmediate(
                date = todayDate.plusDays(1),
                location = settings.location,
                method = settings.calculationMethod
            )

            // Determine next prayer & remaining millis
            var nextPrayer = PrayerType.IMSAK
            var nextZdt = tomorrow.zonedDateTime(PrayerType.IMSAK)
            var isTomorrow = true
            for (p in PrayerType.entries) {
                val z = today.zonedDateTime(p)
                if (z.isAfter(nowZdt)) {
                    nextPrayer = p
                    nextZdt = z
                    isTomorrow = false
                    break
                }
            }

            val remainingMillis = Duration.between(nowZdt, nextZdt).toMillis().coerceAtLeast(1_000L)
            val activeKerahat = KerahatCalculator.findActiveWindow(today, nowZdt)

            val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)

            // Background & status badge (turns amber when inside Kerahat time!)
            if (activeKerahat != null) {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_widget_kerahat)
                views.setTextViewText(R.id.widget_badge, "⚠ ${activeKerahat.type.title}")
                views.setTextColor(R.id.widget_badge, Color.parseColor("#FFF0A8"))
            } else {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_widget_emerald)
                views.setTextViewText(R.id.widget_badge, today.hijriDateText)
                views.setTextColor(R.id.widget_badge, Color.parseColor("#F6DF9E"))
            }

            views.setTextViewText(R.id.widget_location, settings.location.displayTitle)
            views.setTextViewText(R.id.widget_next_label, "${nextPrayer.displayName} Vaktine Kalan")
            views.setTextViewText(
                R.id.widget_next_time,
                "${nextPrayer.displayName} • ${nextZdt.toLocalTime().format(TIME_FMT)}"
            )

            // Native ticking countdown chronometer (zero battery drain)
            val baseElapsed = SystemClock.elapsedRealtime() + remainingMillis
            views.setChronometerCountDown(R.id.widget_countdown, true)
            views.setChronometer(R.id.widget_countdown, baseElapsed, null, true)

            // Populate 6 prayer columns and highlight the active upcoming prayer in warm gold
            val slots = listOf(
                Triple(PrayerType.IMSAK, R.id.w_lbl_imsak, R.id.w_time_imsak),
                Triple(PrayerType.GUNES, R.id.w_lbl_gunes, R.id.w_time_gunes),
                Triple(PrayerType.OGLE, R.id.w_lbl_ogle, R.id.w_time_ogle),
                Triple(PrayerType.IKINDI, R.id.w_lbl_ikindi, R.id.w_time_ikindi),
                Triple(PrayerType.AKSAM, R.id.w_lbl_aksam, R.id.w_time_aksam),
                Triple(PrayerType.YATSI, R.id.w_lbl_yatsi, R.id.w_time_yatsi)
            )
            val displayDay = if (isTomorrow) tomorrow else today
            val gold = Color.parseColor("#F6DF9E")
            val white = Color.WHITE
            val softWhite = Color.parseColor("#CCFFFFFF")

            for ((type, lblId, timeId) in slots) {
                val timeStr = displayDay.times[type]?.format(TIME_FMT) ?: "--:--"
                views.setTextViewText(timeId, timeStr)
                val highlight = type == nextPrayer
                views.setTextColor(lblId, if (highlight) gold else softWhite)
                views.setTextColor(timeId, if (highlight) gold else white)
            }

            // Tap root -> open app
            val openPi = PendingIntent.getActivity(
                context,
                8100,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPi)

            // Tap refresh icon -> force refresh widget immediately
            val refreshPi = PendingIntent.getBroadcast(
                context,
                8101,
                Intent(context, PrayerWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_REFRESH
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPi)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
