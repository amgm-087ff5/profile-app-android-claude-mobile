#!/usr/bin/env bash
# エミュレータ(または接続した実機)で GlassScreenshotTest を動かし、撮った画像を shots/ に取り出す。
# 先に ./gradlew assembleDebug assembleDebugAndroidTest を済ませておくこと。
set -euo pipefail

PKG=com.example.businesscard
RUNNER="$PKG.test/androidx.test.runner.AndroidJUnitRunner"
# Edgelit・Porcelain・Kinu の全部を撮る(am instrument の class はカンマ区切りで複数指定できる)
CLASS="$PKG.ui.GlassScreenshotTest,$PKG.ui.PorcelainScreenshotTest,$PKG.ui.KinuScreenshotTest"
LOG=instrument.log
: > "$LOG"

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

run_tests() {
  # am instrument はテストが失敗しても終了コード0なので、出力に「OK (n tests)」があるかで判定する
  local out
  out=$(adb shell am instrument -w -e class "$1" "$RUNNER" | tee -a "$LOG")
  if ! echo "$out" | grep -q "^OK ("; then
    echo "::group::logcat"; adb logcat -d -t 400 '*:E' | tee -a "$LOG"; echo "::endgroup::"
    return 1
  fi
}

# 縦画面で始める(表示画面のテストは、画面が自分で横向きに切り替える)
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
run_tests "$CLASS"

mkdir -p shots
adb exec-out run-as "$PKG" tar -cf - -C files screenshots | tar -xf - -C shots
ls -la shots/screenshots

# コマ撮りをつないで動きの動画にする
python3 -m venv /tmp/motion-venv
/tmp/motion-venv/bin/pip install --quiet pillow
/tmp/motion-venv/bin/python scripts/make-motion.py shots/screenshots/frames shots/screenshots/interactions
