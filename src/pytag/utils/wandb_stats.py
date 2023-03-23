import os
import wandb
import numpy as np
import pandas as pd
import seaborn as sns
sns.set_theme()

from collections import defaultdict, OrderedDict

agent = ["PPO", "PPO_LSTM"]
games = [ "TAG/Stratego", "TAG/TicTacToe", "TAG/Diamant", "TAG/ExplodingKittens", "TAG/LoveLetter"] # "TAG/TicTacToe", "TAG/Stratego",
opponents = ["random", "osla"]
n_players = [2]
metric = "speed"  # wins, rewards, length
ENTITY = "martinballa"  # '<entity>'
project = "pyTAG"  # '<project>'
api = wandb.Api()

window = 100
results = {}
result_lst = []

if metric == "wins":
    Y_RANGES = [0, 1]
    METRIC_NAME = "charts/episodic_wins"
elif metric == "length":
    Y_RANGES = [0, 1]
    METRIC_NAME = "charts/episodic_length"
elif metric == "speed":
    Y_RANGES = [0, 1]
    METRIC_NAME = "charts/SPS"
else:
    Y_RANGES = [-1, 1]
    METRIC_NAME = "charts/episodic_return"
# for game in games:
#     # for opponent in opponents:
#         for n_player in n_players:
#             if "Stratego" in game and n_player == 4:
#                 continue
#             if "TicTacToe" in game and n_player == 4:
#                 continue
#
#             filename = os.path.expanduser(f"~/data/pyTAG/plots/merged/{metric}_{game}_{n_player}_players.png")
#             metrics = defaultdict(list)
#             title = f"{n_player} Player {game[4:]}" # vs {opponent}"
#             ncols = 2

filters = ["n_players", "opponent", "lstm", "env_id" ]
filter_values = ["", "osla", -1, ""]
labels = []
# labels = [""]

groups = {}
summaries = []

# collect all the data first and then plot them
runs = api.runs(f"{ENTITY}/{project}")

for run in runs:
    if run.state != "finished":
        continue
    if run.config["opponent"] == "mcts":
        continue
    if "old" in run.tags:
        continue
    values = []
    ignore = False

    for filter, val in zip(filters, filter_values):
        if filter in run.config:
            if val != "" and run.config[filter] != val:
                ignore = True
                break
            values.append(run.config[filter])
        else:
            values.append(str(-1)) # means not applicable
    if ignore:
        continue

    # todo update to: scan_history to get all the data
    # data = run.scan_history(keys=["global_step", "charts/episodic_return"])
    data = run.history(keys=["global_step", "charts/SPS", "charts/episodic_return", "charts/episodic_wins", "charts/episodic_length"], samples=5000)
    data["name"] = str(values)
    summaries.append(data)

    if str(values) in groups:
        groups[str(values)].append(data)
    else:
        groups[str(values)] = [data]
        labels.append((values[0], values[-1][4:]))


# todo specify order and rename them
groups = OrderedDict(sorted(groups.items(), key=lambda item: item[0]))

# fig, ax = plt.subplots(figsize=(6, 4))
counter = 0

if len(labels) != len(groups):
    labels = groups
summary_df = pd.DataFrame()
for g, label in zip(groups, labels):
    df = pd.concat(groups[g], ignore_index=True).sort_values("global_step")

    min_period = len(groups[g])# this is how many datapoints we have for the same total_step
    window_ = min_period * window #window * len(dfs)  # adjust window to the extra data points
    # this leaves too few datapoints
    # mean = df["test/rewards"].rolling(window=window_, min_periods=window_).mean().dropna()[0::window_]
    # std = df["test/rewards"].rolling(window=window_, min_periods=window_).sem().dropna()[0::window_]
    # mean = df[METRIC_NAME].dropna().mean()
    # std = df[METRIC_NAME].dropna().sem()
    with pd.option_context('display.float_format', '{:0.2f}'.format):
        last_wins = f"{df['charts/episodic_wins'][-window:].mean():.2f}({df['charts/episodic_wins'][-window:].sem():.2f})"
        last_score = f"{df['charts/episodic_return'][-window:].mean():.2f}({df['charts/episodic_return'][-window:].sem():.2f})"
        last_length = f"{df['charts/episodic_length'][-window:].mean():.2f}({df['charts/episodic_length'][-window:].sem():.2f})"
        last_speed = f"{df['charts/SPS'][-window:].mean():.2f}({df['charts/SPS'][-window:].sem():.2f})"
        mean_speed = f"{df['charts/SPS'].mean():.2f}({df['charts/SPS'].sem():.2f})"
        first_speed = f"{df['charts/SPS'][:window].mean():.2f}({df['charts/SPS'][:window].sem():.2f})"
        # total_steps = df[METRIC_NAME].max()
        # df[METRIC_NAME][-window:].mean() # can access last 100 steps
        # print(f"{g} {metric}: {mean}({std})")
        # todo convert this into an easily readable table - set precision
        print(f"{g} last wins: {last_wins} last score: {last_score} last length: {last_length} last speed: {last_speed} mean speed: {mean_speed} first speed: {first_speed}")
        # print(f"{g[-1]}}")
        result_lst.append([g[1], g[g.find("/")+1:g.find("]")], last_wins, last_score, last_length, last_speed, mean_speed, first_speed])

results_df = pd.DataFrame(result_lst, columns=["n_players", "game", "last_wins", "last_score", "last_length", "last_speed", "mean_speed", "first_speed"])

print(result_lst)
print("################# latex ##################")
print(results_df.to_latex())
print("################# latex (dropped first/last) ##################")
print(results_df.drop(["last_speed", "first_speed"], axis=1).to_latex())
# [print(f"{key} {results[key]}: {len(groups[key])}") for key in groups.keys()]
