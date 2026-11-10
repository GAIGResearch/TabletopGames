## Blackjack

Each player plays against the dealer, not against the other players. Each player starts with {startingChips} chips. The game lasts {nHands} <!-- if nHands == 1 -->hand<!-- else -->hands<!-- end -->, played with one deck that is shuffled before each hand.

**Card values.** Court cards count 10. An Ace counts 11 (and the hand is then soft) unless that would take the total over 21, when it counts 1. A hand over 21 is bust and loses. A natural (Blackjack) is an Ace and a ten-value card as the first two cards, not after a split.

**Each hand.**

1. Each player bets an even number of chips from {minBet} to {maxBet}. A player with fewer than {minBet} chips sits the hand out.
1. Each player is dealt two cards face up. The dealer is dealt one card face up and one face down (the hole card).
1. If the dealer's up card is an Ace or a ten-value card, each player may buy insurance for half their bet.
1. If the dealer has Blackjack, the hand ends. A player with a natural gets the bet back, and every other bet is lost.
<!-- if payout21NaturalOnly -->
1. Otherwise insurance is lost, and each natural is paid at once.
<!-- else -->
1. Otherwise insurance is lost.
<!-- end -->
1. Each player in turn plays their hand.
1. The dealer turns the hole card up and draws until the total is 17 or more<!-- if dealerHitsSoft17 -->, and also draws on a soft 17<!-- else -->, and stands on a soft 17<!-- end -->.
1. A hand wins if its total is higher than the dealer's or the dealer is bust.

**Playing a hand.**

- **Hit** takes a card. The hand ends if it goes bust.
- **Stand** ends the hand.
<!-- if doubleDown -->
- **Double down** doubles the bet on the first two cards, takes one more card and ends the hand. It is not allowed on a natural or after a split.
<!-- end -->
<!-- if splitting -->
- **Split** makes a pair of the same rank into two hands, each with the same bet and a second card. A player may have up to {maxHandsAfterSplit} hands. Split Aces get one card each and end.
<!-- end -->
<!-- if doubleDown and splitting -->

Double down and Split need the chips to match the bet.
<!-- elif doubleDown -->

Double down needs the chips to match the bet.
<!-- elif splitting -->

Split needs the chips to match the bet.
<!-- end -->

**Payouts.**

| Result | Paid |
| :--- | :--- |
| <!-- if payout21NaturalOnly -->Natural<!-- else -->Winning hand of 21<!-- end --> | {payout21} : 1 (rounded down) |
| Other winning hand | 1 : 1 |
| Push (equal totals) | bet returned |
| Insurance, when the dealer has Blackjack | 2 : 1 |

**Winning.** The game ends after the last hand, or sooner if no player has {minBet} chips. A player with more chips than at the start wins, and one with the same number draws.

### Interface

Choose from the action buttons at the bottom. The dealer's area shows the dealer's cards and total ("showing" counts the up card only), the hand number, the stage of the hand and the cards left in the draw deck. Each player's area shows their chips and any insurance, and under each hand its total ("Blackjack!" for a natural) and bet. "(sitting out)" marks a player with no bet this hand.

A blue outline shows whose turn it is, and the hand being played is outlined in yellow. At the end the result is shown after each player's name.
