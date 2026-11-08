## Cuckoo

Each player starts with {nLives} <!-- if nLives == 1 -->life<!-- else -->lives<!-- end -->. The aim is to avoid holding the lowest card.

**Each round** every player still in the game is dealt one card, which only they can see. Kings are high and Aces are low. Suits do not matter.

1. Players decide in turn, clockwise from the dealer's left. Each player chooses Keep card or Swap card.
1. Swap card exchanges your card with the card of the next player on your left. They must accept, unless they hold a King. In that case they show the King, and the swap is refused.
1. The dealer decides last. Swap card by the dealer exchanges their card for the top card of the draw deck. If that card is a King, the dealer keeps their own card.
1. All the cards are then shown. Every player holding the lowest card loses a life.

**Out.** A player with no lives left is out of the game. The deal passes to the next player on the left who is still in.

<!-- if maxDeals > 0 -->
**Winning.** The game ends after {maxDeals} <!-- if maxDeals == 1 -->round<!-- else -->rounds<!-- end -->, or earlier when only one player is left. The players with the most lives win.
<!-- else -->
**Winning.** The last player left wins. If all the players left lose their last life in the same round, they are joint winners.
<!-- end -->

### Interface

Choose Keep card or Swap card from the action buttons at the bottom. Player 0 sits at the bottom of the table, and play goes clockwise. Each seat shows the player's card and their lives (one ♥ for each life). The dealer's seat also shows "Dealer", and the seat of a player who is out shows the round they went out in. The current player's seat has a blue border.

A card is face up when you know it (your own card, a card you gave away, or a King shown to refuse a swap). All the cards are face up when the game is over. The centre of the table shows the draw deck, the round, the dealer, whose turn it is, and who a swap would be with.
