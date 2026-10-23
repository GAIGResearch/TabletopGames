# Two-level time allocation for games with a variable number of rounds: Love Letter and Poker

## Implementation
- `MCTSParams.roundAllocation` (with `BUDGET_GAME_SECONDS`) allocates the game clock in two levels:
  - At the player's first decision in a round, the round gets the time left on the game clock divided by the expected
    rounds left (`expectedRounds`, or `roundTable` by rounds played), times `roundTimeFactor`. The minimum search of
    every decision expected in later rounds is kept back first.
  - Each decision gets the time left in the round's allocation divided by the expected decisions left in the round
    (the per-round decision table, as with `BUDGET_ROUND_SECONDS`). timeFactor, MID and the dynamic strategies apply
    within the round as before.
  - Time a round does not use stays on the game clock and is shared out over the later rounds. Every allocation is
    still capped by what the game clock can pay for, so the game clock is never overrun.
- Default tables: `<Game>_<n>p_rounds.txt` (rounds left, by rounds played) and `<Game>_<n>p_round.txt` (decisions left
  in the round, by decisions made in it). `DecisionTableBuilder table=rounds` and `table=round` build them.
- `GameMetrics$RoundDecisions` now also records at GAME_OVER: the decisions in a last round that did not end with
  ROUND_OVER, and the number of rounds played. Poker ends the game on its last hand without ending the round, so
  without this the last hand was missing from both tables. Over 200 Poker games the per-round decisions add up to
  each player's decisions in the game.
- `MCTSMetrics$TimeManagement` records the round's allocation (`RoundPlanned`).
- Tables (MCTSBudgetSweep self-play at 100 ms per move):

| Game (2 players) | Rounds per game | Decisions per round | Decisions per game |
|---|---|---|---|
| Love Letter (100 games) | 10.8 (7 to 13) | 1.9 | 20.8 |
| Poker (200 games) | 4.0 (39% of games last one hand) | 1.3 | 5.3 |

## Tournaments
Round-robin of 720 games each, every pairing in both seats. Each game's tuned 2-player MCTS with ROBUST selection, on a
game clock (Love Letter 4 s, Poker 2 s; about 200 ms and 400 ms per decision), with flat allocation over the game
(`_game`, the EXP-MOVES game table) or two-level allocation (`_hier`), and with EXP or with STOP (p 0.5).

Search time matters in Love Letter at this budget: the same agent at 300 ms per move beat itself at 10 ms in 61.5% of
400 games.

| Game | Player | Win rate (95% CI) | Clock used (of the budget) |
|---|---|---|---|
| Love Letter | EXP_game | 48.3% (43.2-53.5) | 2.9 s of 4 s |
| | EXP_hier | 52.8% (47.6-57.9) | 3.0 s |
| | STOP_game | 47.2% (42.1-52.4) | 3.0 s |
| | STOP_hier | 51.7% (46.5-56.8) | 3.0 s |
| Poker | EXP_game | 50.7% (45.5-55.9) | 1.0 s of 2 s |
| | EXP_hier | 49.9% (44.7-55.0) | 0.9 s |
| | STOP_game | 51.9% (46.8-57.1) | 0.8 s |
| | STOP_hier | 47.5% (42.3-52.7) | 0.7 s |

No difference is significant. In Love Letter both two-level players are ahead of both flat ones (by 4.5 points on
average over 360 games each), but within the confidence intervals.

## Findings
- The two allocations spread time over the game in opposite ways. Mean time per decision in Love Letter, by round:

| Round | 0 | 2 | 4 | 6 | 8 | 10 | 12 |
|---|---|---|---|---|---|---|---|
| EXP_game | 179 | 181 | 180 | 174 | 160 | 137 | 90 |
| EXP_hier | 116 | 132 | 138 | 173 | 203 | 218 | 161 |

  The flat allocation gives each decision the same share until games run past the table's expected length, and then
  less and less. The two-level allocation gives each round the same share, but within a round it gives most to the
  first decision (about 220, 140 and 80 ms for decisions 1-3), as the round table expects about two. Many rounds end
  sooner than that, so their time is left on the clock and shared out over later rounds, which get more.
- Neither allocation spends the whole clock: about a quarter of it is left at the end of a Love Letter game, and half
  in Poker. Both tables were made from self-play at 100 ms per move, and in these tournaments games were shorter than
  the tables expect (17-18 decisions per Love Letter game, against 20.8), so time is kept for rounds that are not
  played. A table made at the tournament budget, or a `roundTimeFactor` or `timeFactor` above 1, would spend more.
- Poker cannot separate the players with this agent: half its games end on the first hand, and when both agents play
  the same way the deal decides the game (both seat orders use the same deals, so a pairing's results then cancel).
- So the two-level allocation works as designed and costs nothing against flat EXP-MOVES; a gain, if there is one, is
  smaller than these tournaments can resolve. `roundTimeFactor` (and the rest of the search space) could be tuned
  with NTBEA as for the other games.
