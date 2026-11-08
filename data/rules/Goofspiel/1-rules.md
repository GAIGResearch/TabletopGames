## Goofspiel

Win the most valuable prizes by bidding cards from your hand.

**Cards.** The Diamonds are shuffled face down as the prize deck. Each player's hand is one of the other suits (Clubs, Spades, Hearts, repeated from a second pack for more than three players). Each suit has {cardsPerSuit} cards, from <!-- if aceHigh -->2<!-- else -->Ace<!-- end --> to <!-- if cardsPerSuit == 13 and aceHigh -->Ace<!-- elif cardsPerSuit == 13 -->King<!-- elif cardsPerSuit == 12 and aceHigh -->King<!-- elif cardsPerSuit == 12 -->Queen<!-- elif cardsPerSuit == 11 and aceHigh -->Queen<!-- elif cardsPerSuit == 11 -->Jack<!-- elif cardsPerSuit == 10 and aceHigh -->Jack<!-- elif cardsPerSuit == 9 and aceHigh -->10<!-- elif cardsPerSuit == 7 and aceHigh -->8<!-- elif cardsPerSuit == 5 and aceHigh -->6<!-- else -->{cardsPerSuit}<!-- end -->. A number card is worth its number.<!-- if not aceHigh or cardsPerSuit >= 10 --> The other cards are worth:<!-- end -->

<!-- if not aceHigh and cardsPerSuit >= 13 -->
| Card  | Ace | Jack | Queen | King |
|:------|:---:|:----:|:-----:|:----:|
| Value |  1  |  11  |  12   |  13  |
<!-- elif not aceHigh and cardsPerSuit >= 12 -->
| Card  | Ace | Jack | Queen |
|:------|:---:|:----:|:-----:|
| Value |  1  |  11  |  12   |
<!-- elif not aceHigh and cardsPerSuit >= 11 -->
| Card  | Ace | Jack |
|:------|:---:|:----:|
| Value |  1  |  11  |
<!-- elif not aceHigh -->
| Card  | Ace |
|:------|:---:|
| Value |  1  |
<!-- elif cardsPerSuit >= 13 -->
| Card  | Jack | Queen | King | Ace |
|:------|:----:|:-----:|:----:|:---:|
| Value |  11  |  12   |  13  | 14  |
<!-- elif cardsPerSuit >= 12 -->
| Card  | Jack | Queen | King |
|:------|:----:|:-----:|:----:|
| Value |  11  |  12   |  13  |
<!-- elif cardsPerSuit >= 11 -->
| Card  | Jack | Queen |
|:------|:----:|:-----:|
| Value |  11  |  12   |
<!-- elif cardsPerSuit >= 10 -->
| Card  | Jack |
|:------|:----:|
| Value |  11  |
<!-- end -->

**Each round** the top card of the prize deck is turned face up and added to the prizes on offer.

1. Every player bids one card from their hand, face down.
2. When all have bid, the bids are revealed. The bid cards are out of the game.
<!-- if tieRule == CARRY_OVER -->
3. The highest bid wins all the prizes on offer. If the highest bid is tied, nobody wins them. They stay on offer, and the next prize will be added to them.
<!-- elif tieRule == DISCARD -->
3. The highest bid wins all the prizes on offer. If the highest bid is tied, the prizes are won by nobody.
<!-- else -->
3. Bids made by more than one player are set aside. The highest remaining bid wins all the prizes on offer. If no bid remains, the prizes are won by nobody.
<!-- end -->

**End.** The game ends after {cardsPerSuit} rounds, when the hands are empty.<!-- if tieRule == CARRY_OVER --> Prizes still on offer after a tie in the last round are won by nobody.<!-- end --> Your score is the total value of the prizes you have won. The highest score wins, and players with the same highest score draw.

### Interface

Choose your bid from the action buttons at the bottom. The top panel shows the Prize deck (with the number of cards left in it) and the Prizes on offer. Beside them are the round, the total value of the prizes on offer, the tie rule, and the prizes won by nobody.

Each player's area shows their Hand, their Bid for this round (face down to the other players) and their Last bid, revealed at the end of the previous round. Below the cards are their score and the prizes they have won (Won). "(has bid)" appears once they have bid this round. The player to bid next has a blue border. Cards in text are written as rank and suit, so Q♦ is the Queen of Diamonds.
