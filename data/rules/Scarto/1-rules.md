## Scarto

Scarto is a trick-taking game for three players with a 78-card tarot pack. The aim is to capture the cards that score points.

**The pack.** Each of the four suits has a King, a Queen, a Cavalier (C), a Knave (J) and the pips 1 to 10. Swords and Batons are the long suits, and Cups and Coins the round suits. There are also 21 trumps and the Fool.

| Cards | High to low |
| :-- | :-- |
| Long suits | K Q C J 10 9 ... 1 |
| Round suits | K Q C J 1 2 ... 10 |
| Trumps | Angel (20), World (21), 19 18 ... Pagat (1) |

**The deal.** Each player is dealt {handSize} cards. The last {78 - 3 * handSize} cards form the scarto, which will score for the dealer.

<!-- if dealerExchange -->
**The exchange.** The dealer takes the scarto into their hand, and then discards {78 - 3 * handSize} cards face down to form a new scarto. The dealer may not discard a King, the Angel or the Fool. The Pagat may be discarded only if it is the dealer's only trump, with the Fool counted as a trump.

<!-- end -->
### Play

- The player after the dealer leads the first trick. The winner of each trick leads the next.
- You must follow the suit led if you can. If you cannot, you must play a trump if you can. Otherwise you may play any card.
- The Fool may be played at any time instead. If the Fool is led, the next card played sets the suit.
- The highest trump wins the trick. If no trump was played, the highest card of the suit led wins.
- The Fool cannot win a trick. It goes back to the pile of the player who played it, and the winner takes the other two cards.

### Scoring

At the end of a deal each player scores the points of the cards in their pile:

| Card | Points |
| :-- | :-: |
| King | {kingPoints} |
| Pagat, Angel | {honourTrumpPoints} |
| Queen | {queenPoints} |
| Fool | {foolPoints} |
| Cavalier | {cavalierPoints} |
| Knave | {knavePoints} |

Each pile also scores 1 point for every 3 cards in it, counted as (cards + 1) / 3 rounded down. The dealer's pile includes the scarto. There are {pointsPerDeal} points in a deal.

<!-- if nDeals == 1 -->
The game is a single deal. The highest score wins, and players with equal scores share the win.
<!-- else -->
The game has {nDeals} deals. The deal passes to the next player each time, and the scores are added up. The highest total wins, and players with equal totals share the win.
<!-- end -->

### Interface

Player 0 is at the bottom, and players 1 and 2 are across the top. A hand is sorted with the trumps first, then the Fool, then Swords, Batons, Cups and Coins, highest first. The player to act has a blue border. The title under each hand marks the dealer and the player who leads.

The line under each hand gives the number of cards held, the cards won and the points this deal. It also lists the suits the player is known to be void in (Sw, Ba, Cu, Co, Tr).

The centre shows the deal and trick number, the trick so far with the winning card outlined, and what must be played. The line below the trick gives the size of the scarto. After the exchange the dealer also sees the cards in it. The scores under the centre include earlier deals.

Choose a card from the action buttons at the bottom of the screen. In the exchange the buttons are Discard, and in play they are Play.
