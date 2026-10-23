package games.ventlife.components;

/**
 * The top hex at a position of the vent field.
 *
 * @param terrain the terrain of the top hex
 * @param level   1 for a hex on the seafloor, 2 for one stacked on that, and so on
 * @param tileId  the tile the top hex belongs to, numbered in the order the tiles were placed
 */
public record HexCell(Terrain terrain, int level, int tileId) {
}
