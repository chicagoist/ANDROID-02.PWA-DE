#!/usr/bin/env bash
# Build signed release APK for ANDROID-02.PWA-DE.
# Passwords are read from keystore-passwords.txt WITHOUT being printed.
# PKCS12 keystores require key password == store password, and
# build.gradle.kts now defaults keyPassword to KEYSTORE_PASSWORD,
# so we only pass the store password + alias here.
set -e
cd /c/Projects/ANDROID-02.PWA-DE/android

export JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-17.0.20.8-hotspot'
export KEYSTORE_PATH=release.jks
export KEY_ALIAS=upload

STORE_PW=$(sed -n 's/^KEYSTORE_PASSWORD=//p' ../keystore-passwords.txt | tr -d '\r' | head -1)
if [ -z "$STORE_PW" ]; then
  echo "ERROR: KEYSTORE_PASSWORD not found in keystore-passwords.txt" >&2
  exit 1
fi
export KEYSTORE_PASSWORD="$STORE_PW"
# Intentionally do NOT export KEY_PASSWORD: build.gradle.kts falls back to
# KEYSTORE_PASSWORD for PKCS12 (key password == store password).

echo '=== ENV CHECK (no secrets) ==='
[ -n "$KEYSTORE_PASSWORD" ] && echo 'KEYSTORE_PASSWORD=set' || echo 'KEYSTORE_PASSWORD=MISSING'
echo "KEY_ALIAS=$KEY_ALIAS"
echo 'KEY_PASSWORD=not exported (Gradle falls back to store password)'

echo '=== START assembleRelease ==='
./gradlew.bat assembleRelease --no-daemon 2>&1 | tail -20
echo "=== EXIT: ${PIPESTATUS[0]} ==="
