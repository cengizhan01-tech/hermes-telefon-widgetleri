# Claude limit verisini telefona (OR Bakiye uygulamasının klasörüne) yazar.
# Kullanım:  pwsh -File claude-limit-gonder.ps1 -Kisa 66 -KisaEpoch 1790975399 -Uzun 32 -UzunEpoch 1791248399 `
#                -EkstraHarcanan 0 -EkstraLimit 30 -BaglamKullanilan 838641 -BaglamPencere 1000000 -BaglamYuzde 84
# Değerler Claude Code'daki "get_usage" çıktısından (Pusula) alınır. Hiçbir anahtar/oturum bilgisi YAZILMAZ.
param(
  [double]$Kisa = -1, [long]$KisaEpoch = 0,
  [double]$Uzun = -1, [long]$UzunEpoch = 0,
  [double]$EkstraHarcanan = 0, [double]$EkstraLimit = 0, [string]$Para = "EUR", [switch]$EkstraKapali,
  [long]$BaglamKullanilan = 0, [long]$BaglamPencere = 0, [double]$BaglamYuzde = -1, [double]$Otomatik = 97,
  [string]$Plan = "Pro"
)
$adb = Join-Path $PSScriptRoot "platform-tools\adb.exe"
$simdi = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$json = [ordered]@{
  guncellendi = $simdi; plan = $Plan
  kisa = @{ kullanilan = $Kisa; yenilenme = $KisaEpoch }
  uzun = @{ kullanilan = $Uzun; yenilenme = $UzunEpoch }
  ekstra = @{ acik = (-not $EkstraKapali); harcanan = $EkstraHarcanan; limit = $EkstraLimit; para = $Para }
  baglam = @{ kullanilan = $BaglamKullanilan; pencere = $BaglamPencere; yuzde = $BaglamYuzde; otomatik = $Otomatik }
} | ConvertTo-Json -Depth 4 -Compress
$tmp = Join-Path $env:TEMP "claude-limit.json"
[IO.File]::WriteAllText($tmp, $json, (New-Object Text.UTF8Encoding($false)))
& $adb shell mkdir -p /sdcard/Android/data/net.hermes.orbakiye/files | Out-Null
& $adb push $tmp /sdcard/Android/data/net.hermes.orbakiye/files/claude-limit.json
& $adb shell am broadcast -a net.hermes.orbakiye.CLAUDE_REFRESH -n net.hermes.orbakiye/.ClaudeWidget | Out-Null
Remove-Item $tmp -ErrorAction SilentlyContinue
Write-Host "Gonderildi: $json"
