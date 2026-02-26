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

# Run quality scans (return/null/java_syntax/todo/all)
sge-quality type="all":
    #!/usr/bin/env bash
    run_return() {
        echo "=== return keyword usage ==="
        grep -rn '\breturn\b' {{sge_src}}/ --include='*.scala' \
            | grep -v '//.*return' \
            | grep -v '/\*.*return' \
            | grep -v 'boundary' \
            | grep -v 'break' || true
        echo ""
        echo "Files with return:"
        grep -rl '\breturn\b' {{sge_src}}/ --include='*.scala' | wc -l | tr -d ' '
    }
    run_null() {
        echo "=== null checks ==="
        grep -rn '== null\|!= null\|null\.asInstanceOf' {{sge_src}}/ --include='*.scala' || true
        echo ""
        echo "Files with null checks:"
        grep -rl '== null\|!= null\|null\.asInstanceOf' {{sge_src}}/ --include='*.scala' | wc -l | tr -d ' '
    }
    run_java_syntax() {
        echo "=== remaining Java syntax ==="
        grep -rn '\bpublic \|\bprivate \|\bprotected \|\bstatic \|\bvoid \|\bboolean \|\bfinal \|\babstract class\|\bimplements \|\bextends .*{' {{sge_src}}/ --include='*.scala' || true
        echo ""
        echo "Files with Java syntax:"
        grep -rl '\bpublic \|\bprivate \|\bprotected \|\bstatic \|\bvoid \|\bboolean \|\bfinal \|\babstract class\|\bimplements \|\bextends .*{' {{sge_src}}/ --include='*.scala' | wc -l | tr -d ' '
    }
    run_todo() {
        echo "=== TODO/FIXME markers ==="
        grep -rn 'TODO\|FIXME\|HACK\|XXX' {{sge_src}}/ --include='*.scala' || true
        echo ""
        echo "Files with markers:"
        grep -rl 'TODO\|FIXME\|HACK\|XXX' {{sge_src}}/ --include='*.scala' | wc -l | tr -d ' '
    }
    case "{{type}}" in
        return)     run_return ;;
        null)       run_null ;;
        java_syntax) run_java_syntax ;;
        todo)       run_todo ;;
        all)        run_return; echo ""; run_null; echo ""; run_java_syntax; echo ""; run_todo ;;
        *)          echo "Unknown type: {{type}}. Use return/null/java_syntax/todo/all" ;;
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

# Format code
fmt:
    sbt --client scalafmtAll

# Compile, format, compile again
compile-fmt: compile fmt compile

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
