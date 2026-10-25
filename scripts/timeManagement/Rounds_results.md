# A clock per round (BUDGET_ROUND_SECONDS): Sushi Go

## Implementation
- `BUDGET_ROUND_SECONDS`: `budget` is the thinking time in seconds for each round. The player's clock
  (`AbstractPlayer.getGameTimer()`) is reset at its first decision in each round (when `getRoundCounter()` changes), so
  time not used in a round is lost.
- `MCTSTimeManager` manages each round as a game of its own: decisions are counted from the start of the round (for
  the decision table and MID), and the default decision table is `data/timeManagement/<Game>_<n>p_round.txt`.
- `GameMetrics$RoundDecisions` records each player's decisions in each round (on ROUND_OVER), and
  `DecisionTableBuilder ... table=round` builds the round table from it. `MCTSSweepListener.json` now includes it.
- Tables from MCTSBudgetSweep self-play at 100 ms per move (100 games each):
  - Sushi Go, 2 players: 27.5 decisions per game, 9.2 per round.
  - Love Letter, 2 players: 20.8 decisions per game, over 10.8 rounds; 1.9 per player per round.

## Tournament
Sushi Go, 2 players (`json/players/timeManagement/sushiGo/rounds9s/`): the tuned 2-player Sushi Go MCTS with ROBUST
selection, given 9 s for the game (`_game`) or 3 s per round (`_round`), with EXP or with STOP (p 0.5). Round-robin of
720 games, every pairing in both seats.

| Player | Win rate (95% CI) | Clock used per game | Time by round (s) | Stopped early |
|---|---|---|---|---|
| EXP_game | 48.1% (42.9-53.2) | 8.8 s | 2.9 / 2.9 / 3.0 | - |
| EXP_round | 49.9% (44.7-55.0) | 8.7 s | 2.9 / 2.9 / 2.9 | - |
| STOP_game | 52.5% (47.3-57.7) | 8.7 s | 2.1 / 2.4 / 4.1 | 88% |
| STOP_round | 49.6% (44.4-54.7) | 7.9 s | 2.5 / 2.5 / 2.8 | 88% |

No pairing differs significantly (120 games each; the largest gap is STOP_game 55.4% against EXP_round).

## Findings
- The round clock works as intended: every round gets its 3 s, with at least 50 ms left on the clock at the end of
  each round.
- In Sushi Go every round has the same number of decisions, so EXP spreads time the same way with either clock, and
  the two play equally well.
- With STOP the clocks differ: on the game clock the time saved in rounds 1 and 2 is spent in round 3 (4.1 s), while
  on the round clock it can only move to the end of the same round, and 0.4 s per round is not used at all. Neither
  is better at this budget.
- So for a game with a fixed number of rounds of equal length, a round clock is a way to stop a strategy moving time
  between rounds, and costs nothing with EXP. It does not by itself make play stronger.
