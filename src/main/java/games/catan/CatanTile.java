package games.catan;

import java.awt.*;

public class CatanTile {
    public final int radius = 40;
    // x and y are the center locations
    private int x;
    private int y;
    int[] edges;
    int[] vertices;
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
            // first line
            offset_x = width;
            offset_y = height * 0.5;
        } else {
            // second line
            offset_x = width * 0.5;
            offset_y = height * 0.5;
        }
        double x_coord = offset_x + x * width;
        double y_coord = offset_y + y * height * 0.75;
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
}
