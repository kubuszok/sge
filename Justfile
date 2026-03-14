# SGE Porting — Task Runner
# Run `just --list` to see all recipes

set shell := ["bash", "-euo", "pipefail", "-c"]

tsv := "docs/progress/migration-status.tsv"
sge_src := "sge/src/main/scala/sge"
gdx_src := "libgdx/gdx/src/com/badlogic/gdx"

# ── LibGDX reference ──────────────────────────────────────────────

# Find files in libgdx submodule matching a pattern
libgdx-find pattern:
    @find {{gdx_src}} -name '{{pattern}}'

# List Java files in a libgdx package (e.g. `just libgdx-list graphics/g3d`)
libgdx-list package:
    @ls {{gdx_src}}/{{package}}/*.java 2>/dev/null | sort

# Show libgdx + sge file side by side (path relative to gdx root, without extension)
libgdx-compare path:
    @echo "=== LibGDX: {{gdx_src}}/{{path}}.java ==="
    @cat "{{gdx_src}}/{{path}}.java" 2>/dev/null || echo "(not found)"
    @echo ""
    @echo "=== SGE: {{sge_src}}/{{path}}.scala ==="
    @cat "{{sge_src}}/{{path}}.scala" 2>/dev/null || echo "(not found)"

# ── Migration status ──────────────────────────────────────────────

# Show migration status summary, optionally filtered by package
sge-status package="":
    #!/usr/bin/env bash
    if [[ -z "{{package}}" ]]; then
        echo "=== Overall Migration Status ==="
        tail -n +3 {{tsv}} | awk -F'\t' '{counts[$3]++} END {for (s in counts) printf "  %-15s %d\n", s, counts[s]}' | sort
        echo ""
        echo "  Total:          $(tail -n +3 {{tsv}} | wc -l | tr -d ' ')"
    else
        echo "=== Status for {{package}} ==="
        grep "^com/badlogic/gdx/{{package}}/" {{tsv}} | awk -F'\t' '{counts[$3]++} END {for (s in counts) printf "  %-15s %d\n", s, counts[s]}' | sort
        echo ""
        echo "  Files:"
        grep "^com/badlogic/gdx/{{package}}/" {{tsv}} | awk -F'\t' '{printf "  %-60s %s\n", $1, $3}'
    fi

# Pick next N not_started files from a package (default 5)
sge-next-batch package size="5":
    @grep "^com/badlogic/gdx/{{package}}/" {{tsv}} | awk -F'\t' '$3 == "not_started" {print $1}' | head -n {{size}}

# ── Quality scans ─────────────────────────────────────────────────

# Run quality scans (return/null/null_cast/java_syntax/todo/all)
# Add "summary" after type for counts only: just sge-quality all summary
sge-quality type="all" mode="full":
    #!/usr/bin/env bash
    SRC="{{sge_src}}"

    run_return() {
        echo "=== return keyword (actual statements) ==="
        # Exclude @return doc tags, comments containing "return", and throw messages
        local matches
        matches=$(rg '\breturn\b' "$SRC" --type scala -n \
            | rg -v '@return|\*.*return|//.*return' \
            | rg -v 'throw.*return|".*return.*"' || true)
        if [[ -z "$matches" ]]; then
            echo "  (none)"
            echo ""
            echo "  Files: 0  Occurrences: 0"
        else
            if [[ "{{mode}}" == "full" ]]; then
                echo "$matches"
            fi
            local files occ
            files=$(echo "$matches" | awk -F: '{print $1}' | sort -u | wc -l | tr -d ' ')
            occ=$(echo "$matches" | wc -l | tr -d ' ')
            echo ""
            echo "  Files: $files  Occurrences: $occ"
            if [[ "{{mode}}" == "full" ]]; then
                echo ""
                echo "  Top files:"
                echo "$matches" | awk -F: '{print $1}' | sort | uniq -c | sort -rn | head -10 | sed 's/^/    /'
            fi
        fi
    }

    run_null() {
        echo "=== null checks (== null / != null) ==="
        # Exclude comments, string literals, and Nullable internals
        local matches
        matches=$(rg '== null|!= null' "$SRC" --type scala -n \
            | rg -v '//.*null|/\*.*null|\*.*null|".*null.*"' || true)
        if [[ -z "$matches" ]]; then
            echo "  (none)"
            echo ""
            echo "  Files: 0  Occurrences: 0"
        else
            if [[ "{{mode}}" == "full" ]]; then
                echo "$matches"
            fi
            local files occ
            files=$(echo "$matches" | awk -F: '{print $1}' | sort -u | wc -l | tr -d ' ')
            occ=$(echo "$matches" | wc -l | tr -d ' ')
            echo ""
            echo "  Files: $files  Occurrences: $occ"
            if [[ "{{mode}}" == "full" ]]; then
                echo ""
                echo "  Top files:"
                echo "$matches" | awk -F: '{print $1}' | sort | uniq -c | sort -rn | head -10 | sed 's/^/    /'
            fi
        fi
    }

    run_null_cast() {
        echo "=== null.asInstanceOf ==="
        local matches
        matches=$(rg 'null\.asInstanceOf' "$SRC" --type scala -n || true)
        if [[ -z "$matches" ]]; then
            echo "  (none)"
            echo ""
            echo "  Files: 0  Occurrences: 0"
        else
            if [[ "{{mode}}" == "full" ]]; then
                echo "$matches"
            fi
            local files occ
            files=$(echo "$matches" | awk -F: '{print $1}' | sort -u | wc -l | tr -d ' ')
            occ=$(echo "$matches" | wc -l | tr -d ' ')
            echo ""
            echo "  Files: $files  Occurrences: $occ"
        fi
    }

    run_java_syntax() {
        echo "=== remaining Java syntax ==="
        local matches
        matches=$(rg '\b(public|static|void|boolean|implements)\b' \
            "$SRC" --type scala -n \
            | rg -v '//|/\*|\*|".*"' || true)
        if [[ -z "$matches" ]]; then
            echo "  (none)"
            echo ""
            echo "  Files: 0  Occurrences: 0"
        else
            if [[ "{{mode}}" == "full" ]]; then
                echo "$matches"
            fi
            local files occ
            files=$(echo "$matches" | awk -F: '{print $1}' | sort -u | wc -l | tr -d ' ')
            occ=$(echo "$matches" | wc -l | tr -d ' ')
            echo ""
            echo "  Files: $files  Occurrences: $occ"
            if [[ "{{mode}}" == "full" ]]; then
                echo ""
                echo "  Top files:"
                echo "$matches" | awk -F: '{print $1}' | sort | uniq -c | sort -rn | head -10 | sed 's/^/    /'
            fi
        fi
    }

    run_todo() {
        echo "=== TODO/FIXME/HACK/XXX markers ==="
        local matches
        matches=$(rg '\b(TODO|FIXME|HACK|XXX)\b' "$SRC" --type scala -n || true)
        if [[ -z "$matches" ]]; then
            echo "  (none)"
            echo ""
            echo "  Files: 0  Occurrences: 0"
        else
            if [[ "{{mode}}" == "full" ]]; then
                echo "$matches"
            fi
            local files occ
            files=$(echo "$matches" | awk -F: '{print $1}' | sort -u | wc -l | tr -d ' ')
            occ=$(echo "$matches" | wc -l | tr -d ' ')
            echo ""
            echo "  Files: $files  Occurrences: $occ"
            if [[ "{{mode}}" == "full" ]]; then
                echo ""
                echo "  Top files:"
                echo "$matches" | awk -F: '{print $1}' | sort | uniq -c | sort -rn | head -10 | sed 's/^/    /'
            fi
        fi
    }

    case "{{type}}" in
        return)      run_return ;;
        null)        run_null ;;
        null_cast)   run_null_cast ;;
        java_syntax) run_java_syntax ;;
        todo)        run_todo ;;
        all)         run_return; echo ""; run_null; echo ""; run_null_cast; echo ""; run_java_syntax; echo ""; run_todo ;;
        *)           echo "Unknown type: {{type}}. Use return/null/null_cast/java_syntax/todo/all" ;;
    esac

# ── Text transforms ──────────────────────────────────────────────

# Apply a Perl one-liner to file(s) in-place (line-by-line mode)
# Simple:   just transform 's/oldName/newName/g' src/Foo.scala
# Stateful: just transform '$d=1 if /@deprecated/; s/: Unit$/: Unit = ???/ && ($d=0) if $d' src/Foo.scala
# Multi-file: just transform 's/len2/lengthSq/g' src/A.scala src/B.scala
transform expr +files:
    perl -i -pe '{{expr}}' {{files}}

# Apply a Perl one-liner in whole-file mode (for multiline patterns)
# Example: just transform-multi 's/def foo\(.*?\)\K: Unit/: Unit = ???/msg' src/Foo.scala
transform-multi expr +files:
    perl -i -0777 -pe '{{expr}}' {{files}}

# Add `= ???` to abstract methods following a @deprecated annotation
stub-deprecated +files:
    perl -i -pe '$d=1 if /\@deprecated/; if ($d && /: Unit\s*$/ && !/=/) { s/: Unit\s*$/: Unit = ???/; $d=0 }' {{files}}

# ── Build ─────────────────────────────────────────────────────────

# Compile the project (JVM)
compile:
    sbt --client 'sge/compile'

# Compile Scala.js target
compile-js:
    sbt --client 'sgeJS/compile'

# Compile Scala Native target
compile-native:
    sbt --client 'sgeNative/compile'

# Compile and show last N lines (default 30)
compile-tail n="30":
    #!/usr/bin/env bash
    sbt --client 'sge/compile' 2>&1 | tail -n {{n}}

# Compile and show only errors (no info/warn noise)
compile-errors:
    #!/usr/bin/env bash
    output=$(sbt --client 'sge/compile' 2>&1)
    echo "$output" | grep '^\[error\]' || echo "No errors"
    echo ""
    echo "$output" | tail -3

# Compile and show warnings + errors (skip info)
compile-warnings:
    #!/usr/bin/env bash
    output=$(sbt --client 'sge/compile' 2>&1)
    echo "$output" | grep -E '^\[(warn|error)\]' || echo "Clean"
    echo ""
    echo "$output" | tail -3

# Format code
fmt:
    sbt --client scalafmtAll

# Compile, format, compile again
compile-fmt: compile fmt compile

# Publish sge to local Maven (~/.ivy2/local) for the demos sub-build.
# Publishes JVM (with bundled native libs), JS, and Native.
publish-local:
    sbt --client 'sge/publishLocal'
    sbt --client 'sgeJS/publishLocal'
    sbt --client 'sgeNative/publishLocal'

# Publish only the JVM artifact (fastest — enough for `demos/pong/run`)
publish-local-jvm:
    sbt --client 'sge/publishLocal'

# Run JVM tests
test:
    sbt --client 'sge/test'

# Run JVM tests (explicit)
test-jvm:
    sbt --client 'sge/test'

# Run Scala.js tests
test-js:
    sbt --client 'sgeJS/test'

# Run Scala Native tests (requires static-only Rust lib)
test-native: rust-build-static
    sbt --client 'sgeNative/test'

# Run browser integration tests (headless Chromium via Playwright)
test-browser:
    sbt --client 'sge-it-browser/test'

# Run Android integration tests (headless AVD emulator via adb)
# Prerequisites: just android-sdk-setup && just android-emulator-start
test-android:
    sbt --client 'sge-it-android/test'

# ── Cross-platform integration tests ────────────────────────────

# Run all integration tests (desktop + browser + android)
it-all: it-desktop it-browser it-android it-native-ffi

# Quick integration tests (desktop only — no emulator/browser needed)
it-quick: it-desktop

# Desktop integration tests (GLFW + ANGLE + miniaudio, real GL context)
it-desktop: rust-build angle-setup
    sbt --client 'sge-it-desktop/test'

# Browser integration tests (headless Chromium via Playwright)
it-browser:
    sbt --client 'demoJS/fastLinkJS' && sbt --client 'sge-it-browser/test'

# Native FFI wiring validation (Scala Native — exercises every native endpoint)
it-native-ffi: rust-build-static angle-setup
    sbt --client 'sge-it-native-ffi/run'

# Android integration tests (headless AVD emulator)
# Prerequisites: just android-sdk-setup && just android-emulator-start
it-android:
    sbt --client 'sge-android-smoke/androidSign' && sbt --client 'sge-it-android/test'

# ── Android SDK & emulator setup ─────────────────────────────────

# Download Android SDK, system image, and emulator (one-time setup)
android-sdk-setup:
    sbt --client 'sge-android-smoke/androidSdkRoot'
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
    echo "Installing emulator + system image..."
    echo "y" | "$SDKMANAGER" --sdk_root="$SDK" \
        "emulator" \
        "platform-tools" \
        "system-images;android-35;google_apis;arm64-v8a"
    echo "Creating AVD sge-test-avd..."
    echo "no" | "$SDK/cmdline-tools/latest/bin/avdmanager" create avd \
        --name sge-test-avd \
        --package "system-images;android-35;google_apis;arm64-v8a" \
        --force
    echo "Android SDK setup complete."

# Install an Android SDK package (e.g. just android-sdk-install "system-images;android-36;google_apis;arm64-v8a")
android-sdk-install package:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    echo "y" | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "{{package}}"

# Create an Android AVD (e.g. just android-avd-create sge-test-36 "system-images;android-36;google_apis;arm64-v8a")
android-avd-create name image:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    echo "no" | "$SDK/cmdline-tools/latest/bin/avdmanager" create avd \
        --name "{{name}}" \
        --package "{{image}}" \
        --force
    echo "AVD {{name}} created."

# Start headless Android emulator in background (default: sge-test-avd)
android-emulator-start avd="sge-test-avd":
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    ADB="$SDK/platform-tools/adb"
    # Check if already running
    if "$ADB" devices 2>/dev/null | grep -q "emulator-"; then
        echo "Emulator already running."
        exit 0
    fi
    echo "Starting headless emulator (AVD: {{avd}})..."
    "$SDK/emulator/emulator" -avd "{{avd}}" \
        -no-window -gpu swiftshader_indirect \
        -no-snapshot -noaudio -no-boot-anim &
    echo "Waiting for device..."
    "$ADB" wait-for-device
    while [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
        sleep 2
    done
    echo "Emulator booted."

# Stop the Android emulator
android-emulator-stop:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    "$SDK/platform-tools/adb" emu kill 2>/dev/null || true
    echo "Emulator stopped."

# Build the Android smoke test APK
android-build-smoke:
    sbt --client 'sge-android-smoke/androidSign'

# Install an APK on the connected Android device/emulator
android-install apk:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    ADB="$SDK/platform-tools/adb"
    "$ADB" install -r "{{apk}}"

# Uninstall an Android package
android-uninstall package:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    "$SDK/platform-tools/adb" uninstall "{{package}}" 2>/dev/null || echo "Package not installed."

# Launch an Android activity (e.g. just android-launch sge.demos.pong/.AndroidMain)
android-launch activity:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    "$SDK/platform-tools/adb" shell am start -n "{{activity}}"

# Show recent logcat output (default last 200 lines)
android-logcat lines="200":
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    "$SDK/platform-tools/adb" logcat -d -t {{lines}}

# Show logcat filtered for crashes/errors only
android-logcat-errors:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    "$SDK/platform-tools/adb" logcat -d | grep -E "FATAL|AndroidRuntime|Exception|Error" | grep -v "InputManager\|FileUtils\|TapAndPay\|gclu" || echo "No crashes found."

# Clear logcat buffer
android-logcat-clear:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    "$SDK/platform-tools/adb" logcat -c
    echo "Logcat cleared."

# Run an adb shell command (e.g. just adb-shell "settings put global hidden_api_policy 1")
adb-shell cmd:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    "$SDK/platform-tools/adb" shell {{cmd}}

# Run a raw adb command (e.g. just adb devices)
adb +args:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    "$SDK/platform-tools/adb" {{args}}

# Check if an Android process is running (e.g. just android-pidof sge.demos.pong)
android-pidof package:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    PID=$("$SDK/platform-tools/adb" shell pidof "{{package}}" 2>/dev/null || true)
    if [ -n "$PID" ]; then
        echo "{{package}} is running (PID: $PID)"
    else
        echo "{{package}} is NOT running"
    fi

# Full test cycle: uninstall → install → clear logcat → launch → wait → show errors
android-test apk activity package:
    #!/usr/bin/env bash
    set -euo pipefail
    SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-android-sdk}}"
    ADB="$SDK/platform-tools/adb"
    echo "=== Uninstalling {{package}} ==="
    "$ADB" uninstall "{{package}}" 2>/dev/null || true
    echo "=== Installing {{apk}} ==="
    "$ADB" install -r "{{apk}}"
    echo "=== Clearing logcat ==="
    "$ADB" logcat -c
    echo "=== Launching {{activity}} ==="
    "$ADB" shell am start -n "{{activity}}"
    echo "=== Waiting 5s for startup ==="
    sleep 5
    echo "=== Process status ==="
    PID=$("$ADB" shell pidof "{{package}}" 2>/dev/null || true)
    if [ -n "$PID" ]; then
        echo "✓ {{package}} is running (PID: $PID)"
    else
        echo "✗ {{package}} crashed!"
        echo "=== Crash log ==="
        "$ADB" logcat -d | grep -E "FATAL|AndroidRuntime" -A 30 || true
    fi

# Shortcut: build + test Pong on Android emulator
android-test-pong:
    #!/usr/bin/env bash
    set -euo pipefail
    cd demos && sbt --client 'pong/clean; androidPong'
    cd ..
    just android-test \
        demos/pong/target/jvm-3/android/app-debug.apk \
        sge.demos.pong/.AndroidMain \
        sge.demos.pong

# Build Rust + run all platform tests (JVM + JS + Native)
test-all: rust-build test-jvm test-js test-native

# Run all tests and show last N lines (default 20)
test-tail n="20":
    #!/usr/bin/env bash
    sbt --client 'sge/test' 2>&1 | tail -n {{n}}

# Run a specific test suite
test-only suite:
    sbt --client "sge/testOnly {{suite}}"

# Kill all sbt/Java processes started from this project directory
kill-sbt:
    #!/usr/bin/env bash
    set +e
    # Kill sbt server and any forked test JVMs for this project
    pids=$(ps aux | grep '[s]bt' | grep 'sge-porting' | awk '{print $2}')
    if [ -n "$pids" ]; then
        echo "Killing sbt processes: $pids"
        echo "$pids" | xargs kill -9
    fi
    # Also kill any sbt.ForkMain (forked test runner) for this project
    fork_pids=$(ps aux | grep '[s]bt.ForkMain' | awk '{print $2}')
    if [ -n "$fork_pids" ]; then
        echo "Killing forked test processes: $fork_pids"
        echo "$fork_pids" | xargs kill -9
    fi
    # Clean up sbt server socket
    rm -f /Users/dev/.sbt/1.0/server/bd760fdeec54161195a7/sock 2>/dev/null
    echo "Done. sbt processes cleaned up."

# ── Demo ─────────────────────────────────────────────────────────

# Compile the demo module (JVM)
demo-compile:
    sbt --client 'demo/compile'

# Compile the demo module (JS)
demo-compile-js:
    sbt --client 'demoJS/compile'

# Compile the demo module (Native)
demo-compile-native:
    sbt --client 'demoNative/compile'

# Run the demo application (JVM — requires GLFW + ANGLE + miniaudio)
demo-jvm:
    sbt --client 'demo/run'

# Link the demo application (JS — produces .js bundle)
demo-link-js:
    sbt --client 'demoJS/fastLinkJS'

# Run the demo application (Native — requires static Rust lib + GLFW + ANGLE + miniaudio)
demo-native: rust-build-static
    sbt --client 'demoNative/run'

# ── Git — read-only ──────────────────────────────────────────────

# Show git status
git-status:
    git status

# Show diff stats (files changed + insertions/deletions)
diff-stat:
    git diff --stat

# Show full diff (optionally for a specific file)
diff file="":
    git diff {{file}}

# Show staged diff (optionally for a specific file)
diff-staged file="":
    git diff --cached {{file}}

# List only names of changed files (unstaged)
diff-files:
    git diff --name-only

# List only names of staged files
diff-files-staged:
    git diff --cached --name-only

# Count changed files
diff-count:
    #!/usr/bin/env bash
    echo "Staged:    $(git diff --cached --name-only | wc -l | tr -d ' ')"
    echo "Unstaged:  $(git diff --name-only | wc -l | tr -d ' ')"
    echo "Untracked: $(git ls-files --others --exclude-standard | wc -l | tr -d ' ')"

# Show recent commits (default 10)
git-log n="10":
    git log --oneline -{{n}}

# Show detailed log with stats (default 5)
git-log-full n="5":
    git log --stat -{{n}}

# Show a specific commit (default HEAD)
git-show ref="HEAD":
    git show {{ref}}

# Show commit stats only (no diff body)
git-show-stat ref="HEAD":
    git show --stat {{ref}}

# Show current branch name
git-branch:
    git branch --show-current

# List local branches
git-branches:
    git branch -v

# List all branches (including remote)
git-branches-all:
    git branch -av

# List tags
git-tags:
    git tag -l

# Show remote URLs
git-remotes:
    git remote -v

# Blame a file (show who changed each line)
git-blame file:
    git blame {{file}}

# List tracked files (optionally matching a glob pattern)
git-ls pattern="":
    #!/usr/bin/env bash
    if [[ -z "{{pattern}}" ]]; then
        git ls-files
    else
        git ls-files '{{pattern}}'
    fi

# Show stash list
git-stash-list:
    git stash list

# Show ref SHA (default HEAD)
git-rev ref="HEAD":
    git rev-parse {{ref}}

# ── Git — write (staging + commit only) ─────────────────────────

# Stage specific files
stage +files:
    git add {{files}}

# Stage all changes
stage-all:
    git add -A

# Commit staged changes with a message
commit message:
    git commit -m "{{message}}"

# Stage all changes and commit
commit-all message:
    git add -A && git commit -m "{{message}}"

# Push current branch to origin
push:
    git push origin "$(git branch --show-current)"

# ── GitHub CLI — read-only ───────────────────────────────────────

# List open PRs
gh-pr-list:
    gh pr list

# View a PR (by number, or current branch)
gh-pr-view pr="":
    #!/usr/bin/env bash
    if [[ -z "{{pr}}" ]]; then
        gh pr view
    else
        gh pr view {{pr}}
    fi

# Show PR diff
gh-pr-diff pr="":
    #!/usr/bin/env bash
    if [[ -z "{{pr}}" ]]; then
        gh pr diff
    else
        gh pr diff {{pr}}
    fi

# Show PR CI checks
gh-pr-checks pr="":
    #!/usr/bin/env bash
    if [[ -z "{{pr}}" ]]; then
        gh pr checks
    else
        gh pr checks {{pr}}
    fi

# List open issues
gh-issue-list:
    gh issue list

# View an issue by number
gh-issue-view issue:
    gh issue view {{issue}}

# List releases
gh-release-list:
    gh release list

# View a specific release
gh-release-view tag:
    gh release view {{tag}}

# List recent workflow runs
gh-run-list:
    gh run list

# View a workflow run
gh-run-view id:
    gh run view {{id}}

# View repo info
gh-repo:
    gh repo view

# List PR review comments (inline code review comments)
gh-pr-comments pr="2":
    gh api repos/{owner}/{repo}/pulls/{{pr}}/comments --jq '.[] | "---\n\(.path):\(.line // .original_line) (@\(.user.login)):\n\(.body)\n"'

# List PR review comments — summary only (file:line + description)
gh-pr-comments-summary pr="2":
    gh api repos/{owner}/{repo}/pulls/{{pr}}/comments --jq '.[] | "\(.path):\(.line // .original_line) | \(.body | split("### ")[1:] | .[0] | split("\n")[0])"'

# List PR review comments for a specific file
gh-pr-comments-file pr="2" file="":
    gh api repos/{owner}/{repo}/pulls/{{pr}}/comments --jq '[.[] | select(.path == "{{file}}")] | .[] | "---\nL\(.line // .original_line) (@\(.user.login)):\n\(.body)\n"'

# List PR issue comments (top-level conversation comments)
gh-pr-issue-comments pr="2":
    gh api repos/{owner}/{repo}/issues/{{pr}}/comments --jq '.[] | "---\n@\(.user.login):\n\(.body)\n"'

# List PR reviews (approve/request changes/comment)
gh-pr-reviews pr="2":
    gh api repos/{owner}/{repo}/pulls/{{pr}}/reviews --jq '.[] | "---\n@\(.user.login) [\(.state)]:\n\(.body)\n"'

# Show PR checks with conclusions
gh-pr-check-runs pr="2":
    gh api "repos/{owner}/{repo}/commits/$(gh pr view {{pr}} --json headRefOid --jq .headRefOid)/check-runs" --jq '.check_runs[] | "\(.name): \(.conclusion // .status)"'

# Show failed CI run logs
gh-run-log id:
    gh run view {{id}} --log-failed

# Call GitHub API (GET requests)
gh-api endpoint:
    gh api {{endpoint}}

# ── Scalafix ──────────────────────────────────────────────────────

# Run a scalafix rule on the whole project (e.g. `just scalafix NullToNullable`)
scalafix rule:
    sbt --client 'sge / scalafix {{rule}}'

# Run a scalafix rule on a specific file
scalafix-file rule file:
    sbt --client 'sge / scalafix {{rule}} --files={{file}}'

# Check for null patterns (lint only, no changes)
lint-null:
    sbt --client 'sge / scalafix NullToNullable'

# Check for banned syntax (return, null literals, etc.)
lint-syntax:
    sbt --client 'sge / scalafix DisableSyntax'

# ── Search helpers ───────────────────────────────────────────────

# Search SGE source for a pattern (wraps rg). Add -c for counts, -l for files only.
sge-grep pattern *flags:
    rg '{{pattern}}' {{sge_src}} --type scala {{flags}}

# Count occurrences of a pattern in SGE source (files + total)
sge-count pattern:
    #!/usr/bin/env bash
    rg -c '{{pattern}}' {{sge_src}} --type scala 2>/dev/null \
        | awk -F: '{s+=$2; n++; print} END{print ""; print "  Files: " n "  Occurrences: " s}'

# ── Extension Modules ────────────────────────────────────────────

# Compile sge-tools (JVM-only)
compile-tools:
    sbt --client 'sge-tools/compile'

# Compile sge-freetype (JVM)
compile-freetype:
    sbt --client 'sge-freetype/compile'

# Compile sge-physics (JVM)
compile-physics:
    sbt --client 'sge-physics/compile'

# Compile all extension modules
compile-extensions: compile-tools compile-freetype compile-physics

# Run sge-tools tests
test-tools:
    sbt --client 'sge-tools/test'

# Run sge-freetype tests
test-freetype:
    sbt --client 'sge-freetype/test'

# Run sge-physics tests
test-physics:
    sbt --client 'sge-physics/test'

# Run TexturePacker CLI
texture-pack *args:
    sbt --client 'sge-tools/run {{args}}'

# Build Rust with FreeType support
rust-build-freetype:
    cd native-components && cargo build --release --features freetype_support

# Build Rust with physics support
rust-build-physics:
    cd native-components && cargo build --release --features physics

# Build Rust with all extensions
rust-build-all:
    cd native-components && cargo build --release --features all

# ── Native Components (Rust library) ─────────────────────────────

# Build the Rust native library (release mode, C ABI for Panama + Scala Native)
# Also builds vendored GLFW and miniaudio as separate shared libraries.
rust-build:
    cd native-components && cargo build --release

# Copy pre-built ANGLE libraries (libEGL, libGLESv2) into native-components/target/release/
# so they're found alongside other native libs via java.library.path.
# On macOS: uses Homebrew-installed ANGLE (brew install startergo/angle/angle).
# On Linux/Windows: download from LibGDX Maven artifacts (TODO).
angle-setup:
    #!/usr/bin/env bash
    set -euo pipefail
    dest="native-components/target/release"
    mkdir -p "$dest"
    # Check if ANGLE is already in place
    if [[ -f "$dest/libEGL.dylib" && -f "$dest/libGLESv2.dylib" ]]; then
        echo "ANGLE libraries already present in $dest"
        exit 0
    fi
    # macOS: copy from Homebrew Cellar
    if [[ "$(uname)" == "Darwin" ]]; then
        angle_lib=""
        for cellar in /opt/homebrew/Cellar/angle /usr/local/Cellar/angle; do
            if [[ -d "$cellar" ]]; then
                # Find latest version
                angle_lib="$(ls -d "$cellar"/*/lib 2>/dev/null | sort -V | tail -1)"
                break
            fi
        done
        if [[ -z "$angle_lib" ]]; then
            echo "ERROR: ANGLE not found in Homebrew. Install with: brew install startergo/angle/angle"
            exit 1
        fi
        echo "Copying ANGLE from $angle_lib to $dest"
        cp "$angle_lib/libEGL.dylib" "$dest/"
        cp "$angle_lib/libGLESv2.dylib" "$dest/"
        # Fix install names so the dynamic linker finds them in the same directory
        # (Homebrew copies have install names pointing to /opt/homebrew/opt/angle/lib/)
        install_name_tool -id @rpath/libEGL.dylib "$dest/libEGL.dylib"
        install_name_tool -id @rpath/libGLESv2.dylib "$dest/libGLESv2.dylib"
        # libGLESv2 depends on libEGL — fix the reference to use @rpath
        install_name_tool -change /opt/homebrew/opt/angle/lib/libEGL.dylib @rpath/libEGL.dylib "$dest/libGLESv2.dylib" 2>/dev/null || true
        install_name_tool -change /usr/local/opt/angle/lib/libEGL.dylib @rpath/libEGL.dylib "$dest/libGLESv2.dylib" 2>/dev/null || true
        # Re-sign after install_name_tool (macOS SIP kills unsigned modified binaries)
        codesign --force --sign - "$dest/libEGL.dylib"
        codesign --force --sign - "$dest/libGLESv2.dylib"
        echo "ANGLE libraries installed to $dest"
    else
        echo "ERROR: Automatic ANGLE setup not yet implemented for $(uname). Place libEGL and libGLESv2 in $dest manually."
        exit 1
    fi

