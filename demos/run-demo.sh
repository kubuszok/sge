#!/bin/sh
# Build and run one demo in the browser, on Android, on the desktop JVM and as a Scala Native
# executable. sge and the sge-build plugin are published locally only when the checkout changed
# since the last publish (markers under target/demo-runner/). See --help for the options.
set -eu

usage() {
  cat <<'EOF'
Usage: ./demos/run-demo.sh <demo> [--browser] [--android] [--jvm] [--native] [--rebuild]

Builds <demo> (a directory under demos/, e.g. viewer-3d) and runs it. With no target flag it runs
all four targets.

  --browser   package for the browser, serve it on a free local port and open the default browser
  --android   build the APK, boot an emulator if no device is connected, install and launch it
  --jvm       build the desktop JVM release distribution for this machine and run it
  --native    link the Scala Native executable and run it
  --rebuild   republish sge and the sge-build plugin even if this commit is already published
  -h, --help  show this help

Environment:
  SGE_DEMO_AVD          the AVD to boot (default: the first listed AVD whose system image is installed)
  SGE_DEMO_OPEN         set to 0 to print the browser URL without opening a browser
  SGE_DEMO_RUN_SECONDS  run the JVM and Native builds for this many seconds instead of until closed
  ANDROID_HOME          the Android SDK (default: ~/Library/Android/sdk when it exists)

The build needs JDK 25 (JAVA_HOME or java on the PATH). Ctrl-C stops the web server.
EOF
}

say()  { printf '==> %s\n' "$*"; }
die()  { printf 'run-demo: %s\n' "$*" >&2; exit 1; }
fail() { printf 'run-demo: %s\n' "$*" >&2; failed=1; } # one target failed; the others still run

demo=""; want_browser=0; want_android=0; want_jvm=0; want_native=0; rebuild=0
for arg in "$@"; do
  case "$arg" in
    -h|--help) usage; exit 0 ;;
    --browser) want_browser=1 ;;
    --android) want_android=1 ;;
    --jvm)     want_jvm=1 ;;
    --native)  want_native=1 ;;
    --rebuild) rebuild=1 ;;
    -*) die "unknown option '$arg' (see --help)" ;;
    *) [ -z "$demo" ] || die "only one demo at a time (got '$demo' and '$arg')"; demo="$arg" ;;
  esac
done
[ -n "$demo" ] || { usage >&2; exit 1; }
if [ $want_browser = 0 ] && [ $want_android = 0 ] && [ $want_jvm = 0 ] && [ $want_native = 0 ]; then
  want_browser=1; want_android=1; want_jvm=1; want_native=1
fi

# This machine's platform classifier, as the JVM release names it.
case "$(uname -s)-$(uname -m)" in
  Darwin-arm64)               host_platform=macos-aarch64 ;;
  Darwin-x86_64)              host_platform=macos-x86_64 ;;
  Linux-aarch64|Linux-arm64)  host_platform=linux-aarch64 ;;
  Linux-x86_64)               host_platform=linux-x86_64 ;;
  *)                          host_platform="" ;;
esac
[ $want_jvm = 0 ] || [ -n "$host_platform" ] || die "no JVM release platform for $(uname -s) $(uname -m)"

root=$(cd "$(dirname "$0")/.." && pwd)
demos="$root/demos"
state="$root/target/demo-runner"
mkdir -p "$state"

[ -d "$demos/$demo" ] || die "no demo directory demos/$demo"
# The sbt project id of the demo, read from the demos build: `val <id> = demo("<dir>", ...` or
# `val <id> = (projectMatrix in file("<dir>"))`.
project=$(sed -n -E "s/^val ([A-Za-z0-9]+) *= *(demo\(\"$demo\"|\(projectMatrix in file\(\"$demo\"\)).*/\1/p" "$demos/build.sbt" | head -n 1)
[ -n "$project" ] || die "demos/build.sbt declares no project for demos/$demo"
# Its sbt `name`, which names its output directory under demos/target/out/<platform>/<scala>/.
sbt_name=$(sed -n -E "s/^val $project *= *demo\(\"$demo\", *\"([^\"]+)\".*/\1/p" "$demos/build.sbt")
[ -n "$sbt_name" ] || sbt_name=$(sed -n "/^val $project *=/,/^val /p" "$demos/build.sbt" | sed -n -E 's/.*name *:= *"([^"]+)".*/\1/p' | head -n 1)
[ -n "$sbt_name" ] || die "demos/build.sbt gives project $project no name"
if [ $want_android = 1 ] && [ ! -d "$demos/$demo/src/main/scala-android" ]; then
  say "$demo has no Android launcher (no src/main/scala-android); skipping Android"
  want_android=0
