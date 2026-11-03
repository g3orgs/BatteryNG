#!/usr/bin/env bash
set -euo pipefail

die() {
    printf 'BatteryNG release build: %s\n' "$*" >&2
    exit 1
}

project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n "$sdk_root" && -d "$sdk_root" ]] || die 'Set ANDROID_HOME or ANDROID_SDK_ROOT to an installed Android SDK.'

build_tools="$sdk_root/build-tools/35.0.0"
for tool in zipalign apksigner aapt; do
    [[ -x "$build_tools/$tool" ]] || die "Android build-tools 35.0.0 ($tool) are required."
done

config_home="${XDG_CONFIG_HOME:-${HOME:?}/.config}"
[[ "$config_home" = /* ]] || die 'XDG_CONFIG_HOME must be an absolute path.'
config_dir="$config_home/BatteryNG"
keystore="$config_dir/release.p12"
password_file="$config_dir/release.pass"
key_alias="${BATTERYNG_KEY_ALIAS:-batteryng}"

[[ -d "$config_dir" && -f "$keystore" && -f "$password_file" ]] ||
    die "Release key or password file is missing from $config_dir."

for private_path in "$config_dir" "$keystore" "$password_file"; do
    mode="$(stat -Lc '%a' -- "$private_path")"
    (( (8#$mode & 077) == 0 )) || die "Owner-only permissions are required for $private_path."
done

cd -- "$project_root"
export ANDROID_HOME="$sdk_root"
bash gradlew :app:assembleRelease

unsigned_apk="$project_root/app/build/outputs/apk/release/app-release-unsigned.apk"
[[ -f "$unsigned_apk" ]] || die 'Gradle did not produce app-release-unsigned.apk.'

tmp_dir="$(mktemp -d)"
dist_dir="$project_root/dist"
trap 'rm -rf -- "$tmp_dir"; rm -f -- "$dist_dir/.BatteryNG.new.apk"' EXIT

"$build_tools/zipalign" -f -p 4 "$unsigned_apk" "$tmp_dir/aligned.apk"
"$build_tools/zipalign" -c -p 4 "$tmp_dir/aligned.apk" >/dev/null
"$build_tools/apksigner" sign \
    --ks "$keystore" \
    --ks-type PKCS12 \
    --ks-key-alias "$key_alias" \
    --ks-pass "file:$password_file" \
    --out "$tmp_dir/BatteryNG.apk" \
    "$tmp_dir/aligned.apk"

"$build_tools/apksigner" verify --verbose --print-certs "$tmp_dir/BatteryNG.apk" > "$tmp_dir/verification.txt"
if grep -Fq 'CN=Android Debug' "$tmp_dir/verification.txt"; then
    die 'The APK is still signed with a debug certificate.'
fi

badging="$("$build_tools/aapt" dump badging "$tmp_dir/BatteryNG.apk")"
[[ "$badging" == *"package: name='de.batteryng.mvp'"* ]] || die 'Unexpected application ID in release APK.'
[[ "$badging" != *application-debuggable* ]] || die 'The release APK is debuggable.'

mkdir -p -- "$dist_dir"
cp -- "$tmp_dir/BatteryNG.apk" "$dist_dir/.BatteryNG.new.apk"
mv -f -- "$dist_dir/.BatteryNG.new.apk" "$dist_dir/BatteryNG.apk"
sha256sum "$dist_dir/BatteryNG.apk"
