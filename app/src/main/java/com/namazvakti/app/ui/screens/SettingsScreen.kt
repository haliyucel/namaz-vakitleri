package com.namazvakti.app.ui.screens

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.namazvakti.app.model.AppSettings
import com.namazvakti.app.model.CalculationMethod
import com.namazvakti.app.model.PrayerType
import com.namazvakti.app.ui.theme.AmberWarn
import kotlin.math.roundToInt

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

                    // Duration Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Sessiz kalma süresi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(
                                onClick = {
                                    onUpdateSettings {
                                        it.copy(autoSilentDurationMinutes = (it.autoSilentDurationMinutes - 1).coerceAtLeast(5))
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Azalt", modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = "${settings.autoSilentDurationMinutes} dk",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            FilledTonalIconButton(
                                onClick = {
                                    onUpdateSettings {
                                        it.copy(autoSilentDurationMinutes = (it.autoSilentDurationMinutes + 1).coerceAtMost(180))
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Artır", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Slider(
                        value = settings.autoSilentDurationMinutes.coerceIn(5, 120).toFloat(),
                        onValueChange = { value ->
                            val rounded = (value / 5f).roundToInt() * 5
                            onUpdateSettings { it.copy(autoSilentDurationMinutes = rounded.coerceIn(5, 120)) }
                        },
                        valueRange = 5f..120f,
                        steps = 22
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10, 15, 20, 30, 45, 60).forEach { mins ->
                            FilterChip(
                                selected = settings.autoSilentDurationMinutes == mins,
                                onClick = {
                                    onUpdateSettings { it.copy(autoSilentDurationMinutes = mins) }
                                },
                                label = { Text("$mins dk") }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Sessize alınacak vakitler:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PrayerType.ADHAN_PRAYERS.forEach { prayer ->
                            val selected = prayer in settings.autoSilentPrayers
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onUpdateSettings { s ->
                                        val next = s.autoSilentPrayers.toMutableSet()
                                        if (selected) next.remove(prayer) else next.add(prayer)
                                        s.copy(autoSilentPrayers = next)
                                    }
                                },
                                label = { Text(prayer.displayName) }
                            )
                        }
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
