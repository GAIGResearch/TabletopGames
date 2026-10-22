# Tuning time management with NTBEA: Connect4, Dots and Boxes, Can't Stop, Dominion

## Method
- Each player has a 10 s game clock (`BUDGET_GAME_SECONDS`, thread CPU time). The baseline is EXP with the game's
  EXP-MOVES table (`data/timeManagement/<Game>_2p.txt`, from MCTSBudgetSweep self-play at 100 ms per move; Connect4
  at 1 s per move).
- MCTS settings are each game's tuned TAG settings (`json/players/gameSpecific`), with ROBUST selection; Dots and
  Boxes uses UCB-Tuned in place of regret matching, as STOP and CLOSE need the most-visited action to be played.
  Connect4 uses the paper's settings (see `Connect4_results.md`).
- Search space (`json/SearchSpaces/TimeManagement/<Game>_TM_small.json`): timeFactor f (0.5-3; below 1 moves time to
  the endgame), earlyStopP p (0-1), UNST and CLOSE loops and factors.
- Three NTBEA runs per game (StableNTBEA: both seats per trial; 100-120 trials; no evaluation games), against EXP.
- Then a round-robin tournament of EXP and the three recommendations (`scripts/timeManagement/run_tournament.py`):
  720 games (1200 for Connect4), every pairing in both seats.

## Recommendations
| Game | Run | f | p | UNST (loops, factor) | CLOSE (loops, d, factor) |
|---|---|---|---|---|---|
| Connect4 | 51 | 1.5 | 0.4 | 1, 0.5 | - |
| | 52 | 1.5 | 0.6 | 1, 0.5 | 1, 0.3, 1.0 |
| | 53 | 0.5 | 0.2 | 1, 0.5 | - |
| Dots and Boxes | 61 | 0.5 | 0.8 | 2, 1.0 | - |
| | 62 | 0.75 | 0.8 | 2, 0.5 | 1, 0.3, 0.5 |
| | 63 | 0.5 | 0.2 | 2, 0.5 | 1, 0.3, 0.5 |
| Can't Stop | 61 | 0.5 | 0.4 | 1, 0.5 | - |
| | 62 | 1.0 | 1.0 | 2, 1.0 | 1, 0.3, 0.5 |
| | 63 | 0.75 | 0.8 | - | - |
| Dominion | 71 | 0.75 | 0 | 1, 1.0 | 1, 0.5, 0.5 |
| | 72 | 3.0 | 0.8 | 2, 0.5 | 1, 0.5, 1.0 |
| | 73 | 3.0 | 0.8 | 2, 0.5 | 1, 0.3, 1.0 |

The players are in `json/players/timeManagement/<game>/tournament10s/`.

## Tournament results
Win rate over all games (draws count half), 95% confidence interval; then the result against EXP alone.

| Game | Player | All games | Against EXP | Clock used (of 10 s) |
|---|---|---|---|---|
| Connect4 (600 games each) | EXP | 49.0% (45.0-53.0) | - | 7.8 s |
| | NTBEA_51 | 53.0% (49.0-57.0) | **57.5%** (50.6-64.4) | 8.6 s |
| | NTBEA_52 | 48.2% (44.2-52.2) | 47.5% | 8.6 s |
| | NTBEA_53 | 49.8% (45.8-53.8) | 48.0% | 4.8 s |
| Dots and Boxes (360 each) | EXP | 42.8% (37.7-47.9) | - | 9.2 s |
| | NTBEA_61 | 50.3% (45.1-55.4) | 53.3% | 7.6 s |
| | NTBEA_62 | 49.7% (44.6-54.9) | 58.3% | 9.2 s |
| | NTBEA_63 | **57.2%** (52.1-62.3) | **60.0%** (51.2-68.8) | 8.0 s |
| Can't Stop (360 each) | EXP | 48.9% (43.7-54.1) | - | 7.6 s |
| | NTBEA_61 | 52.5% (47.3-57.7) | 57.5% | 5.7 s |
| | NTBEA_62 | 47.5% (42.3-52.7) | 43.3% | 9.0 s |
| | NTBEA_63 | 51.1% (45.9-56.3) | 52.5% | 6.7 s |
| Dominion (360 each) | EXP | 53.6% (48.5-58.8) | - | 8.6 s |
| | NTBEA_71 | 53.3% (48.2-58.5) | 49.6% | 8.6 s |
| | NTBEA_72 | 45.7% (40.5-50.8) | 42.5% | 10.0 s |
| | NTBEA_73 | 47.4% (42.2-52.5) | 47.1% | 9.9 s |

## Findings
- Time management helps in Dots and Boxes: all three recommendations beat EXP, and the best (f 0.5, p 0.2, UNST twice,
  CLOSE once) won 60% against it. In Connect4 the best recommendation (f 1.5, p 0.4, UNST once) won 57.5% against EXP,
  only just significant. In Can't Stop and Dominion no setting was better than EXP.
- Moving time to the opening, the paper's main effect, did not help in any of these games. Most recommendations have
  f <= 1, and in Dominion the two with f = 3 are the worst players. Dots and Boxes and Can't Stop are decided late, so
  the endgame needs its time.
- STOP with p well below 1 is in most good settings (Connect4 0.4, Dots and Boxes 0.2): the time it saves is spent on
  later decisions, or on UNST and CLOSE extensions.
- Win rate is not proportional to the clock used: in Can't Stop the best player used 5.7 s of 10 s, and in Connect4
  NTBEA_53 used 4.8 s and was no worse than EXP. More search buys little at this budget in these games.
- NTBEA estimates are noisy at this budget: one run per game recommended a setting that lost to EXP, which is why the
  tournament is needed.

## Clock overruns, and the fix
In the tournaments some players overran the 10 s clock: by up to 0.4 s in Can't Stop (a player that extended 98% of
searches) and up to 0.9 s in Dominion, including EXP itself. Once the clock was low, every decision still took the
minimum search (10 ms), over many more decisions than expected. Two changes fix this:
- 94f1792: each allocation keeps back, for every expected later decision, the minimum search plus the mean time a
  decision takes outside its search (about 1 ms here).
- e493468: the minimum search is only allowed while the clock above the reserve can pay for it; after that a search
  has a single iteration. Without this, a player that front-loads time (NTBEA_73, f = 3) still overran by 0.2 s in a
  game that outlasted the decision table's expectation.

With both, 96 Dominion games of EXP against NTBEA_73 had no overrun (at least 27 ms left). The tournaments above were
run before these fixes.

## Further points
- Thread CPU time does not include garbage collection on other threads. In Dots and Boxes a game took about 44 s of
  wall time for about 19 s charged to the players' clocks, so a player that allocates heavily gets free time.
- 30 s and 2 minute games were not tuned: one NTBEA run of 100 trials takes about 2 hours at 10 s on this machine, and
  the container was restarted several times during runs of that length. The search spaces and scripts can be used as
  they are on a cluster (`budget=30` or `budget=120` for ParameterSearch and run_tournament.py).
