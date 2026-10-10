## Pitch

Players 0 and 2 (team 0) play against players 1 and 3 (team 1). Each player is dealt {handSize} cards from a 52-card deck, and the other cards are not used. Aces are high. The deal passes to the left after each deal.

**Bidding.** There is one round of bidding, starting on the dealer's left. Each player passes, or bids from {minBid} to {smudgeBid} points. A bid must be higher than the highest bid so far. The dealer bids last, and may take the bid by equalling the highest bid. If everyone else has passed, the dealer must bid. The highest bidder is the pitcher.

**Play.** The pitcher leads to the first trick, and the suit of that card is trumps for the deal. If you hold a card of the suit led, you must play a card of that suit or a trump. Otherwise you may play any card. The highest trump wins the trick. If the trick holds no trump, the highest card of the suit led wins. The winner leads the next trick.

**Points.** At the end of the deal each team takes these points from the cards in the tricks it won.

| | |
|:--|:--|
| High | 1 for the highest trump played |
| Low | 1 for the lowest trump played |
| Jack | 1 for the Jack of trumps, if it was dealt |
| Game | 1 for the higher total of card values below (nobody scores it when the totals are equal) |

<!-- if countHighLowSeparately -->A card that is two or three of High, Low and Jack scores 1 for each of them.<!-- else -->A card that is two or three of High, Low and Jack scores only 1.<!-- end --> The card values for Game are:

| Card  | Ace            | King            | Queen            | Jack            | Ten            |
|:------|:--------------:|:---------------:|:----------------:|:---------------:|:--------------:|
| Value | {gameValueAce} | {gameValueKing} | {gameValueQueen} | {gameValueJack} | {gameValueTen} |

**Scoring a deal.** The other team scores its points. The pitching team scores its points if they reach its bid, and otherwise loses the value of its bid. A bid of {smudgeBid} is a smudge. The pitching team will then score {smudgePoints} if it wins every trick and all four points, and otherwise lose {smudgePoints}.

**Winning.** <!-- if targetScore <= 1 -->The game is a single deal. The team with the higher score wins, and teams with the same score draw.<!-- else -->Deals continue until the pitching team makes its bid and ends the deal with a score of {targetScore} or more. That team wins, whatever the other team's score.<!-- end -->

### Interface

Choose Pass, a Bid or a card to Play from the action buttons at the bottom. Player 0 sits at the bottom, with players 1, 2 and 3 to the left, top and right. Each seat's title gives the player's team, marks the dealer, and shows their bid or "passed". The player to act has a blue border.

The middle of the table shows the current trick, each card on the side of the player who played it. Below the trick are the trump suit, the pitcher and their bid, and each team's score and tricks won in this deal. When the game ends it also shows the points each team took in the last deal.
