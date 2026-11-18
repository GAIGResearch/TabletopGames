## Perudo

Be the last player with dice.

**Each round.** Every player starts the game with {nDicePerPlayer} dice. At the start of each round every player still in rolls their dice in secret. Player 0 opens the first round.

**Bids.** A bid such as "5 x 3" claims that at least 5 of all the dice on the table show a 3. A Paco (the 1, shown as a star) is wild: it counts towards a bid on any other face. Bids go round the table in seat order, and each player must raise the bid or call Dudo. No bid may be for more dice than are on the table.

The opening bid of a round may be on any face but Pacos. A raise is:

- more dice of any face, or the same number of dice of a higher face;
- Pacos, at least half the number of dice bid (rounded up): 5 x 4 may be raised to 3 Pacos;
- from Pacos, more Pacos, or more than twice as many dice of another face: 3 Pacos may be raised to 7 of any face.

**Bid limits.** On each face a bid may be for at most {maxRaise} more dice than the lowest bid allowed on that face, or for up to {maxBidPercent}% of the dice on the table if that is more. No bid may be for fewer than {minBidPercent}% of the dice on the table (rounded down).<!-- if palifico --> Outside a Palifico round<!-- else --> For<!-- end --> Pacos these percentages are halved (rounded up).

**Dudo.** A player who doubts the bid calls Dudo, and all the dice are shown. If fewer dice match the bid than it claims, the bidder loses a die. Otherwise the caller loses a die.
<!-- if calza -->

**Calza.** Instead of Dudo a player may call Calza, claiming the bid is exactly right. If exactly that many dice match, the caller gains a die (up to {nDicePerPlayer}). Otherwise the caller loses a die. The bidder neither gains nor loses. The caller starts the next round. There is no Calza in a Palifico round.
<!-- end -->

**Next round.** The player who lost a die starts the next round. A player who loses their last die is out, and the next player still in starts.
<!-- if palifico -->

**Palifico.** The first time a player is left with one die, the next round is a Palifico round, which they start. In it Pacos are not wild, the opening bid may be on Pacos, and every later bid must be on the same face for more dice. Each player has one Palifico round at most.
<!-- end -->

**End.** The last player with dice wins. The others are ranked by how late they went out.

### Interface

The top of the table shows the current bid, who made it, and the number of dice on the table<!-- if palifico -->, with a note when the round is a Palifico round<!-- end -->. Each player's area shows their dice, face down (?) unless you may see them, and how many dice they hold. A blue outline shows whose turn it is. The action buttons are the legal bids, written "Bid 5 x 3" for five 3s and "Bid 3 Pacos", and Dudo<!-- if calza --> and Calza<!-- end -->. When a challenge is called, the dice shown and the result appear in the history panel.
