# Ventlife agent search log

Goal: the strongest agent for 4-player Ventlife at 64ms per decision. Baseline: `Ventlife_baseline.json` (MCTS,
rolloutLength 0, MaxMC backup, K 0.01, LeaderHeuristic, tree reuse).

Experiments are run with `scripts/runExp.sh` (several RunGames processes in `mode=random`, 4 players, `budget=64`,
listener `json/listeners/VentlifeMetrics.json`) and aggregated by `scripts/aggregate.py`. Each `expNN` directory holds
the agent pool for that experiment. Win rates count a shared first place as a fraction of a win; +- is one standard
error.

## Exp00 - sanity check (8 games: baseline, osla, random)

`osla` (one-step look-ahead on the game's default heuristic: own score as a share of all scores) beat the baseline
in 10 of 13 shared games. MCTS tree statistics (`MCTSMetricsListener`, 2 games) show why:

| decision | median actions at root | median iterations in 64ms |
|:--|--:|--:|
| tile placement | 363 | 70 |
| creature placement | 131 | 86 |

The baseline cannot even expand every root action once: with FPU on, it chooses among the first ~70 actions in list
order. Each iteration costs ~0.9ms, dominated by `computeAvailableActions` (~250us per call on average, against
~30us for a copy, ~30us for `next` and ~30us for four `getGameScore` calls; profiled over 180 random games).

Conclusions: (1) action ordering/pruning matters far more than search depth; (2) forward-model speed is the main
limit on iterations.

