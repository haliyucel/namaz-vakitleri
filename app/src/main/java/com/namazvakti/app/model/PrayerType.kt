package com.namazvakti.app.model

enum class PrayerType(
    val displayName: String,
    val makamName: String?,
    val hasAdhan: Boolean
) {
    IMSAK("İmsak", "Sabah (Saba Makamı)", true),
    GUNES("Güneş", null, false),
    OGLE("Öğle", "Öğle (Uşşak Makamı)", true),
    IKINDI("İkindi", "İkindi (Rast Makamı)", true),
    AKSAM("Akşam", "Akşam (Segâh Makamı)", true),
    YATSI("Yatsı", "Yatsı (Hicaz Makamı)", true);

    companion object {
        val ADHAN_PRAYERS: List<PrayerType> = listOf(IMSAK, OGLE, IKINDI, AKSAM, YATSI)

        fun fromNameOrNull(name: String?): PrayerType? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
