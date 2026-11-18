## Skitgubbe

In phase one you win cards in tricks. In phase two you try to get rid of them. The last player holding cards loses (the skitgubbe).

**Cards.** The pack has 52 cards, and Ace is high.

### Phase one

Each player is dealt {handSize} cards, and the rest form the draw deck. Each trick is played by two players, the leader and the player on their left. Suits do not matter.

- Play any card from your hand, or turn up the top card of the draw deck and play it. You may turn up a card while the draw deck holds more than one card.
- After playing from your hand you draw a card, to keep {handSize} in hand while the draw deck lasts.
- The higher card wins the trick. The winner takes both cards face up, and leads the next trick.
- Cards of the same rank bounce. They are set aside, and the same two players play again. The winner of the next trick will take the bounced cards too.

The last card of the draw deck is the trump card. The player who draws it keeps it face down, apart from their hand.

Phase one ends when the player due to play has no cards. A card led and not answered goes back to its leader. Each player then adds their hand and their bounced cards to the cards they won, and the player who drew the trump card adds it too. The trump card's suit is trumps.

### Phase two

The player who drew the trump card leads first.

- The leader plays any card.
- Each later player must beat the top card of the trick or pick it up. A card beats it if it is a higher card of the same suit, or a trump when the top card is not a trump. You may pick up even when you could beat.
- A trick is complete when it holds as many cards as there were players holding cards when it began. Its cards are then discarded.
<!-- if completerLeads -->
  The player who completed it leads the next trick. If they are out, the next player holding cards leads.
<!-- else -->
  The next player holding cards leads the next trick.
<!-- end -->
- If every card of a trick is picked up, the next player leads.

### Scoring

A player who plays their last card is out. They score the number of players who were holding cards when that trick began. A player with no cards at the start of phase two scores as if out in its first trick. The game ends as soon as only one player holds cards, and that player scores 0. The highest score wins. <!-- if exitOrderTiebreak -->Of players with the same score, the one who went out first ranks higher.<!-- else -->Players with the same score share their place.<!-- end --> The game also ends after {maxPhaseTwoActions} actions in phase two, and every player still holding cards then scores 0.

### Interface

The players are in a column on the left. Each title shows the player's number and agent, the cards won in phase one or the cards to get rid of in phase two, "OUT" and the score once out, and "holds the trump card" for the player who drew it. In phase one a player's hand is above the cards they have won. The player to act has a blue border.

The table shows the draw deck, the trump card, the discarded cards in phase two, and the trick. The trump card is face down except to the player who drew it, and in phase two its suit is shown in its place. In phase one the led card is labelled with its leader (P0, P1 and so on) until it is answered. In phase two the top card is the brightest, and it is the one to beat. The bounced cards are shown beside the number of the player who played them. The lines below say whose turn it is and what they are to do, how many cards the trick needs, and the players who are out with their scores.

The action buttons are Play (a card), Turn up the top card of the draw deck, and Pick up.
