package com.namazvakti.app.model

enum class CalculationMethod(
    val aladhanId: Int,
    val displayName: String,
    val description: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaMinutesAfterMaghrib: Int?,
    val asrShadowFactor: Double = 1.0,
    val imsakOffsetMin: Int = 0,
    val gunesOffsetMin: Int = 0,
    val ogleOffsetMin: Int = 0,
    val ikindiOffsetMin: Int = 0,
    val aksamOffsetMin: Int = 0,
    val yatsiOffsetMin: Int = 0
) {
    DIYANET(
        aladhanId = 13,
        displayName = "Diyanet İşleri Başkanlığı",
        description = "Türkiye ve Avrupa için standart (18° / 17° + temkin)",
        fajrAngle = 18.0,
        ishaAngle = 17.0,
        ishaMinutesAfterMaghrib = null,
        imsakOffsetMin = 0,
        gunesOffsetMin = -7,
        ogleOffsetMin = 5,
        ikindiOffsetMin = 4,
        aksamOffsetMin = 7,
        yatsiOffsetMin = 1
    ),
    MWL(
        aladhanId = 3,
        displayName = "Dünya İslam Birliği (MWL)",
        description = "Avrupa, Uzak Doğu ve genel kullanım (18° / 17°)",
        fajrAngle = 18.0,
        ishaAngle = 17.0,
        ishaMinutesAfterMaghrib = null
    ),
    UMM_AL_QURA(
        aladhanId = 4,
        displayName = "Ümmü'l-Kura (Mekke)",
        description = "Suudi Arabistan ve Körfez bölgesi (18.5° / +90 dk)",
        fajrAngle = 18.5,
        ishaAngle = null,
        ishaMinutesAfterMaghrib = 90
    ),
    EGYPT(
        aladhanId = 5,
        displayName = "Mısır Genel Araştırma Kurumu",
        description = "Afrika, Suriye, Lübnan ve Malezya (19.5° / 17.5°)",
        fajrAngle = 19.5,
        ishaAngle = 17.5,
        ishaMinutesAfterMaghrib = null
    ),
    ISNA(
        aladhanId = 2,
        displayName = "ISNA (Kuzey Amerika)",
        description = "ABD ve Kanada için standart (15° / 15°)",
        fajrAngle = 15.0,
        ishaAngle = 15.0,
        ishaMinutesAfterMaghrib = null
    ),
    KARACHI(
        aladhanId = 1,
        displayName = "Karaçi İslam Bilimleri Üniv.",
        description = "Pakistan, Hindistan, Bangladeş (18° / 18°)",
        fajrAngle = 18.0,
        ishaAngle = 18.0,
        ishaMinutesAfterMaghrib = null
    );

    companion object {
        fun fromNameOrDefault(name: String?): CalculationMethod =
            entries.firstOrNull { it.name == name } ?: DIYANET
    }
}
