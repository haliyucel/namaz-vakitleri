package com.namazvakti.app.data

import java.time.LocalDate

enum class FiqhCategory(val label: String) {
    ABDEST("Abdest ve Gusül"),
    NAMAZ("Namaz"),
    ORUC("Oruç"),
    ZEKAT("Zekât"),
    TEMEL("Temel İbadetler")
}

data class FiqhTopic(
    val id: String,
    val category: FiqhCategory,
    val title: String,
    val summary: String,
    val bullets: List<String>
)

data class TesbihPreset(
    val title: String,
    val arabic: String,
    val meaning: String,
    val target: Int
)

data class ReligiousDayItem(
    val name: String,
    val date: LocalDate,
    val hijriNote: String
)

/**
 * Curated offline Turkish fiqh guide (Diyanet İlmihali / Hanefi standard),
 * Namaz Tesbihatı presets, and upcoming 2026–2028 Religious Days calendar.
 */
object ReligiousGuideData {

    val fiqhTopics: List<FiqhTopic> = listOf(
        // ABDEST & GUSÜL
        FiqhTopic(
            id = "abdest_farzlari",
            category = FiqhCategory.ABDEST,
            title = "Abdestin Farzları (4 Farz)",
            summary = "Mâide Suresi 6. ayet gereği abdestin sahih olması için eksiksiz yapılması gereken 4 temel şart:",
            bullets = listOf(
                "Yüzü yıkamak (alnın saç bitiminden çene altına ve iki kulak yumuşağına kadar).",
                "Kolları dirseklerle birlikte yıkamak.",
                "Başın en az dörtte birini (Hanefî) ıslak elle mesh etmek.",
                "Ayakları topuk kemikleriyle birlikte yıkamak."
            )
        ),
        FiqhTopic(
            id = "abdest_sunnetleri",
            category = FiqhCategory.ABDEST,
            title = "Abdestin Sünnetleri ve Adabı",
            summary = "Abdesti kâmil hâle getiren başlıca sünnetler:",
            bullets = listOf(
                "Niyet etmek ve Eûzü-Besmele ile başlamak.",
                "Elleri bileklere kadar üç kez yıkamak ve parmak aralarını hilâllemek.",
                "Ağza (mazmaza) ve burna (istinşak) üçer kez su vermek.",
                "Yıkanan uzuvları üçer defa yıkamak, sağ uzuvdan başlamak ve ara vermemek (muvâlât).",
                "Başın tamamını mesh etmek, kulakları ve enseyi mesh etmek, suyu israf etmemek."
            )
        ),
        FiqhTopic(
            id = "abdest_bozanlar",
            category = FiqhCategory.ABDEST,
            title = "Abdesti Bozan Durumlar",
            summary = "Hanefî mezhebine göre abdesti bozan başlıca hâller:",
            bullets = listOf(
                "Ön ve arka yollardan idrar, dışkı, yel veya herhangi bir sıvı çıkması.",
                "Vücudun herhangi bir yerinden kan, irin veya sarı su çıkıp etrafa dağılması (ağızdan gelen kan tükürükten fazla veya eşitse bozar).",
                "Ağız dolusu kusmak.",
                "Yatarak veya bir yere yaslanarak uyumak; bayılmak, sarhoş olmak.",
                "Rükûlu ve secdeli namazda yanındakinin duyacağı kadar sesli gülmek (kahkaha hem namazı hem abdesti bozar)."
            )
        ),
        FiqhTopic(
            id = "gusul_teyemmum",
            category = FiqhCategory.ABDEST,
            title = "Gusül, Teyemmüm ve Mest Üzerine Mesh",
            summary = "Boy abdesti, su bulunamadığında teyemmüm ve mest hükümleri:",
            bullets = listOf(
                "Guslün Farzları (3): Ağza su verip çalkalamak, burna su çekmek, bütün bedeni kuru yer kalmayacak şekilde yıkamak.",
                "Teyemmümün Farzları (2): Niyet etmek; temiz toprağa/toprak cinsine elleri vurup yüzü mesh etmek, ikinci kez vurup kolları dirseklerle birlikte mesh etmek.",
                "Mest Üzerine Mesh: Abdestli olarak giyilen ve şartları taşıyan mest üzerine mukim (yerleşik) 24 saat, seferi (yolcu) 72 saat mesh edebilir."
            )
        ),

        // NAMAZ
        FiqhTopic(
            id = "namaz_farzlari",
            category = FiqhCategory.NAMAZ,
            title = "Namazın Farzları (12 Farz)",
            summary = "6'sı namaza başlamadan önce (şartlar), 6'sı namazın içinde (rükünler) olmak üzere 12 farz:",
            bullets = listOf(
                "Dışındaki Şartlar: Hadesten taharet (abdest/gusül), necasetten taharet (temizlik), setr-i avret (örtünme), istikbâl-i kıble (kıbleye dönme), vakit, niyet.",
                "İçindeki Rükünler: İftitah tekbiri (başlama tekbiri), kıyam (ayakta durma), kıraat (Kur'an okuma), rükû (eğilme), sücûd (secde), ka'de-i ahîre (son oturuş)."
            )
        ),
        FiqhTopic(
            id = "namaz_rekatlar",
            category = FiqhCategory.NAMAZ,
            title = "Beş Vakit Namazın Rekât Tablosu",
            summary = "Günlük beş vakit namaz, Vitir ve Cuma namazının rekât sayıları:",
            bullets = listOf(
                "Sabah Namazı (4): 2 ilk sünnet + 2 farz.",
                "Öğle Namazı (10): 4 ilk sünnet + 4 farz + 2 son sünnet.",
                "İkindi Namazı (8): 4 gayr-ı müekked sünnet + 4 farz.",
                "Akşam Namazı (5): 3 farz + 2 son sünnet.",
                "Yatsı ve Vitir (13): 4 ilk sünnet + 4 farz + 2 son sünnet + 3 rekât Vitir (vacip).",
                "Cuma Namazı: 4 ilk sünnet + 2 farz (cemaatle) + 4 son sünnet."
            )
        ),
        FiqhTopic(
            id = "sehiv_secdesi",
            category = FiqhCategory.NAMAZ,
            title = "Namazın Vacipleri ve Sehiv Secdesi",
            summary = "Namazın vaciplerinden biri unutularak terk edilir veya geciktirilirse sehiv secdesi gerekir:",
            bullets = listOf(
                "Başlıca Vacipler: Fatiha okumak, farzların ilk 2 rekâtında zamm-ı sure okumak, tadîl-i erkân, ilk oturuşta Ettehiyyâtü okumak, selâm vermek, Vitir'de Kunut duaları.",
                "Sehiv Secdesi Nasıl Yapılır? Son oturuşta yalnızca Ettehiyyâtü okunur, sağa (veya iki tarafa) selâm verilip iki kez secde edilir; tekrar oturup Ettehiyyâtü, Salli-Bârik ve Rabbenâ okunarak iki tarafa selâm verilir.",
                "Not: Farzlardan biri unutulup terk edilirse sehiv secdesi yetmez, namazın yeniden kılınması gerekir."
            )
        ),
        FiqhTopic(
            id = "namaz_bozanlar",
            category = FiqhCategory.NAMAZ,
            title = "Namazı Bozan Durumlar (Müfsidat)",
            summary = "Namaz esnasında yapıldığında namazı geçersiz kılan durumlar:",
            bullets = listOf(
                "Namazda konuşmak, bir şey yiyip içmek, dünyevi bir sebeple ağlamak veya sesli gülmek.",
                "Göğsü kıbleden çevirmek, abdestin bozulması veya avret mahallinin bir rükün süresi açık kalması.",
                "Amel-i kesîr: Dışarıdan bakanın namazda olmadığını sanacağı kadar ardışık hareket yapmak.",
                "Namazın 12 farzından birini terk etmek veya vaktin (sabah namazında güneşin doğması gibi) çıkması."
            )
        ),
        FiqhTopic(
            id = "kerahat_seferi_kaza",
            category = FiqhCategory.NAMAZ,
            title = "Kerahat Vakitleri, Seferîlik ve Kaza Fıkhı",
            summary = "Günlük hayatta en sık sorulan üç namaz hükmü:",
            bullets = listOf(
                "Kerahat Vakitleri: Güneş doğuşundan 45 dk sonrasına kadar (İşrak), öğleden önceki 45 dk (İstiva) ve akşamdan önceki 45 dk (İsfirar). Bu vakitlerde kaza ve nafile kılınmaz; ancak akşam kerahatinde o günün ikindi farzı kılınabilir.",
                "Seferîlik (Yolculuk): İkamet yerinden en az 90 km uzağa 15 günden az kalmak niyetiyle giden kişi seferî sayılır; 4 rekâtlı farzları (öğle, ikindi, yatsı) 2 rekât kılar.",
                "Kaza Namazı: Yalnızca beş vakit farz namazlar ve Hanefî'de Vitir namazı kaza edilir; sünnetler (sabah namazının aynı gün kuşluk vaktine kadar kaza edilmesi hariç) kaza edilmez."
            )
        ),

        // ORUÇ
        FiqhTopic(
            id = "oruc_farzlari",
            category = FiqhCategory.ORUC,
            title = "Orucun Farzları ve Niyet Zamanı",
            summary = "Oruç ibadetinin geçerlilik şartları ve niyet vakitleri:",
            bullets = listOf(
                "Orucun Farzları (3): Niyet etmek, imsaktan iftara (akşam ezanına) kadar yeme-içme ve cinsel ilişkiden uzak durmak, oruç vaktini bilmek.",
                "Ramazan ve Nafile Oruç Niyeti: Akşam güneş battıktan ertesi gün kuşluk vaktine (öğleye yaklaşık 1 saat kalana / dahve-i kübrâ) kadar niyet edilebilir (imsaktan sonra bir şey yenmemişse).",
                "Kaza ve Kefaret Orucu Niyeti: Mutlaka imsak vaktinden önce (geceden) niyet edilmelidir."
            )
        ),
        FiqhTopic(
            id = "oruc_bozanlar",
            category = FiqhCategory.ORUC,
            title = "Orucu Bozan Durumlar (Kaza ve Kefaret)",
            summary = "Orucun bozulduğu hâller ve gerektirdiği hükümler:",
            bullets = listOf(
                "Sadece Kaza Gerektirenler: Hata ile boğaza su kaçması, bilerek ağız dolusu kusmak, besleyici serum/iğne yaptırmak, imsak geçtiğini bilmeden yemek, güneş battı sanarak erken iftar etmek, Ramazan dışındaki orucu bozmak.",
                "Kaza + Kefaret Gerektirenler: Ramazan orucuna geceden niyet ettikten sonra hiçbir meşru mazeret yokken bilerek ve isteyerek yiyip içmek veya cinsel ilişkide bulunmak (60 gün peş peşe oruç + 1 gün kaza)."
            )
        ),
        FiqhTopic(
            id = "oruc_bozmayanlar",
            category = FiqhCategory.ORUC,
            title = "Orucu Bozmayan Durumlar ve Fidye",
            summary = "Diyanet Din İşleri Yüksek Kurulu kararlarına göre orucu bozmayan durumlar:",
            bullets = listOf(
                "Unutarak yemek veya içmek (hatırlayınca hemen bırakılır, oruca devam edilir).",
                "Kendi tükürüğünü yutmak, misvak veya macun yutmadan diş fırçalamak, göz damlası damlatmak, kan vermek / kan tahlili yaptırmak.",
                "Astım spreyi, dil altı hapı (yutulmadan emilen) ve besleyici olmayan aşı/iğneler (Diyanet fetvasına göre bozmaz).",
                "Fidye: İyileşme ümidi olmayan kronik hastalık veya ileri yaşlılık sebebiyle oruç tutamayanlar, her gün için bir fitre miktarı fidye verir."
            )
        ),

        // ZEKÂT
        FiqhTopic(
            id = "zekat_sartlari",
            category = FiqhCategory.ZEKAT,
            title = "Zekâtın Farz Olma Şartları ve Nisap",
            summary = "Kimlere zekât farzdır ve nisap miktarı nasıl hesaplanır?",
            bullets = listOf(
                "Şartlar: Müslüman, akıllı, ergen ve hür olmak; temel ihtiyaçlar ve borçlar çıktıktan sonra nisap miktarı artıcı (nâmî) mala sahip olmak ve üzerinden 1 kamerî (Hicrî) yıl geçmek.",
                "Nisap Miktarı: 80,18 gram altın (veya bu değerde nakit, döviz, ticaret malı) ya da 561 gram gümüş.",
                "Zekât Oranı: Nakit, altın, gümüş, döviz ve ticaret mallarında kırkta bir (%2,5). Toprak ürünlerinde (öşür) sulama masrafsızsa 1/10 (%10), masraflıysa 1/20 (%5)."
            )
        ),
        FiqhTopic(
            id = "zekat_kimlere",
            category = FiqhCategory.ZEKAT,
            title = "Zekât Kimlere Verilir ve Kimlere Verilmez?",
            summary = "Tevbe Suresi 60. ayete göre zekât verilebilecek ve verilemeyecek kimseler:",
            bullets = listOf(
                "Verilebilen 8 Sınıf: Fakirler, miskinler (düşkünler), zekât memurları, müellefe-i kulûb, kölelikten kurtulacaklar, borçlular, Allah yolunda olanlar (ilim/hizmet), yolda kalmış yolcular.",
                "Verilemeyenler: Anne, baba, dede, nine (usul); evlat ve torunlar (füru); karı-koca birbirine; zengin (nisap miktarı mala sahip) kimseler; cami, yol, köprü gibi kurumlara doğrudan (kişiye temlik şarttır).",
                "Öncelik: Kardeş, amca, hala, dayı, teyze ve yeğen gibi ihtiyaç sahibi yakın akrabalara zekât vermek daha faziletlidir."
            )
        ),

        // TEMEL İBADETLER
        FiqhTopic(
            id = "iman_islam_sartlari",
            category = FiqhCategory.TEMEL,
            title = "İmanın ve İslam'ın Şartları",
            summary = "Her Müslümanın bilmesi gereken temel inanç ve ibadet esasları:",
            bullets = listOf(
                "İmanın 6 Şartı: Allah'a, meleklerine, kitaplarına, peygamberlerine, ahiret gününe, kadere (hayır ve şerrin Allah'tan olduğuna) iman etmek.",
                "İslam'ın 5 Şartı: Kelime-i şehâdet getirmek, beş vakit namaz kılmak, zekât vermek, Ramazan orucunu tutmak, gücü yetenin ömründe bir kez hacca gitmesi.",
                "Kurban ve Fitre: Nisap miktarı mala sahip olanlara Kurban Bayramı'nda kurban kesmek (Hanefî'de vacip) ve Ramazan'da fitre (fıtır sadakası) vermek vaciptir."
            )
        )
    )

