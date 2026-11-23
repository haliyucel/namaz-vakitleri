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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.namazvakti.app.ui.screens.SettingsScreen
import com.namazvakti.app.ui.theme.NamazTheme
import com.namazvakti.app.update.GitHubUpdateManager
import com.namazvakti.app.update.RemoteReleaseInfo
import com.namazvakti.app.update.UpdateCheckResult
import com.namazvakti.app.update.UpdateUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

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
    var showUpdateDialog by remember { mutableStateOf(false) }

    fun openExternalUrl(url: String) {
        runCatching {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    fun checkGitHubUpdate(silent: Boolean) {
        if (updateState.isChecking || updateState.isDownloading) return
        scope.launch {
            updateState = updateState.copy(
                isChecking = true,
                statusMessage = if (silent) updateState.statusMessage else "GitHub'dan yeni sürüm kontrol ediliyor..."
            )
            when (val res = GitHubUpdateManager.checkForUpdate(BuildConfig.VERSION_NAME)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    updateState = updateState.copy(
                        isChecking = false,
                        availableRelease = res.release,
                        statusMessage = "Yeni sürüm bulundu: ${res.release.tagName}"
                    )
                    if (!silent) {
                        showUpdateDialog = true
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    updateState = updateState.copy(
                        isChecking = false,
                        availableRelease = null,
                        statusMessage = if (silent) null else "Uygulamanız güncel (v${res.currentVersion})."
                    )
                    if (!silent) {
                        Toast.makeText(
                            activity,
                            "Uygulamanız güncel (v${res.currentVersion})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                is UpdateCheckResult.Error -> {
                    updateState = updateState.copy(
                        isChecking = false,
                        statusMessage = if (silent) updateState.statusMessage else res.message
                    )
                    if (!silent) {
                        Toast.makeText(activity, res.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun downloadAndInstallUpdate(release: RemoteReleaseInfo) {
        if (updateState.isDownloading) return
        if (!GitHubUpdateManager.canInstallPackages(activity)) {
            Toast.makeText(
                activity,
                "APK kurulumu için bu uygulamaya yükleme izni verin ve tekrar deneyin.",
                Toast.LENGTH_LONG
            ).show()
            runCatching {
                activity.startActivity(GitHubUpdateManager.createInstallPermissionIntent(activity))
            }
            return
        }
        if (release.apkDownloadUrl.isNullOrBlank()) {
            openExternalUrl(release.htmlUrl)
            return
        }
        scope.launch {
            updateState = updateState.copy(
                isDownloading = true,
                downloadedBytes = 0L,
                totalBytes = release.apkSizeBytes,
                statusMessage = "Güncelleme indiriliyor (${release.tagName})..."
            )
            val result = GitHubUpdateManager.downloadApk(activity, release) { dl, total ->
                updateState = updateState.copy(downloadedBytes = dl, totalBytes = total)
            }
            result.onSuccess { apkFile ->
                updateState = updateState.copy(
                    isDownloading = false,
                    statusMessage = "İndirme tamamlandı, kurulum ekranı açılıyor..."
                )
                showUpdateDialog = false
                runCatching {
                    GitHubUpdateManager.launchApkInstaller(activity, apkFile)
                }.onFailure { err ->
                    Toast.makeText(
                        activity,
                        "Kurulum başlatılamadı: ${err.localizedMessage ?: "Bilinmeyen hata"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.onFailure { err ->
                val msg = err.message ?: "Güncelleme indirilemedi."
                updateState = updateState.copy(
                    isDownloading = false,
                    statusMessage = msg
                )
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Silent background update check on startup
    LaunchedEffect(Unit) {
        checkGitHubUpdate(silent = true)
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
            hasDndPolicy = AutoSilentManager.hasDndPermission(activity)
        )
    }

    var permissions by remember { mutableStateOf(readPermissions()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissions = readPermissions()
                AutoSilentManager.syncState(activity)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 1-second live clock ticker
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
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
            isRefreshing = false
        }
    }

    LaunchedEffect(settings.location, settings.calculationMethod, todayDate) {
        syncOnlinePrayers(forceRefresh = false)
    }

    fun updateAndReschedule(transform: (AppSettings) -> AppSettings) {
        val next = app.settingsRepository.update(transform)
        PrayerAlarmScheduler.rescheduleAll(activity, next, app.prayerTimesRepository)
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
            .setContentText("Uyarı bildirimleri sorunsuz çalışıyor.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(activity).notify(7777, notif)
        }
        Toast.makeText(activity, "Test uyarısı gönderildi.", Toast.LENGTH_SHORT).show()
    }

    fun testAutoSilent15Sec() {
        if (!AutoSilentManager.hasDndPermission(activity)) {
            Toast.makeText(
                activity,
                "Önce Rahatsız Etme iznini açmalısınız.",
                Toast.LENGTH_LONG
            ).show()
            openDndSettings()
            return
        }
        val ok = AutoSilentManager.enterSilentMode(
            context = activity,
            prayerName = "Test",
            durationSeconds = 15L
        )
        if (ok) {
            Toast.makeText(
                activity,
                "Telefon sessize alındı. 15 saniye sonra otomatik sesli moda dönecek.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEachIndexed { idx, tab ->
                    val icon = when (tab) {
                        AppTab.TIMES -> Icons.Rounded.AccessTime
                        AppTab.LOCATION -> Icons.Rounded.LocationOn
                        AppTab.QIBLA -> Icons.Rounded.Explore
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
                    availableUpdate = updateState.availableRelease,
                    onOpenLocationPicker = { selectedTab = AppTab.LOCATION.ordinal },
                    onRefresh = { syncOnlinePrayers(forceRefresh = true) },
                    onOpenUpdateDialog = { showUpdateDialog = true },
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

                AppTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    hasDndPermission = permissions.hasDndPolicy,
                    currentlyPlayingAdhan = playingAdhan,
                    updateState = updateState,
                    onUpdateSettings = { updateAndReschedule(it) },
                    onPreviewAdhan = { prayer ->
                        AdhanPlaybackService.start(activity, prayer, isPreview = true)
                    },
                    onStopAdhan = { AdhanPlaybackService.stop(activity) },
                    onOpenDndSettings = { openDndSettings() },
                    onTestWarningNotification = { sendTestWarningNotification() },
                    onTestAutoSilent15Sec = { testAutoSilent15Sec() },
                    onCheckForUpdate = { checkGitHubUpdate(silent = false) },
                    onOpenUpdateDialog = { showUpdateDialog = true },
                    onOpenGitHubRepo = { openExternalUrl(GitHubUpdateManager.GITHUB_REPO_URL) }
                )
            }
        }
    }

    val releaseForDialog = updateState.availableRelease
    if (showUpdateDialog && releaseForDialog != null) {
        AlertDialog(
            onDismissRequest = {
                if (!updateState.isDownloading) showUpdateDialog = false
            },
            title = {
                Text(
                    text = "Yeni Güncelleme (${releaseForDialog.tagName})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = releaseForDialog.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Mevcut: v${BuildConfig.VERSION_NAME} → Yeni: ${releaseForDialog.tagName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (releaseForDialog.apkSizeBytes > 0L) {
                        val sizeMb = releaseForDialog.apkSizeBytes / (1024.0 * 1024.0)
                        Text(
                            text = String.format(Locale.US, "Dosya boyutu: %.1f MB", sizeMb),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (releaseForDialog.releaseNotes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = releaseForDialog.releaseNotes,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (updateState.isDownloading) {
                        Spacer(Modifier.height(12.dp))
                        val p = updateState.progressFraction
                        if (p != null) {
                            LinearProgressIndicator(
                                progress = { p },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "İndiriliyor: %%%d (%.1f / %.1f MB)",
                                    (p * 100f).roundToInt(),
                                    updateState.downloadedBytes / (1024.0 * 1024.0),
                                    updateState.totalBytes / (1024.0 * 1024.0)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { downloadAndInstallUpdate(releaseForDialog) },
                    enabled = !updateState.isDownloading
                ) {
                    Text(if (updateState.isDownloading) "İndiriliyor..." else "İndir ve Kur")
                }
            },
            dismissButton = {
                if (!updateState.isDownloading) {
                    Row {
                        TextButton(onClick = { openExternalUrl(releaseForDialog.htmlUrl) }) {
                            Text("GitHub'da Aç")
                        }
                        TextButton(onClick = { showUpdateDialog = false }) {
                            Text("Sonra")
                        }
                    }
                }
            }
        )
    }
}
