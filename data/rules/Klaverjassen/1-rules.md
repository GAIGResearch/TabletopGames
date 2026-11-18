## Klaverjassen

Players 0 and 2 (team 0) play against players 1 and 3 (team 1). Partners sit opposite each other. The game lasts {nHands} <!-- if nHands == 1 -->hand<!-- else -->hands<!-- end -->.

**The deal.** The 32-card pack (Seven to Ace in each suit) is shuffled, and each player is dealt {handSize} cards. The player on the dealer's left must choose trumps after seeing their hand, and then leads the first trick. The deal passes to the left after each hand.

**Cards.** The cards of each suit rank from high to low as below, with their points.

| Trumps      | J  | 9  | A  | 10 | K | Q | 8 | 7 |
|:------------|:--:|:--:|:--:|:--:|:-:|:-:|:-:|:-:|
| Points      | 20 | 14 | 11 | 10 | 4 | 3 | 0 | 0 |
| Other suits | A  | 10 | K  | Q  | J | 9 | 8 | 7 |
| Points      | 11 | 10 | 4  | 3  | 2 | 0 | 0 | 0 |

The team that wins the last trick of a hand scores {lastTrickBonus} points more, so each hand has {152 + lastTrickBonus} points in all.

**Play.** Any card may be led. The highest trump in a trick wins it. If the trick holds no trump, the highest card of the suit led wins. The winner leads the next trick.

- If a suit other than trumps is led, you must follow suit if you can.
- If trumps are led, you must play a trump higher than every trump in the trick if you can. If you hold only lower trumps, you must play one of them.
- If you cannot follow suit and an opponent is winning the trick, you must play a trump that beats the winning card if you can. If you cannot, you must play a card that is not a trump. You may play a lower trump only if you hold nothing else.
- If you cannot follow suit and your partner is winning the trick, you may play any card. If your partner is winning with a trump, <!-- if partnerTrumpRule == NO_UNDERTRUMP -->you may play a card that is not a trump, or a trump higher than your partner's. You may play a lower trump only if you hold nothing else.<!-- else -->you must play a card that is not a trump if you hold one.<!-- end -->

**Roem.** The team that wins a trick scores roem (bonus points) for the cards in it.

| | |
|:--|--:|
| A run of three in one suit, in the order 7 8 9 10 J Q K A | {runOfThreeBonus} |
| A run of four | {runOfFourBonus} |
| The King and Queen of trumps (stuk), as well as any run | {stukBonus} |
| Four Tens, Queens, Kings or Aces | {fourOfAKindBonus} |
| Four Jacks | {fourJacksBonus} |

A team that wins every trick of a hand scores {pitBonus} roem more.

**Scoring a hand.** Each team's points for the hand are its card points plus its roem. If the team that chose trumps has <!-- if tieIsFailure -->the same or fewer points<!-- else -->fewer points<!-- end --> than the other team, it scores nothing, and the other team scores the points of both teams. Otherwise each team scores its own points.

**Winning.** After the last hand the team with the higher score wins. Teams with the same score draw.

### Interface

Choose trumps or a card to play from the action buttons at the bottom. Player 0 sits at the bottom, with players 1, 2 and 3 to the left, top and right. Below each player's cards are the number of cards they hold, their team, and the suits they are known to be void in. The player to act has a blue border. The title of the dealer shows "dealer", and the title of the player who chose trumps shows "chose trumps".

The centre panel shows the hand and trick number, the trump suit and the team that chose it, and the suit led. Below them are the cards of the trick, each with the player who played it. The card winning the trick is outlined in orange. The bottom line gives each team's card points, roem and tricks in this hand. The score of each team is shown under the panel.
