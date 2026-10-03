## OR Bakiye: Hermes telefon widget'ları v1.1.0

**İndir:** aşağıdaki `hermes-telefon-widgetleri-v1.1.0.apk` dosyasını telefona indirin (Android 7.0 ve üzeri).

**SHA-256:** `6ff002c4dc7bd5f6fd39b6687cbe7950b2ad23bed0ea13909566d42b480c57c5`

### Ne var
Tek uygulamada dört tam sayfa widget: **OR Bakiye** (OpenRouter), **Hermes Sunucu**, **ChatGPT Kota**, **Claude Limiti**.
- OR Bakiye yalnız OpenRouter API anahtarıyla çalışır.
- Diğer üçü kendi sunucunuzu (Homepage, Hermes paneli, Beszel, kota JSON dosyası) okur.

### Bu sürümde yeni
- **Sunucu adresi artık uygulamada girilir**, derlemede sabit değil. APK herkes için kullanılabilir.
- Adres girilmezse sunucu widget'ları "Sunucu adresi girilmemiş" yazar.
- Örnek e-posta ipucu nötrleştirildi.
- Claude widget'ının seçici adı "Claude Limiti" oldu.

### Kolay yol: Obtainium (otomatik güncelleme)
[Obtainium](https://github.com/ImranR98/Obtainium) uygulamasında **Uygulama ekle** deyip `https://github.com/cengizhan01-tech/hermes-telefon-widgetleri` adresini yapıştırın. Yeni sürümler otomatik gelir ve ayarlarınız korunur.

### Elle kurulum
1. APK'yı indirip açın. Telefon "bilinmeyen kaynaktan yükle" izni ister, verin. Play Protect uyarı gösterebilir (APK Play Store'dan gelmiyor).
2. Uygulamayı açın. OpenRouter anahtarınızı ve/veya **Sunucu adresi**nizi (örnek: `100.x.y.z`, port yok) kaydedin.
3. Ana ekrana uzun basın → Widget'lar → OR Bakiye / Hermes Sunucu / ChatGPT Kota / Claude Limiti.

### Güvenlik
- APK'da anahtar, parola veya kişisel adres yoktur. Her şey sizin telefonunuzda, uygulamanın özel alanında saklanır.
- Sunucunuza düz `http` ile bağlanır; sunucunuzu yalnız Tailscale gibi özel ve şifreli bir ağdan erişilebilir tutun.
- SHA-256 değerini yukarıdakiyle karşılaştırın. Kendiniz derlemek için README'deki "Derleme" bölümüne bakın.
