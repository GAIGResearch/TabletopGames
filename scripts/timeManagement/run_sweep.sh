#!/bin/bash
# Runs each strategy against EXP in Connect4 (from the TabletopGames root, after mvn compile).
# usage: scripts/timeManagement/run_sweep.sh SECONDS GAMES TABLE OUTROOT STRATEGY...
# TAG_CP must name a file holding the Maven dependency classpath (mvn dependency:build-classpath -Dmdep.outputFile=...)
SECONDS_PER_GAME=$1; GAMES=$2; TABLE=$3; OUTROOT=$4; shift 4
for s in "$@"; do
  python3 scripts/timeManagement/run_matchup.py "$s" "$SECONDS_PER_GAME" "$GAMES" "$OUTROOT/$s" --table "$TABLE"
  python3 scripts/timeManagement/analyse.py "$OUTROOT/$s"
done
