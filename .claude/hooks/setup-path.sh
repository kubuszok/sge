#!/bin/bash
if [ -n "$CLAUDE_ENV_FILE" ]; then
  echo "export PATH=\"$CLAUDE_PROJECT_DIR/scripts/bin:\$PATH\"" >> "$CLAUDE_ENV_FILE"
fi
