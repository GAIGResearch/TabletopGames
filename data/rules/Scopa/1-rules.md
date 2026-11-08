## Scopa

Two players capture cards from the table.

**Cards.** The pack has 40 cards in four suits (Swords, Batons, Cups and Coins). Each suit has Ace to 7, Knave, Cavalier and King.

| Card | Ace | 2 | 3 | 4 | 5 | 6 | 7 | Knave | Cavalier | King |
| :-- | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
| **Capture value** | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
| **Primiera value** | 16 | 12 | 13 | 14 | 15 | 18 | 21 | 10 | 10 | 10 |

**The deal.** {tableSize} cards are dealt face up to the table and {handSize} to each player.<!-- if redealOnKings --> If three or more Kings are on the table, the cards are dealt again.<!-- end --> When both hands are empty, {handSize} more cards are dealt to each player (none to the table), until the draw deck is empty. The player who did not deal plays first.

**Each turn** you play one card from your hand.

- If a table card has the same rank, your card captures it (one of them, if there are several).
- Otherwise your card captures a set of two or more table cards whose capture values add up to its own.
- A card that can capture must capture. A card that cannot is added to the table.
- A capture that clears the table is a scopa, unless it is made with the last card of the deal.

At the end of the deal the cards left on the table go to the last player to capture.

**Scoring.** At the end of each deal a player scores 1 point for each of these:

- each scopa
- more captured cards than the other player
- more Coins than the other player
- the 7 of Coins
- a higher primiera than the other player

The primiera is the total of the primiera values of your best card in each suit. It is 0 unless you have captured a card of every suit. Equal counts score nothing.

<!-- if targetScore > 0 -->
**Winning.** Each deal's points are added to the players' totals, and the deal passes to the other player. The game ends after a deal when one player has {targetScore} or more points and more than the other. That player wins.
<!-- else -->
**Winning.** The game is a single deal, and the higher score wins. Equal scores are a draw.
<!-- end -->

### Interface

Player 1 sits at the top and player 0 at the bottom. Each player's hand is on the left, titled with the player's number and agent, and with "dealer" for the dealer. The current player's hand has a blue border. Beside it are the cards the player has captured, with a line showing their number of cards, their Coins, whether they include the 7 of Coins, their primiera and the player's scopas.

The table cards are in the middle. On a card, J is the Knave, C the Cavalier and K the King, and the suits are Sw, Ba, Cu and Co. The line above the table shows <!-- if targetScore > 0 -->the deal number and target, and <!-- end -->the player to play. The line below it shows the number of cards in the draw deck, the last player to capture and the scores. A score counts the deal so far<!-- if targetScore > 0 -->, and the points banked from earlier deals are shown beside it<!-- end -->.

Each action button plays a card, either to the table or capturing the cards it names.
