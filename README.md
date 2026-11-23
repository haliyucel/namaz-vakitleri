# Namaz Vakitleri (Android)

**Yapımcı:** Hasan Ali Yücel  
**Depo:** https://github.com/haliyucel/namaz-vakitleri

## Özellikler

- **Konuma Göre Namaz Vakitleri (Türkiye + Dünya Geneli):**
  - GPS ile otomatik konum bulma ve bulunan il/bölgedeki ilçe/şehirlerden anında seçim.
  - Türkiye'nin 81 il ve 973 ilçesi çevrimdışı dahili veri seti.
  - Dünya geneli şehir/ilçe arama (Open-Meteo + Nominatim) ve doğru saat dilimi desteği.
  - Diyanet İşleri Başkanlığı (varsayılan, Aladhan Method 13 aylık önbellek + çevrimdışı astronomik hesaplama), MWL, Ümmü'l-Kura, Mısır, ISNA ve Karaçi yöntemleri.
- **15 Dakika ve 5 Dakika Öncesi Uyarılar:**
  - Ezan vaktinden 15 dk önce 1. uyarı, 5 dk kala 2. uyarı (tam dakikalı alarm, cihaz yeniden başlasa da otomatik planlanır).
- **İsteğe Bağlı 5 Vakit Makamlı Ezan:**
  - Sabah (Saba), Öğle (Uşşak), İkindi (Rast), Akşam (Segâh) ve Yatsı (Hicaz) makamları çevrimdışı olarak dahildir; vakit bazlı açılıp kapatılabilir ve önizlenebilir.
- **Kıble Pusulası:**
  - Manyetik sapma (`GeomagneticField`) düzeltmeli gerçek kuzey pusulası, Kâbe açısı ve mesafesi, ±3° hizalanmada görsel ve titreşimli bildirim.
- **Otomatik Sessiz Mod ve Sesliye Dönüş:**
  - Namaz vakti girdiğinde telefonu otomatik sessize alır; belirlenen süre (5–180 dk) bitiminde telefonu otomatik tekrar sesli moda geçirir.
- **GitHub Uzaktan Güncelleme:**
  - Uygulama açılışında ve Ayarlar ekranından `haliyucel/namaz-vakitleri` GitHub Releases üzerindeki en son sürümü kontrol eder, yeni `.apk` dosyasını indirip kurulumu başlatır.

## Yeni Güncelleme Nasıl Yayınlanır?

1. `app/build.gradle.kts` içinde `versionCode` ve `versionName` değerlerini artırın (ör. `versionCode = 2`, `versionName = "1.0.1"`).
2. Yerel `release.keystore` ile `assembleRelease` derlemesini alın (aynı imza anahtarı sayesinde kullanıcılar verilerini kaybetmeden doğrudan güncelleyebilir).
3. GitHub **Releases** bölümünde yeni sürüm etiketiyle (ör. `v1.0.1`) bir Release oluşturup imzalı `.apk` dosyasını ekleyin.
4. Uygulama kurulu telefonlar yeni sürümü otomatik algılayıp indirip kurabilir.
