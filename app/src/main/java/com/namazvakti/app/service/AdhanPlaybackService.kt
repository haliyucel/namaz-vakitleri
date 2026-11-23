package com.namazvakti.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.namazvakti.app.MainActivity
import com.namazvakti.app.R
import com.namazvakti.app.alarm.NotificationChannels
import com.namazvakti.app.model.PrayerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdhanPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlaybackAndSelf()
                return START_NOT_STICKY
            }
            ACTION_PLAY, ACTION_PREVIEW -> {
                val prayer = PrayerType.fromNameOrNull(intent.getStringExtra(EXTRA_PRAYER_TYPE))
                    ?: PrayerType.OGLE
                val isPreview = intent.action == ACTION_PREVIEW
                startAdhan(prayer, isPreview)
                return START_NOT_STICKY
            }
            else -> {
                stopPlaybackAndSelf()
                return START_NOT_STICKY
            }
        }
    }

    private fun startAdhan(prayer: PrayerType, isPreview: Boolean) {
        releasePlayer()
        acquireWakeLock()

        val title = if (isPreview) {
            "Ezan Önizleme • ${prayer.displayName}"
        } else {
            "${prayer.displayName} Ezanı Okunuyor"
        }
        val subtitle = prayer.makamName ?: prayer.displayName

        val notification = buildNotification(title, subtitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val rawResId = rawResForPrayer(prayer)
        _currentlyPlaying.value = prayer

        try {
            val afd = resources.openRawResourceFd(rawResId)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnCompletionListener {
                    stopPlaybackAndSelf()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlaybackAndSelf()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (_: Exception) {
            stopPlaybackAndSelf()
        }
    }

    private fun buildNotification(title: String, subtitle: String) =
        NotificationCompat.Builder(this, NotificationChannels.CHANNEL_ADHAN)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    5100,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                0,
                "Ezanı Durdur",
                PendingIntent.getService(
                    this,
                    5101,
                    Intent(this, AdhanPlaybackService::class.java).apply { action = ACTION_STOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun rawResForPrayer(prayer: PrayerType): Int = when (prayer) {
        PrayerType.IMSAK -> R.raw.ezan_sabah
        PrayerType.OGLE -> R.raw.ezan_ogle
        PrayerType.IKINDI -> R.raw.ezan_ikindi
        PrayerType.AKSAM -> R.raw.ezan_aksam
        PrayerType.YATSI -> R.raw.ezan_yatsi
        PrayerType.GUNES -> R.raw.ezan_sabah
    }

    private fun acquireWakeLock() {
        releaseWakeLock()
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NamazVakitleri:AdhanLock").apply {
            setReferenceCounted(false)
            acquire(6 * 60 * 1000L) // Max 6 minutes safety timeout
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        wakeLock = null
    }

    private fun releasePlayer() {
        runCatching {
            mediaPlayer?.stop()
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }

    private fun stopPlaybackAndSelf() {
        releasePlayer()
        releaseWakeLock()
        _currentlyPlaying.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releasePlayer()
        releaseWakeLock()
        _currentlyPlaying.value = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.namazvakti.app.ADHAN_PLAY"
        const val ACTION_PREVIEW = "com.namazvakti.app.ADHAN_PREVIEW"
        const val ACTION_STOP = "com.namazvakti.app.ADHAN_STOP"
        const val EXTRA_PRAYER_TYPE = "prayer_type"
        private const val NOTIFICATION_ID = 5100

        private val _currentlyPlaying = MutableStateFlow<PrayerType?>(null)
        val currentlyPlaying: StateFlow<PrayerType?> = _currentlyPlaying.asStateFlow()

        fun start(context: Context, prayer: PrayerType, isPreview: Boolean = false) {
            val intent = Intent(context.applicationContext, AdhanPlaybackService::class.java).apply {
                action = if (isPreview) ACTION_PREVIEW else ACTION_PLAY
                putExtra(EXTRA_PRAYER_TYPE, prayer.name)
            }
            ContextCompat.startForegroundService(context.applicationContext, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context.applicationContext, AdhanPlaybackService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.applicationContext.startService(intent) }
        }
    }
}
