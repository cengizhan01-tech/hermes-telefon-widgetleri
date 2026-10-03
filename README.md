# Hermes telefon widget'ları (OR Bakiye)

Ev sunucusu için (Hermes Agent + Homepage + Beszel, Tailscale ile erişim) tek uygulamada **dört tam sayfa Android widget'ı**: OpenRouter bakiye ve harcama hızı, sunucu durumu, ChatGPT/Codex kotası ve Claude plan limiti. Saf Java, Gradle yok (`aapt2 + javac + d8 + apksigner`).

**Hızlı başlangıç:** [Sürümler](../../releases/latest) sayfasından APK'yı indirin, "bilinmeyen kaynaktan yükle" izni verin, uygulamayı açıp sunucu adresinizi (ve isterseniz OpenRouter anahtarınızı) girin, sonra widget'ları ekleyin. OR Bakiye widget'ı yalnız OpenRouter API anahtarıyla tek başına çalışır; diğer üçü kendi sunucunuzdaki uç noktaları okur (aşağıya bakın). Depoda hiçbir gizli bilgi yoktur.

| Widget | Boyut | Ne gösterir | Neye ihtiyaç duyar |
|---|---|---|---|
| **OR Bakiye** (OpenRouter) | 5×8 | harcama hızı, bitiş tahmini, 12 saatlik grafik, etkin model, anahtar/limit özeti | yalnız OpenRouter API anahtarı |
| **Hermes Sunucu** | 5×8 | sunucu durumu (işlemci/bellek/disk/açık kalma süresi), servisler, etkin Hermes modeli, kısayol düğmeleri | sunucu adresi + Homepage + Hermes paneli (+ Beszel) |
| **ChatGPT Kota** | 5×9 | 5 saatlik ve haftalık Codex kotası, geri sayım, %90 / %85 üstünde bildirim | sunucu adresi + kota JSON'u |
| **Claude Limiti** | 5×9 | 5 saatlik ve haftalık Claude planı, ek kullanım, tempo ve bitiş tahmini | sunucu adresi + kota JSON'u |

