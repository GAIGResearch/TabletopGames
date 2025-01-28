package games.catan.components;

import core.components.BoardNode;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

import static games.catan.CatanConstants.HEX_SIDES;

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

    /*
    Implementation of a Hexagon structure using "even-r" representation, meaning that the hexagons are oriented with
    having their "pointy" side facing up and every odd row is offset by 0.5 * width.
    */

    // x and y are r-even representation coordinates
    public final int x;
    public final int y;

    private final static Integer minusOne = -1;

    Integer[] verticesBoardNodeIDs, edgeIDs;  // ID of board node mapping to each vertex on this tile
    TileType tileType;
    int number;
    boolean robber;

    public CatanTile(int x, int y) {
        super(HEX_SIDES, "");
        this.x = x;
        this.y = y;
        verticesBoardNodeIDs = new Integer[HEX_SIDES];
        edgeIDs = new Integer[HEX_SIDES];
        Arrays.fill(verticesBoardNodeIDs, minusOne);
        Arrays.fill(edgeIDs, minusOne);
        robber = false;
    }

    protected CatanTile(int x, int y, int componentId) {
        super(HEX_SIDES, "", componentId);
        this.x = x;
        this.y = y;
        verticesBoardNodeIDs = new Integer[HEX_SIDES];
        edgeIDs = new Integer[HEX_SIDES];
        Arrays.fill(verticesBoardNodeIDs, minusOne);
        Arrays.fill(edgeIDs, minusOne);
        robber = false;
    }

    public void setTileType(TileType type) {
        this.tileType = type;
    }

    public TileType getTileType() {
        return this.tileType;
    }

    public Polygon getHexagon(double radius) {
        Polygon polygon = new Polygon();
        Point centreCoords = getCentreCoords(radius);
        double x_coord = centreCoords.x;
        double y_coord = centreCoords.y;
        for (int i = 0; i < HEX_SIDES; i++) {
            double angle_deg = i * 60 - 30;
            double angle_rad = Math.PI / 180 * angle_deg;
            int xval = (int) (x_coord + radius * Math.cos(angle_rad));
            int yval = (int) (y_coord + radius * Math.sin(angle_rad));
            polygon.addPoint(xval, yval);
        }
        return polygon;
    }

    public boolean hasRobber() {
        return robber;
    }

    public void placeRobber() {
        this.robber = true;
    }

    public boolean removeRobber() {
        if (this.robber) {
            this.robber = false;
            return true;
        }
        throw new AssertionError("Cannot remove robber");
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Integer[] getVerticesBoardNodeIDs() {
        return verticesBoardNodeIDs;
    }

    public void setVertexBoardNodeID(int vertex, int id) {
        verticesBoardNodeIDs[vertex] = id;
    }

    public Integer[] getEdgeIDs() {
        return edgeIDs;
    }

    public void setEdgeID(int edge, int id) {
        edgeIDs[edge] = id;
    }

    public int getDistanceToTile(CatanTile tile) {
        int[] this_coord = toCube();
        int[] other_coord = tile.toCube();
        return (Math.abs(this_coord[0] - other_coord[0]) +
                Math.abs(this_coord[1] - other_coord[1]) + Math.abs(this_coord[2] - other_coord[2])) / 2;
    }

    public Point getCentreCoords(double radius) {
        // offset used in the even-r representation
        double offset_y;
        double offset_x;

        // width and height of a hexagon in pointy rotation
        double width = Math.sqrt(3) * radius;
        double height = 2 * radius;

        if (y % 2 == 0) {
            // even lines
            offset_x = width;
            offset_y = height * 0.5;
        } else {
            // odd lines
            offset_x = width * 0.5;
            offset_y = height * 0.5;
        }
        double x_coord = offset_x + x * width;
        double y_coord = offset_y + y * height * 0.75;
        return new Point((int) x_coord, (int) y_coord);
    }

    public Point getVerticesCoords(int vertex, double radius) {
        double angle_deg = vertex * 60 - 30;
        double angle_rad = Math.PI / 180 * angle_deg;
        Point centreCoords = getCentreCoords(radius);
        int xval = (int) (centreCoords.x + radius * Math.cos(angle_rad));
        int yval = (int) (centreCoords.y + radius * Math.sin(angle_rad));
        return new Point(xval, yval);
    }

    public Point[] getEdgeCoords(int edge, double radius) {
        return new Point[]{getVerticesCoords(edge, radius), getVerticesCoords((edge + 1) % HEX_SIDES, radius)};
    }

    // Static methods
    public int[] toCube() {
        int[] cube = new int[3];
        cube[0] = x - (y + (y % 2)) / 2;
        cube[2] = y;
        cube[1] = -cube[0] - cube[2];
        return cube;
    }

    public int[] getNeighbourOnEdge(int edge) {
        // returns coordinates to the other tile in the given direction
        // Even-r offset mapping; Different layouts require different values
        int[][][] evenr_directions = {
                {{1, 0}, {1, 1}, {0, 1},
                        {-1, 0}, {0, -1}, {1, -1}},
                {{1, 0}, {0, 1}, {-1, 1},
                        {-1, 0}, {-1, -1}, {0, -1}}
        };
        int parity = y & 1;
        int[] direction = evenr_directions[parity][edge];
        return new int[]{x + direction[0], y + direction[1]};
    }

    public int[][] getNeighboursOnVertex(int vertex) {
        // returns coordinates to the 2 other tiles on a vertex in a clockwise direction
        // Even-r offset mapping; Different layouts require different values
        int[][][] evenr_directions = {
                {{1, 0}, {1, 1}, {0, 1},
                        {-1, 0}, {0, -1}, {1, -1}},
                {{1, 0}, {0, 1}, {-1, 1},
                        {-1, 0}, {-1, -1}, {0, -1}}
        };
        int parity = y & 1;

        // to get the previous element we go back to the previous index (-1) but to go around has to add 6 and check % 6
        // i.e: in case of vertex 0 it should go to 5
        int[] direction_first = evenr_directions[parity][(vertex + 5) % HEX_SIDES];
        int[] direction_second = evenr_directions[parity][vertex];
        // Add both coordinates to the tiles
        return new int[][]{{x + direction_first[0], y + direction_first[1]},
                {x + direction_second[0], y + direction_second[1]}};
    }

    public CatanTile copy() {
        CatanTile copy = new CatanTile(x, y, componentID);
        copy.verticesBoardNodeIDs = verticesBoardNodeIDs.clone();
        copy.edgeIDs = edgeIDs.clone();
        copy.robber = robber;
        copy.tileType = tileType;
        copy.number = number;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatanTile)) return false;
        if (!super.equals(o)) return false;
        CatanTile catanTile = (CatanTile) o;
        return x == catanTile.x && y == catanTile.y && number == catanTile.number && robber == catanTile.robber && Arrays.equals(verticesBoardNodeIDs, catanTile.verticesBoardNodeIDs) && Arrays.equals(edgeIDs, catanTile.edgeIDs) && tileType == catanTile.tileType;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), x, y, tileType, number, robber);
        result = 31 * result + Arrays.hashCode(verticesBoardNodeIDs);
        result = 31 * result + Arrays.hashCode(edgeIDs);
        return result;
    }

    @Override
    public String toString() {
        return tileType + " " + number + " at (" + x + ";" + y + ")" + (robber ? " [R]" : "");
    }
}
