package com.namazvakti.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import com.namazvakti.app.model.LocationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Collator
import java.time.ZoneId
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GpsDetectionResult(
    val detectedLocation: LocationInfo,
    val nearbyOptions: List<LocationInfo>
)

class LocationRepository(private val context: Context) {

    private val trLocale = Locale("tr", "TR")
    private val trCollator: Collator = Collator.getInstance(trLocale).apply {
        strength = Collator.PRIMARY
    }

    private var cachedTurkeyDistricts: List<LocationInfo>? = null

    fun getTurkeyDistricts(): List<LocationInfo> {
        cachedTurkeyDistricts?.let { return it }
        val list = mutableListOf<LocationInfo>()
        runCatching {
            val raw = context.assets.open("turkey_districts.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val il = obj.getString("il").trim()
                val ilce = obj.getString("ilce").trim()
                val lat = obj.getString("lat").toDoubleOrNull() ?: continue
                val lon = obj.getString("lon").toDoubleOrNull() ?: continue
                list.add(
                    LocationInfo(
                        country = "Türkiye",
                        countryCode = "TR",
                        province = il,
                        district = ilce,
                        latitude = lat,
                        longitude = lon,
                        timezoneId = "Europe/Istanbul",
                        isGpsDetected = false
                    )
                )
            }
        }
        val sorted = list.sortedWith { a, b ->
            val cmp = trCollator.compare(a.province, b.province)
            if (cmp != 0) cmp else trCollator.compare(a.district, b.district)
        }
        cachedTurkeyDistricts = sorted
        return sorted
    }

    fun getTurkeyProvinces(): List<String> =
        getTurkeyDistricts().map { it.province }.distinct()

    fun getDistrictsForProvince(province: String): List<LocationInfo> =
        getTurkeyDistricts().filter { it.province.equals(province, ignoreCase = true) }

    fun getPopularWorldCities(): List<LocationInfo> = WORLD_CITIES

    suspend fun searchWorldwide(query: String): List<LocationInfo> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val results = mutableListOf<LocationInfo>()

        // 1. Instant local matches from Turkey districts & popular world cities
        val lower = trimmed.lowercase(trLocale)
        val localTurkey = getTurkeyDistricts()
            .filter {
                it.district.lowercase(trLocale).contains(lower) ||
                    it.province.lowercase(trLocale).contains(lower)
            }
            .take(12)
        val localWorld = WORLD_CITIES.filter {
            it.district.lowercase(trLocale).contains(lower) ||
                it.province.lowercase(trLocale).contains(lower) ||
                it.country.lowercase(trLocale).contains(lower)
        }
        results.addAll(localTurkey)
        results.addAll(localWorld)

        // 2. Live worldwide search via Open-Meteo Geocoding API (Turkish localized)
        val remote = fetchOpenMeteoGeocoding(trimmed, count = 20)
        for (item in remote) {
            val isDuplicate = results.any { existing ->
                existing.countryCode.equals(item.countryCode, ignoreCase = true) &&
                    haversineKm(existing.latitude, existing.longitude, item.latitude, item.longitude) < 8.0
            }
            if (!isDuplicate) {
                results.add(item)
            }
        }

        // 3. Fallback to Nominatim if Open-Meteo returned nothing
        if (remote.isEmpty() && results.size < 5) {
            val nominatim = fetchNominatimSearch(trimmed)
            for (item in nominatim) {
                val isDuplicate = results.any { existing ->
                    haversineKm(existing.latitude, existing.longitude, item.latitude, item.longitude) < 8.0
                }
                if (!isDuplicate) results.add(item)
            }
        }

