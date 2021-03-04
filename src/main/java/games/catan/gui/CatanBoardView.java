package games.catan.gui;

import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTile;
import games.catan.components.Road;
import games.catan.components.Settlement;

import javax.swing.*;
import java.awt.*;

public class CatanBoardView extends JComponent {
    // todo calculate the optimal radius for the hexagons
    CatanGameState gs;
    private int width;
    private int height;

    public CatanBoardView(CatanGameState gs){
        this.gs = gs;
        this.height = 600;
        this.width = 600;
        setPreferredSize(new Dimension(width, height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        super.paintComponent(g);
        drawBoard(g2);
    }

    private void drawBoard(Graphics2D g) {

        // Draw board
        CatanTile[][] board = gs.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                g.setColor(tileColourLookup(tile));
                g.fillPolygon(tile.getHexagon());
                g.setColor(Color.BLACK);
                g.drawPolygon(tile.getHexagon());

                if (tile.hasRobber()){
                    drawRobber(g, new Point((int)tile.x_coord, (int)tile.y_coord));
                }

                String type = "" + tile.getType();
                String number = "" + tile.getNumber();
                g.drawString(type, (int) tile.x_coord - 20, (int) tile.y_coord);
                if (!number.equals("0"))
                    // todo set number back, but this is useful for debugging
//                    g.drawString((tile.x + " " + tile.y), (int) tile.x_coord, (int) tile.y_coord + 20);
                    g.drawString(number, (int) tile.x_coord, (int) tile.y_coord + 20);
            }
        }

        // Draw roads top of board
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];

                if (tile.hasHarbor()){
                    drawHarbor(g, tile);
                }

                // draw roads
                Road[] roads = tile.getRoads();
                for (int i = 0; i < roads.length; i++) {
                    if (roads[i] != null && roads[i].getOwner() != -1)
                        drawRoad(g, tile.getEdgeCoords(i), CatanConstants.PlayerColors[roads[i].getOwner()]);
                }
            }
        }
        // Finally draw settlements
        for (int x = 0; x < board.length; x++){
            for (int y = 0; y < board[x].length; y++){
                CatanTile tile = board[x][y];

                // draw settlements
                Settlement[] settlements = tile.getSettlements();
                for (int i = 0; i < settlements.length; i++){
//                    g.drawString("" + settlements[i].hashCode(), tile.getVerticesCoords(i).x, tile.getVerticesCoords(i).y);
                    if (settlements[i] != null && settlements[i].getOwner() != -1) {
                        drawSettlement(g, tile.getVerticesCoords(i), CatanConstants.PlayerColors[settlements[i].getOwner()], settlements[i].getType());
                    }
                    // todo lines below are useful for debugging as they display settlement IDs
//                    g.setFont(new Font("TimeRoman", Font.PLAIN, 20));
//                    g.setColor(Color.GRAY);
//                    g.drawString(settlements[i].getID() + "", tile.getVerticesCoords(i).x, tile.getVerticesCoords(i).y);
                }

                // lines below render cube coordinates and distances from middle
//                String s = Arrays.toString(tile.to_cube(tile));
//                String mid = Arrays.toString(tile.to_cube(board[3][3]));
//                String dist = "" + tile.distance(board[3][3]);
//                g.drawString(s, (int)tile.x_coord - 20, (int)tile.y_coord - 20);
//                g.drawString(mid, (int)tile.x_coord - 20, (int)tile.y_coord);
//                g.setColor(Color.ORANGE);
//                g.drawString(dist, (int)tile.x_coord - 20, (int)tile.y_coord + 20);


            }
        }
    }

    private Color tileColourLookup(CatanTile tile){
        if (tile.getType() == CatanParameters.TileType.DESERT){
            return Color.YELLOW;
        } else if (tile.getType() == CatanParameters.TileType.SEA){
            return new Color(51, 204, 255);//Color.BLUE;
        }else if (tile.getType() == CatanParameters.TileType.FIELDS){
            return new Color(255, 255, 153); //Color.LIGHT_GRAY;
        }else if (tile.getType() == CatanParameters.TileType.FOREST){
            return new Color(0, 102, 0); //Color.RED;
        }else if (tile.getType() == CatanParameters.TileType.MOUNTAINS){
            return Color.DARK_GRAY;
        }else if (tile.getType() == CatanParameters.TileType.PASTURE){
            return new Color(102, 255, 102); //Color.GREEN;
        } else if (tile.getType() == CatanParameters.TileType.HILLS){
            return new Color(102, 102, 102); //Color.ORANGE;
        } else{
            return Color.WHITE;
        }
    }

    public void drawRobber(Graphics2D g, Point point){
        int RADIUS = 10;
        g.setColor(Color.BLACK);
        g.fillOval(point.x, point.y, RADIUS, RADIUS);
    }

    public void drawHarbor(Graphics2D g, CatanTile tile){
        // todo rotate text? should be clear what harbor it is
        //  rotation below does not work as expected
        // todo draws a black road to represent the harbor for now
        int[] harbors = tile.getHarbors();
        for (int i = 0; i < harbors.length; i++){
            if (harbors[i] > 0){
                Color color = Color.BLACK;
                g.setColor(color);
                Stroke stroke = g.getStroke();
                g.setStroke(new BasicStroke(8));
                Point[] location = tile.getEdgeCoords(i);
                g.drawLine(location[0].x, location[0].y, location[1].x, location[1].y);
                g.setStroke(stroke);
//                AffineTransform original = g.getTransform();
//                g.rotate(Math.toRadians(-60));
                String type = CatanParameters.HarborTypes.values()[harbors[i]].toString();
                g.drawString((type + harbors[i]), (int)tile.x_coord, (int)tile.y_coord+10);
//                g.setTransform(original);
            }
        }

    }

    public void drawRoad(Graphics2D g, Point[] location, Color color){
        g.setColor(color);
        Stroke stroke = g.getStroke();
        g.setStroke(new BasicStroke(5));
        g.drawLine(location[0].x, location[0].y, location[1].x, location[1].y);
        g.setStroke(stroke);

    }

    public void drawSettlement(Graphics2D g, Point point, Color color, int city){

        /* point is the centre of the hexagon
        *  / \  settl.  / \___  city
        * |   |         |    |
        * -----         ------
        * */
        int RADIUS = 10;
        // Create a polygon to contain x,y coordinates
        Polygon settlement = new Polygon();
        settlement.addPoint(point.x - RADIUS/2, point.y-RADIUS/2);
        settlement.addPoint(point.x, point.y - RADIUS);
        settlement.addPoint(point.x + RADIUS/2, point.y-RADIUS/2);
        if (city == 2){
            settlement.addPoint(point.x + RADIUS, point.y - RADIUS/2);
            settlement.addPoint(point.x + RADIUS, point.y + RADIUS/2);
        }
        settlement.addPoint(point.x + RADIUS/2, point.y+RADIUS/2);
        settlement.addPoint(point.x - RADIUS/2, point.y+RADIUS/2);

        g.setColor(color);
        g.fillPolygon(settlement);
        g.setColor(Color.BLACK);
        g.drawPolygon(settlement);

    }
}
