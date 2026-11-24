package com.namazvakti.app

import android.app.Application
import com.namazvakti.app.alarm.AutoSilentManager
import com.namazvakti.app.alarm.NotificationChannels
import com.namazvakti.app.alarm.PrayerAlarmScheduler
import com.namazvakti.app.data.KazaRepository
import com.namazvakti.app.data.LocationRepository
import com.namazvakti.app.data.PrayerTimesRepository
import com.namazvakti.app.data.SettingsRepository
import com.namazvakti.app.widget.PrayerWidgetProvider

class NamazApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var locationRepository: LocationRepository
        private set
    lateinit var prayerTimesRepository: PrayerTimesRepository
        private set
    lateinit var kazaRepository: KazaRepository
        private set

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        settingsRepository = SettingsRepository(this)
        locationRepository = LocationRepository(this)
        prayerTimesRepository = PrayerTimesRepository(this)
        kazaRepository = KazaRepository(this)

        AutoSilentManager.syncState(this)
        PrayerAlarmScheduler.rescheduleAll(
            context = this,
            settings = settingsRepository.current(),
            prayerTimesRepository = prayerTimesRepository
        )
        PrayerWidgetProvider.updateAll(this)
    }
}
