package com.namazvakti.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.namazvakti.app.alarm.AutoSilentManager
import com.namazvakti.app.alarm.NotificationChannels
import com.namazvakti.app.alarm.PrayerAlarmScheduler
import com.namazvakti.app.data.GpsDetectionResult
import com.namazvakti.app.model.AppSettings
import com.namazvakti.app.service.AdhanPlaybackService
import com.namazvakti.app.ui.screens.LocationScreen
import com.namazvakti.app.ui.screens.PermissionSummary
import com.namazvakti.app.ui.screens.PrayerTimesScreen
import com.namazvakti.app.ui.screens.QiblaScreen
import com.namazvakti.app.ui.screens.RehberScreen
import com.namazvakti.app.ui.screens.SettingsScreen
import com.namazvakti.app.ui.theme.NamazTheme
import com.namazvakti.app.update.GitHubUpdateManager
import com.namazvakti.app.update.UpdateCheckResult
import com.namazvakti.app.update.UpdateUiState
import com.namazvakti.app.widget.PrayerWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as NamazApp

        setContent {
            NamazTheme {
                NamazMainApp(activity = this, app = app)
            }
        }
    }
}

private enum class AppTab(val label: String) {
    TIMES("Vakitler"),
    LOCATION("Konum"),
    QIBLA("Kıble"),
    REHBER("Rehber"),
    SETTINGS("Ayarlar")
}

