# Connect4: replication of Baier and Winands (2016), Table VI

## Setup
- Connect4, 7 x 6. MCTS as in the paper's section V: UCB1-Tuned with K = 1.3, random rollouts to the end of the game,
  ROBUST selection (most-visited action), no tree reuse. Closed loop.
- Players in `json/players/timeManagement/connect4/`; each strategy plays EXP, swapping seats every game.
- EXP uses the EXP-MOVES table `data/timeManagement/Connect4_2p.txt`, from 200 self-play games at 1 s per move.
- Time is the player's game timer (`BUDGET_GAME_SECONDS`), which charges the thread CPU time of every decision.
- Run with `scripts/timeManagement/run_sweep.sh`; analysed with `scripts/timeManagement/analyse.py`.

## Two differences from the paper
| | Paper | TAG |
|---|---|---|
| Iterations per second (empty board) | 64,500 | about 12,600 |
| Game length (self-play, 1 s per move) | 37 plies | 22 plies (11.2 decisions per player) |

TAG at 20 s per game therefore searches about as much as the paper's engine at 4 s, and a time factor f moves about
twice the share of the clock to the first move that it did in the paper (5 / 11.2 against 5 / 18.5).

## Win rates against EXP
Draws count half. 95% confidence intervals.

| Strategy (paper's Table VI parameters) | 5 s, 400 games | 20 s, 600 games | Paper (20 s) |
|---|---|---|---|
| STOP (f 5, p 0.9) | 46.6% (41.7-51.5) | 50.9% (46.9-54.9) | 65.0% |
| STOP_B (f 5) | 51.6% (46.7-56.5) | | 63.0% |
| STOP_A | 52.1% (47.2-57.0) | | 50.8% |
| OPEN (f 2.25) | 50.5% (45.6-55.4) | | 55.8% |
| MID (a 2.5, b 15.5, c 10 decisions) | 51.4% (46.5-56.3) | | 57.0% |
| BEHIND (v 0.6, f 1.5) | 52.6% (47.7-57.5) | | 55.8% |
| UNST (f 0.75) | 52.8% (47.9-57.6) | | 54.9% |
| CLOSE (d 0.8, f 1.5) | 49.5% (44.6-54.4) | | 58.7% |

With f scaled to TAG's game length (the paper's f x 11.2 / 18.5):

| Strategy | 5 s, 400 games |
|---|---|
| STOP (f 3, p 0.9) | 54.6% (49.7-59.5) |
| STOP_B (f 3) | 50.7% (45.9-55.6) |
| OPEN (f 1.36) | 51.4% (46.5-56.3) |

MID's b and c are the paper's 30 and 20 plies converted to decisions; in TAG's shorter games the bonus rises over the
whole game, so it acts as an OPEN with f of about 2-3.

## Time per decision (ms, mean over games reaching that decision)
| Decision | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 10 | 12 |
|---|---|---|---|---|---|---|---|---|---|---|
| EXP, 20 s | 1774 | 1773 | 1773 | 1765 | 1707 | 1641 | 1552 | 1468 | 1185 | 803 |
| STOP, 20 s | 8433 | 5332 | 3176 | 1719 | 814 | 332 | 112 | 29 | 9 | 9 |
| EXP, 5 s | 440 | 439 | 439 | 437 | 423 | 406 | 385 | 364 | 294 | 200 |
| STOP, 5 s | 2085 | 1319 | 787 | 432 | 205 | 84 | 29 | 9 | 9 | 9 |
| STOP f 3, 5 s | 1249 | 1024 | 817 | 636 | 462 | 318 | 205 | 125 | 31 | 9 |
| STOP_A, 5 s | 415 | 421 | 422 | 422 | 412 | 400 | 383 | 366 | 301 | 210 |

- EXP uses 77% of its clock on average (15.7 s of 20 s): many games end before the table's expected length.
- STOP with f 5 spends 42% of the clock on the first decision, and plays from decision 8 at the 10 ms minimum (about
  120 iterations). In the paper the endgame still had thousands of iterations per move.
- STOP_A stops early in 89% of searches but saves only about 6% of the search time (the paper: 31%).
- CLOSE with d 0.8 extends every search, and BEHIND with v 0.6 two thirds of them.

## NTBEA at 10 s per game
Four NTBEA runs (`Connect4_TM.json`, 400 trials of one game against EXP, 100 games to evaluate each recommendation)
recommended scattered settings (f from 1.5 to 7, p 0.2-0.4), with estimates of 45-55%. The best estimate (55%; f 3,
p 0.4, UNST twice, CLOSE and BEHIND once; `NTBEA_10s.json`) then won only **44.2% (39.4-49.1)** of 400 games against
EXP: it spends 2.4 s of 10 s on the first decision and plays from decision 12 at the 10 ms minimum.
One game per trial is too noisy for effects of a few percent over 17 parameters. A smaller search space that also
allows f < 1 (more time for the endgame), with both seats per trial (StableNTBEA), is in `Connect4_TM_small.json`.

## Conclusions
- The paper's results do not reproduce with its Connect4 parameters. No strategy differs significantly from EXP at
  5 s, and STOP is at 50.9% at 20 s.
- The parameters depend on game length and engine speed, as the paper itself found across games. Scaling f to TAG's
  game length moves STOP in the paper's direction (54.6%), but not significantly.
- Moving time to the opening does not help in TAG's Connect4, and moving a lot of it hurts (STOP with f 5 at 5 s,
  and the 10 s NTBEA recommendation).