fi

# ── Toolchain ────────────────────────────────────────────────────────
command -v sbt >/dev/null 2>&1 || die "sbt is not on the PATH"
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
java_props=$("$java_bin" -XshowSettings:properties -version 2>&1) || die "no java found (set JAVA_HOME to a JDK 25)"
java_major=$(printf '%s\n' "$java_props" | sed -n -E 's/^ *java\.specification\.version = ([0-9]+).*/\1/p')
[ "$java_major" = "25" ] || die "the build needs JDK 25, found JDK ${java_major:-unknown} (set JAVA_HOME)"
if [ "$(uname -s)" = Darwin ] && [ "$(uname -m)" = arm64 ]; then
  printf '%s\n' "$java_props" | grep -q 'os.arch = aarch64' \
    || die "JAVA_HOME is an x86_64 JDK on an arm64 Mac; Scala Native cannot link with it (use an aarch64 JDK 25)"
fi
export SGE_REMOTE_CACHE="${SGE_REMOTE_CACHE:-0}"

# Always, not only for --android: sge's JVM artifact carries its Android activity only when the
# server that publishes it sees an SDK.
if [ -z "${ANDROID_HOME:-}" ]; then
  if [ -n "${ANDROID_SDK_ROOT:-}" ]; then ANDROID_HOME="$ANDROID_SDK_ROOT"
  elif [ -d "$HOME/Library/Android/sdk" ]; then ANDROID_HOME="$HOME/Library/Android/sdk"
  elif [ -d "$demos/sge-deps/android-sdk" ]; then ANDROID_HOME="$demos/sge-deps/android-sdk"
  fi
  [ -z "${ANDROID_HOME:-}" ] || export ANDROID_HOME
fi

# A device to run on, checked before anything is built; an emulator boots while the demo builds.
booting=0
if [ $want_android = 1 ]; then
  [ -n "${ANDROID_HOME:-}" ] || die "no Android SDK found (set ANDROID_HOME)"
  adb="$ANDROID_HOME/platform-tools/adb"
  [ -x "$adb" ] || die "adb is missing: $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager platform-tools"
  "$adb" start-server >/dev/null 2>&1 || true
  device=$("$adb" devices | sed -n '2,$p' | awk '$2 == "device" { print $1; exit }')
  if [ -n "$device" ]; then
    export ANDROID_SERIAL="${ANDROID_SERIAL:-$device}"
    say "using the connected Android device $ANDROID_SERIAL"
  else
    emulator="$ANDROID_HOME/emulator/emulator"
    [ -x "$emulator" ] || die "the Android emulator is not installed: $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager emulator"
    # The system image an AVD boots, relative to the SDK.
    avd_image() { sed -n -E 's/^image\.sysdir\.1 *= *(.*)/\1/p' "$HOME/.android/avd/$1.avd/config.ini" 2>/dev/null | sed 's:/*$::'; }
    avds=$("$emulator" -list-avds 2>/dev/null)
    [ -n "$avds" ] || die "no AVD exists: create one with $ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd"
    avd="${SGE_DEMO_AVD:-}"
    if [ -z "$avd" ]; then # the first AVD whose system image is installed
      for a in $avds; do
        if [ -d "$ANDROID_HOME/$(avd_image "$a")" ]; then avd="$a"; break; fi
      done
      [ -n "$avd" ] || avd=$(printf '%s\n' "$avds" | head -n 1)
    fi
    sysdir=$(avd_image "$avd")
    if [ -n "$sysdir" ] && [ ! -d "$ANDROID_HOME/$sysdir" ]; then
      die "the system image of AVD '$avd' is not installed: $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager '$(printf '%s' "$sysdir" | tr '/' ';')'"
    fi
    say "booting the Android emulator '$avd' (log: $state/emulator.log)"
    nohup "$emulator" -avd "$avd" -no-snapshot-save -no-boot-anim >"$state/emulator.log" 2>&1 &
    booting=1
  fi
