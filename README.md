# Hermes telefon widget'ları (OR Bakiye)

**EN:** Four full-page Android home-screen widgets for a self-hosted setup (Hermes Agent + Homepage + Beszel on a home server reachable over Tailscale): OpenRouter balance/burn-rate, server status, ChatGPT/Codex quota and Claude plan limits. Plain Java, no Gradle (`aapt2 + javac + d8 + apksigner`). It is built around the author's own stack; adapt the endpoints to yours. No secrets in the repo: API keys and logins are typed into the app.

**TR:** Ev sunucusu için (Hermes Agent + Homepage + Beszel, Tailscale ile erişim) tek uygulamada **dört tam sayfa Android widget'ı**. Gradle yok.

| Widget | Boyut | Ne gösterir |
|---|---|---|
| **OR Bakiye** (OpenRouter) | 5×8 | harcama hızı, bitiş tahmini, 12 saatlik grafik, etkin model, anahtar/limit özeti |
| **Hermes Sunucu** | 5×8 | sunucu durumu (CPU/RAM/disk/açık kalma), servisler, etkin Hermes modeli, kısayol düğmeleri |
| **ChatGPT Kota** | 5×9 | 5 saatlik + haftalık Codex kotası, geri sayım, ≥%90/≥%85 bildirim |
| **Claude Limit** | 5×9 | 5 saatlik + haftalık Claude planı, ek kullanım, tempo/bitiş tahmini |

## Veri nereden geliyor
Hepsi `http://<SUNUCU_IP>` üzerindeki kendi hizmetlerinden okunur (`SUNUCU_IP`: derlerken verilen sunucu adresi):

| Kaynak | Adres | Kullanan |
|---|---|---|
| Homepage kaynak API'si | `:3000/api/widgets/resources?type=cpu\|memory\|disk\|uptime\|cputemp` | Hermes Sunucu |
| Hermes dashboard | `:9119/api/status`, `:9119/api/model/info` | Hermes Sunucu |
| Beszel (PocketBase) | `:8090/api/collections/...` (e-posta/parola uygulamada girilir) | Hermes Sunucu |
| Kota JSON'ları | `:3000/images/codex-kota.json`, `:3000/images/claude-limit.json` | ChatGPT Kota, Claude Limit |
| OpenRouter | `https://openrouter.ai/api/v1/{key,credits,activity}` (anahtar uygulamada girilir) | OR Bakiye |

### Kota JSON biçimi (jeton YOK, yalnız yüzde ve zaman)
Sunucunuzda 5 dakikada bir bir betik ile üretip Homepage'in `images/` klasörüne yazın:

```json
{"guncellendi": 1791017716,
 "kisa":  {"kullanilan": 0.0, "yenilenme": 1791035716},
 "uzun":  {"kullanilan": 2.0, "yenilenme": 1791580475},
 "hata": ""}
```
`claude-limit.json` ayrıca `plan` ve `ekstra: {acik, harcanan, limit, para}` taşır. Değerler yüzde, zamanlar epoch saniyesidir.

## Gizlilik
- Depoda **anahtar, parola, jeton, keystore yoktur**; `.gitignore` bunları engeller. OpenRouter anahtarı ve Beszel girişi **uygulamanın içine** yazılır, yalnız uygulamanın özel alanında (SharedPreferences) tutulur.
- Sunucu adresi kaynakta sabit değildir: `@SUNUCU_IP@` yer tutucusu derleme sırasında `SUNUCU_IP` ile değişir.
- Uygulama düz `http` ile **yalnız verdiğiniz sunucu adresine** bağlanır (`res/xml/network_security_config.xml`). Sunucunuz yalnız Tailscale gibi özel bir ağdan erişilebilir olmalıdır; internete açmayın.

## Derleme
Gerekli: JDK 17, Android SDK (build-tools 34.0.0, platforms/android-34), Windows + Git Bash + Python.
Bu araçları **özel karakter içermeyen** bir klasöre koyun (örn. `C:\android-build`: içinde `jdk-17*/` ve `sdk/`).

```bash
SUNUCU_IP=100.x.y.z KEYSTORE_PASS=<kendi-parolan> TOOLS=/c/android-build bash build-orbakiye.sh
```
Çıktı: `build/orbakiye.apk`. İlk çalıştırmada `$TOOLS/orbakiye.keystore` yoksa oluşturulur. **Keystore'u ve parolasını paylaşmayın/commit etmeyin.**

## Klasörler
- `src/net/hermes/orbakiye/`: Java kaynakları (`*Api`, `*Widget`, `Tema`, `Uyari`, `RefreshService`, `MainActivity`/`HermesActivity`)
- `res/`: düzenler, temalar (aurora, cyber, ice, matrix, ocean, sunset), widget tanımları
- `tools/claude-limit-gonder.ps1`: eski yöntem; Claude limit JSON'unu adb ile telefona iter (artık sunucudan okunuyor, yedek)
- `build-orbakiye.sh`: derleme betiği

## Lisans
MIT, `LICENSE` dosyasına bakın.
