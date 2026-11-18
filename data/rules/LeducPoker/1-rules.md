## Leduc Poker

A two-player poker game. Win chips from your opponent.

**Cards.** The deck has six cards, the Jack, Queen and King of Spades and of Hearts. The King is high, and cards of the same rank are equal.

**Each hand** starts with both players putting the ante in the pot. Each is then dealt one private card, face down.

| | |
|:--|--:|
| Ante | {ante} |
| Raise in betting round 1 | {firstRoundRaise} |
| Raise in betting round 2 | {secondRoundRaise} |
| Raises allowed in each betting round | {maxRaisesPerRound} |

**Betting.** There are two betting rounds. After the first, the top card of the draw deck is turned face up as the board card. The same player acts first in both rounds. On your turn you choose one of these actions:

- **Check** if you owe nothing, or **Call** to put in the chips that match your opponent's.
- **Bet** if you owe nothing, or **Raise**, to match your opponent and add the raise for the betting round. Once the raises allowed have been made, you can only call or fold.
- **Fold**, when you owe chips. Your opponent wins the chips you have put in the pot, and the hand ends.

A betting round ends when both players check, or when a bet or raise is called.

**Showdown.** After the second betting round the private cards are compared. A private card of the same rank as the board card (a pair) wins. If neither player has a pair, <!-- if highCardUsesBoard -->each player's card is the higher of their private card and the board card, and the higher card wins<!-- else -->the higher private card wins<!-- end -->. The winner wins the chips the loser put in the pot. If the cards are equal, no chips change hands.

**Winning.** <!-- if nHands == 1 -->The game is one hand.<!-- else -->The game lasts {nHands} hands, and the other player acts first in each new hand.<!-- end --> The player with more net chips wins. Players with the same net chips draw.

### Interface

Choose Fold, Check or Call, and Bet or Raise from the action buttons at the bottom. The number on Call, Bet and Raise is the chips you will put in the pot. Player 1 is at the top and player 0 at the bottom.

The middle of the table shows the Draw deck (with the number of cards in it), the Board card, the hand and betting round, the raise for the round, the Pot, the raises made this round, and how much the player to act must call.

Each player's area shows their private card (face down to the opponent until the game ends), "(acts first)" for the player who acts first in this hand, their chips In the pot this hand, and their Net chips over the game. The player to act is outlined in blue.
