#!/usr/bin/env bash
# Catches credentials before they reach a public repo.
#
# Not theoretical for this project: a real Neo4j password was committed to
# application.properties and pushed to a public GitHub repo.
#
#   scan-secrets.sh             scan the working tree  (what CI gates on)
#   scan-secrets.sh --history   scan all git history   (audit; may stay red
#                               until the history is rewritten and the
#                               exposed credentials are rotated)
set -euo pipefail
cd "$(dirname "$0")/.."

MODE="tree"
[[ "${1:-}" == "--history" ]] && MODE="history"

if command -v gitleaks >/dev/null 2>&1; then
  if [[ "$MODE" == "history" ]]; then
    echo "scanning full git history with gitleaks..."
    gitleaks detect --no-banner --redact --config .gitleaks.toml --source .
  else
    echo "scanning working tree with gitleaks..."
    gitleaks detect --no-banner --redact --no-git --config .gitleaks.toml --source .
  fi
  echo "gitleaks: clean"
  exit 0
fi

echo "gitleaks not installed - running built-in grep fallback"
echo "  (install for real coverage: https://github.com/gitleaks/gitleaks)"
if [[ "$MODE" == "history" ]]; then
  echo "  --history needs gitleaks; the fallback only sees the working tree." >&2
  exit 2
fi

status=0
report() { echo "  ^ potential secret in $1"; status=1; }

# Placeholder-looking values are fine anywhere.
PLACEHOLDER='your-|example|placeholder|change-?this|local-dev|localhost|dummy|\$\{|<.*>'

# 1. Hardcoded values in configuration files, where a literal password is
#    always wrong. Source files are excluded: `String password = ...` is code.
while IFS= read -r file; do
  case "$file" in
    .env.example|.gitleaks.toml) continue ;;
  esac
  if grep -nEi '^[^#]*(password|secret|token|api[_-]?key)[[:space:]]*[:=][[:space:]]*[^[:space:]#]{8,}' "$file" 2>/dev/null \
      | grep -vEi "$PLACEHOLDER"; then
    report "$file"
  fi
done < <(git ls-files '*.properties' '*.yaml' '*.yml' '*.json' '*.toml' '*.ini' '*.conf' 2>/dev/null || true)

# 2. Unmistakable credential shapes, anywhere in the repo.
while IFS= read -r file; do
  case "$file" in
    .gitleaks.toml|scripts/scan-secrets.sh) continue ;;
  esac
  if grep -nE 'AKIA[0-9A-Z]{16}|-----BEGIN [A-Z ]*PRIVATE KEY-----|xox[baprs]-[0-9A-Za-z-]{10,}|ghp_[0-9A-Za-z]{36}|glpat-[0-9A-Za-z_-]{20}' \
      "$file" 2>/dev/null | grep -vEi "$PLACEHOLDER"; then
    report "$file"
  fi
done < <(git ls-files)

if [[ $status -eq 0 ]]; then
  echo "grep fallback: clean"
else
  echo ""
  echo "Move these to environment variables (see .env.example) and rotate"
  echo "anything that was ever committed - git history keeps the old value."
fi
exit $status
