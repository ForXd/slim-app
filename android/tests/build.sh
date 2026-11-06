#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
stage=$(mktemp -d)
trap 'rm -rf "$stage"' EXIT
mkdir -p "$stage/classes" "$stage/test" "$stage/dex"
for name in tensorflow-lite tensorflow-lite-api; do unzip -p "android/deps/$name.aar" classes.jar > "$stage/$name.jar"; done
"$JAVA_HOME/bin/javac" --release 8 -classpath "$ANDROID_JAR:$stage/tensorflow-lite.jar:$stage/tensorflow-lite-api.jar" -d "$stage/classes" android/src/cn/qingji/diary/*.java
"$JAVA_HOME/bin/javac" --release 8 -classpath "$ANDROID_JAR:$stage/classes" -d "$stage/test" android/tests/*.java
(cd "$stage/test" && "$JAVA_HOME/bin/jar" cf "$stage/test.jar" .)
"$BUILD_TOOLS/d8" --lib "$ANDROID_JAR" --classpath "$stage/classes" --min-api 28 --output "$stage/dex" "$stage/test.jar"
"$BUILD_TOOLS/aapt" package -f -M android/tests/AndroidManifest.xml -I "$ANDROID_JAR" -A tests/photos -F "$stage/test.apk"
(cd "$stage/dex" && zip -q "$stage/test.apk" classes.dex)
"$BUILD_TOOLS/zipalign" -f 4 "$stage/test.apk" "$stage/aligned.apk"
"$BUILD_TOOLS/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-key-alias androiddebugkey --ks-pass pass:android --out /tmp/qingji-native-tests.apk "$stage/aligned.apk"
