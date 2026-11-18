## Schwimmen

Collect {handSize} cards of one suit with the highest total you can.

**Cards.** The pack has 32 cards, Seven to Ace. An Ace counts 11, and a King, Queen or Jack counts 10. A hand is worth its best single-suit total, or one of these values:

| | |
| :-- | :-: |
| Three of a kind | {threeOfAKindValue} |
| Three Aces (Feuer) | {threeAcesValue} |

**The deal.** Each player is dealt {handSize} cards, and an extra hand of {handSize} is dealt face down. The dealer keeps their hand or takes the extra hand without looking at it. The {handSize} cards not chosen go face up on the table.

**Each turn** goes to the next player on the left, starting on the dealer's left.

1. Exchange one hand card for one table card, exchange your whole hand with the table, or pass.
2. If nobody has closed yet, you may close. Each other player will then have one more turn, and the deal will end.

When every player passes in a row, the table cards are discarded and {handSize} new ones are dealt from the draw deck. If the draw deck has fewer than {handSize} cards, the deal ends instead.

A hand of {schnauzTotal} in one suit (Schnauz) or three Aces (Feuer) ends the deal at once. A deal also ends when each player has had {maxCircuitsPerDeal} turns.

<!-- if livesGame -->
**Chips.** Each player starts with {startingChips} chips. At the end of a deal the player with the worst hand loses a chip. After Feuer every other player loses a chip instead. A player with no chips is swimming, and a swimming player who loses again is out. The deal passes to the left. The last player in wins. If more than one player is still in after {maxDeals} deals, the most chips wins.
<!-- else -->
**Winning.** The game is a single deal, and the best hand wins.
<!-- end -->

**Ties.** Of two hands with the same value, the higher three of a kind is better (Ace high). Otherwise the hand whose best total is in the higher suit is better. The suits from the highest are Clubs, Spades, Hearts and Diamonds. Hands that are still equal share the place<!-- if livesGame -->, and all of them lose a chip if they are the worst<!-- end -->.

### Interface

The hands are drawn round the table. Each is titled with the player's number and agent, and with "dealer" for the dealer. The current player's hand has a blue border. A gold dot marks a hand card that every player saw taken from the table. The line under a hand shows <!-- if livesGame -->the player's chips ("swimming" at none, "out" once out), <!-- end -->the hand's value once all its cards are face up, and "closed" for the player who closed.

The centre shows the table (or the extra hand, face down, while the dealer chooses), the draw deck and the discard pile, each with its number of cards. Beside them are <!-- if livesGame -->the deal number, <!-- end -->the passes in a row and what the current player is to do.

The dealer chooses with Keep hand or Take the extra hand. A turn is one of the Exchange buttons (a hand card for a table card), Exchange all or Pass. While nobody has closed, it is followed by Close or Do not close.
