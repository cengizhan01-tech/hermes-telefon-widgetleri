#!/bin/bash
# OR Bakiye APK derleme betiği (Gradle yok; aapt2 + javac + d8 + apksigner).
#
# Gerekli ortam değişkenleri:
#   KEYSTORE_PASS APK imza anahtarının parolası (repoda TUTULMAZ).
#   TOOLS         içinde jdk-17*/ ve sdk/ (build-tools/34.0.0, platforms/android-34) bulunan klasör.
#                 Windows'ta özel karakter/boşluksuz bir yol kullan (örn. C:ndroid-build).
# İsteğe bağlı:  KEYSTORE_FILE (varsayılan: $TOOLS/orbakiye.keystore; yoksa oluşturulur)
#                VERSION_CODE (varsayılan 16), VERSION_NAME (varsayılan 1.1.14)
# Sunucu adresi derlemede VERİLMEZ; uygulamada "Sunucu adresi" kutusuna girilir.
#
# Kullanım (Git Bash):  KEYSTORE_PASS=... TOOLS=/c/android-build bash build-orbakiye.sh
set -e
: "${KEYSTORE_PASS:?KEYSTORE_PASS ayarla (keystore parolasi; repoda tutulmaz)}"
: "${TOOLS:?TOOLS ayarla (jdk-17*/ ve sdk/ içeren klasör)}"
PROJE="$(cd "$(dirname "$0")" && pwd)"
JDK="$(ls -d "$TOOLS"/jdk-17*/ | head -1)"; JDK="${JDK%/}"
BT="$TOOLS/sdk/build-tools/34.0.0"
AJ="$TOOLS/sdk/platforms/android-34/android.jar"
KS="${KEYSTORE_FILE:-$TOOLS/orbakiye.keystore}"
export PATH="$JDK/bin:$PATH"
PY="$(command -v python || command -v py || command -v python3)"

# Çalışma klasörü: kaynak kopyalanır (repodaki dosyalar değişmez)
W="$TOOLS/_work"
rm -rf "$W" && mkdir -p "$W/s" "$W/gen" "$W/classes" "$W/dex"
cp -r "$PROJE/src" "$PROJE/res" "$PROJE/AndroidManifest.xml" "$W/s/"
cd "$W"

"$BT/aapt2.exe" compile --dir s/res -o res.zip
"$BT/aapt2.exe" link -o app.unsigned.apk -I "$AJ" --manifest s/AndroidManifest.xml \
  --java gen --min-sdk-version 24 --target-sdk-version 34 --version-code "${VERSION_CODE:-16}" --version-name "${VERSION_NAME:-1.1.14}" res.zip
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
