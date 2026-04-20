#!/bin/bash
# Auto-lint TypeScript/TSX files after Claude edits them.
# Runs ESLint --fix on the edited file so lint errors surface immediately
# instead of being discovered only when CI runs.

input=$(cat)
file_path=$(echo "$input" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print(data.get('tool_input', {}).get('file_path', ''))
except:
    print('')
" 2>/dev/null)

# Only act on TypeScript/TSX files inside the frontend/src directory
if echo "$file_path" | grep -qE 'frontend/src/.*\.(ts|tsx)$'; then
    root=$(git rev-parse --show-toplevel 2>/dev/null)
    if [ -n "$root" ]; then
        cd "$root/frontend" || exit 0
        # Fix auto-fixable issues silently; non-zero exit is OK (Claude will see the warning)
        npx eslint --fix "$file_path" 2>/dev/null || true
    fi
fi