fi

# ── What changed since the last publish ──────────────────────────────
# A fingerprint of the tracked tree under the given pathspecs: the commit plus any uncommitted
# change and untracked file.
fingerprint() {
  {
    git -C "$root" rev-parse HEAD
    git -C "$root" diff HEAD --ignore-submodules=all --binary -- "$@"
    git -C "$root" ls-files --others --exclude-standard -z -- "$@" | xargs -0 -I{} shasum "$root/{}" 2>/dev/null || true
  } | shasum | cut -d' ' -f1
}

sge_fp=$( { printf 'android-sdk=%s\n' "${ANDROID_HOME:-none}"; fingerprint . ':(exclude)demos' ':(exclude)sge-build'; } | shasum | cut -d' ' -f1)
version=$(cat "$demos/.sge-version" 2>/dev/null || true)

published() { # <artifact dir> <marker>
  [ -n "$version" ] && [ -d "$1/$version" ] && [ "$(cat "$2" 2>/dev/null)" = "$sge_fp" ]
}

m2="$HOME/.m2/repository/com/kubuszok"
platforms="jvm"
[ $want_browser = 0 ] || platforms="$platforms js"
[ $want_native = 0 ] || platforms="$platforms native"

# The version is fixed when the sbt server loads the build, and a load may change it, so when any
# platform needs publishing, every requested platform is published together under one version.
# The sge extensions the demo declares (`extensions = Set(sge.sbt.SgeExtension.Noise)`) are
# published too, through the build's publishLocal-<row>-3 alias (core and every extension).
extensions=$(sed -n "/^val $project *=/,/^val /p" "$demos/build.sbt" | sed -n -E 's/.*SgeExtension\.([A-Za-z0-9]+).*/\1/p' | tr 'A-Z' 'a-z' | sort -u)
ivy="$HOME/.ivy2/local/com.kubuszok"
stale=$rebuild
all_cmds=""
for p in $platforms; do
  case "$p" in
    jvm)    suffix=_3;           task=sge/publishM2 ;;
    js)     suffix=_sjs1_3;      task=sgeJS/publishM2 ;;
    native) suffix=_native0.5_3; task=sgeNative/publishM2 ;;
  esac
  published "$m2/sge$suffix" "$state/sge-$p.published" || stale=1
  for e in $extensions; do
    published "$ivy/sge-extension-$e$suffix" "$state/sge-$p.published" || stale=1
  done
  all_cmds="$all_cmds $task ;"
  [ -z "$extensions" ] || all_cmds="$all_cmds publishLocal-$p-3 ;"
done
publish_cmds=""
[ $stale = 0 ] || publish_cmds="$all_cmds"

republished=0
if [ -n "$publish_cmds" ]; then
  # A fresh server: the version is read when the build loads, and sge must be the first thing
  # the server compiles (ISS-910).
  if [ -f "$root/project/target/active.json" ]; then
    say "stopping the running sbt server in the repository root"
    (cd "$root" && sbt --client shutdown >/dev/null 2>&1) || true
  fi
  say "publishing sge locally ($platforms) — this takes a while the first time"
  (cd "$root" && sbt --client "$publish_cmds writeDemoVersion") || die "publishing sge failed (see the sbt output above)"
  for p in $platforms; do printf '%s\n' "$sge_fp" > "$state/sge-$p.published"; done
  version=$(cat "$demos/.sge-version")
  republished=1
else
  say "sge $version is already published for this checkout ($platforms)"
fi

plugin_fp=$( { printf '%s\n' "$version"; fingerprint sge-build; } | shasum | cut -d' ' -f1)
if [ $rebuild = 1 ] || [ ! -d "$HOME/.ivy2/local/com.kubuszok/sge-build_sbt2_3/$version" ] \
    || [ "$(cat "$state/sge-build.published" 2>/dev/null)" != "$plugin_fp" ]; then
  say "publishing the sge-build plugin $version locally"
  (cd "$root/sge-build" && sbt --client "reload ; publishLocal") || die "publishing the sge-build plugin failed"
  printf '%s\n' "$plugin_fp" > "$state/sge-build.published"
  republished=1
