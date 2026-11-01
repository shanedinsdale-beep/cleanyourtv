#!/usr/bin/env bash
# Undo everything done to the Living Room TV (Kogan AI PONT, Google TV 14): packages re-enabled, stock
# Home back, Private DNS off. The screensaver was never changed (it is still Google's dreamx). Run from
# anywhere with TV_HOST set. Nothing is uninstalled.
#
# Usage: TV_HOST=192.168.86.122 livingroom/restore.sh
set -euo pipefail
: "${TV_HOST:?set TV_HOST to the TV IP address}"
cd "$(dirname "$0")/.."
PY=tools/venv/bin/python
STOCK=com.google.android.apps.tv.launcherx
export TV_HOST

# 1. Packages from the debloat batches. Each list has a recovery record from when it was applied
#    (~/.cleanyourtv/), so packages that were already disabled before stay disabled. The lists must
#    stay at these paths: the records are keyed by them. Pass the stock launcher each time so Home
#    goes back to it. setupwraith (batch 5) comes back last, so it can't take Home first.
for batch in backup/livingroom-batch1.txt backup/livingroom-batch2.txt backup/livingroom-batch3.txt \
             backup/livingroom-batch4.txt backup/livingroom-batch5.txt; do
  [ -f "$batch" ] || continue
  echo "== restoring $batch"
  tools/restore.sh "$batch" "$STOCK"
done

# 2. Things the records don't cover: the stock launcher was disabled by hand, and Private DNS was
#    turned on (it was unset before).
"$PY" -I tools/tv.py "pm enable --user 0 $STOCK"
"$PY" -I tools/tv.py "cmd package set-home-activity --user 0 $STOCK/.home.HomeActivity"
"$PY" -I tools/tv.py 'settings delete global private_dns_specifier'
"$PY" -I tools/tv.py 'settings delete global private_dns_mode'

# 3. Check where Home resolves now. It should name the stock launcher.
HOME_NOW=$("$PY" -I tools/tv.py 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1' | tr -d '\r')
echo "Home now resolves to: $HOME_NOW"

# The launcher (com.gathin.tvhome) stays installed, just not as Home. To remove it entirely:
#   tools/tv.py 'pm uninstall --user 0 com.gathin.tvhome'
