package games.catan.gui;

import core.components.Edge;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.actions.*;
import games.catan.components.CatanTile;
import games.catan.components.Building;
import core.actions.AbstractAction;
import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
public class CatanBoardView extends JComponent {
    CatanGameState gs;
    CatanParameters params;

    private int tileRadius;
    private int robberRadius = 10;
    private int harbourRadius = 10;
    private int buildingRadius = 10;
    private int numberRadius = 25;

    HashMap<CatanTile.TileType, Color> tileColourMap = new HashMap<CatanTile.TileType, Color>() {{
        put(CatanTile.TileType.DESERT, new Color(210, 203, 181));
        put(CatanTile.TileType.SEA, new Color(40, 157, 197));
        put(CatanTile.TileType.FIELDS, new Color(248, 196, 63));
        put(CatanTile.TileType.FOREST, new Color(26, 108, 26));
        put(CatanTile.TileType.MOUNTAINS, new Color(65, 61, 61));
        put(CatanTile.TileType.PASTURE, new Color(140, 220, 127));
        put(CatanTile.TileType.HILLS, new Color(210, 124, 70));
    }};
    HashMap<CatanTile.TileType, Color> textColourMap = new HashMap<CatanTile.TileType, Color>() {{
        put(CatanTile.TileType.DESERT, new Color(77, 61, 10));
        put(CatanTile.TileType.SEA, new Color(229, 235, 238));
        put(CatanTile.TileType.FIELDS, new Color(38, 31, 9));
        put(CatanTile.TileType.FOREST, new Color(213, 236, 213));
        put(CatanTile.TileType.MOUNTAINS, new Color(243, 243, 243));
        put(CatanTile.TileType.PASTURE, new Color(30, 49, 28));
        put(CatanTile.TileType.HILLS, new Color(49, 29, 16));
    }};
    Color numberColor = new Color(227, 211, 169);
    Color tileColorHighlight = new Color(89, 243, 138);
    Dimension size;

    Point tileHighlight;
    Pair<Point, Integer>  buildingHighlight;
    Pair<Point, Integer> roadHighlight;