## Kurulum önerisi: Obtainium (otomatik güncellemeli)
[Obtainium](https://github.com/ImranR98/Obtainium), uygulamaları doğrudan GitHub Sürümleri'nden kuran ve yeni sürüm çıkınca sizi uyaran ücretsiz bir Android uygulamasıdır. Bu depoyu bir kez eklersiniz, sonrası otomatik gider:
1. Obtainium'u telefona kurun ([Sürümler sayfası](https://github.com/ImranR98/Obtainium/releases) veya F-Droid).
2. Obtainium'da **Uygulama ekle** (+) deyip şu adresi yapıştırın, sonra **Ekle**'ye ve **Kur**'a basın:

```
https://github.com/cengizhan01-tech/hermes-telefon-widgetleri
```
Obtainium yüklüyse bu bağlantıya telefonda dokunmak da yeter: [obtainium://add/https://github.com/cengizhan01-tech/hermes-telefon-widgetleri](obtainium://add/https://github.com/cengizhan01-tech/hermes-telefon-widgetleri)

Her yeni sürüm aynı anahtarla imzalandığı için Obtainium onu mevcut uygulamanın üstüne güncelleme olarak kurar; ayarlarınız silinmez. Obtainium kullanmak istemezseniz aşağıdaki elle kurulum da çalışır.

## Elle kurulum (APK)
1. [Sürümler](../../releases/latest) sayfasından `hermes-telefon-widgetleri-vX.Y.Z.apk` dosyasını telefona indirin. Sürüm notundaki SHA-256 ile karşılaştırabilirsiniz.
2. Android 7.0 veya üstü gerekir. Dosyayı açarken telefon, tarayıcı veya dosya yöneticisi için "bu kaynaktan uygulama yüklemeye izin ver" diye sorar. İzin verin. Play Protect "tanınmayan geliştirici" uyarısı gösterebilir; APK Play Store'dan gelmediği için bu normaldir.
3. **OR Bakiye** uygulamasını açın:
   - OpenRouter anahtarınızı yapıştırıp kaydedin (yalnız bu telefonda saklanır).
   - Diğer widget'lar için **Sunucu adresi**ni girin (örnek: `100.x.y.z` ya da `sunucum.ornek.ts.net`). Port yazmayın.
   - İsterseniz Beszel girişini (salt okunur kullanıcı) kaydedin.
4. Ana ekrana uzun basın → Widget'lar → **OR Bakiye / Hermes Sunucu / ChatGPT Kota / Claude Limiti**.

Sunucu adresi girilmezse sunucu widget'ları "Sunucu adresi girilmemiş" yazar; OR Bakiye anahtarla çalışmaya devam eder.

### İsteğe bağlı: bilgisayardan tek komutla kurulum (Windows + adb)
Telefon USB ile bağlıyken, "USB hata ayıklama" açıkken, depo klasöründe:

```powershell
pwsh -File tools	elefona-kur.ps1
```
Betik en son sürümü GitHub'dan indirir, SHA-256 değerini sürüm notuyla karşılaştırır, kurar ve uygulamayı açar. Eski sürüm varsa üstüne güncellenir, ayarlarınız korunur. Yerel bir APK için `-Apk dosya.apk` verin.

## Veri nereden geliyor
Sunucu widget'ları, girdiğiniz adres üzerinde kendi hizmetlerinizden okur:

| Kaynak | Adres | Kullanan |
|---|---|---|
| Homepage kaynak API'si | `:3000/api/widgets/resources?type=cpu\|memory\|disk\|uptime\|cputemp` | Hermes Sunucu |
| Hermes paneli | `:9119/api/status`, `:9119/api/model/info` | Hermes Sunucu |
| Beszel (PocketBase) | `:8090/api/collections/...` (e-posta ve parola uygulamada girilir) | Hermes Sunucu |
| Kota JSON dosyaları | `:3000/images/codex-kota.json`, `:3000/images/claude-limit.json` | ChatGPT Kota, Claude Limiti |
| OpenRouter | `https://openrouter.ai/api/v1/{key,credits,activity}` (anahtar uygulamada girilir) | OR Bakiye |

### Kota JSON biçimi (jeton YOK, yalnız yüzde ve zaman)
Sunucunuzda 5 dakikada bir çalışan bir betikle üretip Homepage'in `images/` klasörüne yazın:

```json
{"guncellendi": 1791017716,
 "kisa":  {"kullanilan": 0.0, "yenilenme": 1791035716},
 "uzun":  {"kullanilan": 2.0, "yenilenme": 1791580475},
 "hata": ""}
```
`claude-limit.json` ayrıca `plan` ve `ekstra: {acik, harcanan, limit, para}` alanlarını taşır. Değerler yüzde, zamanlar epoch saniyesidir.

## Gizlilik ve güvenlik
- Depoda **anahtar, parola, jeton ve imza anahtarı (keystore) yoktur**; `.gitignore` bunları engeller. OpenRouter anahtarı, Beszel girişi ve sunucu adresi **uygulamanın içinde** girilir, yalnız uygulamanın özel alanında (SharedPreferences) saklanır.
- Uygulama sunucunuza düz `http` ile bağlanır (`res/xml/network_security_config.xml`: sunucu adresi kullanıcıya özel olduğu için düz http bütün adreslere serbesttir; https doğrulaması aynen sürer). Bu yüzden sunucunuzu **yalnız Tailscale gibi özel ve şifreli bir ağdan erişilebilir** tutun, internete açmayın.
- APK'yı yalnız bu deponun Sürümler sayfasından indirin ve SHA-256 değerini sürüm notuyla karşılaştırın. İsterseniz aşağıdaki gibi kendiniz derleyin.

## Derleme (kendiniz derlemek isterseniz)
Gerekenler: JDK 17, Android SDK (build-tools 34.0.0, platforms/android-34), Windows, Git Bash ve Python.
Bu araçları **özel karakter içermeyen** bir klasöre koyun (örnek: `C:\android-build`; içinde `jdk-17*/` ve `sdk/` olmalı).

```bash
KEYSTORE_PASS=<kendi-parolan> TOOLS=/c/android-build bash build-orbakiye.sh
```
Çıktı: `build/orbakiye.apk`. İlk çalıştırmada `$TOOLS/orbakiye.keystore` yoksa oluşturulur. **Keystore dosyasını ve parolasını paylaşmayın, depoya eklemeyin.** Kendi anahtarınızla imzalanan APK, Sürümler'deki APK'nın üzerine "güncelleme" olarak kurulmaz (imza farklı olur); önce eskisini kaldırın.

## Klasörler
- `src/net/hermes/orbakiye/`: Java kaynakları (`*Api`, `*Widget`, `Sunucu`, `App`, `Tema`, `Uyari`, `RefreshService`, `MainActivity` / `HermesActivity`)
- `res/`: düzenler, temalar (aurora, cyber, ice, matrix, ocean, sunset), widget tanımları
- `tools/telefona-kur.ps1`: bilgisayardan tek komutla kurulum (adb)
- `tools/claude-limit-gonder.ps1`: eski yöntem; Claude limit JSON'unu adb ile telefona iter (artık sunucudan okunuyor, yedek olarak duruyor)
- `build-orbakiye.sh`: derleme betiği
- `dagitim/`: yayına hazır imzalı APK ve sürüm notu (etiket gönderilince Sürüm buradan otomatik oluşur)
- `.github/workflows/surum.yml`: etiket (`v*`) gönderilince APK'nın SHA-256'sını sürüm notuyla doğrulayıp Sürümü kendiliğinden oluşturan iş akışı

## Sürümler
- **v1.1.1**: Hermes Sunucu widget'ının servis listesinden kullanılmayan "Actual Budget" çıkarıldı (kapalı görünüyordu); servis sayısı 10 → 9.
- **v1.1.0**: Sunucu adresi artık derlemede değil uygulamada girilir (APK herkes için kullanılabilir). Örnek e-posta ipucu nötrleştirildi. Claude widget'ının seçici adı "Claude Limiti" oldu.
- v1.0: ilk sürüm (yalnız kaynak).

## Lisans
MIT. Bağlayıcı metin `LICENSE` dosyasındadır (İngilizce). Bilgi amaçlı Türkçe özet için `LISANS.tr.md` dosyasına bakın.
