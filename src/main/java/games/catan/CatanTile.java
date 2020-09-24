package games.catan;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

public class CatanTile {
    public final int radius = 40;
    // todo re-rendering might be required due resizing the window so instead of hexagon getHexagon() could work better
    // x and y are the centre locations
    private int x;
    private int y;

    public double x_coord;
    public double y_coord;
    int[] edges;
    int[] vertices;
    // hexagon is the actual object that can be drawn on the screen
    private Polygon hexagon;
    CatanParameters.TileType tileType;
    int number;

    public CatanTile(int x, int y) {
        this.x = x;
        this.y = y;
        this.hexagon = createHexagon();
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
        if (this.edges[edge] == 1) return false;
        this.edges[edge] = 1;
        return true;
    }

    public boolean addSettlement(int vertex){
        if (this.vertices[vertex] >= 1) return false;
        this.vertices[vertex] = 1;
        return true;
    }

    public boolean addCity(int vertex){
        if (this.vertices[vertex] != 1) return false;
        this.vertices[vertex] = 2;
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
}
