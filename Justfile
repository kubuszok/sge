# SGE Porting — Task Runner
# Run `just --list` to see all recipes

set shell := ["bash", "-euo", "pipefail", "-c"]

tsv := "docs/progress/migration-status.tsv"
sge_src := "core/src/main/scala/sge"
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

# Compile the project
compile:
    sbt --client compile

# Compile and show last N lines (default 30)
compile-tail n="30":
    #!/usr/bin/env bash
    sbt --client 'core / compile' 2>&1 | tail -n {{n}}

# Compile and show only errors (no info/warn noise)
compile-errors:
    #!/usr/bin/env bash
    output=$(sbt --client 'core / compile' 2>&1)
    echo "$output" | grep '^\[error\]' || echo "No errors"
    echo ""
    echo "$output" | tail -3

# Compile and show warnings + errors (skip info)
compile-warnings:
    #!/usr/bin/env bash
    output=$(sbt --client 'core / compile' 2>&1)
    echo "$output" | grep -E '^\[(warn|error)\]' || echo "Clean"
    echo ""
    echo "$output" | tail -3

# Format code
fmt:
    sbt --client scalafmtAll

# Compile, format, compile again
compile-fmt: compile fmt compile

# Run all tests
test:
    sbt --client test

# Run all tests and show last N lines (default 20)
test-tail n="20":
    #!/usr/bin/env bash
    sbt --client 'core / test' 2>&1 | tail -n {{n}}

# Run a specific test suite
test-only suite:
    sbt --client "core/testOnly {{suite}}"

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

# Call GitHub API (GET requests)
gh-api endpoint:
    gh api {{endpoint}}

# ── Scalafix ──────────────────────────────────────────────────────

# Run a scalafix rule on the whole project (e.g. `just scalafix NullToNullable`)
scalafix rule:
    sbt --client 'core / scalafix {{rule}}'

# Run a scalafix rule on a specific file
scalafix-file rule file:
    sbt --client 'core / scalafix {{rule}} --files={{file}}'

# Check for null patterns (lint only, no changes)
lint-null:
    sbt --client 'core / scalafix NullToNullable'

# Check for banned syntax (return, null literals, etc.)
lint-syntax:
    sbt --client 'core / scalafix DisableSyntax'

# ── Search helpers ───────────────────────────────────────────────

# Search SGE source for a pattern (wraps rg). Add -c for counts, -l for files only.
sge-grep pattern *flags:
    rg '{{pattern}}' {{sge_src}} --type scala {{flags}}

# Count occurrences of a pattern in SGE source (files + total)
sge-count pattern:
    #!/usr/bin/env bash
    rg -c '{{pattern}}' {{sge_src}} --type scala 2>/dev/null \
        | awk -F: '{s+=$2; n++; print} END{print ""; print "  Files: " n "  Occurrences: " s}'

# ── Metals MCP ────────────────────────────────────────────────────

# metals-mcp snapshot version (check https://github.com/scalameta/metals/actions for latest)
metals_mcp_version := "1.6.5+61-683485a8-SNAPSHOT"

# Install metals-mcp-server binary via Coursier (snapshot)
metals-install:
    cs bootstrap org.scalameta:metals-mcp_2.13:{{metals_mcp_version}} \
      -r https://central.sonatype.com/repository/maven-snapshots/ \
      -o metals-mcp-server -f

# Start metals-mcp server
metals:
    ./metals-mcp-server --workspace . --port 7845 --client claude --default-bsp-to-build-tool
