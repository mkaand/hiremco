# Hiremco Starter 2.2 — gerçek Gradle/Android SDK derlemesi

## Neden önceki denemeler çalışmadı

Eklediğiniz `build_starter_v22.py` dosyasına bakıldığında, önceki asistan javac +
d8 + aapt2 + apksigner zincirini kullanmak yerine DEX bayt kodunu, ikili
AndroidManifest.xml'i (AXML) ve APK Signature Scheme v2 imzasını Python ile elle
üretmeye çalışmış. Bu, bir derleyici yerine "derleyicinin çıktısını taklit eden"
bir yaklaşım ve son derece kırılgan: string/tür/metot indekslerinden birinde tek
bir hata olsa bile APK ya kurulamaz ya da kurulur ama çöker.

Kodun içinde en az bir somut, çalışma zamanı hatası da buldum: erişilebilirlik
servisi `registerReceiver(..., Context.RECEIVER_NOT_EXPORTED)` üç parametreli
metodunu koşulsuz çağırıyordu. Bu metot yalnızca Android 13 (API 33) ve
üzerinde var; Mi Box (2. Nesil) Google TV büyük ihtimalle Android 11 (API 30)
çalıştırdığından, bu satır cihazda `NoSuchMethodError` ile servisi çökertirdi.
Aşağıdaki yeniden yazımda bunu iki parametreli, tüm sürümlerde çalışan
`registerReceiver(receiver, filter)` ile değiştirdim.

## Bu projede ne var

Standart bir Android Studio / Gradle modülü:

```
HiremcoStarter/
├── build.gradle, settings.gradle, gradle.properties
├── app/
│   ├── build.gradle                (AGP 8.5.2, compileSdk 34, minSdk 27, targetSdk 33)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mkaand/hiremcostarter/
│       │   ├── MainActivity.java              (ayarlar ekranı)
│       │   ├── StarterAccessibilityService.java (asıl mantık)
│       │   └── ScreenReceiver.java             (ekran uyanma tetikleyicisi)
│       └── res/...
└── .github/workflows/build-apk.yml (bulutta gerçek Gradle derlemesi)
```

Mantık, orijinal README'nizdeki v2.2 mimarisiyle birebir aynı: manifestte
BOOT_COMPLETED alıcısı yok, `boot_count` karşılaştırmasıyla açılış tek seferde
yakalanıyor, SCREEN_ON için dinamik alıcı var, tekrar tetiklenmeyi önleyen
koruma bayrakları var, gecikmeler 5/10/15/20/30/45/60 sn (açılış) ve
2/5/10/15/20/30 sn (uyanma) arasında dönüyor.

Eklediğim tek pratik özellik: Hiremco uygulamasının tam paket adını bilmediğim
için, ayarlar ekranına "Yüklü uygulamalarda ara" düğmesi koydum — paket adında
veya uygulama adında "hiremco" geçen yüklü uygulamaları listeler, seçince
otomatik doldurur. Elle de yazabilirsiniz.

## Neden ben burada gerçekten derleyemiyorum

Bu sohbetin çalıştığı sandbox'ta internet erişimi kapalı ve Android
SDK/JDK/Gradle kurulu değil (yalnızca çıplak bir JRE var, javac bile yok).
Yani ben de burada gerçek bir Gradle/SDK zinciri çalıştıramıyorum — ve
`build_starter_v22.py`'nin yaptığı gibi ikinci bir elle-DEX-üretme denemesi
yapmak, sizin tam olarak kaçınmak istediğiniz hatayı tekrarlamak olurdu.
Bunun yerine gerçek zinciri *sizin* çalıştırmanızı sağlayacak iki yol
hazırladım (aşağıda).
