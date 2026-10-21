# No cmdline-tools. But can create AVD manually with config.ini in workspace and use -avd with ANDROID_AVD_HOME. Need create AVD directory + .ini. The emulator needs system image path and userdata images. It can create userdata if absent. Copy config.ini from Pixel and adjust. Use ANDROID_AVD_HOME=$PWD/.avdhome and create directory AirTouch_API_34.avd with config.ini plus AirTouch_API_34.ini containing path. Then emulator uses system image 4.2G read-only; writes userdata into workspace. Should work.

# Need maybe the "snapshot pending" is because $HOME/.android/avd/*.avd/snapshots stale, but new AVD no snapshot.

# Let's create. Use config from Pixel with necessary fields:


mkdir -p .avdhome/AirTouch_API_34.avd && cat > .avdhome/AirTouch_API_34.avd/config.ini <<'EOF'
AvdId=AirTouch_API_34
PlayStore.enabled=false
abi.type=arm64-v8a
avd.ini.displayname=AirTouch API 34
avd.ini.encoding=UTF-8
disk.dataPartition.size=4G
fastboot.forceColdBoot=yes
fastboot.forceFastBoot=no
hw.accelerometer=yes
hw.audioInput=yes
hw.audioOutput=yes
hw.battery=yes
hw.camera.back=virtualscene
hw.camera.front=emulated
hw.cpu.arch=arm64
hw.cpu.ncore=4
hw.dPad=no
hw.device.manufacturer=Google
hw.device.name=pixel_6
hw.gps=yes
hw.gpu.enabled=yes
hw.gpu.mode=swiftshader_indirect
hw.initialOrientation=Portrait
hw.keyboard=yes
hw.lcd.density=420
hw.lcd.height=2400
hw.lcd.width=1080
hw.mainKeys=no
hw.ramSize=3072
hw.sdCard=no
hw.sensors.proximity=yes
hw.trackBall=no
image.sysdir.1=system-images/android-34/google_apis/arm64-v8a/
tag.display=Google APIs
tag.id=google_apis
vm.heapSize=256
EOF
cat > .avdhome/AirTouch_API_34.ini <<EOF
avd.ini.encoding=UTF-8
path=$PWD/.avdhome/AirTouch_API_34.avd
target=android-34
EOF
ANDROID_AVD_HOME=$PWD/.avdhome ANDROID_USER_HOME=$PWD/.android-user $HOME/Library/Android/sdk/emulator/emulator -avd AirTouch_API_34 -no-snapshot -no-audio -gpu swiftshader_indirect -no-boot-anim -port 5556 2>&1 | tee /tmp/emulator4.log


sleep 5; tail -30 /tmp/emulator4.log; echo '---'; adb devices -l
#  Emulator booting. Great. Wait for device.

for i in $(seq 1 60); do state=$(adb -s emulator-5556 get-state 2>/dev/null); boot=$(adb -s emulator-5556 shell getprop sys.boot_completed 2>/dev/null | tr -d '\r'); echo "$i state=$state boot=$boot"; if [ "$boot" = "1" ]; then break; fi; sleep 3; done; adb -s emulator-5556 shell wm size; adb -s emulator-5556 shell wm density

# Install APK on emulator.

adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk 2>&1 | tail -3; adb -s emulator-5556 shell pm grant com.airtouch.app android.permission.BLUETOOTH_CONNECT 2>/dev/null; adb -s emulator-5556 shell pm grant com.airtouch.app android.permission.BLUETOOTH_SCAN 2>/dev/null; adb -s emulator-5556 shell pm grant com.airtouch.app android.permission.BLUETOOTH_ADVERTISE 2>/dev/null; adb -s emulator-5556 shell am start -n com.airtouch.app/.MainActivity 2>&1 | tail -3; sleep 5; adb -s emulator-5556 exec-out screencap -p > /tmp/airtouch-screen1.png; ls -lh /tmp/airtouch-screen1.png

# ScreenCap

adb -s emulator-5556 shell input tap 84 120; sleep 1; adb -s emulator-5556 exec-out screencap -p > /tmp/airtouch-drawer.png; echo done
