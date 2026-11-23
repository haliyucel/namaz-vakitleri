package com.namazvakti.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.namazvakti.app.data.GpsDetectionResult
import com.namazvakti.app.data.LocationRepository
import com.namazvakti.app.model.LocationInfo
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun LocationScreen(
    currentLocation: LocationInfo,
    locationRepository: LocationRepository,
    gpsResult: GpsDetectionResult?,
    isDetectingGps: Boolean,
    gpsError: String?,
    onRequestGpsLocation: () -> Unit,
    onSelectLocation: (LocationInfo) -> Unit
) {
    var modeTab by remember { mutableIntStateOf(0) } // 0 = Türkiye (İl/İlçe), 1 = Dünya Geneli
    var trSearch by remember { mutableStateOf("") }
    var selectedProvince by remember { mutableStateOf<String?>(null) }

    var worldSearch by remember { mutableStateOf("") }
    var worldResults by remember { mutableStateOf<List<LocationInfo>>(emptyList()) }
    var isSearchingWorld by remember { mutableStateOf(false) }

    val trLocale = remember { Locale("tr", "TR") }
    val allProvinces = remember { locationRepository.getTurkeyProvinces() }
    val allDistricts = remember { locationRepository.getTurkeyDistricts() }
    val popularWorld = remember { locationRepository.getPopularWorldCities() }

    LaunchedEffect(worldSearch) {
        val q = worldSearch.trim()
        if (q.length < 2) {
            worldResults = emptyList()
            isSearchingWorld = false
            return@LaunchedEffect
        }
        isSearchingWorld = true
        delay(350)
        worldResults = locationRepository.searchWorldwide(q)
        isSearchingWorld = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Current Location & GPS Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Seçili Konum",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                            Text(
                                text = currentLocation.displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${currentLocation.displaySubtitle} • ${currentLocation.timezoneId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Button(
                        onClick = onRequestGpsLocation,
                        enabled = !isDetectingGps,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isDetectingGps) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Konum Belirleniyor...")
                        } else {
                            Icon(Icons.Rounded.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Konumumu Bul (GPS)")
                        }
                    }

                    if (!gpsError.isNullOrBlank()) {
                        Text(
                            text = gpsError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Show selectable districts/cities in the GPS-detected province/region!
                    if (gpsResult != null && gpsResult.nearbyOptions.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                        Text(
                            text = "${gpsResult.detectedLocation.province} bölgesindeki il/ilçelerden seç:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(gpsResult.nearbyOptions) { option ->
                                val isSelected = option.district.equals(currentLocation.district, ignoreCase = true) &&
                                    option.province.equals(currentLocation.province, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectLocation(option) },
                                    label = { Text(option.district.ifBlank { option.province }) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Mode Switcher: Türkiye (İl/İlçe) vs Dünya Geneli
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = modeTab == 0,
                    onClick = { modeTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Türkiye (İl / İlçe)") }
                )
                SegmentedButton(
                    selected = modeTab == 1,
                    onClick = { modeTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Dünya Geneli") }
                )
            }
        }

        if (modeTab == 0) {
            // Turkey Search Bar
            item {
                OutlinedTextField(
                    value = trSearch,
                    onValueChange = { trSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("İl veya ilçe ara (örn. Kadıköy, Çankaya, Bursa)") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (trSearch.isNotEmpty()) {
                            IconButton(onClick = { trSearch = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Temizle")
                            }
                        }
                    }
                )
            }

            if (trSearch.trim().length >= 2) {
                val q = trSearch.trim().lowercase(trLocale)
                val matches = allDistricts.filter {
                    it.district.lowercase(trLocale).contains(q) ||
                        it.province.lowercase(trLocale).contains(q)
                }.take(60)

                if (matches.isEmpty()) {
                    item {
                        Text(
                            text = "Eşleşen il/ilçe bulunamadı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(matches) { loc ->
                        LocationListItem(
                            location = loc,
                            isSelected = isSameLocation(loc, currentLocation),
                            onClick = { onSelectLocation(loc) }
                        )
                    }
                }
            } else if (selectedProvince != null) {
                val prov = selectedProvince!!
                val districts = locationRepository.getDistrictsForProvince(prov)
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProvince = null }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri")
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$prov İlçeleri (${districts.size}) — Tüm İllere Dön",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
                items(districts) { loc ->
                    LocationListItem(
                        location = loc,
                        isSelected = isSameLocation(loc, currentLocation),
                        onClick = { onSelectLocation(loc) }
                    )
                }
            } else {
                item {
                    Text(
                        text = "81 İl (Çevrimdışı) — İlçelerini görmek için il seçin",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(allProvinces) { province ->
                    val count = locationRepository.getDistrictsForProvince(province).size
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProvince = province }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = province,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$count ilçe",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // Worldwide Search
            item {
                OutlinedTextField(
                    value = worldSearch,
                    onValueChange = { worldSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Dünya geneli şehir/ilçe ara (Berlin, Tokyo, Mekke...)") },
                    leadingIcon = { Icon(Icons.Rounded.Public, contentDescription = null) },
                    trailingIcon = {
                        when {
                            isSearchingWorld -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            worldSearch.isNotEmpty() -> IconButton(onClick = { worldSearch = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Temizle")
                            }
                        }
                    }
                )
            }

            if (worldSearch.trim().length >= 2) {
                if (!isSearchingWorld && worldResults.isEmpty()) {
                    item {
                        Text(
                            text = "Sonuç bulunamadı. Farklı bir şehir veya ilçe adı deneyin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else {
                    items(worldResults) { loc ->
                        LocationListItem(
                            location = loc,
                            isSelected = isSameLocation(loc, currentLocation),
                            onClick = { onSelectLocation(loc) }
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Popüler Dünya Şehirleri (Çevrimdışı Hazır)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(popularWorld) { loc ->
                    LocationListItem(
                        location = loc,
                        isSelected = isSameLocation(loc, currentLocation),
                        onClick = { onSelectLocation(loc) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationListItem(
    location: LocationInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${location.country} • ${location.timezoneId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Seçili",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun isSameLocation(a: LocationInfo, b: LocationInfo): Boolean =
    a.province.equals(b.province, ignoreCase = true) &&
        a.district.equals(b.district, ignoreCase = true) &&
        a.countryCode.equals(b.countryCode, ignoreCase = true)
