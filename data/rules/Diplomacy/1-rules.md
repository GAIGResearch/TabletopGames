## Diplomacy

[The year](#the-year) - [Orders](#orders) - [Resolution](#resolution) - [Retreats](#retreats) - [Builds and disbands](#builds-and-disbands) - [Interface](#interface)

Seven powers fight for the supply centres of Europe. A power that controls {map.victoryCentres} supply centres after a Fall turn wins. Otherwise the game ends after the Fall turn of {lastYear}, and the power with the most supply centres wins (powers with the same number share the win). There is no negotiation in this implementation.

### The year

Each year has a Spring turn and a Fall turn. Each turn has an orders phase and, if any unit was dislodged, a retreat phase. After the Fall turn every supply centre with a unit in it comes under that unit's power, and the powers then build or disband units to match their supply centres. (The other land provinces change hands at the same time, but only the supply centres count.)

In each phase the powers give their orders one at a time, one order per unit. The orders stay hidden until every power has given them; then all are carried out together.

### Orders

- **Hold**: the unit stays.
- **Move**: an army moves to an adjacent land or coastal province, and a fleet to an adjacent sea or coastal province along the coast. A fleet entering Spain, St. Petersburg or Bulgaria names the coast.
- **Support** (S): the unit adds 1 to the strength of a unit holding, or moving, in a province it could move to itself.
- **Convoy** (C): a fleet at sea carries an army across the water. A chain of fleets can carry it over several seas. A move marked *via convoy* goes by sea even where it could go by land. An army is also convoyed when a fleet of its own power convoys it.

### Resolution

- Every unit has strength 1, plus 1 for each support that is not cut.
- A move succeeds if it is stronger than the unit holding there and than every other move into the same province. Moves of equal strength into one province all fail (a standoff).
- A unit that is beaten by a move into its province is dislodged.
- A support is cut if its unit is attacked from any province except the one it supports into, or if its unit is dislodged.
- A power cannot dislodge its own unit, and its supports do not count against its own unit. An attack by a power on its own unit does not cut support.
- Two units cannot swap places unless one of them is convoyed.
- A convoy fails only if every route it could take has a fleet dislodged.
<!-- if paradoxRule == SZYKMAN -->
- In a convoy paradox the convoying fleets hold (the Szykman rule).
<!-- else -->
- A convoyed army does not cut the support of an attack on one of its convoying fleets, unless it has another route (rules 21 and 22 of the 2000 rulebook). In any other convoy paradox the convoying fleets hold (the Szykman rule).
<!-- end -->

### Retreats

A dislodged unit retreats to an adjacent province it could move to, that is empty, that is not the province its attacker came from (unless the attacker was convoyed) and that was not left empty by a standoff. Two units retreating to one province are both disbanded. A unit with no retreat is disbanded at once, and any unit may be disbanded instead of retreating.

### Builds and disbands

A power with more supply centres than units may build one unit for each, in its home supply centres that it still controls and that are empty. An army may be built in any of them, a fleet only on a coast. **Waive builds** gives up the rest. A power with more units than supply centres disbands the units of its choice.

### Interface

- Each land province is shaded in the colour of the power controlling it, and left buff if nobody does. A star marks a supply centre. The grey hatched land is impassable.
- A cannon in a power's colour is an army, and a ship is a fleet. A red-bordered unit beside its province has been dislodged and must retreat.
- The last orders carried out are drawn on the map: black arrows for moves that succeeded, red dashed arrows ending in a cross for moves that failed, green dotted lines for supports, blue for convoys and orange arrows for retreats. A dashed ring marks a unit built, and a red cross a unit disbanded. Purple shows the orders given so far this phase by the powers you may see (your own, when you play).
- On the right are the powers, with their supply centres and units, and the list of last orders.
- The action buttons are the orders for the unit being ordered now. You may also click on the map: see How to Play.
