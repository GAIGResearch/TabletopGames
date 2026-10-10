## Ventlife

[Turn](#turn) | [Tiles](#tiles) | [Covering](#covering) | [Species](#species) | [End](#end) | [Interface](#interface)

Each player holds one hidden tile.
<!-- if speciesSelection == RANDOM -->
{nSpecies} of the 7 species are chosen at random.
<!-- elif speciesSelection == FIRST_GAME -->
The species are Tube Worms, Blind Vent Shrimp, Volcano Snails and Yeti Crabs.
<!-- else -->
Before the first tile, the players take turns to draft species, starting with player 0. With 2 or 4 players they draft the {nSpecies} species that will be used. With 3 players they draft the {7 - nSpecies} species that will not be used.
<!-- end -->
Each player has {tokensPerSpecies} tokens of each species in play.

### Turn

1. **Place your tile** on the seafloor or on top of the field.
2. **Resolve covered creatures** (see Covering).
3. **Place creatures of one species** from your supply. This is compulsory whenever any placement is possible.
4. **Draw a tile**, if any are left.

### Tiles

Each tile has a Black Smoker and two seafloor terrains. The first tile goes with its Black Smoker at (0,0).

- **Seafloor.** All three hexes go on empty seafloor, and at least one touches the field along a side. The hexes are at level 1.
- **Plateau.** The tile's Black Smoker goes on a Black Smoker, all three hexes lie on hexes of the same level, and they rest on at least two different tiles. The hexes are one level higher.

### Covering

Creatures under the new tile move or return to their owner's supply. Your own creatures are resolved first, then those of the other players in clockwise order. A creature moves to an empty neighbouring hex its species allows:

- If there is no such hex, it returns to the supply.
- If there is one, it moves there.
- If there are several, its owner chooses one.

<!-- if smokersOnlyForWormsAndShrimp -->
Only Tube Worms and Shrimp may be placed on, or moved onto, a Black Smoker.
<!-- else -->
Fish may not be placed on, or moved onto, a Black Smoker. Every other species may.
<!-- end -->

### Species

| Species | Placed on | When covered | Scores |
|:--------|:----------|:-------------|:-------|
| Tube Worms (W) | An empty Black Smoker. <!-- if lowVentBonus -->On a level-1 Black Smoker you may place {1 + lowVentBonus} (the Low-Vent Bonus).<!-- end --> | They climb onto a Black Smoker placed on theirs; otherwise they return. | Each scores the level of its hex. |
| Volcano Snails (Sn) | Basalt Ridge. You may place up to {maxSnailsPerTurn}. Each one after the first goes next to a Snail placed this turn. | To a lower hex. | {snailPointsPerHeightEdge} for each neighbouring hex of a different level. |
| Blind Vent Shrimp (Sh) | Any hex but Basalt Ridge. | To any hex but Basalt Ridge. | {shrimpPointsOneTerrain} next to Diffuse Vents or Microbial Mat, {shrimpPointsBothTerrains} next to both. |
| Eelpout Fish (F) | A level-1 hex, or a hex next to one of your Fish. | To a hex of the same or a lower level. | Each shoal of 2 or more connected Fish scores {shoalPoints} for each Fish after the first. |
| Yeti Crabs (C) | A hex of level 2 or more. | To a hex of the same level. | On each plateau (2 or more connected hexes of the same level), the player with the most Crabs scores its size, and the player with the second most scores half of it (rounded down). Players tied for the most all score the size, and no second place is scored. |
| Deep-Sea Octopus (O) | An edge hex (next to empty seafloor). | Its owner chooses a direction; it moves in a straight line to the first empty edge hex. | {octopusPointsPerSpecies} for each other species next to it. |
| Vent Sponges (Sp) | Any hex. | They return. | {spongePointsPerTerrain} for each terrain next to them. |

### End

The game ends when every tile has been placed. The highest score wins. Ties are broken by the best single species score, then by the Tube Worm score.

### Interface

- Each hex shows its terrain colour and its level. Black Smokers are near-black, Basalt Ridge grey, Diffuse Vents blue and Microbial Mat white. Thick lines separate tiles, and the last tile placed is outlined in orange.
- Creatures are discs in their owner's colour, marked with their species' letters (Wx2 is two Tube Worms).
- A creature drawn faded in a dashed ring is covered and waiting for its owner's choice.
- Positions are written (q,r), as the action buttons name them. Empty seafloor next to the field is outlined.
- The action buttons name a tile by its terrains, left / right. Orientation n puts the right terrain in direction n from the Black Smoker and the left terrain in the next direction clockwise. The directions are 0 east, 1 south-east, 2 south-west, 3 west, 4 north-west and 5 north-east, so orientation 1 is the tile as printed, with the Black Smoker at the top.
- The panel on the right lists the players (the one to act is marked >), their score so far, the tokens left of each species, and their tile when you may see it.

The How to Play tab explains how to choose by clicking on the field and the species key.