# Build the Rust native library with Android JNI bridge
rust-build-android:
    cd native-components && cargo build --release --features android

# Build Rust and remove .dylib so Scala Native links statically
rust-build-static:
    cd native-components && cargo build --release
    rm -f native-components/target/release/libsge_native_ops.dylib native-components/target/release/libsge_native_ops.so

# Run Rust unit tests
rust-test:
    cd native-components && cargo test

# ── Rust Cross-Compilation ───────────────────────────────────────

# All 6 desktop targets: (Linux, macOS, Windows) × (x86_64, aarch64)
rust_targets := "x86_64-apple-darwin aarch64-apple-darwin x86_64-unknown-linux-gnu aarch64-unknown-linux-gnu x86_64-pc-windows-msvc aarch64-pc-windows-msvc"
rust_out := "native-components/target/cross"

# Build Rust for a specific target triple
rust-cross target:
    #!/usr/bin/env bash
    cd native-components
    case "{{target}}" in
        *-apple-darwin)
            # macOS targets: native cross-compile (both archs available on macOS)
            rustup target add "{{target}}" 2>/dev/null || true
            cargo build --release --target "{{target}}"
            ;;
        *-linux-gnu)
            # Linux targets: use cargo-zigbuild (no Docker needed)
            rustup target add "{{target}}" 2>/dev/null || true
            cargo zigbuild --release --target "{{target}}"
            ;;
        *-windows-msvc)
            # Windows targets: use cargo-xwin (downloads MSVC CRT automatically)
            rustup target add "{{target}}" 2>/dev/null || true
            cargo xwin build --release --target "{{target}}"
            ;;
        *)
            echo "Unknown target: {{target}}"
            exit 1
            ;;
    esac

