# Catan Implementation details

## Board representation
We used a 2D array to represent the board. Each entry represent a hexagonal tile in the game.
The representation that best suited The Settlers of Catan is "r even", which means that every even row is offset by 0.5 width.
[This blogpost](https://www.redblobgames.com/grids/hexagons/) has more information on this representation.

## CatanTile
The tiles are represented as individual Hexagons using the "r-even" representation meaning that the hexagons are facing with their pointy sides up and every even column is offset by 0.5 * width (shifted to the right).

Each tile contains its x (row) and y (column) coordinates in the "r-even" representation. "x_coord" and "y_coord" are their coordinates on the rendered screen. 

Currently, working on setting references to all edges and vertices, so adding a new settlement to a hex would also show up on the neighbouring tiles and same with the roads (edges).
Copying the tiles alter the reference so once we got the graph structure set up their IDs are used to check equality.

# Graph
To facilitate working with a graph structure along with the tile representation we keep a graph representation as well. All of them uses the same references to Settlements and roads. 
In this case the Settlements are the vertices and Roads are the edges connecting them.

# Helper functions
- Find reference to roads from a settlement - ```Graph.getNeighbourNodes``` and ```Graph.getEdges``` functions
- Find the longest road: [solution](https://stackoverflow.com/questions/3191460/finding-the-longest-road-in-a-settlers-of-catan-game-algorithmically)

Longest road calculation
- 1, Get Settlements along the road we just placed on the board
- 2, Follow the road in one direction and then in the other and merge the set of found roads

- We keep track of a set of expanded and unexpanded settlements.
