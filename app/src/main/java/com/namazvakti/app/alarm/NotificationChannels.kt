package com.namazvakti.app.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import com.namazvakti.app.R

object NotificationChannels {
    private const val LEGACY_CHANNEL_WARNINGS = "prayer_warnings"
    const val CHANNEL_WARNINGS = "prayer_warnings_v2"
    const val CHANNEL_ADHAN = "adhan_playback"
    const val CHANNEL_AUTO_SILENT = "auto_silent_status"

    fun warningSoundUri(context: Context): Uri =
        Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.uyari_tonu}")

    fun createAll(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Remove legacy channel so upgraded installs pick up the new bundled custom chime
        runCatching { nm.deleteNotificationChannel(LEGACY_CHANNEL_WARNINGS) }

        val warningSound = warningSoundUri(context)
        val warningAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val warnings = NotificationChannel(
            CHANNEL_WARNINGS,
            "Ezan Öncesi Uyarılar (15 dk / 5 dk)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Ezan vaktine 15 dakika ve 5 dakika kala gönderilen hatırlatma uyarıları"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 280, 140, 280)
            setSound(warningSound, warningAttrs)
            setBypassDnd(true)
        }

        val adhan = NotificationChannel(
            CHANNEL_ADHAN,
            "Vakit Ezanı Okuma",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Vakit girdiğinde okunan ezan ve durdurma kontrolü"
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
        }

        val silent = NotificationChannel(
            CHANNEL_AUTO_SILENT,
            "Otomatik Sessiz Mod Durumu",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Namaz vaktinde sessize alma ve süre sonunda sesliye dönüş bildirimi"
            setSound(null, null)
            enableVibration(false)
        }

        nm.createNotificationChannels(listOf(warnings, adhan, silent))
    }
}
