package com.namazvakti.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.namazvakti.app.alarm.AutoSilentStatus
import com.namazvakti.app.domain.KerahatCalculator
import com.namazvakti.app.domain.KerahatWindow
import com.namazvakti.app.model.AppSettings
import com.namazvakti.app.model.PrayerDay
import com.namazvakti.app.model.PrayerType
import com.namazvakti.app.update.GitHubUpdateManager
import com.namazvakti.app.update.UpdateUiState
import kotlin.math.roundToInt
import com.namazvakti.app.ui.theme.AmberWarn
import com.namazvakti.app.ui.theme.EmeraldDeep
import com.namazvakti.app.ui.theme.EmeraldLight
import com.namazvakti.app.ui.theme.EmeraldPrimary
import com.namazvakti.app.ui.theme.GoldAccent
import com.namazvakti.app.ui.theme.GoldSoft
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PermissionSummary(
    val hasNotification: Boolean,
    val hasExactAlarm: Boolean,
    val hasDndPolicy: Boolean
)

@Composable
fun PrayerTimesScreen(
    settings: AppSettings,
    todayPrayers: PrayerDay,
    tomorrowPrayers: PrayerDay,
    nowEpochMillis: Long,
    isRefreshing: Boolean,
    currentlyPlayingAdhan: PrayerType?,
    autoSilentStatus: AutoSilentStatus,
    permissions: PermissionSummary,
    updateState: UpdateUiState,
    onOpenLocationPicker: () -> Unit,
    onRefresh: () -> Unit,
    onInstallDownloadedUpdate: () -> Unit,
    onStopAdhan: () -> Unit,
    onManualRestoreSound: () -> Unit,
    onRequestNotificationPerm: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenDndSettings: () -> Unit
) {
    var showTomorrow by remember { mutableStateOf(false) }

    val zone = remember(todayPrayers.timezoneId) {
        runCatching { ZoneId.of(todayPrayers.timezoneId) }.getOrDefault(ZoneId.systemDefault())
    }
    val nowZdt = Instant.ofEpochMilli(nowEpochMillis).atZone(zone)
    val countdownInfo = remember(todayPrayers, tomorrowPrayers, nowEpochMillis / 1000L) {
        computeNextPrayer(todayPrayers, tomorrowPrayers, nowZdt)
    }

    val displayedDay = if (showTomorrow) tomorrowPrayers else todayPrayers
    val kerahatWindows = remember(displayedDay) {
        KerahatCalculator.calculateWindows(displayedDay)
    }
    val activeKerahat = remember(todayPrayers, nowEpochMillis / 1000L) {
        KerahatCalculator.findActiveWindow(todayPrayers, nowZdt)
    }
    val nextKerahat = remember(todayPrayers, tomorrowPrayers, nowEpochMillis / 60_000L) {
        KerahatCalculator.findNextWindow(todayPrayers, tomorrowPrayers, nowZdt)
    }
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("tr", "TR")) }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 0. Top Developer Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Namaz Vakitleri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Yapımcı: ${GitHubUpdateManager.DEVELOPER_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // 1. Hero Card (turns amber-warning immediately when inside Kerahat time!)
        item {
            HeroCountdownCard(
                settings = settings,
                dateText = todayPrayers.date.format(dateFmt),
                hijriText = todayPrayers.hijriDateText,
                countdown = countdownInfo,
                activeKerahat = activeKerahat,
                nextKerahat = nextKerahat,
                nowZdt = nowZdt,
                timeFmt = timeFmt,
                isRefreshing = isRefreshing,
                onOpenLocationPicker = onOpenLocationPicker,
                onRefresh = onRefresh
            )
        }

        // 1.5 Automatic Update Download / Install Banner (no GitHub branding)
        val pendingRelease = updateState.availableRelease
        if (pendingRelease != null && (updateState.isDownloading || updateState.downloadedApkPath != null)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    border = BorderStroke(1.dp, GoldAccent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (updateState.isDownloading) {
                                        "Güncelleme İndiriliyor (${pendingRelease.tagName})"
                                    } else {
                                        "Güncelleme Kuruluma Hazır (${pendingRelease.tagName})"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                val sub = if (updateState.isDownloading) {
                                    val p = updateState.progressFraction
                                    if (p != null) {
                                        "Otomatik indiriliyor • %${(p * 100f).roundToInt()}"
                                    } else {
                                        "Otomatik indiriliyor..."
                                    }
                                } else {
                                    "Kurulumu tamamlamak için dokunun"
                                }
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            if (!updateState.isDownloading && updateState.downloadedApkPath != null) {
                                FilledTonalButton(onClick = onInstallDownloadedUpdate) {
                                    Text("Yükle")
                                }
                            }
                        }
                        if (updateState.isDownloading) {
                            val p = updateState.progressFraction
                            if (p != null) {
                                LinearProgressIndicator(
                                    progress = { p },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }

        // 2. Active Adhan Banner
        if (currentlyPlayingAdhan != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = GoldSoft
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${currentlyPlayingAdhan.displayName} Ezanı Okunuyor",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = currentlyPlayingAdhan.makamName ?: "",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        FilledTonalButton(
                            onClick = onStopAdhan,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.18f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Durdur")
                        }
                    }
                }
            }
        }

        // 3. Active Auto-Silent Banner
        if (autoSilentStatus.isActive && autoSilentStatus.restoreAtMillis > nowEpochMillis) {
            item {
                val remSec = ((autoSilentStatus.restoreAtMillis - nowEpochMillis) / 1000L).coerceAtLeast(0L)
                val remMin = remSec / 60
                val remS = remSec % 60
                val restoreStr = Instant.ofEpochMilli(autoSilentStatus.restoreAtMillis)
                    .atZone(ZoneId.systemDefault())
                    .format(timeFmt)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Otomatik Sessiz Mod Aktif (${autoSilentStatus.prayerName})",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Sesli moda dönüş: %s (%02d:%02d kaldı)",
                                    restoreStr, remMin, remS
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        FilledTonalButton(onClick = onManualRestoreSound) {
                            Text("Sesliye Al")
                        }
                    }
                }
            }
        }

        // 4. Missing Permission Helper Banner (only visible if a required permission is missing)
        val needsNotif = !permissions.hasNotification
        val needsExact = !permissions.hasExactAlarm
        val needsDnd = settings.autoSilentEnabled && !permissions.hasDndPolicy
        if (needsNotif || needsExact || needsDnd) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AmberWarn.copy(alpha = 0.13f)
                    ),
                    border = BorderStroke(1.dp, AmberWarn.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = AmberWarn,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Uyarı ve sessiz mod için izin gerekiyor",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (needsNotif) {
                            PermissionActionRow(
                                text = "15 dk ve 5 dk uyarıları için bildirim izni",
                                buttonLabel = "İzin Ver",
                                onClick = onRequestNotificationPerm
                            )
                        }
                        if (needsDnd) {
                            PermissionActionRow(
                                text = "Tam sessiz mod (titreşimsiz) için ses kontrol izni",
                                buttonLabel = "Ayarları Aç",
                                onClick = onOpenDndSettings
                            )
                        }
                        if (needsExact) {
                            PermissionActionRow(
                                text = "Tam dakikalı ezan alarmı için kesin alarm izni",
                                buttonLabel = "Ayarları Aç",
                                onClick = onOpenExactAlarmSettings
                            )
                        }
                    }
                }
            }
        }

        // 5. Day Toggle Header (Bugün / Yarın)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showTomorrow) "Yarının Vakitleri" else "Günün Namaz Vakitleri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        DayPill(
                            label = "Bugün",
                            selected = !showTomorrow,
                            onClick = { showTomorrow = false }
                        )
                        DayPill(
                            label = "Yarın",
                            selected = showTomorrow,
                            onClick = { showTomorrow = true }
                        )
                    }
                }
            }
        }

        // 6. Prayer Time Rows
        items(PrayerType.entries) { prayer ->
            val time = displayedDay.times[prayer]
            val isNextToday = !showTomorrow && !countdownInfo.isTomorrow && countdownInfo.nextPrayer == prayer
            val isPassedToday = !showTomorrow && time != null &&
                displayedDay.zonedDateTime(prayer).isBefore(nowZdt) && !isNextToday
            val hasSilent = settings.autoSilentEnabled && prayer in settings.autoSilentPrayers
            val silentMinutes = if (hasSilent) {
                settings.silentDurationFor(prayer, displayedDay.date.dayOfWeek)
            } else null

            PrayerRowCard(
                prayer = prayer,
                timeText = time?.format(timeFmt) ?: "--:--",
                isHighlighted = isNextToday,
                isPassed = isPassedToday,
                hasWarn = (settings.warn15MinEnabled || settings.warn5MinEnabled) && prayer in settings.alertEnabledPrayers,
                hasAdhan = settings.adhanEnabled && prayer.hasAdhan && prayer in settings.adhanEnabledPrayers,
                hasSilent = hasSilent,
                silentDurationMinutes = silentMinutes
            )
        }

        // 6.5 Kerahat Vakitleri Card (Sabah, Öğle, Akşam — Diyanet 45 dk)
        if (kerahatWindows.isNotEmpty()) {
            item {
                KerahatScheduleCard(
                    windows = kerahatWindows,
                    activeWindow = if (!showTomorrow) activeKerahat else null,
                    nowZdt = nowZdt,
                    timeFmt = timeFmt
                )
            }
        }

        // 7. Source & Active Features Summary Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Kaynak: ${displayedDay.sourceLabel} • Saat Dilimi: ${displayedDay.timezoneId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val summary = buildList {
                    if (settings.warn15MinEnabled) add("15 dk uyarı")
                    if (settings.warn5MinEnabled) add("5 dk uyarı")
                    if (settings.kerahatAlertEnabled) add("Kerahat uyarısı")
                    if (settings.adhanEnabled) add("Ezan açık")
                    if (settings.autoSilentEnabled && settings.autoSilentPrayers.isNotEmpty()) {
                        val activeMins = settings.autoSilentPrayers.map {
                            settings.silentDurationFor(it, displayedDay.date.dayOfWeek)
                        }
                        val minM = activeMins.minOrNull() ?: settings.autoSilentDurationMinutes
                        val maxM = activeMins.maxOrNull() ?: settings.autoSilentDurationMinutes
                        if (minM == maxM) {
                            add("$minM dk otomatik sessiz")
                        } else {
                            add("Vakite göre sessiz ($minM-$maxM dk)")
                        }
                    }
                }.joinToString(" • ")
                if (summary.isNotEmpty()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCountdownCard(
    settings: AppSettings,
    dateText: String,
    hijriText: String,
    countdown: NextPrayerCountdown,
    activeKerahat: KerahatWindow?,
    nextKerahat: KerahatWindow?,
    nowZdt: ZonedDateTime,
    timeFmt: DateTimeFormatter,
    isRefreshing: Boolean,
    onOpenLocationPicker: () -> Unit,
    onRefresh: () -> Unit
) {
    val isKerahatNow = activeKerahat != null
    val gradientColors = if (isKerahatNow) {
        listOf(Color(0xFF7A2E00), Color(0xFFA84708), Color(0xFFD06A16))
    } else {
        listOf(EmeraldDeep, EmeraldPrimary, EmeraldLight)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = gradientColors))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Unmistakable top pill right inside Hero Card when currently inside Kerahat time!
            if (activeKerahat != null) {
                val rem = activeKerahat.remainingDuration(nowZdt)
                val remMin = rem.toMinutes()
                val remSec = rem.seconds % 60
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFFF0A8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ŞU AN KERAHAT VAKTİ • ${activeKerahat.type.title.uppercase(Locale("tr", "TR"))}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${activeKerahat.start.format(timeFmt)}-${activeKerahat.end.format(timeFmt)} • ${activeKerahat.type.fiqhNote}",
                                color = Color.White.copy(alpha = 0.92f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.28f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", remMin, remSec),
                                    color = Color(0xFFFFF0A8),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Çıkışa",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Top bar: Location pill + Refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { onOpenLocationPicker() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (settings.location.isGpsDetected) {
                                Icons.Rounded.GpsFixed
                            } else {
                                Icons.Rounded.LocationOn
                            },
                            contentDescription = "Konum değiştir",
                            tint = GoldSoft,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = settings.location.displayTitle,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = GoldSoft,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Vakitleri yenile",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Dates
            Column {
                Text(
                    text = dateText,
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = hijriText,
                    color = GoldSoft,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            // Next prayer countdown
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${countdown.nextPrayer.displayName} Vaktine Kalan Süre",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = countdown.formattedRemaining,
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFeatureSettings = "tnum"
                    )
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { countdown.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = GoldSoft,
                    trackColor = Color.White.copy(alpha = 0.18f)
                )
                if (activeKerahat == null && nextKerahat != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.13f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = GoldSoft,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Sıradaki Kerahat: ${nextKerahat.type.title} (${nextKerahat.start.format(timeFmt)} - ${nextKerahat.end.format(timeFmt)})",
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerRowCard(
    prayer: PrayerType,
    timeText: String,
    isHighlighted: Boolean,
    isPassed: Boolean,
    hasWarn: Boolean,
    hasAdhan: Boolean,
    hasSilent: Boolean,
    silentDurationMinutes: Int?
) {
    val containerColor = when {
        isHighlighted -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val border = if (isHighlighted) {
        BorderStroke(1.5.dp, GoldAccent)
    } else {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
    val contentAlpha = if (isPassed) 0.55f else 1f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 3.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isHighlighted -> GoldAccent
                            isPassed -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        }
                    )
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prayer.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    if (isHighlighted) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = GoldAccent,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Sıradaki",
                                color = Color(0xFF2B1E00),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                val subtitleParts = buildList {
                    if (prayer.makamName != null && hasAdhan) add(prayer.makamName)
                    if (hasSilent && silentDurationMinutes != null) add("$silentDurationMinutes dk sessiz")
                }
                if (subtitleParts.isNotEmpty()) {
                    Text(
                        text = subtitleParts.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Feature badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasWarn) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                    contentDescription = "Uyarı",
                    tint = if (hasWarn) MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    modifier = Modifier.size(17.dp)
                )
                if (prayer.hasAdhan) {
                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = "Ezan",
                        tint = if (hasAdhan) GoldAccent.copy(alpha = contentAlpha)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        modifier = Modifier.size(17.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.VolumeOff,
                        contentDescription = "Otomatik sessiz",
                        tint = if (hasSilent) MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )
        }
    }
}

@Composable
private fun KerahatScheduleCard(
    windows: List<KerahatWindow>,
    activeWindow: KerahatWindow?,
    nowZdt: ZonedDateTime,
    timeFmt: DateTimeFormatter
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AmberWarn.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = AmberWarn,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Kerahat Vakitleri",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            windows.forEachIndexed { idx, w ->
                val isNow = activeWindow?.type == w.type
                if (idx > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = w.type.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isNow) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isNow) AmberWarn else MaterialTheme.colorScheme.onSurface
                            )
                            if (isNow && activeWindow != null) {
                                val rem = activeWindow.remainingDuration(nowZdt)
                                val remMin = rem.toMinutes()
                                val remSec = rem.seconds % 60
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    color = AmberWarn,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "Aktif • %02d:%02d", remMin, remSec),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFeatureSettings = "tnum"
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = w.type.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${w.start.format(timeFmt)} - ${w.end.format(timeFmt)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = if (isNow) AmberWarn else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DayPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun PermissionActionRow(text: String, buttonLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Button(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(buttonLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private data class NextPrayerCountdown(
    val nextPrayer: PrayerType,
    val isTomorrow: Boolean,
    val formattedRemaining: String,
    val progressFraction: Float
)

private fun computeNextPrayer(
    today: PrayerDay,
    tomorrow: PrayerDay,
    now: ZonedDateTime
): NextPrayerCountdown {
    val ordered = PrayerType.entries
    var nextPrayer: PrayerType? = null
    var nextZdt: ZonedDateTime? = null
    var prevZdt: ZonedDateTime? = null
    var isTomorrow = false

    for (i in ordered.indices) {
        val p = ordered[i]
        val zdt = today.zonedDateTime(p)
        if (zdt.isAfter(now)) {
            nextPrayer = p
            nextZdt = zdt
            prevZdt = if (i > 0) today.zonedDateTime(ordered[i - 1])
            else today.zonedDateTime(PrayerType.YATSI).minusDays(1)
            break
        }
    }

    if (nextPrayer == null || nextZdt == null) {
        nextPrayer = PrayerType.IMSAK
        nextZdt = tomorrow.zonedDateTime(PrayerType.IMSAK)
        prevZdt = today.zonedDateTime(PrayerType.YATSI)
        isTomorrow = true
    }

    val remaining = Duration.between(now, nextZdt).coerceAtLeast(Duration.ZERO)
    val totalInterval = Duration.between(prevZdt, nextZdt).seconds.coerceAtLeast(1L)
    val elapsed = Duration.between(prevZdt, now).seconds.coerceIn(0L, totalInterval)
    val progress = (elapsed.toFloat() / totalInterval.toFloat()).coerceIn(0f, 1f)

    val hours = remaining.toHours()
    val minutes = (remaining.toMinutes() % 60)
    val seconds = (remaining.seconds % 60)
    val formatted = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

    return NextPrayerCountdown(
        nextPrayer = nextPrayer,
        isTomorrow = isTomorrow,
        formattedRemaining = formatted,
        progressFraction = progress
    )
}