# Build Rust for Android NDK targets (JNI bridge)
# Uses rustup toolchain (not Homebrew cargo) for cross-compilation targets.
# Sets CC/CXX/AR for the cc crate to find NDK clang.
rust-cross-android target:
    #!/usr/bin/env bash
    set -euo pipefail
    # Prefer rustup cargo/rustup over Homebrew
    RUSTUP="${HOME}/.rustup/toolchains/stable-$(rustc -vV 2>/dev/null | sed -n 's/host: //p' || echo aarch64-apple-darwin)/bin"
    if [ -d "$RUSTUP" ]; then
        export PATH="$RUSTUP:$PATH"
    fi
    ANDROID_NDK="$(pwd)/demos/android-sdk/ndk/27.2.12479018"
    export ANDROID_NDK_HOME="$ANDROID_NDK"
    NDK_BIN="$ANDROID_NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin"
    # Map Rust target triple to NDK clang prefix (API 26 = Android 8.0)
    case "{{target}}" in
        aarch64-linux-android)   NDK_PREFIX="aarch64-linux-android26" ;;
        armv7-linux-androideabi) NDK_PREFIX="armv7a-linux-androideabi26" ;;
        x86_64-linux-android)   NDK_PREFIX="x86_64-linux-android26" ;;
        *) echo "Unknown Android target: {{target}}"; exit 1 ;;
    esac
    TARGET_UNDER="$(echo "{{target}}" | tr '-' '_')"
    export "CC_${TARGET_UNDER}=$NDK_BIN/${NDK_PREFIX}-clang"
    export "CXX_${TARGET_UNDER}=$NDK_BIN/${NDK_PREFIX}-clang++"
    export "AR_${TARGET_UNDER}=$NDK_BIN/llvm-ar"
    cd native-components
    rustup target add "{{target}}" 2>/dev/null || true
    cargo build --release --target "{{target}}" --features android

