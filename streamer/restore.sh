#!/usr/bin/env bash
# Undo everything done to this Google TV Streamer: packages re-enabled, stock Home back, screensaver
# back to Google's, Private DNS off. Run from anywhere with TV_HOST set. Nothing is uninstalled.
#
# Usage: TV_HOST=192.168.86.45 streamer/restore.sh
set -euo pipefail
: "${TV_HOST:?set TV_HOST to the TV IP address}"
cd "$(dirname "$0")/.."
PY=tools/venv/bin/python
STOCK=com.google.android.apps.tv.launcherx
export TV_HOST

# 1. Packages from the debloat batches. Each list has a recovery record from when it was applied
#    (~/.cleanyourtv/), so packages that were already disabled before stay disabled. The lists must
#    stay at these paths: the records are keyed by them. Pass the stock launcher each time so Home
#    goes back to it. setupwraith (batch 4) comes back last, so it can't take Home first.
for batch in backup/streamer-batch1.txt backup/streamer-batch2.txt backup/streamer-batch3.txt backup/streamer-batch4.txt; do
  echo "== restoring $batch"
  tools/restore.sh "$batch" "$STOCK"
done

# 2. Things the records don't cover: the stock launcher was disabled by hand, the screensaver was
#    pointed at the launcher's clock, and Private DNS was turned on.
"$PY" -I tools/tv.py 'pm enable --user 0 com.google.android.apps.tv.launcherx'
"$PY" -I tools/tv.py 'cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity'
"$PY" -I tools/tv.py 'settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop'
"$PY" -I tools/tv.py 'settings put secure screensaver_default_component com.google.android.apps.tv.dreamx/.service.Backdrop'
"$PY" -I tools/tv.py 'settings delete global private_dns_specifier'
"$PY" -I tools/tv.py 'settings delete global private_dns_mode'

# 3. Check where Home resolves now. It should name the stock launcher.
HOME_NOW=$("$PY" -I tools/tv.py 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1' | tr -d '\r')
echo "Home now resolves to: $HOME_NOW"

# The launcher (com.gathin.tvhome) stays installed, just not as Home. To remove it entirely:
#   tools/tv.py 'pm uninstall --user 0 com.gathin.tvhome'