@Composable
private fun NamazMainApp(activity: ComponentActivity, app: NamazApp) {
    val scope = rememberCoroutineScope()
    val settings by app.settingsRepository.settings.collectAsState()
    val playingAdhan by AdhanPlaybackService.currentlyPlaying.collectAsState()
    val autoSilentStatus by AutoSilentManager.status.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isRefreshing by remember { mutableStateOf(false) }

    var gpsResult by remember { mutableStateOf<GpsDetectionResult?>(null) }
    var isDetectingGps by remember { mutableStateOf(false) }
    var gpsError by remember { mutableStateOf<String?>(null) }

    var updateState by remember { mutableStateOf(UpdateUiState()) }
    var showInstallPermissionDialog by remember { mutableStateOf(false) }

    fun installDownloadedApk(apkPath: String) {
        val file = File(apkPath)
        if (!file.exists()) return
        if (!GitHubUpdateManager.canInstallPackages(activity)) {
            updateState = updateState.copy(awaitingInstallPermission = true)
            showInstallPermissionDialog = true
            return
        }
        updateState = updateState.copy(awaitingInstallPermission = false)
        showInstallPermissionDialog = false
        runCatching {
            GitHubUpdateManager.launchApkInstaller(activity, file)
        }
    }

    // Automatic silent background update check & download on startup (hidden, no GitHub UI)
    LaunchedEffect(Unit) {
        val check = GitHubUpdateManager.checkForUpdate(BuildConfig.VERSION_NAME)
        if (check is UpdateCheckResult.UpdateAvailable && !check.release.apkDownloadUrl.isNullOrBlank()) {
            val release = check.release
            updateState = UpdateUiState(
                availableRelease = release,
                isDownloading = true,
                downloadedBytes = 0L,
                totalBytes = release.apkSizeBytes
            )
            val result = GitHubUpdateManager.downloadApk(activity, release) { dl, total ->
                updateState = updateState.copy(downloadedBytes = dl, totalBytes = total)
            }
            result.onSuccess { apkFile ->
                updateState = updateState.copy(
                    isDownloading = false,
                    downloadedApkPath = apkFile.absolutePath
                )
                installDownloadedApk(apkFile.absolutePath)
            }.onFailure {
                // Fail silently in background
                updateState = UpdateUiState()
            }
        }
    }

    // Live permissions state refreshed on resume
    fun readPermissions(): PermissionSummary {
        val hasNotif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        return PermissionSummary(
            hasNotification = hasNotif,
            hasExactAlarm = PrayerAlarmScheduler.canScheduleExactAlarms(activity),
            hasDndPolicy = AutoSilentManager.hasSilentModePermission(activity)
        )
    }

    var permissions by remember { mutableStateOf(readPermissions()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = readPermissions()
                AutoSilentManager.syncState(activity)
                val apkPath = updateState.downloadedApkPath
                if (updateState.awaitingInstallPermission &&
                    !apkPath.isNullOrBlank() &&
                    GitHubUpdateManager.canInstallPackages(activity)
                ) {
                    installDownloadedApk(apkPath)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 1-second live clock ticker: pauses automatically in background & on tabs without a live countdown
    LaunchedEffect(lifecycleOwner, selectedTab) {
        if (selectedTab == AppTab.TIMES.ordinal) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    nowMillis = System.currentTimeMillis()
                    delay(1000L)
                }
            }
        }
    }

    // Prayer times for today & tomorrow
    val zone = remember(settings.location.timezoneId) {
        runCatching { ZoneId.of(settings.location.timezoneId) }.getOrDefault(ZoneId.systemDefault())
    }
    val todayDate = LocalDate.now(zone)

    var todayPrayers by remember(settings.location, settings.calculationMethod, todayDate) {
        mutableStateOf(
            app.prayerTimesRepository.getPrayerDayImmediate(
                date = todayDate,
                location = settings.location,
                method = settings.calculationMethod
            )
        )
    }
    var tomorrowPrayers by remember(settings.location, settings.calculationMethod, todayDate) {
        mutableStateOf(
            app.prayerTimesRepository.getPrayerDayImmediate(
                date = todayDate.plusDays(1),
                location = settings.location,
                method = settings.calculationMethod
            )
        )
    }

    fun syncOnlinePrayers(forceRefresh: Boolean = false) {
        scope.launch {
            isRefreshing = true
            val currentSettings = app.settingsRepository.current()
            val z = runCatching { ZoneId.of(currentSettings.location.timezoneId) }
                .getOrDefault(ZoneId.systemDefault())
            val d = LocalDate.now(z)
            todayPrayers = app.prayerTimesRepository.getOrFetchPrayerDay(
                date = d,
                location = currentSettings.location,
                method = currentSettings.calculationMethod,
                forceRefresh = forceRefresh
            )
            tomorrowPrayers = app.prayerTimesRepository.getOrFetchPrayerDay(
                date = d.plusDays(1),
                location = currentSettings.location,
                method = currentSettings.calculationMethod,
                forceRefresh = forceRefresh
            )
            PrayerAlarmScheduler.rescheduleAll(activity, currentSettings, app.prayerTimesRepository)
            PrayerWidgetProvider.updateAll(activity)
            isRefreshing = false
        }
    }

    LaunchedEffect(settings.location, settings.calculationMethod, todayDate) {
        syncOnlinePrayers(forceRefresh = false)
    }

    fun updateAndReschedule(transform: (AppSettings) -> AppSettings) {
        val next = app.settingsRepository.update(transform)
        PrayerAlarmScheduler.rescheduleAll(activity, next, app.prayerTimesRepository)
        PrayerWidgetProvider.updateAll(activity)
    }

    // Permission launchers
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissions = readPermissions()
    }

    fun runGpsDetection() {
        scope.launch {
            isDetectingGps = true
            gpsError = null
            val res = app.locationRepository.detectCurrentLocation()
            res.onSuccess { detected ->
                gpsResult = detected
                updateAndReschedule { it.copy(location = detected.detectedLocation) }
                Toast.makeText(
                    activity,
                    "Konum bulundu: ${detected.detectedLocation.displayTitle}",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { err ->
                gpsError = err.message ?: "Konum alınamadı."
            }
            isDetectingGps = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            runGpsDetection()
        } else {
            gpsError = "GPS konumunu bulmak için konum izni gereklidir."
        }
    }

    fun requestGpsLocation() {
        val fine = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            runGpsDetection()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun openDndSettings() {
        runCatching {
            activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                activity.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                )
            }
        }
    }

    fun sendTestWarningNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissions.hasNotification) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val notif = NotificationCompat.Builder(activity, NotificationChannels.CHANNEL_WARNINGS)
            .setSmallIcon(R.drawable.ic_stat_mosque)
            .setContentTitle("Test: Ezan Vaktine 15 Dakika Kaldı")
            .setContentText("Özel uyarı tonu ve bildirimler sorunsuz çalışıyor.")
            .setSound(NotificationChannels.warningSoundUri(activity))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching {
            val nm = NotificationManagerCompat.from(activity)
            nm.cancel(7777)
            nm.notify(7777, notif)
        }
        Toast.makeText(activity, "Test uyarısı gönderildi.", Toast.LENGTH_SHORT).show()
    }

    fun testAutoSilent15Sec() {
        val hasFullPerm = AutoSilentManager.hasSilentModePermission(activity)
        PrayerAlarmScheduler.scheduleSilentTest(
            context = activity,
            delaySeconds = 3L,
            silentDurationSeconds = 15L
        )
        val msg = if (hasFullPerm) {
            "3 sn sonra telefon sessize alınacak, 15 sn sonra tekrar sesliye dönecek (ekranı kapatıp arka planda test edebilirsiniz)."
        } else {
            "3 sn sonra zil sesi kapatılacak, 15 sn sonra sesliye dönecek. Tam titreşimsiz sessiz mod için yukarıdan izin verin."
        }
        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEachIndexed { idx, tab ->
                    val icon = when (tab) {
                        AppTab.TIMES -> Icons.Rounded.AccessTime
                        AppTab.LOCATION -> Icons.Rounded.LocationOn
                        AppTab.QIBLA -> Icons.Rounded.Explore
                        AppTab.REHBER -> Icons.Rounded.MenuBook
                        AppTab.SETTINGS -> Icons.Rounded.Settings
                    }
                    NavigationBarItem(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (AppTab.entries[selectedTab]) {
                AppTab.TIMES -> PrayerTimesScreen(
                    settings = settings,
                    todayPrayers = todayPrayers,
                    tomorrowPrayers = tomorrowPrayers,
                    nowEpochMillis = nowMillis,
                    isRefreshing = isRefreshing,
                    currentlyPlayingAdhan = playingAdhan,
                    autoSilentStatus = autoSilentStatus,
                    permissions = permissions,
                    updateState = updateState,
                    onOpenLocationPicker = { selectedTab = AppTab.LOCATION.ordinal },
                    onRefresh = { syncOnlinePrayers(forceRefresh = true) },
                    onInstallDownloadedUpdate = {
                        updateState.downloadedApkPath?.let { installDownloadedApk(it) }
                    },
                    onStopAdhan = { AdhanPlaybackService.stop(activity) },
                    onManualRestoreSound = {
                        AutoSilentManager.restoreSoundMode(
                            context = activity,
                            restoreAlwaysLoud = settings.restoreAlwaysLoud,
                            isManual = true
                        )
                    },
                    onRequestNotificationPerm = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onOpenExactAlarmSettings = { openExactAlarmSettings() },
                    onOpenDndSettings = { openDndSettings() }
                )

                AppTab.LOCATION -> LocationScreen(
                    currentLocation = settings.location,
                    locationRepository = app.locationRepository,
                    gpsResult = gpsResult,
                    isDetectingGps = isDetectingGps,
                    gpsError = gpsError,
                    onRequestGpsLocation = { requestGpsLocation() },
                    onSelectLocation = { loc ->
                        updateAndReschedule { it.copy(location = loc) }
                        Toast.makeText(activity, "Konum seçildi: ${loc.displayTitle}", Toast.LENGTH_SHORT).show()
                        selectedTab = AppTab.TIMES.ordinal
                    }
                )

                AppTab.QIBLA -> QiblaScreen(location = settings.location)

                AppTab.REHBER -> RehberScreen(kazaRepository = app.kazaRepository)

                AppTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    hasDndPermission = permissions.hasDndPolicy,
                    currentlyPlayingAdhan = playingAdhan,
                    onUpdateSettings = { updateAndReschedule(it) },
                    onPreviewAdhan = { prayer ->
                        AdhanPlaybackService.start(activity, prayer, isPreview = true)
                    },
                    onStopAdhan = { AdhanPlaybackService.stop(activity) },
                    onOpenDndSettings = { openDndSettings() },
                    onTestWarningNotification = { sendTestWarningNotification() },
                    onTestAutoSilent15Sec = { testAutoSilent15Sec() },
                    onAddHomeWidget = {
                        val pinned = PrayerWidgetProvider.requestPin(activity)
                        if (!pinned) {
                            Toast.makeText(
                                activity,
                                "Ana ekrana uzun basıp Widget'lar → Namaz Vakitleri seçeneğinden ekleyebilirsiniz.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
        }
    }

    if (showInstallPermissionDialog && updateState.availableRelease != null) {
        AlertDialog(
            onDismissRequest = { showInstallPermissionDialog = false },
            title = {
                Text(
                    text = "Güncelleme Hazır (${updateState.availableRelease!!.tagName})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Yeni sürüm otomatik indirildi. Kurulumu tamamlamak için uygulama yükleme iznini açmanız gerekiyor."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showInstallPermissionDialog = false
                        runCatching {
                            activity.startActivity(GitHubUpdateManager.createInstallPermissionIntent(activity))
                        }
                    }
                ) {
                    Text("İzin Ver ve Yükle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPermissionDialog = false }) {
                    Text("Sonra")
                }
            }
        )
    }
}
