# Catan Implementation details

## Board representation
We used a 2D array to represent the board. Each entry represent a hexagonal tile in the game.
The representation that best suited The Settlers of Catan is "r even", which means that every even row is offset by 0.5 width.
[This blogpost](https://www.redblobgames.com/grids/hexagons/) has more information on this representation.

## CatanTile
The tiles are represented as individual Hexagons using the "r-even" representation meaning that the hexagons are facing with their pointy sides up and every even column is offset by 0.5 * width (shifted to the right).

Each tile contains its x (row) and y (column) coordinates in the "r-even" representation. "x_coord" and "y_coord" are their coordinates on the rendered screen. 

Currently working on setting references to all edges and vertices, so adding a new settlement to a hex would also show up on the neightbouring tiles and same with the roads (edges).

Easier tasks:
- Setup scoring system ```(gameState.score[playerID])``` - longest road, most knights + other calculations and related events
- Distribute and handle cards
- Actions related to buying - check if player has resources, return card to game and purchase item - if card keep it hand, if settlement, road then place it on the map
- Robber rules
- Play development cards
- Assign Harbors
- Harbor actions

Harder tasks:
- Edge, vertex referencing correctly + distance rule (+ longest road calculation)
- TurnOrder
- Event Reaction system
- Trading - how to handle negotiation?

## Catan Actions

### Progress
- [x] build Settlement
- [x] build roads
- [ ] play cards
- [ ] trading (Reaction system)

## Forward Model

Keeps track of the rules of the game.
### Progress
- [x] board setup
- [ ] initial phase for placing settlements with roads
- [ ] turns
- [ ] game logic