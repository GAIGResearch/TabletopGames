"""
Runs one time-management strategy against the EXP baseline in Connect4, split over parallel processes.

usage: python3 run_matchup.py STRATEGY SECONDS GAMES OUTDIR [--table FILE] [--procs N] [--baseline EXP]

The player configs are taken from json/players/timeManagement/connect4, with the game budget set to SECONDS and
the decision table (EXP-MOVES) set to FILE. Run from the TabletopGames root, after mvn compile.
"""
import argparse, json, os, subprocess

CONFIGS = "json/players/timeManagement/connect4"

ap = argparse.ArgumentParser()
ap.add_argument("strategy")
ap.add_argument("seconds", type=int)
ap.add_argument("games", type=int)
ap.add_argument("outdir")
ap.add_argument("--table", default=None)
ap.add_argument("--procs", type=int, default=4)
ap.add_argument("--baseline", default="EXP")
ap.add_argument("--classpath", default=None)
args = ap.parse_args()

players = os.path.join(args.outdir, "players")
os.makedirs(players, exist_ok=True)
for name in (args.baseline, args.strategy):
    config = json.load(open(os.path.join(CONFIGS, name + ".json")))
    config["budget"] = args.seconds
    if args.table:
        config["decisionTable"] = args.table
    json.dump(config, open(os.path.join(players, name + ".json"), "w"), indent=2)

cp = args.classpath or "target/classes:" + open(os.environ["TAG_CP"]).read().strip()
procs = []
for i in range(args.procs):
    dest = os.path.join(args.outdir, "run%d" % i)
    log = open(os.path.join(args.outdir, "run%d.log" % i), "w")
    cmd = ["java", "-Xmx2g", "-cp", cp, "evaluation.RunGames", "game=Connect4", "nPlayers=2", "mode=exhaustive",
           "playerDirectory=" + players, "matchups=%d" % (args.games // args.procs),
           "listener=metrics/timeManagement.json", "destDir=" + dest, "seed=%d" % (1000 + 7919 * i)]
    procs.append(subprocess.Popen(cmd, stdout=log, stderr=subprocess.STDOUT))
codes = [p.wait() for p in procs]
print(args.strategy, "exit codes", codes)
