## Crazy Eights

Be the first player to get rid of all your cards.

**Deal.** Each player is dealt {nCardsPerPlayer} cards ({nCardsPerPlayerTwoPlayers} cards with two players). The last player is the dealer. The next card is turned up to start the discards (the starter), and its suit is the suit to match. <!-- if dealerNominatesStarterSuit -->If the starter is an Eight, the dealer nominates the suit to match before play starts.<!-- else -->If the starter is an Eight, the suit to match is {starterEightSuit}.<!-- end --> Player 0 plays first.

**Each turn.**

- Play a card of the suit to match, or of the same rank as the top discard. Its suit becomes the suit to match.
- An Eight can always be played, and the player nominates the next suit to match.
- A player who can play must play. A player who cannot play draws one card, and the turn ends.
- When the stock is empty, the discards except the top card are shuffled to make a new stock. If there is nothing to draw, the player passes.

**End.** The first player with no cards wins. If every player passes in a row, the game is blocked, and every player with the fewest cards wins.

**Penalty.** When a player goes out, the other players are ranked by the penalty points for the cards left in their hands (fewest first):

| | |
| :--- | ---: |
| Eight | {eightPenalty} |
| Jack, Queen, King | {pictureCardPenalty} |
| Ace | {acePenalty} |
| 2 to 10 | face value |

### Interface

Choose from the action buttons at the bottom. An Eight has one button for each suit it can nominate ("nominating Spades"). The button that names no suit keeps the Eight's own suit. The centre shows the Stock, the Discards and the Suit to match, with the number of passes in a row. Each player's area shows their hand and the number of cards, and the penalty when the hand is shown. The dealer is marked "dealer" below their area, and a blue outline shows whose turn it is.
