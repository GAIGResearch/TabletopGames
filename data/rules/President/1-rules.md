## President

The aim is to be the first to play all your cards. The whole 52-card pack is dealt. Suits do not matter. The ranks from high to low are 2 A K Q J 10 9 8 7 6 5 4 3.

### Play

A set is one or more cards of the same rank. Player 0 leads the first trick.

- The leader plays any set.
- Each following player either passes or plays a set of the same number of cards and a higher rank.
- A player who has passed may still play later in the same trick.
- Players who are out are skipped.
- When all the other players still holding cards have passed since the last set, the trick is discarded. The player who played that set leads the next trick. If they are out, the next player holding cards leads.

### Scoring

The deal ends when only one player holds cards, and that player is the Scum. Points go by the order in which the players went out:

| Place | Points |
| :-- | :-: |
| 1st (President) | {presidentPoints} |
| 2nd (Vice-President) | {vicePresidentPoints} |
| Others | 0 |

<!-- if targetScore <= 1 -->
The game is a single deal, and the player with the most points wins.
<!-- else -->
Deals continue until a player has {targetScore} points or more. The highest score then wins. Players with equal scores are ranked by the order in which they went out in the last deal.
<!-- if exchangeCards == 0 -->

### Later deals

Each later deal is dealt starting with the President of the last deal, who leads the first trick.
<!-- else -->

### The exchange

Each later deal is dealt starting with the President of the last deal. The Scum of the last deal then gives the President their {exchangeCards} highest <!-- if exchangeCards == 1 -->card<!-- else -->cards<!-- end -->. The President gives back any {exchangeCards} <!-- if exchangeCards == 1 -->card<!-- else -->cards<!-- end -->, and leads the first trick.
<!-- end -->
<!-- end -->

### Interface

The hands are on the left, in turn order from the top, with the lowest rank on the left. A hand you cannot see is face down, with its number of cards. The title of each hand gives the player's score, and their place once they are out. The player to act has a blue border.

The current trick is on the right. The set to beat is the brightest, and the earlier sets are dimmed. The lines below the trick give the set to beat, the passes since it was played, the players out this deal and the scores.

The action buttons are Play, Pass and (in the exchange) Give. For example, Play 2 x 7 plays a pair of Sevens.
