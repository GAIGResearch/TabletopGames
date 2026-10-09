## Six-card Golf

The aim is to score as few points as possible with the cards in your grid.

**The deal.** Each player is dealt 6 cards face down, in a grid of 3 columns and two rows. Nobody may look at a face-down card, including its owner. The top card of the draw deck is turned face up to start the discard pile.
<!-- if initialFaceUp != 0 -->

**Before play** each player in turn turns {initialFaceUp} of their cards face up.
<!-- end -->

**Each turn** has two steps.

1. Draw from the draw deck, or draw from the discard pile.
1. Put the drawn card face up in your grid in place of any card, face up or face down. The card it replaces goes face up on the discard pile. A card drawn from the draw deck may instead be discarded. A card drawn from the discard pile must be placed in the grid.

When the draw deck runs out, the discard pile except its top card is shuffled to form a new draw deck.

**End of a deal.** <!-- if finalTurns -->When all of a player's cards are face up, each other player will have one more turn. The deal is then scored.<!-- else -->The deal is scored as soon as all of a player's cards are face up.<!-- end --> A deal is also scored once each player has had {maxTurnsPerPlayer} turns. All the cards are turned face up and scored.

| Card | Ace | Two | Jack, Queen | King |
| :--- | :---: | :---: | :---: | :---: |
| **Points** | {aceValue} | {twoValue} | {courtValue} | {kingValue} |

The other cards score their number. Two cards of the same rank in a column score nothing (a pair of Twos included).

**Winning.** <!-- if nDeals == 1 -->The game is a single deal. The lowest score wins.<!-- else -->The game is {nDeals} deals, and the deal passes to the next player each time. The lowest total wins.<!-- end --> Players with the same lowest score draw.

### Interface

Choose from the action buttons at the bottom. Turn up position, Draw from draw deck, Draw from discard pile, Replace position and Discard drawn card are the steps above. The number on each card is its position, from 0 to 2 along the top row and 3 to 5 along the bottom row.

Player 0's grid is at the bottom left, and play goes round the table in the order of the player numbers. Under each grid are the points its face-up cards show<!-- if nDeals > 1 -->, and the player's total from earlier deals<!-- end -->. The current player's grid has a blue border. The centre shows the draw deck and the discard pile (with the number of cards in each), the drawn card, the dealer, and what the current player is to do. A card drawn from the draw deck is face up only to the player who drew it.
