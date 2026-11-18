## Whist

Four players play in two fixed teams, with partners sitting opposite each other. Team 0 is players 0 and 2, and team 1 is players 1 and 3.

**The deal.** All 52 cards are dealt one at a time, starting on the dealer's left, so each player holds 13. Aces are high.

<!-- if trumpMode == TURN_UP -->
**Trumps.** The dealer's last card is turned face up, and its suit is trumps. The dealer keeps the card, and may play it like any other card.
<!-- else -->
**Trumps.** Trumps change with each deal, in the order Hearts, Diamonds, Spades, Clubs<!-- if noTrumpsInRotation -->, and then a deal with no trumps<!-- end -->. The order then repeats.
<!-- end -->

**Play.** The player on the dealer's left leads to the first trick with any card.

- Each player in turn must follow the suit led if they can. A player who cannot follow may play any card.
- The highest trump wins the trick. If no trump was played, the highest card of the suit led wins.
- The winner of a trick leads the next one.

**Scoring.** After the 13 tricks, a team that won more than six scores one point for each trick over six. The deal then passes to the left.

**Winning.** <!-- if nDeals == 1 -->The game is a single deal. The team with more points wins.<!-- else -->After {nDeals} deals the team with more points wins.<!-- end --> Teams with the same points draw.

### Interface

Choose a card to play from the action buttons at the bottom. Player 0 sits at the bottom of the table, and play goes clockwise. Each player's area shows their hand, the number of cards in it, their team and the tricks they have taken. "Void in" lists the suits a player is known to hold none of, because they did not follow that suit in this deal. The title under the area adds "dealer" for the dealer and "led" for the player who led the current trick. The current player's area has a blue border.

The centre shows the deal and trick numbers, what is trumps, the suit led, and the trick so far. The card winning the trick is outlined in orange. Below the trick are the tricks each team has taken in this deal, and below that the points of each team.