        results
    }

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentLocation(): Result<GpsDetectionResult> = withContext(Dispatchers.IO) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext Result.failure(IllegalStateException("Konum servisi kullanılamıyor"))

        val location = acquireBestLocation(lm)
            ?: return@withContext Result.failure(
                IllegalStateException("Konum alınamadı. GPS veya ağ konum servisinin açık olduğundan emin olun.")
            )

        val lat = location.latitude
        val lon = location.longitude
        val deviceTz = ZoneId.systemDefault().id

        // Check if within Turkey and match nearest official district
        val nearestTr = findNearestTurkeyDistrict(lat, lon)
        if (nearestTr != null && nearestTr.second <= 55.0) {
            val matched = nearestTr.first
            val detected = matched.copy(
                latitude = lat,
                longitude = lon,
                isGpsDetected = true
            )
            val provinceDistricts = getDistrictsForProvince(matched.province)
            return@withContext Result.success(
                GpsDetectionResult(
                    detectedLocation = detected,
                    nearbyOptions = provinceDistricts
                )
            )
        }

        // Worldwide reverse geocoding (Android Geocoder + Nominatim fallback)
        val reverseInfo = reverseGeocodeWorldwide(lat, lon, deviceTz)

        // Fetch selectable cities/districts in the detected province/region worldwide
        val nearby = mutableListOf<LocationInfo>()
        if (reverseInfo.province.isNotBlank()) {
            val regionResults = fetchOpenMeteoGeocoding(reverseInfo.province, count = 15)
                .filter {
                    reverseInfo.countryCode.isBlank() ||
                        it.countryCode.equals(reverseInfo.countryCode, ignoreCase = true)
                }
            nearby.addAll(regionResults)
        }
        if (nearby.isEmpty() && reverseInfo.district.isNotBlank()) {
            nearby.addAll(fetchOpenMeteoGeocoding(reverseInfo.district, count = 10))
        }

        Result.success(
            GpsDetectionResult(
                detectedLocation = reverseInfo,
                nearbyOptions = nearby
            )
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun acquireBestLocation(lm: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        // Try fresh single fix first (with 9s timeout)
        val activeProvider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER }
        if (activeProvider != null) {
            val fresh = withTimeoutOrNull(9_000L) {
                requestSingleFix(lm, activeProvider)
            }
            if (fresh != null) return fresh
        }

        // Fallback to best recent lastKnownLocation across all enabled providers
        val candidates = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).mapNotNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }

        return candidates.maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFix(lm: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                runCatching {
                    lm.getCurrentLocation(provider, signal, context.mainExecutor) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        lm.removeUpdates(this)
                        if (cont.isActive) cont.resume(location)
                    }
                    @Deprecated("Deprecated in API 29")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
                runCatching {
                    @Suppress("DEPRECATION")
                    lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }

    private fun findNearestTurkeyDistrict(lat: Double, lon: Double): Pair<LocationInfo, Double>? {
        if (lat !in 35.5..42.5 || lon !in 25.4..45.2) return null
        val all = getTurkeyDistricts()
        if (all.isEmpty()) return null
        var best: LocationInfo? = null
        var bestDist = Double.MAX_VALUE
        for (item in all) {
            val d = haversineKm(lat, lon, item.latitude, item.longitude)
            if (d < bestDist) {
                bestDist = d
                best = item
            }
        }
        return best?.let { it to bestDist }
    }

    private fun reverseGeocodeWorldwide(lat: Double, lon: Double, fallbackTz: String): LocationInfo {
        // 1. Try Android Geocoder
        runCatching {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, trLocale)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addr = addresses?.firstOrNull()
                if (addr != null) {
                    val country = addr.countryName ?: ""
                    val countryCode = addr.countryCode ?: ""
                    val province = addr.adminArea ?: addr.locality ?: ""
                    val district = addr.subAdminArea ?: addr.locality ?: addr.subLocality ?: province
                    if (province.isNotBlank() || district.isNotBlank()) {
                        return LocationInfo(
                            country = country.ifBlank { "Konum" },
                            countryCode = countryCode,
                            province = province.ifBlank { district },
                            district = district.ifBlank { province },
                            latitude = lat,
                            longitude = lon,
                            timezoneId = fallbackTz,
                            isGpsDetected = true
                        )
                    }
                }
            }
        }

        // 2. Fallback to OpenStreetMap Nominatim reverse geocoding
        runCatching {
            val url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon&accept-language=tr"
            val body = httpGet(url)
            if (body != null) {
                val obj = JSONObject(body)
                val addr = obj.optJSONObject("address")
                if (addr != null) {
                    val country = addr.optString("country", "")
                    val countryCode = addr.optString("country_code", "").uppercase(Locale.US)
                    val province = addr.optString("province")
                        .ifBlank { addr.optString("state") }
                        .ifBlank { addr.optString("city") }
                    val district = addr.optString("town")
                        .ifBlank { addr.optString("county") }
                        .ifBlank { addr.optString("city_district") }
                        .ifBlank { addr.optString("suburb") }
                        .ifBlank { addr.optString("city") }
                        .ifBlank { province }
                    if (province.isNotBlank() || district.isNotBlank()) {
                        return LocationInfo(
                            country = country.ifBlank { "Dünya" },
                            countryCode = countryCode,
                            province = province.ifBlank { district },
                            district = district.ifBlank { province },
                            latitude = lat,
                            longitude = lon,
                            timezoneId = fallbackTz,
                            isGpsDetected = true
                        )
                    }
                }
            }
        }

        // 3. Coordinate fallback
        return LocationInfo(
            country = "GPS Konumu",
            countryCode = "",
            province = String.format(Locale.US, "%.4f°, %.4f°", lat, lon),
            district = "",
            latitude = lat,
            longitude = lon,
            timezoneId = fallbackTz,
            isGpsDetected = true
        )
    }

    private fun fetchOpenMeteoGeocoding(query: String, count: Int): List<LocationInfo> {
        val list = mutableListOf<LocationInfo>()
        runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=$count&language=tr&format=json"
            val body = httpGet(url) ?: return@runCatching
            val root = JSONObject(body)
            val arr = root.optJSONArray("results") ?: return@runCatching
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val name = o.optString("name", "").trim()
                if (name.isBlank()) continue
                val country = o.optString("country", "").trim()
                val cc = o.optString("country_code", "").trim().uppercase(Locale.US)
                val admin1 = o.optString("admin1", "").trim()
                val admin2 = o.optString("admin2", "").trim()
                val lat = o.optDouble("latitude", Double.NaN)
                val lon = o.optDouble("longitude", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) continue
                val tz = o.optString("timezone", ZoneId.systemDefault().id).ifBlank { ZoneId.systemDefault().id }

                val province = admin1.ifBlank { name }
                val district = when {
                    !name.equals(province, ignoreCase = true) -> name
                    admin2.isNotBlank() -> admin2
                    else -> name
                }

                list.add(
                    LocationInfo(
                        country = country.ifBlank { cc },
                        countryCode = cc,
                        province = province,
                        district = district,
                        latitude = lat,
                        longitude = lon,
                        timezoneId = tz,
                        isGpsDetected = false
                    )
                )
            }
        }
        return list
    }

    private fun fetchNominatimSearch(query: String): List<LocationInfo> {
        val list = mutableListOf<LocationInfo>()
        runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=jsonv2&addressdetails=1&limit=10&accept-language=tr"
            val body = httpGet(url) ?: return@runCatching
            val arr = JSONArray(body)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val lat = o.optString("lat").toDoubleOrNull() ?: continue
                val lon = o.optString("lon").toDoubleOrNull() ?: continue
                val addr = o.optJSONObject("address")
                val country = addr?.optString("country", "") ?: ""
                val cc = addr?.optString("country_code", "")?.uppercase(Locale.US) ?: ""
                val province = addr?.optString("state")?.ifBlank { addr.optString("city") }
                    ?: o.optString("name", "")
                val district = o.optString("name", "").ifBlank { province }
                list.add(
                    LocationInfo(
                        country = country.ifBlank { "Dünya" },
                        countryCode = cc,
                        province = province.ifBlank { district },
                        district = district,
                        latitude = lat,
                        longitude = lon,
                        timezoneId = ZoneId.systemDefault().id,
                        isGpsDetected = false
                    )
                )
            }
        }
        return list
    }

    private fun httpGet(urlStr: String): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
            setRequestProperty("User-Agent", "NamazVakitleriAndroid/1.0")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2.0).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2.0).pow(2)
        return 6371.0088 * 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    }

    companion object {
        val WORLD_CITIES: List<LocationInfo> = listOf(
            LocationInfo("Suudi Arabistan", "SA", "Mekke", "Mekke-i Mükerreme", 21.4225, 39.8262, "Asia/Riyadh"),
            LocationInfo("Suudi Arabistan", "SA", "Medine", "Medine-i Münevvere", 24.4672, 39.6111, "Asia/Riyadh"),
            LocationInfo("Filistin", "PS", "Kudüs", "Mescid-i Aksa", 31.7767, 35.2345, "Asia/Jerusalem"),
            LocationInfo("Kuzey Kıbrıs", "CY", "Lefkoşa", "Merkez", 35.1856, 33.3823, "Asia/Nicosia"),
            LocationInfo("Azerbaycan", "AZ", "Bakü", "Merkez", 40.4093, 49.8671, "Asia/Baku"),
            LocationInfo("Bosna-Hersek", "BA", "Saraybosna", "Merkez", 43.8563, 18.4131, "Europe/Sarajevo"),
            LocationInfo("Almanya", "DE", "Berlin", "Merkez", 52.5200, 13.4050, "Europe/Berlin"),
            LocationInfo("Almanya", "DE", "Köln", "Merkez", 50.9375, 6.9603, "Europe/Berlin"),
            LocationInfo("Almanya", "DE", "Münih", "Merkez", 48.1351, 11.5820, "Europe/Berlin"),
            LocationInfo("Almanya", "DE", "Frankfurt", "Merkez", 50.1109, 8.6821, "Europe/Berlin"),
            LocationInfo("Birleşik Krallık", "GB", "Londra", "Merkez", 51.5074, -0.1278, "Europe/London"),
            LocationInfo("Fransa", "FR", "Paris", "Merkez", 48.8566, 2.3522, "Europe/Paris"),
            LocationInfo("Hollanda", "NL", "Amsterdam", "Merkez", 52.3676, 4.9041, "Europe/Amsterdam"),
            LocationInfo("Belçika", "BE", "Brüksel", "Merkez", 50.8503, 4.3517, "Europe/Brussels"),
            LocationInfo("Avusturya", "AT", "Viyana", "Merkez", 48.2082, 16.3738, "Europe/Vienna"),
            LocationInfo("İsviçre", "CH", "Zürih", "Merkez", 47.3769, 8.5417, "Europe/Zurich"),
            LocationInfo("İsveç", "SE", "Stockholm", "Merkez", 59.3293, 18.0686, "Europe/Stockholm"),
            LocationInfo("ABD", "US", "New York", "Manhattan", 40.7128, -74.0060, "America/New_York"),
            LocationInfo("ABD", "US", "Chicago", "Merkez", 41.8781, -87.6298, "America/Chicago"),
            LocationInfo("ABD", "US", "Los Angeles", "Merkez", 34.0522, -118.2437, "America/Los_Angeles"),
            LocationInfo("Kanada", "CA", "Toronto", "Merkez", 43.6532, -79.3832, "America/Toronto"),
            LocationInfo("Mısır", "EG", "Kahire", "Merkez", 30.0444, 31.2357, "Africa/Cairo"),
            LocationInfo("BAE", "AE", "Dubai", "Merkez", 25.2048, 55.2708, "Asia/Dubai"),
            LocationInfo("Katar", "QA", "Doha", "Merkez", 25.2854, 51.5310, "Asia/Qatar"),
            LocationInfo("Malezya", "MY", "Kuala Lumpur", "Merkez", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
            LocationInfo("Endonezya", "ID", "Cakarta", "Merkez", -6.2088, 106.8456, "Asia/Jakarta"),
            LocationInfo("Japonya", "JP", "Tokyo", "Merkez", 35.6762, 139.6503, "Asia/Tokyo"),
            LocationInfo("Avustralya", "AU", "Sidney", "Merkez", -33.8688, 151.2093, "Australia/Sydney")
        )
    }
}
