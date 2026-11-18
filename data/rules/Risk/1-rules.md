## Risk

[Setup](#setup) | [Turn](#turn) | [Cards](#cards) | [End](#end) | [Screen](#screen)

### Setup

<!-- if randomTerritoryDeal or secretMission -->The territories are dealt at random, one army on each.<!-- else -->In turn, each player puts one army on an empty territory until all are claimed.<!-- end --> Then each player in turn places <!-- if placementBatch > 1 -->{placementBatch} armies at a time while more than {placementBatch} are left, then one at a time<!-- else -->one army at a time<!-- end --> on a territory they hold, until all their starting armies are on the board.

| Players | 3 | 4 | 5 | 6 |
| --- | --- | --- | --- | --- |
| Starting armies | {startArmies3} | {startArmies4} | {startArmies5} | {startArmies6} |

### Turn

1. **Reinforce.** You receive the number of territories you hold divided by {territoriesPerArmy} (at least {minReinforcements}), plus the bonus for each continent you hold entirely (shown on the map). Place them <!-- if placementBatch > 1 -->{placementBatch} armies at a time while more than {placementBatch} are left, then one at a time<!-- else -->one army at a time<!-- end -->. You may trade in cards before placing the last.
2. **Attack** (optional). Attack from a territory with at least 2 armies to a neighbouring enemy territory, rolling up to {maxAttackDice} dice but fewer than your armies there. The defender <!-- if defenderChoosesDice -->chooses to roll from 1 to <!-- else -->rolls <!-- end -->{maxDefendDice} dice, but no more than their armies there. The highest dice are compared in pairs, and the lower of each pair loses one army. The defender wins a tie. <!-- if allowBlitz -->Blitz attacks again and again with the most dice until the territory is taken or you are down to one army. <!-- end -->When you take a territory you move in at least as many armies as dice rolled.
3. **Fortify** (optional). Move armies once, from one territory <!-- if fortifyAlongPath -->to another territory you hold that is joined to it by a chain of territories you hold<!-- else -->to a neighbouring territory you hold<!-- end -->, leaving at least one behind. Then your turn ends.

<!-- if maxArmiesPerTerritory > 0 -->
No territory may hold more than {maxArmiesPerTerritory} armies. Armies that cannot be placed for this reason are lost.

<!-- end -->
When moving armies (fortifying or moving in), at most {maxMoveChoices} numbers are offered: the least, the most, and evenly spaced numbers between.

### Cards

If you took at least one territory in your turn, you draw one card when you end your attacks. A set is three cards with the same symbol, one of each symbol, or any two with a wild card. The sets traded in by all players are worth {tradeValue(1)}, {tradeValue(2)}, {tradeValue(3)}, {tradeValue(4)}, {tradeValue(5)}, {tradeValue(6)}, {tradeValue(7)}, {tradeValue(8)}<!-- if linearTradeValues -->, and so on.<!-- else -->, and then {tradeValueIncrement} more each.<!-- end --> If a card in the set shows a territory you hold, {territoryBonus} extra armies go on it (once a turn). With {handLimit} or more cards you must trade before placing. If you eliminate a player you take their cards, and with {eliminationTradeLimit} or more you must trade at once until you hold fewer than {handLimit}.

### End

<!-- if secretMission -->
**Winning.** Each player has a secret mission. The first to complete it wins at once. Taking every territory also wins. If another player eliminates the player your mission tells you to destroy, you take over the eliminated player's mission. A mission to destroy yourself, or a player who is not in the game or is already out, is to occupy {map.backupTerritories} territories instead.
<!-- else -->
**Winning.** The player who holds every territory wins.
<!-- end -->

A player who loses their last territory is out. After {maxRounds} rounds the game ends, and the players still in are ranked by territories, then armies.

### Screen

- Each disc is a territory, in its owner's colour with its armies, ringed in its continent's colour. Lines join neighbours. Alaska and Kamchatka are joined off the edges of the map.
- A black ring marks the two territories of an attack, move-in or defence waiting for a choice.
- The panel on the right lists the players (the one to act is marked >), their cards by symbol (I Infantry, C Cavalry, A Artillery, W wild) and their mission when you may see them.
- The actions are listed below the map. ChooseAttack(from, to) picks the attack, then Attack(from, to, n) rolls n dice once<!-- if allowBlitz --> or Blitz attacks to the end<!-- end -->.
