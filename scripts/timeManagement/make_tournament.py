"""
Makes the players for a tournament between EXP and the recommendations of NTBEA runs.

usage: python3 make_tournament.py CONFIG_DIR SECONDS OUT_DIR NAME=Recommended_Final.json [NAME=... ...]
CONFIG_DIR holds the game's EXP.json; each recommendation gives the time-management parameters to use with it.
"""
import json, os, sys

TM = ["timeFactor", "earlyStopP", "midHeight", "midCentre", "midWidth", "behindLoops", "behindThreshold",
      "behindFactor", "unstableLoops", "unstableFactor", "closeLoops", "closeThreshold", "closeFactor"]
config_dir, seconds, out_dir = sys.argv[1], int(sys.argv[2]), sys.argv[3]
os.makedirs(out_dir, exist_ok=True)
base = json.load(open(os.path.join(config_dir, "EXP.json")))
base["budget"] = seconds
json.dump(base, open(os.path.join(out_dir, "EXP.json"), "w"), indent=2)
for arg in sys.argv[4:]:
    name, file = arg.split("=", 1)
    recommended = json.load(open(file))
    player = dict(base)
    player.update({k: recommended[k] for k in TM if k in recommended})
    json.dump(player, open(os.path.join(out_dir, name + ".json"), "w"), indent=2)
    print(name, {k: recommended[k] for k in TM if k in recommended})
