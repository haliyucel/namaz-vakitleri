package com.namazvakti.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.namazvakti.app.data.FiqhCategory
import com.namazvakti.app.data.FiqhTopic
import com.namazvakti.app.data.KazaPrayerType
import com.namazvakti.app.data.KazaRepository
import com.namazvakti.app.data.KazaTrackerState
import com.namazvakti.app.data.ReligiousGuideData
import com.namazvakti.app.domain.KazaCalculator
import com.namazvakti.app.ui.theme.EmeraldDeep
import com.namazvakti.app.ui.theme.EmeraldLight
import com.namazvakti.app.ui.theme.EmeraldPrimary
import com.namazvakti.app.ui.theme.GoldAccent
import com.namazvakti.app.ui.theme.GoldSoft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RehberScreen(kazaRepository: KazaRepository) {
    var subTab by remember { mutableIntStateOf(0) } // 0 = Kaza, 1 = Dini Bilgiler, 2 = Tesbih & Günler
    val kazaState by kazaRepository.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Clean 3-part segmented top switcher
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SegmentedButton(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                label = { Text("Kaza Hesabı", maxLines = 1) }
            )
            SegmentedButton(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                label = { Text("Dini Bilgiler", maxLines = 1) }
            )
            SegmentedButton(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                label = { Text("Tesbih", maxLines = 1) }
            )
        }

        when (subTab) {
            0 -> KazaCalculatorTab(
                state = kazaState,
                repository = kazaRepository
            )
            1 -> ReligiousGuideTab()
            2 -> TesbihAndDaysTab(
                state = kazaState,
                repository = kazaRepository
            )
        }
    }
}

