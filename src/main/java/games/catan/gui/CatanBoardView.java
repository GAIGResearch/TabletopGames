package games.catan.gui;

import core.actions.AbstractAction;
import core.components.Edge;
import core.components.grid.HexCoord;
import core.components.grid.HexSide;
import core.components.grid.HexVertex;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.actions.build.BuildCity;
import games.catan.actions.build.BuildRoad;
import games.catan.actions.build.BuildSettlement;
import games.catan.actions.robber.MoveRobber;
import games.catan.actions.setup.DeepPlaceSettlementThenRoad;
import games.catan.actions.setup.PlaceSettlementWithRoad;
import games.catan.components.Building;
import games.catan.components.CatanTile;
import gui.HexLayout;
import gui.IScreenHighlight;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static games.catan.CatanConstants.HEX_SIDES;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
public class CatanBoardView extends JComponent implements IScreenHighlight {
    CatanGameState gs;
    CatanParameters params;

    private int tileRadius;
    private int robberRadius = 20;
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

    HexLayout layout;
    HexCoord tileHighlight;
    HexVertex buildingHighlight;
    HexSide roadHighlight;

    HashMap<Integer, Integer> nDotsPerRoll = new HashMap<>();

    // Highlights from clicking on the board
    HashMap<HexVertex, Rectangle> vertexToRectMap;
    Set<HexVertex> vertexHighlight;
    HashMap<HexSide, Rectangle> edgeToRectMap;
    Set<HexSide> edgeHighlight;
    HashMap<HexCoord, Rectangle> hexToRectMap;
    Set<HexCoord> hexHighlight;
    int minSize = 10;