else
  say "the sge-build plugin $version is already published"
fi

# ── Build the demo: one sbt session in demos/ ────────────────────────
build_cmds=""
[ $republished = 0 ] || build_cmds="reload ;"
[ $want_browser = 0 ] || build_cmds="$build_cmds ${project}JS/sgePackageBrowser ;"
[ $want_native = 0 ]  || build_cmds="$build_cmds ${project}Native/sgePackageNative ;"
[ $want_jvm = 0 ]     || build_cmds="$build_cmds ${project}/releasePlatform $host_platform ;"
[ $want_android = 0 ] || build_cmds="$build_cmds ${project}Android/androidSign ;"
say "building $demo ($project): $build_cmds"
(cd "$demos" && sbt --client "${build_cmds% ;}") || die "building $demo failed (see the sbt output above)"

# The newest match among a project's build outputs (demos/target/out/<platform>/<scala>/<name>/).
newest() { # <project name> <find predicates...>
  name=$1; shift
  find "$demos/target/out" -path "*/$name/*" "$@" -print 2>/dev/null | while IFS= read -r f; do
    printf '%s %s\n' "$(stat -f '%m' "$f" 2>/dev/null || stat -c '%Y' "$f")" "$f"
  done | sort -rn | head -n 1 | cut -d' ' -f2-
}

server_pid=""
cleanup() {
  if [ -n "$server_pid" ]; then kill "$server_pid" 2>/dev/null || true; server_pid=""; fi
}
trap cleanup EXIT
trap 'exit 130' INT TERM

# ── Android ─────────────────────────────────────────────────────────
run_android() {
  apk=$(newest "$sbt_name-android" -name app-debug.apk -type f)
  [ -n "$apk" ] || die "the build produced no app-debug.apk under demos/target/out"
  aapt2=$(ls -d "$ANDROID_HOME"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -n 1)
  [ -n "$aapt2" ] || die "aapt2 is missing from $ANDROID_HOME/build-tools"
  badging=$("$aapt2" dump badging "$apk")
  package=$(printf '%s\n' "$badging" | sed -n -E "s/^package: name='([^']+)'.*/\1/p")
  activity=$(printf '%s\n' "$badging" | sed -n -E "s/^launchable-activity: name='([^']+)'.*/\1/p")
  [ -n "$package" ] && [ -n "$activity" ] || die "the APK declares no launchable activity ($apk)"

  if [ $booting = 1 ]; then
    say "waiting for the emulator to finish booting"
    "$adb" wait-for-device
    waited=0
    until [ "$("$adb" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do
      [ $waited -lt 300 ] || die "the emulator did not finish booting in 5 minutes (log: $state/emulator.log)"
      sleep 2; waited=$((waited + 2))
    done
  fi
  say "installing $apk"
  # The debug keystore is generated under target/, so a clean build signs with a new key and the
  # device refuses it as an update: uninstall the old copy and install again.
  if ! installed=$("$adb" install -r "$apk" 2>&1); then
    case "$installed" in
      *INSTALL_FAILED_UPDATE_INCOMPATIBLE*)
        say "the installed $package was signed with another key; reinstalling it"
        "$adb" uninstall "$package" >/dev/null 2>&1 || true
        installed=$("$adb" install "$apk" 2>&1) || die "adb install failed: $installed" ;;
      *) die "adb install failed: $installed" ;;
    esac
  fi
  "$adb" logcat -c 2>/dev/null || true
  say "launching $package/$activity"
  started=$("$adb" shell am start -W -n "$package/$activity" 2>&1) || die "adb could not start $package/$activity"
  case "$started" in *Error*) die "adb could not start $package/$activity: $started" ;; esac
  sleep 3
  if [ -z "$("$adb" shell pidof "$package" 2>/dev/null | tr -d '\r')" ]; then
    "$adb" logcat -d -b crash >&2 2>/dev/null || true
    printf 'run-demo: %s exited right after launch (crash log above)\n' "$package" >&2
    failed=1
    return 0
  fi
  say "$package is running on $("$adb" get-serialno 2>/dev/null)"
}

