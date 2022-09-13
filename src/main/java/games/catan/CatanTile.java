package games.catan;

import games.catan.components.Road;
import games.catan.components.Settlement;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

import static games.catan.CatanConstants.HEX_SIDES;

public class CatanTile {
    /*
    Implementation of a Hexagon structure using "even-r" representation, meaning that the hexagons are oriented with
    having their "pointy" side facing up and every odd row is offset by 0.5 * width.
    */

    // x and y are r-even representation coordinates
    public final int x;
    public final int y;

    Road[] roads;
    Settlement[] settlements;
    int[] harbors;

    CatanParameters.TileType tileType;
    private boolean hasHarbor;
    int number;
    boolean robber;

    public CatanTile(int x, int y) {
        this.x = x;
        this.y = y;
        roads = new Road[HEX_SIDES];
        harbors = new int[HEX_SIDES];
        settlements = new Settlement[HEX_SIDES];
        robber = false;
    }

    public CatanTile(int x, int y, Road[] edges, Settlement[] vertices) {
        this.x = x;
        this.y = y;
        this.roads = edges;
        this.settlements = vertices;
        robber = false;
    }

    public void setTileType(CatanParameters.TileType type) {
        this.tileType = type;
    }

    public CatanParameters.TileType getType() {
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

    public boolean setRoad(int edge, Road road) {
        // if null -> uninitialized
        if (this.roads[edge] == null || this.roads[edge].getOwner() == -1) {
            this.roads[edge] = road;
            return true;
        }
        throw new AssertionError("Cannot set road: edge: " + edge);
    }

    public boolean addRoad(int edge, int playerID) {
        // if null -> uninitialized
        if (this.roads[edge].getOwner() == -1) {
            this.roads[edge].setOwner(playerID);
            return true;
        }
        throw new AssertionError("Cannot add road: edge: " + edge);
    }

    public Road[] getRoads() {
        return roads;
    }

    public boolean addHarbor(int edge, int type) {
        if (!hasHarbor) {
            this.harbors[edge] = type;
            this.hasHarbor = true;
            this.settlements[edge].setHarbour(CatanParameters.HarborTypes.values()[type]);
            this.settlements[(edge + 1) % 6].setHarbour(CatanParameters.HarborTypes.values()[type]);
            return true;
        }
        throw new AssertionError("Cannot add harbour: edge: " + edge);
    }

    public int[] getHarbors() {
        return harbors;
    }

    public boolean hasHarbor() {
        return hasHarbor;
    }

    public boolean addSettlement(int vertex, int playerID) {
        if (this.settlements[vertex].getOwner() == -1) {
            this.settlements[vertex].setOwner(playerID);
            return true;
        }
        throw new AssertionError("Cannot add settlement: vertex: " + vertex);
    }

    public boolean setSettlement(int vertex, Settlement settlement) {
        // if null -> uninitialized
        if (this.settlements[vertex] == null || this.settlements[vertex].getOwner() == -1) {
            this.settlements[vertex] = settlement;
            return true;
        }
        throw new AssertionError("Cannot set settlement: vertex: " + vertex);
    }

    public Settlement[] getSettlements() {
        return this.settlements;
    }

    public boolean addCity(int vertex) {
        if (this.settlements[vertex] == null) {
            throw new AssertionError("Cannot add city: vertex: " + vertex);
        } else {
            return this.settlements[vertex].upgrade();
        }
    }

    public int getDistanceToTile(CatanTile tile) {
        int[] this_coord = toCube(this);
        int[] other_coord = toCube(tile);
        int dist = (Math.abs(this_coord[0] - other_coord[0]) +
                Math.abs(this_coord[1] - other_coord[1]) + Math.abs(this_coord[2] - other_coord[2])) / 2;
        return dist;
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
    public static int[] toCube(CatanTile tile) {
        int[] cube = new int[3];
        cube[0] = tile.x - (tile.y + (tile.y % 2)) / 2;
        cube[2] = tile.y;
        cube[1] = -cube[0] - cube[2];
        return cube;
    }

    public static int[] getNeighbourOnEdge(CatanTile tile, int edge) {
        // returns coordinates to the other tile in the given direction
        // Even-r offset mapping; Different layouts require different values
        int[][][] evenr_directions = {
                {{1, 0}, {1, 1}, {0, 1},
                        {-1, 0}, {0, -1}, {1, -1}},
                {{1, 0}, {0, 1}, {-1, 1},
                        {-1, 0}, {-1, -1}, {0, -1}}
        };
        int parity = tile.y & 1;
        int[] direction = evenr_directions[parity][edge];
        return new int[]{tile.x + direction[0], tile.y + direction[1]};
    }

    public static int[][] getNeighboursOnVertex(CatanTile tile, int vertex) {
        // returns coordinates to the 2 other tiles on a vertex in a clockwise direction
        // Even-r offset mapping; Different layouts require different values
        int[][][] evenr_directions = {
                {{1, 0}, {1, 1}, {0, 1},
                        {-1, 0}, {0, -1}, {1, -1}},
                {{1, 0}, {0, 1}, {-1, 1},
                        {-1, 0}, {-1, -1}, {0, -1}}
        };
        int parity = tile.y & 1;

        // to get the previous element we go back to the previous index (-1) but to go around has to add 6 and check % 6
        // i.e: in case of vertex 0 it should go to 5
        int[] direction_first = evenr_directions[parity][(vertex + 5) % HEX_SIDES];
        int[] direction_second = evenr_directions[parity][vertex];
        // Add both coordinates to the tiles
        int[][] coords = {{tile.x + direction_first[0], tile.y + direction_first[1]},
                {tile.x + direction_second[0], tile.y + direction_second[1]}};
        return coords;
    }

    public CatanTile copy() {
        CatanTile copy = new CatanTile(x, y);
        copy.roads = new Road[HEX_SIDES];
        for (int i = 0; i < roads.length; i++) {
            copy.roads[i] = roads[i].copy();
        }
        copy.hasHarbor = this.hasHarbor;
        copy.harbors = Arrays.copyOf(harbors, harbors.length);
        copy.settlements = new Settlement[HEX_SIDES];
        for (int i = 0; i < settlements.length; i++) {
            copy.settlements[i] = settlements[i].copy();
        }
        copy.robber = robber;
        copy.tileType = tileType;
        copy.number = number;

        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof CatanTile) {
            CatanTile other = (CatanTile) obj;
            return x == other.x && y == other.y && robber == other.robber && hasHarbor == other.hasHarbor && other.number == number &&
                    Arrays.equals(roads, other.roads) && Arrays.equals(settlements, other.settlements) &&
                    Arrays.equals(harbors, other.harbors);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(x, y, tileType, number, hasHarbor, robber);
        result = result * 31 + Arrays.hashCode(roads);
        // The direct settlements hashcode does not include the possible state changes
        // because it is used as the key in the Graph of board nodes (not the ideal design choice)
        // which means that its hashcode is restricted to just the id (so that the key value does not change)
        int settlementHashCode = 0;
        for (Settlement settlement : settlements) {
            settlementHashCode = settlementHashCode + 3 * settlement.extendedHashCode();
        }
        result = result * 31 + settlementHashCode;
        result = result * 31 + Arrays.hashCode(harbors);
        return result;
    }

    @Override
    public String toString() {
        return "CatanTile at x " + x + " y " + y + " robber = " + robber;
    }
}