    public CatanBoardView(CatanGameState gs) {
        this.gs = gs;
        this.params = (CatanParameters) gs.getGameParameters();
        this.tileRadius = 40;
        size = new Dimension((params.n_tiles_per_row-1) * tileRadius * 2 + 10, (params.n_tiles_per_row-1) * tileRadius * 2);
        layout = new HexLayout(tileRadius, size.width / 2.0, size.height / 2.0);

        int nDots = 0;
        for (int i = params.nDice; i <= params.nDice*params.dieType.nSides; i++) {
            if (i <= params.robber_die_roll) {
                nDots++;
            } else if (nDots > 0) {
                nDots--;
            }
            nDotsPerRoll.put(i, nDots);
        }

        edgeToRectMap = new HashMap<>();
        vertexToRectMap = new HashMap<>();
        hexToRectMap = new HashMap<>();
        vertexHighlight = new HashSet<>();
        edgeHighlight = new HashSet<>();
        hexHighlight = new HashSet<>();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                vertexHighlight.clear();
                edgeHighlight.clear();
                hexHighlight.clear();
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Left-click
                    for (Map.Entry<HexVertex, Rectangle> entry: vertexToRectMap.entrySet()) {
                        if (entry.getValue().contains(e.getPoint())) {
                            vertexHighlight.add(entry.getKey());
                        }
                    }
                    for (Map.Entry<HexSide, Rectangle> entry: edgeToRectMap.entrySet()) {
                        if (entry.getValue().contains(e.getPoint())) {
                            edgeHighlight.add(entry.getKey());
                        }
                    }
                    for (Map.Entry<HexCoord, Rectangle> entry: hexToRectMap.entrySet()) {
                        if (entry.getValue().contains(e.getPoint())) {
                            hexHighlight.add(entry.getKey());
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        super.paintComponent(g);
        drawBoard(g2);
    }

    private void drawBoard(Graphics2D g) {
        // Draw board
        FontMetrics fm = g.getFontMetrics();
        Font f = g.getFont();
        Font boldFont = new Font(g.getFont().getName(), Font.BOLD, 12);
        for (CatanTile tile : gs.getTiles()) {
            Point centreCoords = layout.pixel(tile.hex);
            hexToRectMap.put(tile.hex, new Rectangle(centreCoords.x - tileRadius/2, centreCoords.y - tileRadius/2, tileRadius, tileRadius));

            g.setColor(tileColourMap.get(tile.getTileType()));
            g.fill(layout.outline(tile.hex, tileRadius));
            g.setColor(Color.BLACK);
            g.draw(layout.outline(tile.hex, tileRadius));

            if (tile.getTileType() != CatanTile.TileType.SEA && tile.getTileType() != CatanTile.TileType.DESERT) {
                g.setColor(textColourMap.get(tile.getTileType()));
                String type = "" + tile.getTileType();
                int number = tile.getNumber();
                g.drawString(type, centreCoords.x - fm.stringWidth(type)/2, centreCoords.y);
                if (number != 0) {
                    String nDots = "";
                    for (int i = 0; i < nDotsPerRoll.get(number); i++) {
                        nDots += ".";
                    }
                    int nSize = fm.stringWidth(nDots);
                    int nSize2 = fm.stringWidth(""+number);
                    g.setColor(numberColor);
                    g.fillOval(centreCoords.x - numberRadius / 2, centreCoords.y + 20 - numberRadius / 2, numberRadius, numberRadius);
                    g.setColor(Color.BLACK);
                    g.drawOval(centreCoords.x - numberRadius / 2, centreCoords.y + 20 - numberRadius / 2, numberRadius, numberRadius);
                    if (number-1 == params.robber_die_roll || number+1 == params.robber_die_roll) g.setColor(Color.red);
                    g.setFont(boldFont);
                    g.drawString(""+number, centreCoords.x - nSize2/2, centreCoords.y + 25);
                    g.drawString(nDots, centreCoords.x - nSize/2, centreCoords.y + 28);
                    g.setFont(f);
                }
            }

            if (gs.hasRobber(tile)) {
                drawRobber(g, centreCoords);
            }
        }

        // Draw harbours
        HashSet<Integer> harboursDrawn = new HashSet<>();  // avoid overlap
        for (CatanTile tile : gs.getTiles()) {
            Building[] settlements = gs.getBuildings(tile);
            for (int i = 0; i < settlements.length; i++) {
                if (settlements[i].getHarbour() != null && tile.getTileType() == CatanTile.TileType.SEA &&
                        !harboursDrawn.contains(settlements[i].getComponentID()) &&
                        settlements[(i+1)%HEX_SIDES].getHarbour() != null && !harboursDrawn.contains(settlements[(i+1)%HEX_SIDES].getComponentID())) {
                    CatanTile across = gs.getTile(tile.hex.neighbour(i));
                    if (across == null || across.getTileType() == CatanTile.TileType.SEA) continue;
                    CatanParameters.Resource type = settlements[i].getHarbour();
                    drawHarbour(g, tile, i, type);
                    harboursDrawn.add(settlements[i].getComponentID());
                    harboursDrawn.add(settlements[(i + 1)%HEX_SIDES].getComponentID());
                }
            }
        }

        // Draw roads top of board
        for (CatanTile tile : gs.getTiles()) {
            // draw roads
            Edge[] roads = gs.getRoads(tile);
            for (int i = 0; i < roads.length; i++) {
                Point[] p = {corner(tile, i), corner(tile, i + 1)};

                // Save position of edge
                Rectangle r = new Rectangle(Math.min(p[0].x, p[1].x), Math.min(p[0].y, p[1].y), Math.abs(p[0].x-p[1].x), Math.abs(p[0].y-p[1].y));
                if (r.width == 0) {
                    r.x -= minSize/2;
                    r.width = minSize;
                }
                if (r.height == 0) {
                    r.y -= minSize/2;
                    r.height = minSize;
                }
                edgeToRectMap.put(new HexSide(tile.hex, i), r);

                // Draw road if it exists or if highlighted
                if (roads[i] != null && roads[i].getOwnerId() != -1 ||
                        new HexSide(tile.hex, i).equals(roadHighlight)) {
                    drawRoad(g, i, p, CatanConstants.getPlayerColor(roads[i].getOwnerId()));
                }
            }
        }

        // Finally draw settlements
        HashSet<Integer> buildingsDrawn = new HashSet<>();  // avoid overlap
        for (CatanTile tile : gs.getTiles()) {
            // draw settlements
            Building[] settlements = gs.getBuildings(tile);
            for (int i = 0; i < settlements.length; i++) {
                Point p = corner(tile, i);
                HexVertex vertex = new HexVertex(tile.hex, i);

                // Save position of settlement
                vertexToRectMap.put(vertex, new Rectangle(p.x-buildingRadius/2,p.y-buildingRadius/2, buildingRadius, buildingRadius));

                if (!buildingsDrawn.contains(settlements[i].getComponentID()) && settlements[i].getOwnerId() != -1 ||
                    vertex.equals(buildingHighlight)) {
                    drawSettlement(g, vertex, p, CatanConstants.getPlayerColor(settlements[i].getOwnerId()), settlements[i].getBuildingType());
                    buildingsDrawn.add(settlements[i].getComponentID());
                }
            }
        }

        Stroke s = g.getStroke();
        if (tileHighlight != null) {
            g.setColor(tileColorHighlight);
            g.setStroke(new BasicStroke(8));
            g.draw(layout.outline(tileHighlight, tileRadius));
        }
        g.setStroke(new BasicStroke(3));
        if (!vertexHighlight.isEmpty()) {
            g.setColor(tileColorHighlight);
            Rectangle r = vertexToRectMap.get(vertexHighlight.iterator().next());
            g.drawRect(r.x, r.y, r.width, r.height);
        }
        if (!edgeHighlight.isEmpty()) {
            g.setColor(tileColorHighlight);
            Rectangle r = edgeToRectMap.get(edgeHighlight.iterator().next());
            g.drawRect(r.x, r.y, r.width, r.height);
        }
        if (!hexHighlight.isEmpty()) {
            g.setColor(tileColorHighlight);
            Rectangle r = hexToRectMap.get(hexHighlight.iterator().next());
            g.drawRect(r.x, r.y, r.width, r.height);
        }
        g.setStroke(s);
    }

    public void drawRobber(Graphics2D g, Point point){
        g.setColor(Color.BLACK);
        g.fillOval(point.x-robberRadius/2, point.y-robberRadius/2, robberRadius, robberRadius);
        Stroke s = g.getStroke();
        g.setColor(Color.red);
        g.setStroke(new BasicStroke(3));
        g.drawOval(point.x-robberRadius/2, point.y-robberRadius/2, robberRadius, robberRadius);
        g.setStroke(s);
    }

    public void drawHarbour(Graphics2D g, CatanTile tile, int i, CatanParameters.Resource type) {
        Point point1 = corner(tile, i);
        Point point2 = corner(tile, i + 1);
        Point middle = layout.pixel(tile.hex);
        FontMetrics fm = g.getFontMetrics();
        int exchangeRate = params.harbour_exchange_rate;
        if (type == CatanParameters.Resource.WILD) exchangeRate = params.harbour_wild_exchange_rate;
        String text = type.name() + " " + exchangeRate + ":1";
        int width = fm.stringWidth(text);

        g.setColor(Color.WHITE);
        Stroke s = g.getStroke();
        g.setStroke(new BasicStroke(4));
        g.drawLine(point1.x, point1.y, (middle.x + point1.x)/2, (middle.y+point1.y)/2);
        g.drawLine(point2.x, point2.y, (middle.x + point2.x)/2, (middle.y+point2.y)/2);
//        g.fillOval(point.x, point.y, harbourRadius, harbourRadius);
        g.setStroke(s);
        g.drawString(text, middle.x-width/2, middle.y+10);
    }

    public void drawRoad(Graphics2D g, int edge, Point[] location, Color color){
        Stroke stroke = g.getStroke();
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(5));
        g.drawLine(location[0].x, location[0].y, location[1].x, location[1].y);

        g.setColor(color);
        g.setStroke(new BasicStroke(3));
        g.drawLine(location[0].x, location[0].y, location[1].x, location[1].y);
        g.setStroke(stroke);

//        g.drawString("" + edge, location[0].x + 5, location[0].y);
    }

    /**
     * The pixel at the vertex of the tile.
     */
    private Point corner(CatanTile tile, int vertex) {
        Point2D p = layout.corner(tile.hex, vertex);
        return new Point((int) Math.round(p.getX()), (int) Math.round(p.getY()));
    }

    public void drawSettlement(Graphics2D g, HexVertex vertex, Point point, Color color, Building.Type type){

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
        if (vertex.equals(buildingHighlight)) g.setColor(tileColorHighlight);
        else g.setColor(Color.BLACK);
        g.drawPolygon(settlement);

//        g.drawString("" + vertex, point.x, point.y);
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        return size;
    }

    public void highlight(AbstractAction action) {
        if (action instanceof BuildCity) {
            // Highlight board node
            BuildCity buildCity = (BuildCity)action;
            buildingHighlight = new HexVertex(buildCity.hex, buildCity.vertex);
        }
        else if (action instanceof BuildRoad) {
            // Highlight edge
            BuildRoad buildRoad = (BuildRoad) action;
            roadHighlight = new HexSide(buildRoad.hex, buildRoad.edge);
        }
        else if (action instanceof BuildSettlement) {
            // Highlight board node
            BuildSettlement buildCity = (BuildSettlement)action;
            buildingHighlight = new HexVertex(buildCity.hex, buildCity.vertex);
        }
        else if (action instanceof DeepPlaceSettlementThenRoad) {
            // settle
            DeepPlaceSettlementThenRoad buildCity = (DeepPlaceSettlementThenRoad)action;
            buildingHighlight = new HexVertex(buildCity.hex, buildCity.vertex);
        }
        else if (action instanceof MoveRobber) {  // Includes MoveRobberAndSteal
            // tile
            MoveRobber mr = (MoveRobber) action;
            tileHighlight = mr.hex;
        }
        else if (action instanceof PlaceSettlementWithRoad) {
            // settle + road
            PlaceSettlementWithRoad pswr = (PlaceSettlementWithRoad) action;
            buildingHighlight = new HexVertex(pswr.hex, pswr.vertex);
            roadHighlight = new HexSide(pswr.hex, pswr.edge);
        }
    }

    @Override
    public void clearHighlights() {
        tileHighlight = null;
        buildingHighlight = null;
        roadHighlight = null;
        edgeHighlight.clear();
        vertexHighlight.clear();
        hexHighlight.clear();
    }
}
