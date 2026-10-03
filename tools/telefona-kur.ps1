# Telefona tek komutla kurulum (Windows + adb).
#   pwsh -File tools\telefona-kur.ps1                 en son sürümü GitHub'dan indirir, SHA-256'yı doğrular, kurar, açar
#   pwsh -File tools\telefona-kur.ps1 -Apk dosya.apk   yerel APK'yı kurar (indirme yok)
# Gerekenler: adb (Android platform-tools), USB ile bağlı telefon, "USB hata ayıklama" açık.
param(
  [string]$Apk,
  [string]$Adb = "adb",
  [string]$Depo = "cengizhan01-tech/hermes-telefon-widgetleri"
)
$ErrorActionPreference = "Stop"
function Yaz($m, $renk = "Cyan") { Write-Host $m -ForegroundColor $renk }
function Dur($m) { Write-Host "DUR: $m" -ForegroundColor Red; exit 1 }

if (-not (Get-Command $Adb -ErrorAction SilentlyContinue)) {
  Dur "adb bulunamadı. Android platform-tools'u kurun ya da -Adb <yol> verin."
}
$cihazlar = @(& $Adb devices | Select-String "\sdevice$")
if ($cihazlar.Count -eq 0) {
  Dur "Telefon bulunamadı. USB ile bağlayın, Geliştirici seçenekleri'nden 'USB hata ayıklama'yı açın ve telefondaki izin sorusunu onaylayın."
}
if ($cihazlar.Count -gt 1) { Dur "Birden fazla cihaz bağlı. Yalnız bir telefon bağlı bırakın." }

if (-not $Apk) {
  Yaz "En son sürüm aranıyor ($Depo)..."
  $s = Invoke-RestMethod "https://api.github.com/repos/$Depo/releases/latest" -Headers @{ "User-Agent" = "telefona-kur" }
  $varlik = $s.assets | Where-Object { $_.name -like "*.apk" } | Select-Object -First 1
  if (-not $varlik) { Dur "Son sürümde APK bulunamadı: https://github.com/$Depo/releases" }
  $Apk = Join-Path $env:TEMP $varlik.name
  Yaz "İndiriliyor: $($varlik.name) ($([int]($varlik.size/1KB)) KB)"
  Invoke-WebRequest $varlik.browser_download_url -OutFile $Apk -UseBasicParsing
  if ($s.body -match "SHA-256:\*\*\s*``([0-9a-fA-F]{64})``") {
    $beklenen = $Matches[1].ToLower()
    $gercek = (Get-FileHash $Apk -Algorithm SHA256).Hash.ToLower()
    if ($gercek -ne $beklenen) { Remove-Item $Apk -Force; Dur "SHA-256 uyuşmuyor. APK silindi, kurulmadı." }
    Yaz "SHA-256 doğrulandı." "Green"
  } else {
    Yaz "Uyarı: sürüm notunda SHA-256 bulunamadı, doğrulama atlandı." "Yellow"
  }
}
if (-not (Test-Path -LiteralPath $Apk)) { Dur "APK bulunamadı: $Apk" }

Yaz "Kuruluyor (eski sürüm varsa üstüne güncellenir, ayarlar korunur)..."
$cikti = & $Adb install -r $Apk 2>&1
if ($cikti -notmatch "Success") {
  Dur ("Kurulum başarısız: " + ($cikti -join " ") + "  (İmza farklıysa önce eski uygulamayı kaldırın.)")
}
& $Adb shell am start -n net.hermes.orbakiye/.MainActivity | Out-Null
Yaz "Kuruldu ve açıldı." "Green"
Yaz "Şimdi telefonda: OpenRouter anahtarını ve 'Sunucu adresi'ni kaydedin, sonra ana ekrana uzun basıp Widget'lar'dan ekleyin."