    public CatanBoardView(CatanGameState gs) {
        this.gs = gs;
        this.params = (CatanParameters) gs.getGameParameters();
        this.tileRadius = 40;
        size = new Dimension((params.n_tiles_per_row-1) * tileRadius * 2 + 10, (params.n_tiles_per_row-1) * tileRadius * 2);
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

        int midX = board.length/2;
        int midY = board[0].length/2;
        CatanTile midTile = new CatanTile(midX, midY);


        for (CatanTile[] tiles : board) {
            for (CatanTile tile : tiles) {
                // mid_x should be the same as the distance
                if (midTile.getDistanceToTile(tile) >= midX + 1) {
                    continue;
                }
                Point centreCoords = tile.getCentreCoords(tileRadius);

                g.setColor(tileColourMap.get(tile.getTileType()));
                Polygon tileHex = tile.getHexagon(tileRadius);
                g.fillPolygon(tileHex);
                if (tileHighlight != null && tile.x == tileHighlight.x && tile.y == tileHighlight.y) g.setColor(tileColorHighlight);
                else g.setColor(Color.BLACK);
                g.drawPolygon(tileHex);

                if (tile.hasRobber()) {
                    drawRobber(g, centreCoords);
                }

                if (tile.getTileType() != CatanTile.TileType.SEA && tile.getTileType() != CatanTile.TileType.DESERT) {
                    g.setColor(textColourMap.get(tile.getTileType()));
                    String type = "" + tile.getTileType();
                    int number = tile.getNumber();
                    g.drawString(type, centreCoords.x - 20, centreCoords.y);
                    if (number != 0) {
//                    g.drawString((tile.x + " " + tile.y), (int) tile.x_coord, (int) tile.y_coord + 20);
                        g.setColor(numberColor);
                        g.fillOval(centreCoords.x - numberRadius / 2, centreCoords.y + 20 - numberRadius / 2, numberRadius, numberRadius);
                        g.setColor(Color.BLACK);
                        g.drawOval(centreCoords.x - numberRadius / 2, centreCoords.y + 20 - numberRadius / 2, numberRadius, numberRadius);
                        g.drawString(""+number, centreCoords.x - (number < 10? 4: 8), centreCoords.y + 25);
                    }
                }
            }
        }

        // Draw roads top of board
        for (CatanTile[] catanTiles : board) {
            for (CatanTile tile : catanTiles) {
                // draw roads
                Edge[] roads = gs.getRoads(tile);
                for (int i = 0; i < roads.length; i++) {
                    if (roads[i] != null && roads[i].getOwnerId() != -1)
                        drawRoad(g, i, tile.getEdgeCoords(i, tileRadius), CatanConstants.PlayerColors[roads[i].getOwnerId()]);

                    // Useful for showing road IDs on the GUI
//                        g.setFont(new Font("TimeRoman", Font.PLAIN, 10));
//                        g.setColor(Color.BLACK);
//                        Point[] location = tile.getEdgeCoords(i, tileRadius);
//                        g.drawLine(location[0].x, location[0].y, location[1].x, location[1].y);
//                        g.drawString(i + "", ((location[0].x + location[1].x) / 2), ((location[0].y + location[1].y) / 2));
                }
            }
        }

        // Finally draw settlements
        HashSet<Integer> buildingsDrawn = new HashSet<>();  // avoid overlap
        for (CatanTile[] catanTiles : board) {
            for (CatanTile tile : catanTiles) {
                // draw settlements
                Building[] settlements = gs.getBuildings(tile);
                for (int i = 0; i < settlements.length; i++) {
//                    g.drawString("" + settlements[i].hashCode(), tile.getVerticesCoords(i).x, tile.getVerticesCoords(i).y);
                    if (!buildingsDrawn.contains(settlements[i].getComponentID()) && settlements[i].getOwnerId() != -1 ||
                        buildingHighlight != null && buildingHighlight.a.x == tile.x && buildingHighlight.a.y == tile.y && buildingHighlight.b == i) {
                        drawSettlement(g, tile.x, tile.y, i, tile.getVerticesCoords(i, tileRadius), CatanConstants.getPlayerColor(settlements[i].getOwnerId()), settlements[i].getBuildingType());
                        buildingsDrawn.add(settlements[i].getComponentID());
                    }

                    // Lines below are useful for debugging as they display settlement IDs
                    /*
                        g.setFont(new Font("TimeRoman", Font.PLAIN, 20));
                        g.setColor(Color.GRAY);
                        g.drawString(settlements[i].getComponentID() + "", tile.getVerticesCoords(i, tileRadius).x, tile.getVerticesCoords(i, tileRadius).y);
                     */

                    if (settlements[i].getHarbour() != null && !buildingsDrawn.contains(settlements[i].getComponentID())) {
                        CatanParameters.Resource type = settlements[i].getHarbour();
                        drawHarbour(g, tile.getVerticesCoords(i, tileRadius), type);
                        buildingsDrawn.add(settlements[i].getComponentID());
                    }
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

    public void drawRobber(Graphics2D g, Point point){
        g.setColor(Color.BLACK);
        g.fillOval(point.x, point.y, robberRadius, robberRadius);
    }

    public void drawHarbour(Graphics2D g, Point point, CatanParameters.Resource type) {
        g.setColor(Color.WHITE);
        g.fillOval(point.x, point.y, harbourRadius, harbourRadius);
        int exchangeRate = params.harbour_exchange_rate;
        if (type == CatanParameters.Resource.WILD) exchangeRate = params.harbour_wild_exchange_rate;
        g.drawString(type.name() + " " + exchangeRate + ":1", point.x, point.y+10);
    }

    public void drawRoad(Graphics2D g, int edge, Point[] location, Color color){
        g.setColor(color);
        Stroke stroke = g.getStroke();
        g.setStroke(new BasicStroke(5));
        g.drawLine(location[0].x, location[0].y, location[1].x, location[1].y);
        g.setStroke(stroke);

//        g.drawString("" + edge, location[0].x + 5, location[0].y);
    }

    public void drawSettlement(Graphics2D g, int x, int y, int vertex, Point point, Color color, Building.Type type){

        /* point is the centre of the hexagon
        *  / \  settl.  / \___  city
        * |   |         |    |
        * -----         ------
        * */
        // Create a polygon to contain x,y coordinates
        Polygon settlement = new Polygon();
        settlement.addPoint(point.x - buildingRadius /2, point.y- buildingRadius /2);
        settlement.addPoint(point.x, point.y - buildingRadius);
        settlement.addPoint(point.x + buildingRadius /2, point.y- buildingRadius /2);
        if (type == Building.Type.City){
            settlement.addPoint(point.x + buildingRadius, point.y - buildingRadius /2);
            settlement.addPoint(point.x + buildingRadius, point.y + buildingRadius /2);
        }
        settlement.addPoint(point.x + buildingRadius /2, point.y+ buildingRadius /2);
        settlement.addPoint(point.x - buildingRadius /2, point.y+ buildingRadius /2);

        g.setColor(color);
        g.fillPolygon(settlement);
        if (buildingHighlight != null && buildingHighlight.a.x == x && buildingHighlight.a.y == y && buildingHighlight.b == vertex) g.setColor(tileColorHighlight);
        else g.setColor(Color.BLACK);
        g.drawPolygon(settlement);

//        g.drawString("" + vertex, point.x, point.y);
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

    public void highlight(AbstractAction action) {
        if (action instanceof BuildCity) {
            // Highlight board node
        }
        else if (action instanceof BuildRoad) {
            // Highlight edge
        }
        else if (action instanceof BuildSettlement) {
            // Highlight board node
        }
        else if (action instanceof DeepPlaceSettlementThenRoad) {
            // settle
        }
        else if (action instanceof MoveRobber) {  // Includes MoveRobberAndSteal
            // tile
            MoveRobber mr = (MoveRobber) action;
            tileHighlight = new Point(mr.x, mr.y);
        }
        else if (action instanceof PlaceSettlementWithRoad) {
            // settle + road
            PlaceSettlementWithRoad pswr = (PlaceSettlementWithRoad) action;
            buildingHighlight = new Pair<>(new Point(pswr.x, pswr.y), pswr.vertex);
            roadHighlight = new Pair<>(new Point(pswr.x, pswr.y), pswr.edge);
        }
    }

    public void clearHighlight() {
        tileHighlight = null;
        buildingHighlight = null;
        roadHighlight = null;
    }
}
