## Go Fish

Collect books. A book is all four cards of a rank. The player with the most books wins, and players tied for the most books share first place.

**The deal.** Each player is dealt {startingHandSize} cards ({twoPlayerHandSize} each with 2 players). The rest form the draw deck. Player 0 asks first.

**Each turn** you ask another player for a rank that you hold. Asking shows everyone one of your cards of that rank.

- If they have any cards of that rank, <!-- if continueOnSuccess -->they must give you all of them, face up, and you ask again<!-- else -->they must give you all of them, face up, and the turn passes to the next player<!-- end -->.
- If they have none, they say Go fish, and you draw the top card of the draw deck. <!-- if continueOnDrawingSameRank -->If it is the rank you asked for, you show it and ask again. Otherwise the turn passes to the next player<!-- else -->The turn then passes to the next player<!-- end -->.

As soon as you hold all four cards of a rank, they are laid down as a book.

<!-- if playUntilAllBooks -->
**The end.** Play goes on after a hand or the draw deck is empty. A player whose turn starts with an empty hand draws a card, or is skipped if the draw deck is empty. You may ask only a player who holds cards. The game ends when the player to ask has nobody to ask.
<!-- else -->
**The end.** The game ends as soon as any player's hand or the draw deck is empty.
<!-- end -->

### Interface

The players' hands are in two rows, each titled with the player's number and agent. The current player's hand has a blue border. In a hidden hand, the cards shown to the table are face up. The line under a hand shows the number of cards, the ranks of the player's books, and the ranks the player is known not to hold ("has no"). A player is known not to hold a rank after saying Go fish or giving those cards away, until they next draw. J, Q, K and A stand for Jack, Queen, King and Ace.

The centre shows the draw deck with its number of cards, when the game will end, and the player to ask. Each action button is an ask, such as "Ask P1 for Kings". How to Play explains asking by clicking on the table.
