## Cribbage

A game for two players, who deal in turn. Player 0 deals first. The dealer owns the crib.

**Card values.** Ace counts 1 and court cards count 10. For pairs and runs the cards rank from Ace (low) to King.

**Each round.**

1. Each player is dealt {nCardsDealt} cards and discards {nCardsToCrib} of them face down to the crib. The non-dealer discards first.
1. The top card of the deck is turned up as the starter. If it is a Jack, the dealer scores {hisHeelsPoints} (his heels).
1. In the play, the non-dealer leads. The players then take turns to play a card, and each card adds its value to the count. The count may not go over {maxCount}.
1. A player who cannot play is skipped (a go), and the other player plays on alone. When neither can play, or the count reaches {maxCount}, the count starts again from 0. The opponent of the player of the last card leads.
1. In the show, the {nCardsDealt - nCardsToCrib} cards each player played are scored with the starter. The non-dealer is scored first, then the dealer, then the crib (for the dealer).

**Scoring.**

| Combination | The play | The show |
| :--- | :---: | :---: |
| Fifteen | {playFifteenPoints} | {fifteenPoints} each |
| Count of {maxCount} | {thirtyOnePoints} | |
| Last card | {lastCardPoints} | |
| Pair | {pairPoints} | {pairPoints} |
| Three of a kind | {pairRoyalPoints} | {pairRoyalPoints} |
| Four of a kind | {doublePairRoyalPoints} | {doublePairRoyalPoints} |
| Run of 3 or more | 1 per card | 1 per card |
| Flush | | {flushPoints}, or {flushPoints + 1} with the starter |
| His nobs | | {hisNobsPoints} |

- In the play, a card scores for the count it makes, and for a pair or run it makes with the cards just before it in the same count. A run may be in any order. The last card scores only if the count is below {maxCount}.
- In the show, every combination of cards that adds up to 15 scores. Only the longest runs score<!-- if not runsIncludeStarter -->, and they are made without the starter<!-- end -->. Each different set of cards making a run counts, so 6-7-7-8 is two runs.
- A flush is all {nCardsDealt - nCardsToCrib} cards of one suit<!-- if cribFlushNeedsStarter -->. In the crib a flush scores only if the starter matches too<!-- end -->. His nobs is the Jack of the starter's suit in the hand or crib.

**Winning.** <!-- if targetScore > 0 -->The game ends at once when a player reaches {targetScore}, or after {nRounds} rounds.<!-- else -->The game ends after {nRounds} rounds.<!-- end --> The higher score wins, and equal scores draw.

### Interface

Choose a pair of cards to discard ("Discard ... to crib"), then a card to play, from the action buttons at the bottom. The centre shows the Starter, the Crib (face down, except the cards a human player discarded) and the Count with the cards played in it. The line below says whose turn it is. Each player's area shows their Hand and the cards they have Played this round, with their score and "dealer (owns the crib)" for the dealer. A blue outline shows whose turn it is.
