# Seed repeatability of games

Which games give exactly the same game when played again with the same seed, as checked by
`SeedRepeatabilityTester`. Re-run the tester and update this file when fixing a game.

## How this was checked

- Run on 2026-10-18, with seed 42.
- Player count: 3, or the nearest count the game allows (so 2 for two-player games, 4 for
  Spades/Whist/Euchre and so on, 5 for Resistance, 7 for Diplomacy, 1 for Mastermind).
- Three agent set-ups, with the same agent in every seat:
  - **Random**: `agent=random`
  - **MCTS (IS)**: `MCTSPlayer` with default parameters (so `information: Information_Set`,
    `rolloutLength: 1000`), with `budget=500` FM calls
  - **MCTS (OL)**: `MCTSPlayer` with default parameters except for those in this JSON file:
    ```json
    {
      "class" : "players.mcts.MCTSParams",
      "information" : "Open_Loop",
      "budgetType" : "BUDGET_FM_CALLS",
      "budget" : 1000,
      "rolloutLength" : 30
    }
    ```
- Each set-up was checked within one JVM (`runs=2`) and across JVM launches (`launches=2`).

Example: `SeedRepeatabilityTester game=Catan nPlayers=3 seed=42 agent=random launches=2`

## Summary

- **Random agents**: all 67 games are repeatable, within one JVM and across launches.
- **MCTS (IS and OL)**: all games are repeatable, except that Descent2e did not finish within the
  20-minute limit with either MCTS set-up, so is unchecked.

## Results

| Game | Players | Random | MCTS (IS) | MCTS (OL) |
|------|--------:|:------:|:---------:|:---------:|
| Agram | 3 | Yes | Yes | Yes |
| Backgammon | 2 | Yes | Yes | Yes |
| Battlelore | 2 | Yes | Yes | Yes |
| Blackjack | 3 | Yes | Yes | Yes |
| CantStop | 3 | Yes | Yes | Yes |
| Catan | 3 | Yes | Yes | Yes |
| Chess | 2 | Yes | Yes | Yes |
| ChineseCheckers | 3 | Yes | Yes | Yes |
| ColtExpress | 3 | Yes | Yes | Yes |
| Connect4 | 2 | Yes | Yes | Yes |
| CrazyEights | 3 | Yes | Yes | Yes |
| Cribbage | 2 | Yes | Yes | Yes |
| Cuckoo | 4 | Yes | Yes | Yes |
| Descent2e | 3 | Yes | timeout | timeout |
| Diamant | 3 | Yes | Yes | Yes |
| Diplomacy | 7 | Yes | Yes | Yes |
| Dominion | 3 | Yes | Yes | Yes |
| DominionFG | 3 | Yes | Yes | Yes |
| DominionImprovements | 3 | Yes | Yes | Yes |
| DominionSizeDistortion | 3 | Yes | Yes | Yes |
| DotsAndBoxes | 3 | Yes | Yes | Yes |
| Euchre | 4 | Yes | Yes | Yes |
| ExplodingKittens | 3 | Yes | Yes | Yes |
| GoFish | 3 | Yes | Yes | Yes |
| GolfSix | 3 | Yes | Yes | Yes |
| Goofspiel | 3 | Yes | Yes | Yes |
| Hanabi | 3 | Yes | Yes | Yes |
| HareAndTortoise | 3 | Yes | Yes | Yes |
| Hearts | 3 | Yes | Yes | Yes |
| Klaverjassen | 4 | Yes | Yes | Yes |
| LawnAndOrder | 3 | Yes | Yes | Yes |
| LeducPoker | 2 | Yes | Yes | Yes |
| LoveLetter | 3 | Yes | Yes | Yes |
| Mastermind | 1 | Yes | Yes | Yes |
| Monopoly | 3 | Yes | Yes | Yes |
| MonopolyDeal | 3 | Yes | Yes | Yes |
| Pandemic | 3 | Yes | Yes | Yes |
| PenteGrammai | 2 | Yes | Yes | Yes |
| Pickomino | 3 | Yes | Yes | Yes |
| Pitch | 4 | Yes | Yes | Yes |
| Poker | 3 | Yes | Yes | Yes |
| PowerGrid | 3 | Yes | Yes | Yes |
| President | 4 | Yes | Yes | Yes |
| PuertoRico | 3 | Yes | Yes | Yes |
| Resistance | 5 | Yes | Yes | Yes |
| Risk | 3 | Yes | Yes | Yes |
| Root | 3 | Yes | Yes | Yes |
| Rummy | 3 | Yes | Yes | Yes |
| Saboteur | 3 | Yes | Yes | Yes |
| Scarto | 3 | Yes | Yes | Yes |
| Schwimmen | 3 | Yes | Yes | Yes |
| Scopa | 2 | Yes | Yes | Yes |
| SeaSaltPaper | 3 | Yes | Yes | Yes |
| Skitgubbe | 3 | Yes | Yes | Yes |
| Spades | 4 | Yes | Yes | Yes |
| Stratego | 2 | Yes | Yes | Yes |
| Sueca | 4 | Yes | Yes | Yes |
| SushiGo | 3 | Yes | Yes | Yes |
| TerraformingMars | 3 | Yes | Yes | Yes |
| TicTacToe | 2 | Yes | Yes | Yes |
| Uno | 3 | Yes | Yes | Yes |
| Ventlife | 3 | Yes | Yes | Yes |
| Virus | 3 | Yes | Yes | Yes |
| WarOfTheToads | 2 | Yes | Yes | Yes |
| Whist | 4 | Yes | Yes | Yes |
| Wonders7 | 3 | Yes | Yes | Yes |
| XIIScripta | 2 | Yes | Yes | Yes |
