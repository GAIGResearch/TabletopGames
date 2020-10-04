package games.catan;

import games.catan.components.Road;
import games.catan.components.Settlement;

import java.awt.*;

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

    // coordinates to vertices and edges to facilitate drawing roads
    Point[] verticesCoords;
    Point[][] edgeCoords;
    // hexagon is the actual object that can be drawn on the screen
    private Polygon hexagon;
    CatanParameters.TileType tileType;
    int number;

    public CatanTile(int x, int y) {
        this.x = x;
        this.y = y;
        roads = new Road[6];
        settlements = new Settlement[6];
        verticesCoords = new Point[6];
        edgeCoords = new Point[6][2];
        hexagon = createHexagon();
    }

    public CatanTile(int x, int y, Road[] edges, Settlement[] vertices) {
        this.x = x;
        this.y = y;
        this.roads = edges;
        this.settlements = vertices;
        verticesCoords = new Point[6];
        edgeCoords = new Point[6][2];
        hexagon = createHexagon();
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
        for (int i = 0; i < 6; i++) {
            double angle_deg = i * 60 - 30;
            double angle_rad = Math.PI / 180 * angle_deg;
            int xval = (int) (x_coord + radius * Math.cos(angle_rad));
            int yval = (int) (y_coord + radius * Math.sin(angle_rad));
            verticesCoords[i] = new Point(xval, yval);
            edgeCoords[i] = new Point[]{new Point(xval, yval), new Point(xval + 2, yval + 2)};
            polygon.addPoint(xval, yval);
        }
        return polygon;
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

    public boolean addRoad(int edge, int playerID){
        // todo test
        // if null -> uninitialized
        if (this.roads[edge] == null){
            this.roads[edge] = new Road(playerID);
            return true;
        }
        return false;
    }

    public Road[] getRoads(){
        return roads;
    }

    public boolean addSettlement(int vertex, int playerID){
        if (this.settlements[vertex] == null){
            this.settlements[vertex] = new Settlement(playerID);
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
                {{1, 0}, {1, -1}, {0, -1},
                        {-1, 0}, {0, 1}, {1, 1}},
                {{1, 0}, {0, -1}, {-1, -1},
                        {-1, 0}, {-1, 1}, {0, 1}}
        };
        int parity = tile.x & 1;
        int[] direction = evenr_directions[parity][edge];
        return new int[]{tile.x + direction[0], tile.y + direction[1]};

    }
}