// =========================================================================
// 1. KAZA NAMAZI VE ORUÇ HESAPLAYICI + TAKİP
// =========================================================================
@Composable
private fun KazaCalculatorTab(
    state: KazaTrackerState,
    repository: KazaRepository
) {
    val haptic = LocalHapticFeedback.current
    var calcMode by remember { mutableIntStateOf(0) } // 0 = Süreye göre, 1 = Yaşa göre
    var yearsInput by remember { mutableStateOf("1") }
    var monthsInput by remember { mutableStateOf("0") }
    var daysInput by remember { mutableStateOf("0") }
    var pubertyAgeInput by remember { mutableStateOf("12") }
    var startAgeInput by remember { mutableStateOf("20") }
    var exemptDaysPerMonth by remember { mutableIntStateOf(0) }
    var calculatorExpanded by remember { mutableStateOf(state.totalRemainingPrayers == 0 && state.fastingDays == 0) }
    var historyExpanded by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Direct debt edit dialogs
    var editingPrayer by remember { mutableStateOf<KazaPrayerType?>(null) }
    var editingPrayerText by remember { mutableStateOf("") }
    var showFastingEditDialog by remember { mutableStateOf(false) }
    var fastingEditText by remember { mutableStateOf("") }

    val trLocale = remember { Locale("tr", "TR") }
    val historyDateFmt = remember { DateTimeFormatter.ofPattern("d MMM HH:mm", trLocale) }

    val preview = remember(
        calcMode, yearsInput, monthsInput, daysInput, pubertyAgeInput, startAgeInput, exemptDaysPerMonth
    ) {
        if (calcMode == 0) {
            KazaCalculator.calculateFromDuration(
                years = yearsInput.toIntOrNull() ?: 0,
                months = monthsInput.toIntOrNull() ?: 0,
                days = daysInput.toIntOrNull() ?: 0,
                monthlyExemptionDays = exemptDaysPerMonth
            )
        } else {
            KazaCalculator.calculateFromAges(
                pubertyAge = pubertyAgeInput.toIntOrNull() ?: 12,
                startPrayingAge = startAgeInput.toIntOrNull() ?: 12,
                monthlyExemptionDays = exemptDaysPerMonth
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Summary Hero Card (Both Kaza Namazı & Kaza Orucu + Today + Undo)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(EmeraldDeep, EmeraldPrimary, EmeraldLight)))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kalan Kaza Namazı",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = String.format(trLocale, "%,d vakit", state.totalRemainingPrayers),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kılınan: ${state.completedPrayers}",
                                color = GoldSoft,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kalan Kaza Orucu",
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = String.format(trLocale, "%,d gün", state.fastingDays),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tutulan: ${state.completedFastingDays}",
                                color = GoldSoft,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (state.totalRemainingPrayers > 0 || state.fastingDays > 0 ||
                            state.completedPrayers > 0 || state.completedFastingDays > 0
                        ) {
                            IconButton(onClick = { showResetConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Sıfırla",
                                    tint = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // Today's completed badge
                    if (state.todayCompletedPrayers > 0 || state.todayCompletedFasts > 0) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "Bugün yerine getirilen: ${state.todayCompletedPrayers} namaz • ${state.todayCompletedFasts} oruç",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (state.totalRemainingPrayers > 0) {
                        val est = KazaCalculator.estimatedCompletionText(state.totalRemainingPrayers, dailySets = 1)
                        Text(
                            text = "Günde 1 günlük (6 vakit) kaza ile tahmini süre: $est",
                            color = GoldSoft,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (state.completedPrayers > 0) {
                        LinearProgressIndicator(
                            progress = { state.prayerCompletionFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = GoldSoft,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }

                    // Quick action buttons
                    if (state.totalRemainingPrayers > 0) {
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                repository.completeOneFullDay()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("1 Günlük Namaz Kıldım (Borçtan -6 Düş)")
                        }
                    }

                    if (state.canUndo) {
                        OutlinedButton(
                            onClick = { repository.undoLastAction() },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Undo,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Son İşlemi Geri Al", color = Color.White)
                        }
                    }
                }
            }
        }

        // 2. Expandable Prayer Debt Calculator Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { calculatorExpanded = !calculatorExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Kaza Namazı Hesaplayıcı",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (calculatorExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = calculatorExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = calcMode == 0,
                                    onClick = { calcMode = 0 },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                                    label = { Text("Süreye Göre") }
                                )
                                SegmentedButton(
                                    selected = calcMode == 1,
                                    onClick = { calcMode = 1 },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                                    label = { Text("Yaşa Göre") }
                                )
                            }

                            if (calcMode == 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NumericField(
                                        modifier = Modifier.weight(1f),
                                        label = "Yıl",
                                        value = yearsInput,
                                        onValueChange = { yearsInput = it }
                                    )
                                    NumericField(
                                        modifier = Modifier.weight(1f),
                                        label = "Ay",
                                        value = monthsInput,
                                        onValueChange = { monthsInput = it }
                                    )
                                    NumericField(
                                        modifier = Modifier.weight(1f),
                                        label = "Gün",
                                        value = daysInput,
                                        onValueChange = { daysInput = it }
                                    )
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NumericField(
                                        modifier = Modifier.weight(1f),
                                        label = "Buluğ Yaşı",
                                        value = pubertyAgeInput,
                                        onValueChange = { pubertyAgeInput = it }
                                    )
                                    NumericField(
                                        modifier = Modifier.weight(1f),
                                        label = "Başlama Yaşı",
                                        value = startAgeInput,
                                        onValueChange = { startAgeInput = it }
                                    )
                                }
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Aylık mazeret günü düşümü (isteğe bağlı)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (exemptDaysPerMonth == 0) "Yok" else "$exemptDaysPerMonth gün/ay",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = exemptDaysPerMonth.toFloat(),
                                    onValueChange = { exemptDaysPerMonth = it.roundToInt() },
                                    valueRange = 0f..10f,
                                    steps = 9
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Hesaplanan Net Borç: ${preview.netPrayerDays} gün (her vakit için)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    val exemptNote = if (preview.exemptedDays > 0) {
                                        " (${preview.exemptedDays} mazeret günü düşüldü)"
                                    } else ""
                                    Text(
                                        text = "Toplam ${preview.totalPrayersIncludingVitir} vakit (Vitir dahil)$exemptNote",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        if (preview.netPrayerDays > 0) {
                                            repository.addDaysToAllPrayers(preview.netPrayerDays, replaceExisting = true)
                                            calculatorExpanded = false
                                        }
                                    },
                                    enabled = preview.netPrayerDays > 0,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Sıfırdan Ayarla")
                                }
                                Button(
                                    onClick = {
                                        if (preview.netPrayerDays > 0) {
                                            repository.addDaysToAllPrayers(preview.netPrayerDays, replaceExisting = false)
                                            calculatorExpanded = false
                                        }
                                    },
                                    enabled = preview.netPrayerDays > 0,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Borca Ekle")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. 6 Prayer Kaza Tracking Cards (Clear "Kıldım (-1)" + Direct Debt Edit)
        item {
            Text(
                text = "Vakit Bazlı Kaza Namazı Takibi",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        items(KazaPrayerType.entries) { prayer ->
            val count = state.counts[prayer] ?: 0
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = prayer.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${prayer.rekats} • Kalan: $count",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (count > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (count > 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    IconButton(
                        onClick = {
                            editingPrayer = prayer
                            editingPrayerText = count.toString()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Borç düzenle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            repository.fulfillPrayer(prayer, count = 1)
                        },
                        enabled = count > 0,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kıldım", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 4. Dedicated Kaza Orucu Tracking Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.NightlightRound,
                            contentDescription = null,
                            tint = GoldAccent
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kaza Orucu Takibi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kalan: ${state.fastingDays} gün • Tutulan: ${state.completedFastingDays} gün",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                fastingEditText = ""
                                showFastingEditDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Borç Ekle", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (state.completedFastingDays > 0 || state.fastingDays > 0) {
                        LinearProgressIndicator(
                            progress = { state.fastingCompletionFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = GoldAccent
                        )
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            repository.fulfillFastingDay(1)
                        },
                        enabled = state.fastingDays > 0,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("1 Gün Kaza Orucu Tuttum (Borçtan Düş)")
                    }
                }
            }
        }

        // 5. Recent Fulfillment History (Collapsible)
        if (state.history.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { historyExpanded = !historyExpanded },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Son Takip Geçmişi (${state.history.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (historyExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = historyExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.history.take(15).forEachIndexed { idx, entry ->
                                    if (idx > 0) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    }
                                    val timeStr = Instant.ofEpochMilli(entry.timestampMillis)
                                        .atZone(ZoneId.systemDefault())
                                        .format(historyDateFmt)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = timeStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (entry.isFulfillment) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = entry.badge,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (entry.isFulfillment) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    // Prayer Debt Edit Dialog
    val targetPrayer = editingPrayer
    if (targetPrayer != null) {
        AlertDialog(
            onDismissRequest = { editingPrayer = null },
            title = { Text("${targetPrayer.displayName} Kaza Borcu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editingPrayerText,
                        onValueChange = { editingPrayerText = it.filter { c -> c.isDigit() }.take(5) },
                        singleLine = true,
                        label = { Text("Kalan borç adedi") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 10, 30).forEach { delta ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val cur = editingPrayerText.toIntOrNull() ?: 0
                                    editingPrayerText = (cur + delta).toString()
                                },
                                label = { Text("+$delta") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val next = editingPrayerText.toIntOrNull() ?: 0
                        repository.setPrayerDebt(targetPrayer, next)
                        editingPrayer = null
                    }
                ) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { editingPrayer = null }) { Text("İptal") }
            }
        )
    }

    // Fasting Debt Add / Set Dialog
    if (showFastingEditDialog) {
        AlertDialog(
            onDismissRequest = { showFastingEditDialog = false },
            title = { Text("Kaza Orucu Borcu Ekle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fastingEditText,
                        onValueChange = { fastingEditText = it.filter { c -> c.isDigit() }.take(4) },
                        singleLine = true,
                        label = { Text("Eklenecek / ayarlanacak gün sayısı") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 7, 30).forEach { add ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val cur = fastingEditText.toIntOrNull() ?: 0
                                    fastingEditText = (cur + add).toString()
                                },
                                label = { Text("+$add gün") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = fastingEditText.toIntOrNull() ?: 0
                        if (days > 0) repository.addFastingDebt(days, replaceExisting = false)
                        showFastingEditDialog = false
                    },
                    enabled = (fastingEditText.toIntOrNull() ?: 0) > 0
                ) { Text("Borca Ekle") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val days = fastingEditText.toIntOrNull() ?: 0
                            repository.addFastingDebt(days, replaceExisting = true)
                            showFastingEditDialog = false
                        }
                    ) { Text("Sıfırdan Ayarla") }
                    TextButton(onClick = { showFastingEditDialog = false }) { Text("İptal") }
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Kaza Kayıtlarını Sıfırla") },
            text = { Text("Tüm kaza namazı, kaza orucu ve geçmiş kayıtları sıfırlansın mı?") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resetAllKaza()
                        showResetConfirm = false
                    }
                ) { Text("Sıfırla") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun NumericField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(3)
            onValueChange(digits)
        },
        modifier = modifier,
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp)
    )
}

// =========================================================================
// 2. DİNİ BİLGİLER (FIKIH REHBERİ + HIZLI ZEKÂT HESAPLAYICI)
// =========================================================================
@Composable
private fun ReligiousGuideTab() {
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FiqhCategory?>(null) }
    var expandedId by remember { mutableStateOf<String?>(ReligiousGuideData.fiqhTopics.first().id) }
    var zekatAmountInput by remember { mutableStateOf("") }

    val trLocale = remember { Locale("tr", "TR") }
    val filteredTopics = remember(search, selectedCategory) {
        val q = search.trim().lowercase(trLocale)
        ReligiousGuideData.fiqhTopics.filter { topic ->
            val catMatch = selectedCategory == null || topic.category == selectedCategory
            val qMatch = q.isBlank() ||
                topic.title.lowercase(trLocale).contains(q) ||
                topic.summary.lowercase(trLocale).contains(q) ||
                topic.bullets.any { it.lowercase(trLocale).contains(q) }
            catMatch && qMatch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Bar
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Fıkıh konusu ara (abdest, sehiv, oruç, zekât...)") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Temizle")
                        }
                    }
                }
            )
        }

        // Category Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Tümü") }
                    )
                }
                items(FiqhCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.label) }
                    )
                }
            }
        }

        // Compact Zekat %2.5 quick calculator when Zekat category is selected
        if (selectedCategory == FiqhCategory.ZEKAT) {
            item {
                val netAmount = zekatAmountInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                val zekatDue = netAmount / 40.0
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Hızlı Zekât Hesaplayıcı (1/40 = %2,5)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        OutlinedTextField(
                            value = zekatAmountInput,
                            onValueChange = { v ->
                                zekatAmountInput = v.filter { it.isDigit() || it == '.' || it == ',' }.take(12)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Borçlar hariç net mal / birikim tutarı") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (netAmount > 0.0) {
                            Text(
                                text = String.format(
                                    Locale("tr", "TR"),
                                    "Verilecek Zekât (%2,5): %,.2f",
                                    zekatDue
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Not: Nisap sınırı 80,18 gr altın değeridir ve malın üzerinden 1 Hicrî yıl geçmiş olmalıdır.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Fiqh Topics Accordion
        if (filteredTopics.isEmpty()) {
            item {
                Text(
                    text = "Aramanızla eşleşen fıkıh başlığı bulunamadı.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            items(filteredTopics, key = { it.id }) { topic ->
                FiqhTopicCard(
                    topic = topic,
                    expanded = expandedId == topic.id,
                    onToggle = { expandedId = if (expandedId == topic.id) null else topic.id }
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun FiqhTopicCard(
    topic: FiqhTopic,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (expanded) 1.dp else 0.5.dp,
            color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = topic.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Text(
                        text = topic.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    topic.bullets.forEach { bullet ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. TESBİH (ZİKİRMATİK) VE YAKLAŞAN DİNİ GÜNLER
// =========================================================================
@Composable
private fun TesbihAndDaysTab(
    state: KazaTrackerState,
    repository: KazaRepository
) {
    val haptic = LocalHapticFeedback.current
    val presets = ReligiousGuideData.tesbihPresets
    val currentIdx = state.tesbihIndex.coerceIn(0, presets.lastIndex)
    val preset = presets[currentIdx]

    val today = remember { LocalDate.now() }
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr", "TR")) }
    val upcomingDays = remember(today) {
        ReligiousGuideData.religiousDays
            .filter { !it.date.isBefore(today) }
            .take(8)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tesbih Preset Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(presets) { idx, item ->
                    FilterChip(
                        selected = idx == currentIdx,
                        onClick = { repository.updateTesbih(idx, 0, state.tesbihTotal) },
                        label = { Text("${item.title} (${item.target})") }
                    )
                }
            }
        }

        // Tactile Circular Tesbih Counter Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = preset.arabic,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = preset.meaning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(4.dp))
                    // Big circular tap button
                    Box(
                        modifier = Modifier
                            .size(164.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(EmeraldLight, EmeraldDeep)))
                            .clickable {
                                val nextCount = state.tesbihCount + 1
                                val nextTotal = state.tesbihTotal + 1
                                if (nextCount >= preset.target) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Auto-advance Namaz Tesbihatı (33 Sübhanallah -> 33 Elhamdülillah -> 33 Allahu Ekber)
                                    val nextIdx = if (currentIdx in 0..1) currentIdx + 1 else currentIdx
                                    repository.updateTesbih(nextIdx, 0, nextTotal)
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    repository.updateTesbih(currentIdx, nextCount, nextTotal)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.tesbihCount}",
                                color = Color.White,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum")
                            )
                            Text(
                                text = "/ ${preset.target}",
                                color = GoldSoft,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Toplam Zikir: ${state.tesbihTotal}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { repository.updateTesbih(currentIdx, 0, state.tesbihTotal) }
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Tur Sıfırla")
                        }
                    }
                }
            }
        }

        // Upcoming Religious Days & Kandils
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Yaklaşan Dini Günler ve Kandiller",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    upcomingDays.forEachIndexed { idx, item ->
                        if (idx > 0) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        }
                        val daysLeft = ChronoUnit.DAYS.between(today, item.date)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${item.date.format(dateFmt)} • ${item.hijriNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (daysLeft == 0L) GoldAccent else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (daysLeft == 0L) "Bugün" else "$daysLeft gün",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (daysLeft == 0L) Color(0xFF2B1E00) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
