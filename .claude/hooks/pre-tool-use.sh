#!/bin/bash
# PreToolUse hook: delegates to sge-dev hook, compiling if needed
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
SGE_DEV="$SCRIPT_DIR/scripts/bin/sge-dev"
SRC_DIR="$SCRIPT_DIR/scripts/src"

if [ ! -x "$SGE_DEV" ]; then
  scala-cli package "$SRC_DIR" -o "$SGE_DEV" -f 1>&2
fi

exec "$SGE_DEV" hook
