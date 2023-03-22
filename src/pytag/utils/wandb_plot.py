import os
import wandb
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
sns.set_theme()

from collections import defaultdict, OrderedDict

agent = ["PPO", "PPO_LSTM"]
games = ["TAG/Diamant", "TAG/ExplodingKittens", "TAG/LoveLetter"] # "TAG/TicTacToe", "TAG/Stratego",
opponents = ["random", "osla"]
n_players = [2, 4]
for game in games:
    for opponent in opponents:
        for n_player in n_players:
            if "Stratego" in game and n_player == 4:
                continue
            if "TicTacToe" in game and n_player == 4:
                continue
            ENTITY = "martinballa" # '<entity>'
            project = "pyTAG"  # '<project>'
            METRIC_NAME = "charts/episodic_return" #"charts/episodic_wins" # '<metric>'
            Y_RANGES = [-1, 1]
            filename = os.path.expanduser(f"~/data/pyTAG/plots/{game}_n_players{n_player}_{opponent}_results.png")
            window = 100

            api = wandb.Api()
            metrics = defaultdict(list)
            title = f"{n_player}P {game[4:]} vs {opponent}"
            ncols = 3

            filters = ["lstm", "env_id", "n_players", "opponent"]
            filter_values = ["", game, n_player, opponent]
            labels = ["PPO", "PPO-LSTM"]
            # labels = [""]

            groups = {}
            summaries = []

            # collect all the data first and then plot them
            runs = api.runs(f"{ENTITY}/{project}")

            for run in runs:
                if run.state != "finished":
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
                data = run.history(keys=["global_step", METRIC_NAME], samples=5000)
                data["name"] = str(values)
                summaries.append(data)

                if str(values) in groups:
                    groups[str(values)].append(data)
                else:
                    groups[str(values)] = [data]

                # history = run.scan_history(keys=["test/rewards"])
                # test_rewards = [row["test/rewards"] for row in history]
                # df = pd.DataFrame(test_rewards)

                # name = run.name # run.config["name"]
                # data = run.history(keys=["test/total_steps", "test/rewards"])
                # data["name"] = name
                # summaries.append(data)

        # todo specify order and rename them
        groups = OrderedDict(sorted(groups.items(), key=lambda item: item[0]))

        fig, ax = plt.subplots(figsize=(6, 4))
        counter = 0
        colors = plt.cm.rainbow(np.linspace(0, 1, len(groups)))
        label = groups.keys()
        # https://matplotlib.org/stable/gallery/lines_bars_and_markers/linestyles.html
        line_types = ["-", "--"] # (0, (5, 10))]
        if len(labels) != len(groups):
            labels = groups
        for g, label in zip(groups, labels):
            df = pd.concat(groups[g], ignore_index=True).sort_values("global_step")

            line_type = line_types[0]
            col = colors[counter]

            min_period = len(groups[g])# this is how many datapoints we have for the same total_step
            window_ = min_period * window #window * len(dfs)  # adjust window to the extra data points
            # this leaves too few datapoints
            # mean = df["test/rewards"].rolling(window=window_, min_periods=window_).mean().dropna()[0::window_]
            # std = df["test/rewards"].rolling(window=window_, min_periods=window_).sem().dropna()[0::window_]
            mean = df[METRIC_NAME].dropna().rolling(window=window_, min_periods=min_period).mean()
            std = df[METRIC_NAME].dropna().rolling(window=window_, min_periods=min_period).sem()
            total_steps = df[METRIC_NAME].dropna().rolling(window=window_, min_periods=min_period).max()

            total_steps = np.linspace(0, np.max(total_steps), len(mean))
            kwargs = dict(alpha=0.2, linewidths=0, color=col, zorder=10 - counter)
            ax.fill_between(total_steps, mean - std, mean + std, **kwargs)

            ax.plot(total_steps, mean, linestyle=line_type, linewidth=1, label=label, color=col, zorder=100 - counter)
            counter += 1

        # ncol=2,
        ax.legend(ncol=ncols, frameon=False)  # bbox_to_anchor=(0.90, 0.1)) #, loc='lower right', frameon=False)
        plt.title(title)
        plt.xlabel("1e6 steps", labelpad=0)
        plt.ylabel("rewards")
        ax.set_ylim(Y_RANGES)
        ax.spines["top"].set_visible(False)
        # ax.spines["bottom"].set_visible(False)
        ax.spines["right"].set_visible(False)

        if not os.path.exists(os.path.dirname(filename)):
            os.makedirs(os.path.dirname(filename))
        plt.savefig(filename, bbox_inches="tight", dpi='figure', pad_inches=0)

        # save plots
        ax.spines["left"].set_visible(False)
        if filename is not None:
            plt.margins(0, 0)
            plt.savefig(filename, bbox_inches="tight", dpi='figure', pad_inches=0)
            print("figure saved")

        plt.show()
        print("")
        [print(f"{key}: {len(groups[key])}") for key in groups.keys()]
