package com.namazvakti.app.model

data class LocationInfo(
    val country: String,
    val countryCode: String,
    val province: String,
    val district: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneId: String,
    val isGpsDetected: Boolean = false
) {
    val displayTitle: String
        get() = when {
            district.isNotBlank() && province.isNotBlank() && !district.equals(province, ignoreCase = true) ->
                "$province, $district"
            province.isNotBlank() -> province
            district.isNotBlank() -> district
            else -> country
        }

    val displaySubtitle: String
        get() = buildString {
            if (country.isNotBlank()) append(country)
            if (isGpsDetected) {
                if (isNotEmpty()) append(" • ")
                append("GPS")
            }
        }

    companion object {
        val DEFAULT_ISTANBUL = LocationInfo(
            country = "Türkiye",
            countryCode = "TR",
            province = "İstanbul",
            district = "Fatih",
            latitude = 41.0186,
            longitude = 28.9497,
            timezoneId = "Europe/Istanbul",
            isGpsDetected = false
        )
    }
}
