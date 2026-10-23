## War of the Toads

[A Battle](#a-battle) | [Lanes](#lanes) | [Hostages and Flags](#hostages-and-flags) | [Winning](#winning) | [Cards](#cards) | [Interface](#interface)

Each of the two players has a deck of {cardDeck.size} toads, and they fight two Wars. The aim is to capture more Hostages than your opponent.

**Each War** starts with a new hand. <!-- if not openingReturn -->Each player draws {handSize} cards.<!-- else -->Each player draws {handSize + 1} cards, and then puts one of them on the bottom of their deck.<!-- end --> Player 0 attacks first in War 1. The War has 4 Battles, and the Attacker and the Defender swap roles after each one.

### A Battle

<!-- if discardOption -->
1. Each player may put one card from their hand on the bottom of their deck, and then draws the top card of the deck.
<!-- end -->
1. The Attacker plays a card face up.
1. At the same time, the Attacker chooses a hidden card, and the Defender chooses a face-up card and a hidden card.
1. The hidden cards are revealed<!-- if useTactics --> and their Tactics act<!-- end -->.
1. Each lane is won by one card or tied (see [Lanes](#lanes)).
1. Each player draws 2 cards, or the rest of their deck if it has fewer.
<!-- if useTactics -->
1. A Scout's or a Siege Cannon's Tactic then takes effect (see [Cards](#cards)).
<!-- end -->

<!-- if useTactics -->
Only the Tactic of each hidden card acts. A card's Ally is the other card on its side, and the Ally's Foe is the card opposite the Ally. The Tactics act in four stages, in the order Block, Start, During and After. Within a stage both Tactics act at the same time.
<!-- else -->
In this game the cards' Tactics do not act.
<!-- end -->

### Lanes

The face-up cards fight in the Face-up lane, and the hidden cards in the Hidden lane. The higher Strength wins the lane. A card's Special Attribute applies in either lane, and takes the place of the Strength comparison. A tied lane stays tied unless exactly one of its cards breaks ties.

### Hostages and Flags

- The winner of a lane captures the losing card as a Hostage.
- In a tied lane both cards go to the Shrine, and each player gains a Flag.
- A player who wins both lanes while Calm keeps one Hostage. The other goes to the Shrine, and each player gains a Flag.
- A player who wins both lanes while Angry keeps both Hostages.

You are Angry when you have fewer Hostages than your opponent in this War, or when your Berserker makes you Angry. Otherwise you are Calm.

**Between the Wars** the card left in each hand becomes that player's Casualty. Each player's Casualty gives them one Flag at the start of War 2. The cards each player played in War 1 are shuffled to form their opponent's deck for War 2. <!-- if secondRoundStart == ONE -->Player 0 attacks first in War 2.<!-- elif secondRoundStart == TWO -->Player 1 attacks first in War 2.<!-- elif secondRoundStart == LOSER -->The loser of War 1 attacks first in War 2 (Player 1 after a Stalemate).<!-- else -->The winner of War 1 attacks first in War 2 (Player 1 after a Stalemate).<!-- end -->

### Winning

The player with more Hostages wins a War. A War with equal Hostages is a Stalemate. The winner of War 2 wins the game. If War 2 is a Stalemate, the winner of War 1 wins. If both Wars are Stalemates, the lower Casualty wins (the Siege Cannon is the lowest). Equal Casualties draw.

### Cards

<!-- if cardFile == "cards.json" -->
Each deck holds one of each card.

| Card | Strength | Special Attribute | Tactic |
|:-----|:--------:|:------------------|:-------|
| Assassin | 1 | Beats a General. | <!-- if useTactics -->Adds 2.5 to the lower of your Ally and its Foe (not in a lane with a Siege Cannon).<!-- end --> |
| Scout | 2 | | <!-- if useTactics -->Adds 1 to your Ally. After the Battle your opponent will show you 3 cards from their hand.<!-- end --> |
| Saboteur | 3 | Beats a Siege Cannon. | <!-- if useTactics -->Your Ally breaks ties.<!-- end --> |
| Trickster | 4 | | <!-- if useTactics -->Switches lanes with your Ally. It cannot be blocked.<!-- end --> |
| Berserker | 5 | | <!-- if useTactics -->Makes you Angry for this Battle.<!-- end --> |
| Bodyguard | 6 | | <!-- if useTactics -->Blocks the Tactic of your opponent's hidden card.<!-- end --> |
| General One | 7 | Loses to an Assassin. | <!-- if useTactics -->Adds 1 to your Ally for each Hostage your opponent has this War.<!-- end --> |
| General Two | 7 | Loses to an Assassin. | <!-- if useTactics -->Adds 1 to your Ally for each of your Flags this War.<!-- end --> |
| Siege Cannon | 0 | Wins in Attack, except against a Saboteur. Loses in Defence. | <!-- if useTactics -->After the Battle you will guess a card in your opponent's hand.<!-- end --> |
<!-- else -->
Each deck holds one of each card.

| Card | Strength | Special Attribute | Tactic |
|:-----|:--------:|:------------------|:-------|
| Assassin | 1 | | |
| Scout | 2 | | <!-- if useTactics -->Adds 1 to your Ally. After the Battle your opponent will show you 3 cards from their hand.<!-- end --> |
<!-- if cardFile == "cards_005.json" -->
| Saboteur | 3 | | |
| Trickster | 4 | | |
<!-- else -->
| Trickster | 3 | | |
| Saboteur | 4 | | |
<!-- end -->
| Berserker | 5 | | |
| Icon Bearer | 6 | | |
| General One | 7 | | |
| General Two | 7 | | |
| Assault Cannon | 0 | | |
<!-- end -->

After a Scout's Battle, an opponent holding 4 cards chooses one of them to keep hidden. A Siege Cannon's owner names a card type, and sees one card of that type if the opponent holds it. The owner cannot name their own Casualty, or a card the opponent has already played in this War.

### Interface

Player 1's area is at the top and Player 0's at the bottom. Each area shows the player's role (Attacker or Defender), their Hostages, Flags and mood in this War, their Hand, their Deck with its size, and their Casualty. The bottom card of a deck is named on it when you know it. The player to act has a blue outline.

The middle panel shows the Face-up lane and the Hidden lane, with Player 1's card on the left of each. The last Battle's cards stay there, dimmed, until the next Battle starts. The text beside the lanes gives the War and Battle number, who is to act, and the Hostages each player took in the last Battle.

A card shows its Strength in the circle, its Special Attribute in italics and its Tactic in the box at the bottom. Cards shown to you by a Scout or a Siege Cannon are face up in your opponent's hand.

Choose from the action list at the bottom of the screen. The action buttons call the Face-up lane the field, and the Hidden lane the flank.