# ── Browser ─────────────────────────────────────────────────────────
run_browser() {
  command -v python3 >/dev/null 2>&1 || die "python3 is needed to serve the browser package"
  site=$(newest "$sbt_name" -type d -path '*/sge-browser/*' -prune)
  [ -n "$site" ] && [ -f "$site/index.html" ] || die "the build produced no browser package under demos/target/out"
  port=$(python3 -c 'import socket; s = socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1])')
  python3 -m http.server "$port" --bind 127.0.0.1 --directory "$site" >"$state/http.log" 2>&1 &
  server_pid=$!
  url="http://127.0.0.1:$port/"
  sleep 1
  kill -0 "$server_pid" 2>/dev/null || die "the web server did not start (log: $state/http.log)"
  say "serving $site at $url"
  if [ "${SGE_DEMO_OPEN:-1}" != 0 ]; then
    if command -v open >/dev/null 2>&1; then open "$url"; elif command -v xdg-open >/dev/null 2>&1; then xdg-open "$url"; fi
  fi
}

# Runs a desktop executable in the foreground until its window closes, or for
# $SGE_DEMO_RUN_SECONDS seconds when that is set (then it is stopped; exiting earlier is a failure).
run_app() { # <label> <executable>
  label=$1; exe=$2
  if [ -z "${SGE_DEMO_RUN_SECONDS:-}" ]; then
    say "running $label: $exe (close the window to continue)"
    "$exe" || { say "$label exited with status $?"; failed=1; }
    return 0
  fi
  say "running $label for $SGE_DEMO_RUN_SECONDS s: $exe"
  "$exe" &
  app_pid=$!
  ran=0
  while [ $ran -lt "$SGE_DEMO_RUN_SECONDS" ] && kill -0 $app_pid 2>/dev/null; do sleep 1; ran=$((ran + 1)); done
  if kill -0 $app_pid 2>/dev/null; then
    kill $app_pid 2>/dev/null; wait $app_pid 2>/dev/null || true
    say "$label ran $SGE_DEMO_RUN_SECONDS s without exiting"
  else
    wait $app_pid && status=0 || status=$?
    printf 'run-demo: %s exited after %s s with status %s\n' "$label" "$ran" "$status" >&2
    failed=1
  fi
}

# ── JVM (the release distribution for this host) ────────────────────
run_jvm() {
  # release-dist/ holds <App>-<platform>/ (an .app bundle on macOS) beside the cached runtime-<platform>/.
  if [ "$(uname -s)" = Darwin ]; then
    exe=$(newest "$sbt_name" -type f -perm -u+x -path "*/release-dist/*-$host_platform/*.app/Contents/MacOS/*" ! -path '*/Contents/MacOS/*/*')
  else
    exe=$(newest "$sbt_name" -type f -perm -u+x -path "*/release-dist/*-$host_platform/*" ! -path "*/release-dist/runtime-*" ! -path "*/release-dist/*-$host_platform/*/*")
  fi
  [ -n "$exe" ] || { fail "the build produced no JVM launcher for $host_platform under demos/target/out"; return 0; }
  run_app "the JVM build" "$exe"
}

# ── Native ──────────────────────────────────────────────────────────
run_native() {
  dir=$(newest "$sbt_name" -type d -path '*/sge-native/*' -prune)
  [ -n "$dir" ] || { fail "the build produced no native package under demos/target/out"; return 0; }
  exe=$(find "$dir" -maxdepth 1 -type f -perm -u+x | head -n 1)
  [ -n "$exe" ] || { fail "no executable in $dir"; return 0; }
  run_app "the native executable" "$exe"
}

# A demo that crashes on one target does not stop the others; the exit status still says so.
failed=0
[ $want_android = 0 ] || run_android
[ $want_browser = 0 ] || run_browser
[ $want_jvm = 0 ]     || run_jvm
[ $want_native = 0 ]  || run_native

if [ -n "$server_pid" ]; then
  say "press Ctrl-C to stop the web server"
  wait "$server_pid" || true
fi
exit $failed
