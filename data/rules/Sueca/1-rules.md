## Sueca

Four players play in two teams. Team 0 is players 0 and 2, and team 1 is players 1 and 3. Partners sit opposite each other.

**The deal.** The pack has 40 cards (a standard pack without the Eights, Nines and Tens). Each player is dealt {handSize} cards. The dealer's last card is turned face up and sets trumps. The dealer keeps it in their hand, and everyone knows they hold it until they play it. The player after the dealer leads the first trick.

**Tricks.** In every suit the cards rank A 7 K J Q 6 5 4 3 2.

- Follow the suit led if you can. Otherwise play any card.
- The highest trump wins the trick. If no trump was played, the highest card of the suit led wins.
- The winner leads the next trick.

**Card points.** The cards a team wins in tricks score card points, 120 in all. The other cards score nothing.

| Card | Ace | Seven | King | Jack | Queen |
| :-- | :-: | :-: | :-: | :-: | :-: |
| **Card points** | 11 | 10 | 4 | 3 | 2 |

<!-- if playRubber -->
**Winning.** The game is a rubber of several deals. After each deal the team with more than 60 card points scores games:

| Deal won with | Games |
| :-- | :-: |
| 61 to 90 card points | 1 |
| 91 or more card points | 2 |
| every trick | 4 |

After a deal tied at 60 card points each, nobody scores, and the next deal is worth one more game to the team that wins it. Each further tie adds another game. The deal passes to the next player. The first team to {targetGames} games wins.
<!-- else -->
**Winning.** The game is a single deal. The team with more card points wins, and 60 card points each is a draw.
<!-- end -->

### Interface

Player 0 sits at the bottom, and the others follow clockwise. Each player's area is titled with the player's number and agent, and with "dealer" for the dealer. The current player's area has a blue border. The line under a player's cards shows their number of cards, their team, the trump card while the dealer holds it, and the suits the player is known to be void in (from failing to follow suit).

The centre shows the trick so far. Each card is labelled with the player who played it, and "(led)" marks the first. The card winning the trick so far has an orange outline. Above the trick, one line shows <!-- if playRubber -->the deal number, <!-- end -->the trick number and the trump card, and the next shows the suit led (or the player to lead). The line below the trick shows each team's card points and tricks in this deal.<!-- if playRubber --> The bottom line shows each team's games in the rubber.<!-- end -->


Each action button plays one card from your hand.
