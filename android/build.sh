#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
root=$PWD
bt=${BUILD_TOOLS:-"${ANDROID_HOME:?Set ANDROID_HOME}/build-tools/35.0.0"}
platform=${ANDROID_JAR:-"${ANDROID_HOME:?Set ANDROID_HOME}/platforms/android-35/android.jar"}
stage=$(mktemp -d)
trap 'rm -rf "$stage"' EXIT
mkdir -p "$stage/classes" "$stage/dex" "$stage/assets" artifacts
cp public/journal/migrate.html "$stage/assets/"
cp -R android/model/. android/data/. "$stage/assets/"
python3 - "$stage/AndroidManifest.xml" <<'PY'
import os,sys,xml.etree.ElementTree as E
tree=E.parse('android/AndroidManifest.xml'); ns='{http://schemas.android.com/apk/res/android}'
E.register_namespace('android',ns[1:-1])
if os.environ.get('VERSION_NAME'):tree.getroot().set(ns+'versionName',os.environ['VERSION_NAME'])
if os.environ.get('VERSION_CODE'):tree.getroot().set(ns+'versionCode',os.environ['VERSION_CODE'])
tree.write(sys.argv[1],encoding='utf-8')
PY
version=$(python3 -c 'import sys,xml.etree.ElementTree as E;print(E.parse(sys.argv[1]).getroot().get("{http://schemas.android.com/apk/res/android}versionName"))' "$stage/AndroidManifest.xml")
for name in tensorflow-lite tensorflow-lite-api; do
    unzip -p "android/deps/$name.aar" classes.jar > "$stage/$name.jar"
done
unzip -qo android/deps/tensorflow-lite.aar 'jni/*' -d "$stage"
mv "$stage/jni" "$stage/lib"
unzip -p android/deps/tensorflow-lite.aar LICENSE > "$stage/assets/TensorFlow-LICENSE.txt"
"$bt/aapt" package -f -M "$stage/AndroidManifest.xml" -I "$platform" -A "$stage/assets" -F "$stage/base.apk"
"$JAVA_HOME/bin/javac" --release 8 -classpath "$platform:$stage/tensorflow-lite.jar:$stage/tensorflow-lite-api.jar" -d "$stage/classes" android/src/cn/qingji/diary/*.java
(cd "$stage/classes" && "$JAVA_HOME/bin/jar" cf "$stage/app.jar" .)
"$bt/d8" --lib "$platform" --min-api 28 --output "$stage/dex" "$stage/app.jar" "$stage/tensorflow-lite.jar" "$stage/tensorflow-lite-api.jar"
(cd "$stage/dex" && zip -q "$stage/base.apk" classes.dex)
(cd "$stage" && zip -qr base.apk lib)
"$bt/zipalign" -f 4 "$stage/base.apk" "$stage/aligned.apk"
if [ "${SIGN_MODE:-signed}" = unsigned ]; then
    cp "$stage/aligned.apk" "$root/artifacts/qingji-$version-unsigned.apk"
else
    store=${SIGNING_KEYSTORE:-"$HOME/.android/debug.keystore"}
    export SIGNING_PASSWORD=${SIGNING_PASSWORD:-android}
    "$bt/apksigner" sign --ks "$store" --ks-key-alias "${SIGNING_ALIAS:-androiddebugkey}" --ks-pass env:SIGNING_PASSWORD --out "$root/artifacts/qingji-$version.apk" "$stage/aligned.apk"
    "$bt/apksigner" verify --verbose "$root/artifacts/qingji-$version.apk"
fi
