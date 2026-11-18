## Rummy

The aim is to get rid of your cards by forming melds. Each player is dealt a hand from a 52-card pack:

| Players | 2 | 3-4 | 5-6 |
| :-- | :-: | :-: | :-: |
| Cards | {cardsFor2Players} | {cardsFor3To4Players} | {cardsFor5To6Players} |

The next card starts the discard pile, face up, and the rest form the draw deck. Player 0 plays first.

### A turn

1. Draw the top card of the draw deck, or take the top card of the discard pile.
2. In any order, meld at most once and lay off any number of cards.
3. Discard one card, which ends the turn. A card taken from the discard pile this turn may be discarded only if it is your last card.

### Melds

A set is 3 or 4 cards of one rank. A run is 3 or more cards of one suit in sequence. Aces are low, so A-2-3 is a run but Q-K-A is not.

You may lay off a card onto any meld on the table, including another player's. A set takes a card of its rank. A run takes the card just below or just above it in its suit.

### End of a deal

A deal ends when a player's hand is empty, whether by melding, laying off or discarding. It also ends after the turn in which the draw deck runs out, or after {maxTurnsPerDeal} turns. Each card left in hand is worth its number in points, with an Ace worth 1 and a court card 10.

<!-- if targetScore == 0 -->
The game is a single deal. The player with the fewest points in hand wins, and players with equal points share the place.
<!-- else -->
At the end of each deal one player scores the points left in the other hands. That player is the one who went out or, if nobody did, the one player with the fewest points in hand. If the fewest points are tied, nobody scores. The next deal will be started by the next player in turn order. The game ends when a player's score reaches {targetScore}, and the highest score wins.
<!-- end -->

### Interface

The hands are on the left, in turn order from the top. The player to act has a blue border. A face-up hand is sorted by rank. A card taken from the discard pile is known to every player, so it has a gold outline and is shown face up even in a face-down hand.

On the right are the draw deck and the discard pile with their sizes, and the melds below them. The lines at the bottom give what the player to act must do, the card they took from the discard pile, and the turn number in this deal.

A card is written as \{Hearts 5}. The action button Lay off \{Hearts 5} below a run places the Five below the run whose lowest card is the Six of Hearts.
