#!/bin/bash
# OR Bakiye APK derleme betiği (Gradle yok; aapt2 + javac + d8 + apksigner).
#
# Gerekli ortam değişkenleri:
#   SUNUCU_IP     kendi sunucunun IP'si (örn. Tailscale IP'n). Kaynaktaki @SUNUCU_IP@ yer tutucusu derlemede bununla değişir.
#   KEYSTORE_PASS APK imza anahtarının parolası (repoda TUTULMAZ).
#   TOOLS         içinde jdk-17*/ ve sdk/ (build-tools/34.0.0, platforms/android-34) bulunan klasör.
#                 Windows'ta özel karakter/boşluksuz bir yol kullan (örn. C:\android-build).
# İsteğe bağlı:  KEYSTORE_FILE (varsayılan: $TOOLS/orbakiye.keystore; yoksa oluşturulur)
#
# Kullanım (Git Bash):  SUNUCU_IP=100.x.y.z KEYSTORE_PASS=... TOOLS=/c/android-build bash build-orbakiye.sh
set -e
: "${SUNUCU_IP:?SUNUCU_IP ayarla (kendi sunucu IP adresin)}"
: "${KEYSTORE_PASS:?KEYSTORE_PASS ayarla (keystore parolasi; repoda tutulmaz)}"
: "${TOOLS:?TOOLS ayarla (jdk-17*/ ve sdk/ içeren klasör)}"
PROJE="$(cd "$(dirname "$0")" && pwd)"
JDK="$(ls -d "$TOOLS"/jdk-17*/ | head -1)"; JDK="${JDK%/}"
BT="$TOOLS/sdk/build-tools/34.0.0"
AJ="$TOOLS/sdk/platforms/android-34/android.jar"
KS="${KEYSTORE_FILE:-$TOOLS/orbakiye.keystore}"
export PATH="$JDK/bin:$PATH"
PY="$(command -v python || command -v py || command -v python3)"

# Çalışma klasörü: kaynak kopyalanır, @SUNUCU_IP@ yer tutucusu değiştirilir (repodaki dosyalar değişmez)
W="$TOOLS/_work"
rm -rf "$W" && mkdir -p "$W/s" "$W/gen" "$W/classes" "$W/dex"
cp -r "$PROJE/src" "$PROJE/res" "$PROJE/AndroidManifest.xml" "$W/s/"
find "$W/s" -type f \( -name '*.java' -o -name '*.xml' \) -exec sed -i "s/@SUNUCU_IP@/$SUNUCU_IP/g" {} +
cd "$W"

"$BT/aapt2.exe" compile --dir s/res -o res.zip
"$BT/aapt2.exe" link -o app.unsigned.apk -I "$AJ" --manifest s/AndroidManifest.xml \
  --java gen --min-sdk-version 24 --target-sdk-version 34 --version-code 1 --version-name 1.0 res.zip
javac --release 8 -encoding UTF-8 -classpath "$AJ" -d classes $(find s/src gen -name '*.java') 2>&1 | grep -v '^Note:' || true
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --lib "$AJ" --min-api 24 --output dex $(find classes -name '*.class')
"$PY" - <<'PY'
import zipfile
z = zipfile.ZipFile('app.unsigned.apk', 'a')
z.write('dex/classes.dex', 'classes.dex')
z.close()
PY
"$BT/zipalign.exe" -f -p 4 app.unsigned.apk app.aligned.apk
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass "$KEYSTORE_PASS" -keypass "$KEYSTORE_PASS" \
    -alias orbakiye -keyalg RSA -keysize 2048 -validity 9000 -dname "CN=OR Bakiye" 2>/dev/null
fi
mkdir -p "$PROJE/build"
java -jar "$BT/lib/apksigner.jar" sign --ks "$KS" --ks-pass "pass:$KEYSTORE_PASS" --key-pass "pass:$KEYSTORE_PASS" \
  --out "$PROJE/build/orbakiye.apk" app.aligned.apk
java -jar "$BT/lib/apksigner.jar" verify "$PROJE/build/orbakiye.apk" && echo "APK HAZIR: $PROJE/build/orbakiye.apk" && ls -l "$PROJE/build/orbakiye.apk"