# Build all 6 desktop targets and collect into cross/ output directory
rust-cross-all:
    #!/usr/bin/env bash
    set -euo pipefail
    for target in {{rust_targets}}; do
        echo "=== Building $target ==="
        just rust-cross "$target"
    done
    echo ""
    echo "=== Collecting artifacts ==="
    just rust-collect

# Build all Android targets
rust-cross-android-all:
    #!/usr/bin/env bash
    set -euo pipefail
    for target in aarch64-linux-android armv7-linux-androideabi x86_64-linux-android; do
        echo "=== Building $target ==="
        just rust-cross-android "$target"
    done

# Collect cross-compiled artifacts into a flat output directory
rust-collect:
    #!/usr/bin/env bash
    set -euo pipefail
    out="{{rust_out}}"
    rm -rf "$out"
    for target in {{rust_targets}}; do
        dir="native-components/target/$target/release"
        case "$target" in
            x86_64-apple-darwin)      plat="macos-x86_64" ;;
            aarch64-apple-darwin)     plat="macos-aarch64" ;;
            x86_64-unknown-linux-gnu) plat="linux-x86_64" ;;
            aarch64-unknown-linux-gnu) plat="linux-aarch64" ;;
            x86_64-pc-windows-msvc)   plat="windows-x86_64" ;;
            aarch64-pc-windows-msvc)  plat="windows-aarch64" ;;
        esac
        mkdir -p "$out/$plat"
        # Copy whichever library files exist for this target
        # Rust-built libs: sge_native_ops, sge_audio, glfw
        cp "$dir"/libsge_native_ops.dylib "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libsge_native_ops.so    "$out/$plat/" 2>/dev/null || true
        cp "$dir"/sge_native_ops.dll      "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libsge_native_ops.a     "$out/$plat/" 2>/dev/null || true
        cp "$dir"/sge_native_ops.lib      "$out/$plat/" 2>/dev/null || true
        cp "$dir"/sge_native_ops.dll.lib  "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libsge_audio.dylib      "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libsge_audio.so         "$out/$plat/" 2>/dev/null || true
        cp "$dir"/sge_audio.dll           "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libsge_audio.a          "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libglfw.dylib           "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libglfw.so              "$out/$plat/" 2>/dev/null || true
        cp "$dir"/glfw3.dll               "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libglfw3.a              "$out/$plat/" 2>/dev/null || true
        # ANGLE libs (if present alongside Rust libs)
        cp "$dir"/libEGL.dylib            "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libEGL.so               "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libEGL.dll              "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libGLESv2.dylib         "$out/$plat/" 2>/dev/null || true
        cp "$dir"/libGLESv2.so            "$out/$plat/" 2>/dev/null || true
        cp "$dir"/GLESv2.dll              "$out/$plat/" 2>/dev/null || true
        echo "  $plat: $(ls "$out/$plat/" | tr '\n' ' ')"
    done
    echo ""
    echo "Artifacts collected in $out/"

# Install cross-compilation toolchain prerequisites
rust-cross-setup:
    #!/usr/bin/env bash
    echo "Installing Rust cross-compilation tools..."
    echo ""
    echo "1. cargo-zigbuild (for Linux targets):"
    cargo install cargo-zigbuild
    echo ""
    echo "2. cargo-xwin (for Windows targets):"
    cargo install cargo-xwin
    echo ""
    echo "3. Zig (required by cargo-zigbuild):"
    if command -v brew &>/dev/null; then
        brew install zig
    else
        echo "  Install Zig from https://ziglang.org/download/"
    fi
    echo ""
    echo "4. Adding Rust targets..."
    for target in {{rust_targets}}; do
        rustup target add "$target"
    done
    echo ""
    echo "Done! Run 'just rust-cross-all' to build all targets."

# ── Metals MCP ────────────────────────────────────────────────────

# Install metals-mcp-server binary via Coursier (snapshot)
metals-install:
    cs install metals-mcp

# Start metals-mcp server
metals:
    metals-mcp --workspace . --port 7845 --client claude --default-bsp-to-build-tool
