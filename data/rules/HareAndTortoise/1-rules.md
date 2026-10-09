## Hare and Tortoise

The rules of the 1978 Ravensburger edition. Be the first to get your runner from START to HOME. The others play on for second place, third place and so on, until one runner is left.

**Each player** starts with {startCarrots} carrots and {startLettuces} lettuces. Every payment is made openly.

### Moving

On your turn you move forwards any number of squares and pay carrots for the distance:

| Squares | 1 | 2 | 3 | 4  | 5  | 6  | 7  | 8  | 9  | 10 |
|:--------|:-:|:-:|:-:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| Carrots | 1 | 3 | 6 | 10 | 15 | 21 | 28 | 36 | 45 | 55 |

Moving n squares costs 1 + 2 + ... + n carrots.

- You may not end a move on an occupied square.
- You may not move forwards onto a tortoise square.
- You may not move onto a lettuce square once your lettuces are gone.

**Position in the race** is 1st for the runner furthest ahead. A runner HOME keeps the place it finished in, ahead of every runner still racing.

### Squares

- **Tortoise.** Instead of moving forwards, you may move back to the nearest tortoise square behind you, if it is free. You pay nothing and draw {carrotsPerTortoiseStep} carrots for each square moved back.
- **Carrot.** When your turn starts here, you may stay and draw {carrotsPerChew} carrots, or pay {carrotsPerChew} carrots if you hold at least {carrotsPerChew}.
- **Lettuce.** Your next turn is spent chewing a lettuce: you discard one and draw {carrotsPerRacePosition} carrots for each place in the race ({carrotsPerRacePosition} in 1st, {2 * carrotsPerRacePosition} in 2nd, and so on). On the turn after that you must move on.
- **Number (2, 3, 4) and flag (1, 5, 6).** When your turn starts here and your position in the race matches the number, you draw {carrotsPerRacePosition} carrots for each place.
- **Hare.** After paying to move here you draw a hare card and do what it says. The card then goes to the bottom of the pile.

### Hare cards

| Card | Effect |
|:-----|:-------|
| Fall back one position ({nFallBackOnePosition}) | Move back, free, to the first free square behind the runner behind you. |
| Your last turn costs nothing ({nLastTurnFree}) | Take back the carrots you paid for the move. |
| Either draw or discard {carrotsPerChew} carrots ({nDrawOrDiscard}) | Choose which. |
| Leap ahead by one position ({nLeapAheadOnePosition}) | Move, free, to the first free square beyond the runner ahead of you (not a tortoise square). |
| Leap ahead to the next carrot square ({nNextCarrotSquare}) | Free, to the next free carrot square. |
| Fall back to the previous carrot square ({nPreviousCarrotSquare}) | Free, to the previous free carrot square. |
| Have another turn ({nAnotherTurn}) | Take another turn at once. |
| Miss a turn ({nMissATurn}) | Your next turn is skipped. |
| Chew a lettuce ({nChewALettuce}) | If you hold a lettuce, discard one and draw {carrotsPerRacePosition} carrots for each place. |

A card that has no square to send you to has no effect.

### Getting HOME

You may move HOME only with no lettuces left, and if after paying you hold no more than {homeCarrotsPerRacePosition} carrots for each place you will finish in ({homeCarrotsPerRacePosition} for 1st, {2 * homeCarrotsPerRacePosition} for 2nd, and so on).

**Stuck.** A player who has no legal action when their turn starts goes back to START with {startCarrots} carrots, keeps the lettuces they still hold, and moves off at once.

The game also ends after {maxRounds} rounds; the runners are then ranked by position in the race.

### Interface

- Each runner is a numbered disc on the board. The runner whose turn it is has a white ring.
- The table beside the board shows each runner's square, place, carrots and lettuces, and whether it will chew a lettuce or miss its next turn.
- Below it are the number of hare cards never drawn and the card drawn last.
- Each action button names the square moved to and the carrots paid or drawn.
