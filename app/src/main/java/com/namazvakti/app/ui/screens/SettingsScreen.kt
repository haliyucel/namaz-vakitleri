package com.namazvakti.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.namazvakti.app.model.AppSettings
import com.namazvakti.app.model.CalculationMethod
import com.namazvakti.app.model.PrayerType
import com.namazvakti.app.ui.theme.AmberWarn
import kotlin.math.roundToInt

private sealed interface SilentEditTarget {
    data class Prayer(val prayer: PrayerType) : SilentEditTarget
    data object Friday : SilentEditTarget
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    hasDndPermission: Boolean,
    currentlyPlayingAdhan: PrayerType?,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onPreviewAdhan: (PrayerType) -> Unit,
    onStopAdhan: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onTestWarningNotification: () -> Unit,
    onTestKerahatNotification: () -> Unit,
    onTestAutoSilent15Sec: () -> Unit,
    onAddHomeWidget: () -> Unit
) {
    var editingSilentTarget by remember { mutableStateOf<SilentEditTarget?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Pre-Adhan Warnings (15 min & 5 min)
        item {
            SettingsSectionCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "Ezan Öncesi Uyarılar"
            ) {
                SwitchRow(
                    title = "15 dakika önce 1. uyarı",
                    subtitle = "Ezan vaktine 15 dk kala uyarır ve bildirimde vakte kadar geri sayar",
                    checked = settings.warn15MinEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings { it.copy(warn15MinEnabled = enabled) }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SwitchRow(
                    title = "5 dakika önce 2. uyarı",
                    subtitle = "Ezan vaktine 5 dk kala son uyarıyı verir ve bildirimde geri sayar",
                    checked = settings.warn5MinEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings { it.copy(warn5MinEnabled = enabled) }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SwitchRow(
                    title = "Kerahat vakti girdiğinde uyar",
                    subtitle = "Sabah, Öğle ve Akşam kerahat vakti başladığında uyarır ve çıkana kadar geri sayar",
                    checked = settings.kerahatAlertEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings { it.copy(kerahatAlertEnabled = enabled) }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SwitchRow(
                    title = "Cuma namazı hatırlatıcısı",
                    subtitle = "Cuma günleri öğle vaktinden ${settings.fridayReminderMinutesBefore} dk önce hatırlatır",
                    checked = settings.fridayReminderEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings { it.copy(fridayReminderEnabled = enabled) }
                    }
                )
                if (settings.fridayReminderEnabled) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(30, 45, 60, 90).forEach { mins ->
                            FilterChip(
                                selected = settings.fridayReminderMinutesBefore == mins,
                                onClick = {
                                    onUpdateSettings { it.copy(fridayReminderMinutesBefore = mins) }
                                },
                                label = { Text("$mins dk önce") }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Uyarı verilecek vakitler:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PrayerType.entries.forEach { prayer ->
                        val selected = prayer in settings.alertEnabledPrayers
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onUpdateSettings { s ->
                                    val next = s.alertEnabledPrayers.toMutableSet()
                                    if (selected) next.remove(prayer) else next.add(prayer)
                                    s.copy(alertEnabledPrayers = next)
                                }
                            },
                            label = { Text(prayer.displayName) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTestWarningNotification,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ezan Uyarısı Test", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onTestKerahatNotification,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Kerahat Test", maxLines = 1)
                    }
                }
            }
        }

        // 2. Optional Adhan Recitation (Per Vakit Makam + Preview)
        item {
            SettingsSectionCard(
                icon = Icons.Rounded.VolumeUp,
                title = "Vaktin Ezanını Okuma (İsteğe Bağlı)"
            ) {
                SwitchRow(
                    title = "Vakit girdiğinde ezan oku",
                    subtitle = "Her vakit kendi Türk makamında ezan okur (çevrimdışı çalışır)",
                    checked = settings.adhanEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings { it.copy(adhanEnabled = enabled) }
                    }
                )
                if (settings.adhanEnabled) {
                    Spacer(Modifier.height(6.dp))
                    PrayerType.ADHAN_PRAYERS.forEach { prayer ->
                        val enabled = prayer in settings.adhanEnabledPrayers
                        val isPlaying = currentlyPlayingAdhan == prayer
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prayer.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = prayer.makamName ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (isPlaying) onStopAdhan() else onPreviewAdhan(prayer)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Durdur" else "Dinle",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { on ->
                                    onUpdateSettings { s ->
                                        val next = s.adhanEnabledPrayers.toMutableSet()
                                        if (on) next.add(prayer) else next.remove(prayer)
                                        s.copy(adhanEnabledPrayers = next)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Auto-Silent & Auto-Restore Sound Mode (Silent Mode only, NOT Do Not Disturb)
        item {
            SettingsSectionCard(
                icon = Icons.Rounded.VolumeOff,
                title = "Otomatik Sessiz Mod"
            ) {
                SwitchRow(
                    title = "Namaz vaktinde telefonu sessize al",
                    subtitle = "Rahatsız Etme modunu açmadan telefonu doğrudan sessize alır, süre bitince sesliye döndürür",
                    checked = settings.autoSilentEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateSettings { it.copy(autoSilentEnabled = enabled) }
                    }
                )

                Spacer(Modifier.height(8.dp))
                // Silent mode permission status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (hasDndPermission) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (hasDndPermission) MaterialTheme.colorScheme.primary else AmberWarn,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (hasDndPermission) "Tam sessiz mod izni verildi (Rahatsız Etme açılmaz)"
                            else "Tam sessiz (titreşimsiz) mod için ses kontrol izni verin",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (!hasDndPermission) {
                        FilledTonalButton(onClick = onOpenDndSettings) {
                            Text("İzin Ver")
                        }
                    }
                }

                if (settings.autoSilentEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Per-Prayer Silent Duration Header + Recommended Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vakitlere göre sessiz süreleri",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Süreye dokunarak ince ayar yapabilirsiniz",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                onUpdateSettings {
                                    it.copy(
                                        autoSilentDurationsByPrayer = AppSettings.DEFAULT_SILENT_DURATIONS,
                                        fridaySilentDurationMinutes = AppSettings.DEFAULT_FRIDAY_SILENT_MINUTES
                                    )
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Önerilen", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    PrayerType.ADHAN_PRAYERS.forEachIndexed { idx, prayer ->
                        val enabled = prayer in settings.autoSilentPrayers
                        val mins = settings.silentDurationFor(prayer)
                        val label = if (prayer == PrayerType.IMSAK) "Sabah (İmsak)" else prayer.displayName

                        if (idx > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                        PrayerSilentDurationRow(
                            title = label,
                            subtitle = if (enabled) null else "Sessize alma kapalı",
                            minutes = mins,
                            enabled = enabled,
                            showToggle = true,
                            onToggle = { on ->
                                onUpdateSettings { s ->
                                    val next = s.autoSilentPrayers.toMutableSet()
                                    if (on) next.add(prayer) else next.remove(prayer)
                                    s.copy(autoSilentPrayers = next)
                                }
                            },
                            onStepMinutes = { delta ->
                                onUpdateSettings { s ->
                                    s.withPrayerSilentDuration(prayer, s.silentDurationFor(prayer) + delta)
                                }
                            },
                            onOpenDialog = { editingSilentTarget = SilentEditTarget.Prayer(prayer) }
                        )
                    }

                    // Dedicated Cuma (Friday Öğle) silent duration row when Öğle auto-silent is active
                    if (PrayerType.OGLE in settings.autoSilentPrayers) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        PrayerSilentDurationRow(
                            title = "Cuma (Öğle)",
                            subtitle = "Cuma günleri öğle yerine uygulanır",
                            minutes = settings.fridaySilentDurationMinutes,
                            enabled = true,
                            showToggle = false,
                            onToggle = {},
                            onStepMinutes = { delta ->
                                onUpdateSettings { s ->
                                    s.copy(
                                        fridaySilentDurationMinutes = (s.fridaySilentDurationMinutes + delta)
                                            .coerceIn(AppSettings.MIN_SILENT_MINUTES, AppSettings.MAX_SILENT_MINUTES)
                                    )
                                }
                            },
                            onOpenDialog = { editingSilentTarget = SilentEditTarget.Friday }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SwitchRow(
                        title = "Süre bitince her zaman sesli moda al",
                        subtitle = "Açıkken telefonu normal sesliye çevirir; kapalıyken namaz öncesi moda döner",
                        checked = settings.restoreAlwaysLoud,
                        onCheckedChange = { on ->
                            onUpdateSettings { it.copy(restoreAlwaysLoud = on) }
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onTestAutoSilent15Sec,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("15 Saniye Sessiz Modu Test Et")
                    }
                }
            }
        }

        // 4. Calculation Method
        item {
            SettingsSectionCard(
                icon = Icons.Rounded.Calculate,
                title = "Hesaplama Yöntemi"
            ) {
                CalculationMethod.entries.forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.calculationMethod == method,
                            onClick = {
                                onUpdateSettings { it.copy(calculationMethod = method) }
                            }
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = method.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = method.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 5. Home Screen Widget Quick Add
        item {
            SettingsSectionCard(
                icon = Icons.Rounded.Widgets,
                title = "Ana Ekran Widget'ı"
            ) {
                Text(
                    text = "Uygulamayı açmadan telefonun ana ekranında sıradaki vakti, canlı geri sayımı, 6 vakit özetini ve aktif kerahat uyarısını görün.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onAddHomeWidget,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ana Ekrana Widget Ekle")
                }
            }
        }
    }

    val activeEdit = editingSilentTarget
    if (activeEdit != null) {
        val title = when (activeEdit) {
            is SilentEditTarget.Prayer -> {
                val pName = if (activeEdit.prayer == PrayerType.IMSAK) "Sabah (İmsak)" else activeEdit.prayer.displayName
                "$pName Sessiz Süresi"
            }
            SilentEditTarget.Friday -> "Cuma Namazı Sessiz Süresi"
        }
        val initialMinutes = when (activeEdit) {
            is SilentEditTarget.Prayer -> settings.silentDurationFor(activeEdit.prayer)
            SilentEditTarget.Friday -> settings.fridaySilentDurationMinutes
        }
        val presets = when (activeEdit) {
            SilentEditTarget.Friday -> listOf(30, 40, 45, 50, 60, 75, 90)
            is SilentEditTarget.Prayer -> listOf(10, 15, 20, 25, 30, 45, 60, 90)
        }
        val note = when (activeEdit) {
            SilentEditTarget.Friday -> "Cuma günleri Öğle vakti yerine otomatik uygulanır."
            is SilentEditTarget.Prayer -> if (activeEdit.prayer == PrayerType.YATSI) {
                "Teravih veya özel geceler için 180 dk'ya kadar ayarlayabilirsiniz."
            } else {
                "Vakit girdiğinde telefon bu süre boyunca sessizde kalır."
            }
        }
        SilentDurationPickerDialog(
            title = title,
            note = note,
            initialMinutes = initialMinutes,
            presets = presets,
            onDismiss = { editingSilentTarget = null },
            onConfirm = { chosenMinutes ->
                onUpdateSettings { s ->
                    when (activeEdit) {
                        is SilentEditTarget.Prayer -> s.withPrayerSilentDuration(activeEdit.prayer, chosenMinutes)
                        SilentEditTarget.Friday -> s.copy(
                            fridaySilentDurationMinutes = chosenMinutes.coerceIn(
                                AppSettings.MIN_SILENT_MINUTES,
                                AppSettings.MAX_SILENT_MINUTES
                            )
                        )
                    }
                }
                editingSilentTarget = null
            }
        )
    }
}

@Composable
private fun PrayerSilentDurationRow(
    title: String,
    subtitle: String?,
    minutes: Int,
    enabled: Boolean,
    showToggle: Boolean,
    onToggle: (Boolean) -> Unit,
    onStepMinutes: (Int) -> Unit,
    onOpenDialog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (enabled) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = { onStepMinutes(-5) },
                    enabled = minutes > AppSettings.MIN_SILENT_MINUTES,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "5 dk azalt", modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenDialog() }
                ) {
                    Text(
                        text = "$minutes dk",
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                FilledTonalIconButton(
                    onClick = { onStepMinutes(5) },
                    enabled = minutes < AppSettings.MAX_SILENT_MINUTES,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "5 dk artır", modifier = Modifier.size(15.dp))
                }
            }
        }
        if (showToggle) {
            Spacer(Modifier.width(8.dp))
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SilentDurationPickerDialog(
    title: String,
    note: String,
    initialMinutes: Int,
    presets: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var minutes by remember(initialMinutes) {
        mutableIntStateOf(
            initialMinutes.coerceIn(AppSettings.MIN_SILENT_MINUTES, AppSettings.MAX_SILENT_MINUTES)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { minutes = (minutes - 1).coerceAtLeast(AppSettings.MIN_SILENT_MINUTES) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "1 dk azalt")
                    }
                    Text(
                        text = "$minutes dk",
                        style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    FilledTonalIconButton(
                        onClick = { minutes = (minutes + 1).coerceAtMost(AppSettings.MAX_SILENT_MINUTES) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "1 dk artır")
                    }
                }
                Slider(
                    value = minutes.coerceIn(5, 120).toFloat(),
                    onValueChange = { v ->
                        val rounded = (v / 5f).roundToInt() * 5
                        minutes = rounded.coerceIn(5, 120)
                    },
                    valueRange = 5f..120f,
                    steps = 22
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = minutes == preset,
                            onClick = { minutes = preset },
                            label = { Text("$preset dk") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(minutes) }) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
private fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
