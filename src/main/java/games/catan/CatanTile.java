package games.catan;

import java.awt.*;

public class CatanTile {
    // todo (mb) variables should be private

    public final int radius = 40;
    // x and y are r-even representation coordinates
    public int x;
    public int y;
    // x_coord, y_coord are the coordinates to the centre of the hex in pixels
    public double x_coord;
    public double y_coord;
    int[] roads;
    int[] settlements;

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
        roads = new int[6];
        settlements = new int[6];
        verticesCoords = new Point[6];
        edgeCoords = new Point[6][2];
        hexagon = createHexagon();
    }

    public CatanTile(int x, int y, int[] edges, int[] vertices) {
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

    public boolean addRoad(int edge){
        if (this.roads[edge] == 1) return false;
        this.roads[edge] = 1;
        return true;
    }

    public int[] getRoads(){
        return roads;
    }

    public boolean addSettlement(int vertex){
        if (this.settlements[vertex] >= 1) return false;
        this.settlements[vertex] = 1;
        return true;
    }

    public int[] getSettlements(){
        return this.settlements;
    }

    public boolean addCity(int vertex){
        if (this.settlements[vertex] != 1) return false;
        this.settlements[vertex] = 2;
        return true;
    }

    public int distance(CatanTile tile){
        int[] this_coord = to_cube(this);
        int[] other_coord = to_cube(tile);
        int dist = (Math.abs(this_coord[0] - other_coord[0]) +
                Math.abs(this_coord[1] - other_coord[1]) + Math.abs(this_coord[2] - other_coord[2])) / 2;
        return dist;
    }

    public int[] to_cube(CatanTile tile){
        int[] cube = new int[3];
        cube[0] = tile.x - (tile.y + (tile.y % 2)) / 2;
        cube[2] = tile.y;
        cube[1] = - cube[0] - cube[2];
        return cube;
    }

    public Point getVerticesCoords(int vertex){
        return verticesCoords[vertex];
    }

    public Point[] getEdgeCoords(int edge){
        return edgeCoords[edge];
    }
}
