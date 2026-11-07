#!/usr/bin/env bash
# Runs inside the emulator step: installs the direct RELEASE APK (as shipped) and walks its main screens.
# STEPS_FILE picks the walk: steps-release.txt (default: launch, screens, Buy -> Stripe Checkout) or steps-restore.txt
# (Restore purchases by email after Tab's single-use 100% coupon test checkout).
set -u
export OUT=/tmp/dsk-out; mkdir -p $OUT
D=.github/direct
STEPS="${STEPS_FILE:-steps-release.txt}"; [ -f "$D/$STEPS" ] || STEPS=steps-release.txt
LABEL=release; [ "$STEPS" = steps-restore.txt ] && LABEL=restore
BT=$ANDROID_HOME/build-tools/$(ls $ANDROID_HOME/build-tools | sort -V | tail -1)
adb wait-for-device; adb shell getprop ro.build.version.release </dev/null | sed 's/^/android /' | tee $OUT/device.txt
adb shell pm list packages </dev/null | grep -iE "google|gms|vending" | tee -a $OUT/device.txt
A=$(ls dsk-apk/*-direct-release.apk | head -1)
PKG=$($BT/aapt2 dump packagename "$A" 2>/dev/null || cat $D/package.txt)
echo "steps: $STEPS" | tee -a $OUT/device.txt
bash $D/direct-test.sh "$A" "$PKG" "$D/$STEPS" $LABEL || true
ls -la $OUT | tail -60
cat $OUT/*-fatal.txt 2>/dev/null | head -40
