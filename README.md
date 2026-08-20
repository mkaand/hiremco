# Hiremco Starter 3.0

Hiremco Starter, Android TV'de Hiremco uygulamasını (`com.superdtv`) cihaz
açıldığında veya ekran uyandığında isteğe bağlı bir gecikmeyle başlatan yardımcı
bir uygulamadır. APK yalnızca standart Android Gradle araç zinciriyle derlenir:
Android Gradle Plugin, AAPT2, D8/R8 ve Gradle. Elle DEX veya APK üretilmez.

## Özellikler

- Otomatik başlatmayı tamamen kapatan ana anahtar
- Ayrı açılış ve ekran-uyanma anahtarları
- Açılış gecikmeleri: 5, 10, 15, 20, 30, 45 veya 60 saniye
- Uyanma gecikmeleri: 2, 5, 10, 15, 20 veya 30 saniye
- Varsayılan hedef paket: `com.superdtv`
- Uygulamaya özel "diğer uygulamaların üzerinde göster" izin sayfası
- Kullanıcının Android Erişilebilirlik ayarlarından kapatabildiği hizmet

## Mimari

Manifestte `BOOT_COMPLETED` alıcısı yoktur. Etkin erişilebilirlik hizmeti
bağlandığında `Settings.Global.BOOT_COUNT`, saklanan değerle karşılaştırılır.
Yeni değer herhangi bir başlatma planlanmadan önce kalıcılaştırılır; böylece
hizmetin aynı açılışta yeniden bağlanması ikinci bir açılış başlatmaz.

Ekran uyanması, hizmet çalışırken dinamik kaydedilen `SCREEN_ON` alıcısıyla
izlenir. Android 13 ve üzerindeki cihazlarda alıcı
`Context.RECEIVER_NOT_EXPORTED` ile kaydedilir. Erişilebilirlik olayları hiçbir
zaman uygulama başlatmaz; yalnızca hedefin zaten ön planda olup olmadığını pasif
olarak izler. Tek bir bekleyen başlatmaya izin verilir ve ana/ilgili özellik
anahtarı gecikmenin sonunda yeniden kontrol edilir.

## Derleme

Proje AGP 8.5.2, Gradle 8.7, Java 17, `compileSdk 34`, `minSdk 27` ve
`targetSdk 33` kullanır. Depoda wrapper JAR bulunmadığından yerel derleme için
Gradle 8.7 kurulumu gerekir:

```text
gradle :app:assembleDebug --stacktrace
```

`Build Hiremco Starter APK` GitHub Actions iş akışı `main` dalına her push'ta
ve elle çalıştırılabilir. Android SDK platform 34 ile gerçek debug APK derler ve
`Hiremco-Starter-v3.0-debug` adlı artifact'i yükler.

## Lisans

Bu proje [MIT Lisansı](LICENSE) altında lisanslanmıştır.