    val tesbihPresets: List<TesbihPreset> = listOf(
        TesbihPreset("Sübhanallah", "سُبْحَانَ اللّٰهِ", "Allah tüm eksikliklerden münezzehtir", 33),
        TesbihPreset("Elhamdülillah", "اَلْحَمْدُ لِلّٰهِ", "Hamd Allah'a mahsustur", 33),
        TesbihPreset("Allahu Ekber", "اَللّٰهُ اَكْبَرُ", "Allah en büyüktür", 33),
        TesbihPreset("Lâ ilâhe illallah", "لَا اِلٰهَ اِلَّا اللّٰهُ", "Allah'tan başka ilah yoktur", 100),
        TesbihPreset("Estağfirullah", "اَسْتَغْفِرُ اللّٰهَ", "Allah'tan bağışlanma dilerim", 100),
        TesbihPreset("Salavat-ı Şerife", "اَللّٰهُمَّ صَلِّ عَلٰى مُحَمَّدٍ", "Allah'ım, Muhammed'e (s.a.v.) salât eyle", 100)
    )

    /** Official Diyanet religious days calendar (2026–2028) for upcoming countdowns. */
    val religiousDays: List<ReligiousDayItem> = listOf(
        ReligiousDayItem("Üç Ayların Başlangıcı", LocalDate.of(2026, 12, 10), "1 Recep 1448"),
        ReligiousDayItem("Regaip Kandili", LocalDate.of(2026, 12, 10), "Recep Ayının İlk Cuma Gecesi"),
        ReligiousDayItem("Miraç Kandili", LocalDate.of(2027, 1, 5), "27 Recep 1448"),
        ReligiousDayItem("Berat Kandili", LocalDate.of(2027, 1, 22), "15 Şaban 1448"),
        ReligiousDayItem("Ramazan Başlangıcı", LocalDate.of(2027, 2, 8), "1 Ramazan 1448"),
        ReligiousDayItem("Kadir Gecesi", LocalDate.of(2027, 3, 5), "27 Ramazan 1448"),
        ReligiousDayItem("Ramazan Bayramı (1. Gün)", LocalDate.of(2027, 3, 9), "1 Şevval 1448"),
        ReligiousDayItem("Kurban Bayramı (1. Gün)", LocalDate.of(2027, 5, 16), "10 Zilhicce 1448"),
        ReligiousDayItem("Hicri Yılbaşı (1449)", LocalDate.of(2027, 6, 6), "1 Muharrem 1449"),
        ReligiousDayItem("Aşure Günü", LocalDate.of(2027, 6, 15), "10 Muharrem 1449"),
        ReligiousDayItem("Mevlid Kandili", LocalDate.of(2027, 8, 14), "12 Rebiülevvel 1449"),
        ReligiousDayItem("Üç Ayların Başlangıcı", LocalDate.of(2027, 11, 29), "1 Recep 1449"),
        ReligiousDayItem("Regaip Kandili", LocalDate.of(2027, 12, 2), "Recep Ayının İlk Cuma Gecesi"),
        ReligiousDayItem("Miraç Kandili", LocalDate.of(2027, 12, 25), "27 Recep 1449"),
        ReligiousDayItem("Berat Kandili", LocalDate.of(2028, 1, 11), "15 Şaban 1449"),
        ReligiousDayItem("Ramazan Başlangıcı", LocalDate.of(2028, 1, 28), "1 Ramazan 1449")
    )
}
