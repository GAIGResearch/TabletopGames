# Catan Implementation details

## Board representation
We used a 2D array to represent the board. Each entry represent a hexagonal tile in the game.
The representation that best suited The Settlers of Catan is "r even", which means that every even row is offset by 0.5 width.
[This blogpost](https://www.redblobgames.com/grids/hexagons/) has more information on this representation.

## CatanTile
The tiles are represented as individual Hexagons using the "r-even" representation meaning that the hexagons are facing with their pointy sides up and every even column is offset by 0.5 * width (shifted to the right).

Each tile contains its x (row) and y (column) coordinates in the "r-even" representation. "x_coord" and "y_coord" are their coordinates on the rendered screen. 

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