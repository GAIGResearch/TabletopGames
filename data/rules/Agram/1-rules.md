## Agram

A trick-taking game from West Africa. The winner of the last trick wins the deal.

**Cards.** The deck has 35 cards: Ace and 10 down to 3 in each suit, without the Ace of Spades. Ace is high.

**Deal.** Each player is dealt {nCardsPerPlayer} cards, so there are {nCardsPerPlayer} tricks. The cards left over are not used.

**Play.**

- The player after the dealer leads the first trick with any card.
- The other players must follow suit if they can, but need not play higher.
- A player who cannot follow suit may play any card. The others will then know that player has none of that suit.
- The highest card of the suit led wins the trick. There are no trumps.
- The winner of a trick leads the next.

**Winning.** The winner of trick {nCardsPerPlayer} wins the deal. The earlier tricks score nothing.
<!-- if nDeals > 1 -->

**Match.** A match is {nDeals} deals. The winner of each deal deals the next, so the player after them leads. The player who has won most deals wins the match, and players with the same number of deals share the place.
<!-- end -->

### Interface

Choose a card from the action buttons at the bottom. Each player's area shows their hand, with a status line underneath: the number of cards, <!-- if nDeals > 1 -->the deals won, <!-- end -->and the suits the player is known to be void in ("void in"). The name below the area is marked "dealer" for the dealer, and a blue outline shows whose turn it is.

The centre shows the trick number, the suit led and the cards played so far, each labelled with the player who played it. The card winning the trick is outlined in orange. The bottom line counts the tricks played this deal and the cards not dealt.
