package games.catan;

import games.catan.components.Road;
import games.catan.components.Settlement;

import java.awt.*;
import java.util.Arrays;

import static games.catan.CatanConstants.HEX_SIDES;

public class CatanTile {
    /*
    Implementation of a Hexagon structure using "even-r" representation, meaning that the hexagons are oriented with
    having their "pointy" side facing up and every odd row is offset by 0.5 * width.
    */
    // todo (mb) variables should be private

    public final int radius = 40;
    // x and y are r-even representation coordinates
    public int x;
    public int y;

    // x_coord, y_coord are the coordinates to the centre of the hex in pixels
    public double x_coord;
    public double y_coord;

    Road[] roads;
    Settlement[] settlements;
    int[] harbors;
    private boolean hasHarbor;

    // coordinates to vertices and edges to facilitate drawing roads
    // hexagon is the actual object that can be drawn on the screen
    Point[] verticesCoords;
    Point[][] edgeCoords;
    private Polygon hexagon;

    CatanParameters.TileType tileType;
    int number;
    boolean robber;

    public CatanTile(int x, int y) {
        this.x = x;
        this.y = y;
        roads = new Road[HEX_SIDES];
        harbors = new int[HEX_SIDES];
        settlements = new Settlement[HEX_SIDES];
        verticesCoords = new Point[HEX_SIDES];
        edgeCoords = new Point[HEX_SIDES][2];
        hexagon = createHexagon();
        robber = false;
    }

    public CatanTile(int x, int y, Road[] edges, Settlement[] vertices) {
        this.x = x;
        this.y = y;
        this.roads = edges;
        this.settlements = vertices;
        verticesCoords = new Point[HEX_SIDES];
        edgeCoords = new Point[HEX_SIDES][2];
        hexagon = createHexagon();
        robber = false;
    }

    public void setTileType(CatanParameters.TileType type){
        this.tileType = type;
    }
    public CatanParameters.TileType getType(){
        return this.tileType;
    }

    private Polygon createHexagon() {
        Polygon polygon = new Polygon();

        // width and height of a hexagon in pointy rotation
        double width = Math.sqrt(3) * radius;
        double height = 2 * radius;

        // uses "even r" representation for efficiency
        // offset is the shift from the origin for the first hexagons on the board
        double offset_y ;
        double offset_x;
        if (y % 2 == 0) {
            // even lines
            offset_x = width;
            offset_y = height * 0.5;
        } else {
            // odd lines
            offset_x = width * 0.5;
            offset_y = height * 0.5;
        }
        x_coord = offset_x + x * width;
        y_coord = offset_y + y * height * 0.75;
        for (int i = 0; i < HEX_SIDES; i++) {
            double angle_deg = i * 60 - 30;
            double angle_rad = Math.PI / 180 * angle_deg;
            int xval = (int) (x_coord + radius * Math.cos(angle_rad));
            int yval = (int) (y_coord + radius * Math.sin(angle_rad));
            verticesCoords[i] = new Point(xval, yval);
            polygon.addPoint(xval, yval);
        }
        // set correct coordinates for edges
        for (int i = 0; i < verticesCoords.length; i++){
            edgeCoords[i] = new Point[]{verticesCoords[i], verticesCoords[(i + 1) % HEX_SIDES]};
        }
        return polygon;
    }

    public boolean hasRobber(){
        return robber;
    }

    public void placeRobber(){
        this.robber = true;
    }

    public boolean removeRobber(){
        if (this.robber){
            this.robber = false;
            return true;
        }
        return false;
    }

    public Polygon getHexagon(){
        return hexagon;
    }

    public int getNumber(){
        return number;
    }

    public void setNumber(int number){
        this.number = number;
    }

    public boolean setRoad(int edge, Road road){
        // if null -> uninitialized
        if (this.roads[edge] == null || this.roads[edge].getOwner() == -1){
            this.roads[edge] = road;
            return true;
        }
        return false;
    }

    public boolean addRoad(int edge, int playerID){
        // if null -> uninitialized
        if (this.roads[edge].getOwner() == -1){
            this.roads[edge].setOwner(playerID);
            return true;
        }
        return false;
    }

    public Road[] getRoads(){
        return roads;
    }

    public boolean addHarbor(int edge, int type){
        if (!hasHarbor) {
            this.harbors[edge] = type;
            this.hasHarbor = true;
            return true;
        }
        return false;
    }

    public int[] getHarbors(){
        return harbors;
    }

    public boolean hasHarbor(){
        return hasHarbor;
    }

    public boolean addSettlement(int vertex, int playerID){
        if (this.settlements[vertex].getOwner() == -1){
            this.settlements[vertex].setOwner(playerID);
            return true;
        }
        return false;
    }

    public boolean setSettlement(int vertex, Settlement settlement){
        // if null -> uninitialized
        if (this.settlements[vertex] == null || this.settlements[vertex].getOwner() == -1){
            this.settlements[vertex] = settlement;
            return true;
        }
        return false;
    }

    public Settlement[] getSettlements(){
        return this.settlements;
    }

    public boolean addCity(int vertex){
        if (this.settlements[vertex] == null){
            return false;
        } else{
            return this.settlements[vertex].upgrade();
        }
    }

    public int distance(CatanTile tile){
        int[] this_coord = to_cube(this);
        int[] other_coord = to_cube(tile);
        int dist = (Math.abs(this_coord[0] - other_coord[0]) +
                Math.abs(this_coord[1] - other_coord[1]) + Math.abs(this_coord[2] - other_coord[2])) / 2;
        return dist;
    }

    public Point getVerticesCoords(int vertex){
        return verticesCoords[vertex];
    }

    public Point[] getEdgeCoords(int edge){
        return edgeCoords[edge];
    }

    // Static methods
    public static int[] to_cube(CatanTile tile){
        int[] cube = new int[3];
        cube[0] = tile.x - (tile.y + (tile.y % 2)) / 2;
        cube[2] = tile.y;
        cube[1] = - cube[0] - cube[2];
        return cube;
    }

    public static int[] get_neighbour_on_edge(CatanTile tile, int edge){
        // returns coordinates to the other tile in the given direction
        // Even-r offset mapping; Different layouts require different values
        int[][][] evenr_directions = {
                {{1, 0}, {1, 1}, {0, 1},
                        {-1, 0}, {0, -1}, {1, -1}},
                {{1, 0}, {0, 1}, {-1, 1},
                        {-1, 0}, {-1, -1}, {0, -1}}
        };
        int parity = tile.y  & 1;
        int[] direction = evenr_directions[parity][edge];
        return new int[]{tile.x + direction[0], tile.y + direction[1]};
    }

    public static int[][] get_neighbours_on_vertex(CatanTile tile, int vertex){
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
        for (int i = 0 ; i < roads.length; i++){
            copy.roads[i] = roads[i].copy();
        }
        copy.harbors = new int[HEX_SIDES];
        copy.harbors = Arrays.copyOf(harbors, harbors.length);
        copy.settlements = new Settlement[HEX_SIDES];
        for (int i = 0 ; i < settlements.length; i++){
            copy.settlements[i] = settlements[i].copy();
        }
        copy.robber = robber;
        copy.tileType = tileType;

        return copy;
    }
}
