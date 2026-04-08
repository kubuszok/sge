#!/bin/bash
# PreToolUse hook: delegates to `re-scale hook`.
#
# re-scale is the cross-project successor to the legacy sge-dev tool.
# Source: https://github.com/kubuszok/re-scale
#
# Install once via:
#
#   git clone https://github.com/kubuszok/re-scale.git
#   cd re-scale && ./scripts/install.sh
#
# That builds the Scala Native binary + wrapper and copies them into
# $HOME/bin/. The wrapper sets SCALANATIVE_MAX_HEAP_SIZE so this hook
# can never accidentally allocate unbounded memory. After install,
# `re-scale --version` from any directory should work.
#
# If re-scale isn't on $PATH, this hook fails loudly so the user
# knows to install it — there's no longer a `scripts/src/sge-dev`
# fallback to compile from source.

set -euo pipefail

if ! command -v re-scale >/dev/null 2>&1; then
  echo "re-scale: not found on \$PATH" 1>&2
  echo "Install: git clone https://github.com/kubuszok/re-scale.git && cd re-scale && ./scripts/install.sh" 1>&2
  exit 1
fi

exec re-scale hook
