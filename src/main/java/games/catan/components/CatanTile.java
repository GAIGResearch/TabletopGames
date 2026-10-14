package games.catan.components;

import core.components.BoardNode;
import core.components.grid.HexCoord;

import java.util.Arrays;
import java.util.Objects;

import static games.catan.CatanConstants.HEX_SIDES;

/**
 * A tile of the board. Tiles are immutable, so copies of the board share them.
 */
public class CatanTile extends BoardNode {
    public enum TileType {
        HILLS,
        FOREST,
        MOUNTAINS,
        FIELDS,
        PASTURE,
        DESERT,
        SEA
    }

    public final HexCoord hex;
    private final TileType tileType;
    private final int number;
    // the ids of the buildings at the vertices and of the roads along the sides, -1 where there are none
    private final int[] vertexIDs, edgeIDs;

    /**
     * A tile with no buildings or roads.
     */
    public CatanTile(HexCoord hex, TileType tileType, int number) {
        super(HEX_SIDES, "");
        this.hex = hex;
        this.tileType = tileType;
        this.number = number;
        vertexIDs = new int[HEX_SIDES];
        edgeIDs = new int[HEX_SIDES];
        Arrays.fill(vertexIDs, -1);
        Arrays.fill(edgeIDs, -1);
    }

    private CatanTile(CatanTile orig, int[] vertexIDs, int[] edgeIDs) {
        super(HEX_SIDES, "", orig.componentID);
        this.hex = orig.hex;
        this.tileType = orig.tileType;
        this.number = orig.number;
        this.vertexIDs = vertexIDs.clone();
        this.edgeIDs = edgeIDs.clone();
    }

    /**
     * This tile, with the buildings at its vertices and the roads along its sides given by their ids.
     */
    public CatanTile withIDs(int[] vertexIDs, int[] edgeIDs) {
        return new CatanTile(this, vertexIDs, edgeIDs);
    }

    public TileType getTileType() {
        return tileType;
    }

    public int getNumber() {
        return number;
    }

    public int getVertexID(int vertex) {
        return vertexIDs[vertex];
    }

    public int getEdgeID(int side) {
        return edgeIDs[side];
    }

    /**
     * The two other hexes at the vertex, clockwise. The vertex is vertex + 2 of the first and vertex + 4 of the
     * second.
     */
    public HexCoord[] getNeighboursOnVertex(int vertex) {
        return new HexCoord[]{hex.neighbour((vertex + 5) % HEX_SIDES), hex.neighbour(vertex)};
    }

    @Override
    public CatanTile copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatanTile catanTile)) return false;
        if (!super.equals(o)) return false;
        return hex.equals(catanTile.hex) && number == catanTile.number && Arrays.equals(vertexIDs, catanTile.vertexIDs)
                && Arrays.equals(edgeIDs, catanTile.edgeIDs) && tileType == catanTile.tileType;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), hex, tileType, number);
        result = 31 * result + Arrays.hashCode(vertexIDs);
        result = 31 * result + Arrays.hashCode(edgeIDs);
        return result;
    }

    @Override
    public String toString() {
        return tileType + " " + number + " at " + hex;
    }
}
