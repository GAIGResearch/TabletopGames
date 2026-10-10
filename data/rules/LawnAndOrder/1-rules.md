## Lawn & Order

Build matching sets on your lawn while the Homeowners Association condemns attributes one by one.

**Cards.** Each Lawn card has a Type (Ornament, Furniture, Structure, Water Feature), a Colour (Red, Yellow, Pink, Blue) and a Feature (Oversized, Illuminated, Plastic, Repurposed). The Agenda holds {ruleCards.size} Rule cards. Each Standard Rule condemns one attribute. The others are {nAdministrativeError} Administrative Errors, {nEmergencySession} Emergency Session and {nZeroTolerance} Zero Tolerance Policy.

**Each round** starts with {handSize} Lawn cards dealt to each player. One Rule card is dealt face down between each pair of neighbours as an Insider Tip. You may look at the two beside you.

**Each turn** all active players act together:

1. Each chooses a card from their hand face down. The cards are then revealed onto the lawns.
2. Each gains 1 Citation for each attribute of their new card that is already condemned.
3. The top Agenda card is revealed, and stays in force for the round. An Administrative Error does nothing. An Emergency Session reveals the next {emergencySessionReveals} cards too. Zero Tolerance lowers every Citation limit by {zeroToleranceReduction}.
4. Each active player gains 1 Citation for each card on their lawn with an attribute condemned this turn. Players who have passed are immune.
5. A player with more Citations than lawn cards receives a Cease & Desist. Their lawn and hand are cleared, and they score nothing this round.
6. Each active player chooses in secret to **Continue** (draw a card and play again) or to **Pass** (keep their lawn, safe from later rules).

**Scoring.** The round ends when no player is active, or when the Agenda runs out. Each category scores on its own track: Type on Improvements, Colour on Colour, Feature on Character. In each category, every group of lawn cards that share an attribute scores:

| Cards  | 2 | 3 | 4 | 5 | 6+ |
|:-------|:-:|:-:|:-:|:-:|:--:|
| Points | 1 | 2 | 4 | 7 | 10 |

**Goodwill.** A player who received a Cease & Desist has a Citation limit {goodwillBonus} higher in the next round.

**Winning.** The first player with {targetScore} or more on all three tracks wins. If several reach it in the same round, the highest total of the three tracks wins. After {maxRounds} rounds the highest total wins.

### Interface

Choose from the action buttons at the bottom. The top panel shows the rules revealed this round and the Insider Tips (face up only to the players beside them). Each player's area shows their status and Citations, their hand, the card they have chosen this turn (face down), their lawn, the attributes with two or more cards on it, and their three tracks (★ = target reached).
