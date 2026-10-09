## Euchre

Four players play in two fixed teams, with partners sitting opposite each other. Team 0 is players 0 and 2, and team 1 is players 1 and 3.

**The deal.** The deck has {4 * (15 - lowestCard)} cards, from {lowestCard} to Ace in each suit. Each player is dealt {handSize} cards. The other {4 * (15 - lowestCard) - 4 * handSize} go face down to the kitty, and its top card is turned face up as the up-card.

**Choosing trumps.** Starting on the dealer's left, each player in turn chooses Pass or calls the up-card's suit as trumps. After a call the dealer takes the up-card and discards any card face down. If all four pass, the up-card is turned down. Each player in turn then chooses Pass or calls any other suit. The dealer may not pass in this second round.

**Going alone.** The player who calls trumps is the maker. A maker who calls alone plays without their partner, who sits out the deal. <!-- if sittingOutDealerPicksUp -->If that partner is the dealer, the dealer still takes the up-card and discards<!-- else -->If that partner is the dealer, the up-card stays in the kitty<!-- end -->.

**Card order.** The Jack of trumps (the right bower) is the highest trump. The other Jack of the same colour (the left bower) is the next highest, and belongs to the trump suit, not its own. The rest of the trumps follow in the order A, K, Q, 10, 9. The other suits run A, K, Q, J, 10, 9.

**Play.** The player on the dealer's left leads the first trick. If the maker is alone, the player on the maker's left leads instead.

- Each player in turn must follow the suit led if they can. A player who cannot follow may play any card.
- The highest trump wins the trick. If no trump was played, the highest card of the suit led wins.
- The winner of a trick leads the next one.

**Scoring.** After the {handSize} tricks, one team scores points.

| Tricks taken by the makers | Points |
| :--- | :---: |
| {handSize / 2 + 1} to {handSize - 1} | {pointsMade} to the makers |
| All {handSize} (a march) | {pointsMarch} to the makers |
| All {handSize}, by a maker going alone | {pointsAloneMarch} to the makers |
| Fewer than {handSize / 2 + 1} (euchred) | {pointsEuchred} to the defenders |

**Winning.** The deal passes to the left after each deal. The game ends after the deal in which a team reaches {targetScore} <!-- if targetScore == 1 -->point<!-- else -->points<!-- end -->, and that team wins.

### Interface

Choose from the action buttons at the bottom. Pass, Call (a suit) and Call (a suit) alone choose trumps. Discard is the dealer's discard, and Play plays a card. Player 0 sits at the bottom of the table, and play goes clockwise.

Each player's area shows their hand, the number of cards in it, their team and the tricks they have taken, or "sitting out". "Void in" lists the suits a player is known to hold none of, because they did not follow that suit in this deal (the left bower counts as a trump). The title under the area adds "dealer" and "maker". The current player's area has a blue border.

In the centre, the up-card is shown with what became of it (on offer as trumps, turned down, taken by the dealer, or left in the kitty). Beside it are what is trumps and who made them, the suit led, and the trick so far. The card winning the trick is outlined in orange. Below the trick are the tricks each team has taken in this deal, and below that the points of each team and the target.
