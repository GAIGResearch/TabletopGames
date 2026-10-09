## Monopoly

The classic rules on the UK (London) board, without trading between players. The last player not bankrupt wins.

[Turn](#turn) · [Squares](#squares) · [Jail](#jail) · [Building](#building) · [Mortgages](#mortgages) · [Auctions](#auctions) · [Debts and bankruptcy](#debts-and-bankruptcy) · [End of the game](#end-of-the-game) · [Interface](#interface)

**Each player** starts on GO with {board.currency}{startingCash}. The Bank has unlimited money, houses and hotels.

### Turn

1. Roll the two dice and move your token clockwise by their total.
2. Do what the square you stop on says.
3. Then build, sell buildings, mortgage and unmortgage as you choose.
4. After a double, roll again. Your <!-- if maxDoubles == 1 -->1st<!-- elif maxDoubles == 2 -->2nd<!-- elif maxDoubles == 3 -->3rd<!-- else -->{maxDoubles}th<!-- end --> double in a row in one turn sends you to Jail instead of moving.

Each time you pass or stop on GO you collect {board.currency}{board.goSalary}.

### Squares

- **Unowned property.** Buy it at its price, or decline it and it is auctioned.
- **Another player's property.** Pay them rent, unless it is mortgaged.
- **Street rent** is on the title deed. It is doubled on a street with no buildings when its owner holds every street of the colour group.
- **Station rent** is {board.currency}25, {board.currency}50, {board.currency}100, {board.currency}200 for 1 to 4 stations owned.
- **Utility rent** is 4 times the dice with one utility owned, and 10 times with both.
- **Chance and Community Chest.** Take the top card and do what it says. It then goes to the bottom of its pile. A Get Out of Jail Free card is kept until it is used.
<!-- if incomeTaxPercent == 0 -->
- **Income Tax and Super Tax.** Pay the amount on the square to the Bank.
<!-- else -->
- **Income Tax.** Choose to pay the amount on the square, or {incomeTaxPercent}% of your total worth (rounded down).
- **Super Tax.** Pay the amount on the square to the Bank.
<!-- end -->
- **Go To Jail.** Go straight to Jail without passing GO.

### Jail

In Jail you still collect rent. Before rolling you may pay the {board.currency}{jailFine} fine or use a Get Out of Jail Free card, then roll and move as usual. Otherwise you roll: a double frees you and you move by it, with no further roll. After {maxJailRolls} failed rolls you pay the fine and move by the last roll.

### Building

When you own every street of a colour group and none of them is mortgaged, you may build houses on them at the cost on the title deed. Build evenly: a street may not have more than one building more than another street of the group. A hotel replaces 4 houses. Buildings sell back to the Bank for {buildingSalePercent}% of their cost, evenly too. Selling a hotel leaves 4 houses.

### Mortgages

Mortgage a property for its mortgage value when no street of its group has buildings. To unmortgage it, pay the mortgage value plus {mortgageInterestPercent}% interest (rounded up). A mortgaged property charges no rent.

### Auctions

Players bid in turn, starting with the player after the one who declined. The opening bid is {board.currency}{minimumBid}. Each bid after it raises the high bid by {board.currency}10, {board.currency}50 or {board.currency}100. A player who passes is out of the auction. The last player in pays their bid for the property. If nobody bids, the property stays with the Bank.

### Debts and bankruptcy

A player who owes more than their cash must raise money by selling buildings and mortgaging properties, and pays once they have enough. A player who could not raise enough is bankrupt, and their buildings are sold to the Bank.

- **Bankrupt to a player.** That player takes all their cash, properties and Get Out of Jail Free cards. They pay {mortgageInterestPercent}% interest at once on each mortgaged property they receive.
- **Bankrupt to the Bank.** The properties go back to the Bank unmortgaged, and are auctioned one by one.

### End of the game

The last player not bankrupt wins. The game also ends after {maxRounds} rounds. The players not bankrupt are then ranked by total worth: cash, the price of each unmortgaged property, the mortgage value of each mortgaged one, and the cost of the buildings.

### Interface

- Each token is a disc with the player's number. A token with bars across it is in Jail. On the Jail square without bars, it is Just Visiting.
- An owned property has a frame in its owner's colour. Green squares in the colour band are houses, and a red bar is a hotel. A grey MORTGAGED cover marks a mortgaged property.
- The middle of the board shows the last roll, the decision being made, and the card drawn last from each pile.
- The table beside the board shows each player's cash, total worth, properties and square. The player to move is highlighted.
