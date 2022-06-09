package games.descent2e.gui;

import core.components.BoardNode;
import core.components.GridBoard;
import core.properties.PropertyColor;
import core.properties.PropertyString;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.DescentTypes;
import games.descent2e.components.Hero;
import games.descent2e.components.tokens.DToken;
import games.descent2e.components.Monster;
import gui.views.ComponentView;
import utilities.ImageIO;
import utilities.Pair;
import utilities.Vector2D;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import static core.CoreConstants.*;
import static gui.AbstractGUIManager.defaultItemSize;
import static utilities.Utils.*;

public class DescentGridBoardView extends ComponentView {

    public static HashMap<String, Color> colorMap = new HashMap<String, Color>() {{
        put("null", Color.gray);
        put(null, Color.gray);
        put("edge", Color.gray);
        put("plain", Color.white);
        put("block", Color.red);
        put("lava", Color.orange);
        put("water", Color.blue);
        put("hazard", Color.green);
        put("open", Color.lightGray);
        put("pit", Color.darkGray);
    }};

    private DescentGameState gameState;
    static int descentItemSize;

    int panX, panY;
    double scale = 1;
    Set<Vector2D> highlights;
    int maxHighlights = 3;

    public DescentGridBoardView(GridBoard gridBoard, DescentGameState gameState, int width, int height) {
        super(gridBoard, width, height);
        this.gameState = gameState;
        updateScale(scale);

        // Focus on hero characters
        Hero hero = gameState.getHeroes().get(0);
        Vector2D pos = hero.getPosition();
        panX = pos.getX() * descentItemSize / 2;
        panY = -pos.getY() * descentItemSize / 2 - descentItemSize * 3;

        highlights = new HashSet<>();
        addMouseWheelListener(e -> {
            double amount = 0.2 * Math.abs(e.getPreciseWheelRotation());
            if (e.getPreciseWheelRotation() > 0) {
                // Rotated down, zoom out
                updateScale(scale - amount);
            } else {
                updateScale(scale + amount);
            }
            highlights.clear();
        });
        addMouseListener(new MouseAdapter() {
            Point start;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    // Middle (wheel) click, pan around
                    start = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2 && start != null) {
                    // Middle (wheel) click, pan around
                    Point end = e.getPoint();
                    panX += (int)(scale * (end.x - start.x));
                    panY += (int)(scale * (end.y - start.y));
                    start = null;
                    highlights.clear();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 || highlights.size() >= maxHighlights) {
                    highlights.clear();
                    return;
                }
//                Point p = new Point(e.getX() - panX, e.getY() - panY);
//                if (infectionDeckLocation.contains(p)) {
//                    highlights.put("infectionDeck", infectionDeckLocation);
//                }
            }
        });
    }
    private void updateScale(double scale) {
        this.scale = scale;
        descentItemSize = (int)(scale * defaultItemSize);
    }

    public void updateGameState(DescentGameState gameState) {
        this.gameState = gameState;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGridBoardWithGraphConnectivity((Graphics2D)g, (GridBoard) component, panX, panY, gameState.getGridReferences(), gameState.getTileReferences());
        String dataPath = ((DescentParameters) gameState.getGameParameters()).dataPath + "img/";

        // Draw tokens
        for (DToken dt: gameState.getTokens()) {
            if (dt.getPosition() != null) {
                String imgPath = dataPath + dt.getDescentTokenType().getImgPath(new Random(gameState.getGameParameters().getRandomSeed()));
                Image img = ImageIO.GetInstance().getImage(imgPath);
                g.drawImage(img, panX + dt.getPosition().getX() * descentItemSize, panY + dt.getPosition().getY() * descentItemSize, descentItemSize, descentItemSize, null);
            }
        }

        // Draw heroes
        for (Hero f: gameState.getHeroes()) {
            Vector2D loc = f.getPosition();
            DescentTypes.Archetype archetype = DescentTypes.Archetype.valueOf(((PropertyString)f.getProperty("archetype")).value);

            // Color
//            g.setColor(archetype.getColor());
//            g.fillOval(panX + loc.getX() * itemSize, panY + loc.getY() * itemSize, itemSize, itemSize);
//            g.setColor(Color.black);
//            g.drawOval(panX + loc.getX() * itemSize, panY + loc.getY() * itemSize, itemSize, itemSize);

            // Or image
            Image img = ImageIO.GetInstance().getImage(dataPath + "heroes/" + archetype.name().toLowerCase() + ".png");
            g.drawImage(img, panX + loc.getX() * descentItemSize, panY + loc.getY() * descentItemSize, descentItemSize, descentItemSize, null);
        }
        // Draw monsters
        for (List<Monster> monsterGroup: gameState.getMonsters()) {
            String path = ((PropertyString) monsterGroup.get(0).getProperty(imgHash)).value;

            for (Monster m: monsterGroup) {
                Vector2D loc = m.getPosition();
                if (loc == null) continue;
                int orientation = m.getOrientation();

                // Get the size of the monster, and scale according to item size
                Pair<Integer, Integer> size = m.getSize().copy();
                size.a *= descentItemSize;
                size.b *= descentItemSize;

                String imagePath = dataPath;
                if (((PropertyColor) m.getProperty(colorHash)).valueStr.equals("red")) {
                    imagePath += path.replace(".png", "-master.png");
                } else {
                    imagePath += path;
                }
                Image imgRaw = ImageIO.GetInstance().getImage(imagePath);
                BufferedImage imgToDraw = rotateImage((BufferedImage) imgRaw, size, orientation);
                g.drawImage(imgToDraw, panX + loc.getX() * descentItemSize, panY + loc.getY() * descentItemSize,null);
            }
        }

        // Draw highlights
        g.setColor(new Color(207, 75, 220));
        for (Vector2D pos: highlights) {

            int xC = panX + pos.getX() * descentItemSize;
            int yC = panY + pos.getY() * descentItemSize;

            // Paint cell background
            g.fillRect(xC+ descentItemSize /4, yC+ descentItemSize /4, descentItemSize /2, descentItemSize /2);
        }
    }

    public void drawGridBoardWithGraphConnectivity(Graphics2D g, GridBoard gridBoard, int x, int y,
                                                          Map<String, Map<Vector2D,Vector2D>> gridReferences,
                                                          int[][] tileReferences) {
        int width = gridBoard.getWidth() * descentItemSize;
        int height = gridBoard.getHeight() * descentItemSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                drawCell(g, gridBoard.getElement(j, i), j, i, gridBoard.getWidth(), gridBoard.getHeight(), x, y);
            }
        }
        // Draw connectivity graph
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                drawNeighbourConnections(g, gridBoard.getElement(j, i), j, i, x, y);
            }
        }

        // Draw grid references
        g.setColor(Color.black);
//        for (String tile: gridReferences.keySet()) {
//            for (Vector2D t: gridReferences.get(tile)) {
//                g.drawString(tile, x+t.getX()*defaultItemSize + 10, y+t.getY()*defaultItemSize + 10);
//            }
//        }

        // Draw tile references
//        for (int i = 0; i < gridBoard.getHeight(); i++) {
//            for (int j = 0; j < gridBoard.getWidth(); j++) {
//                g.drawString(""+tileReferences[i][j], x+j*defaultItemSize + 10, y+i*defaultItemSize + 10);
//            }
//        }
    }


    private void drawCell(Graphics2D g, BoardNode bn, int x, int y, int gridWidth, int gridHeight, int offsetX, int offsetY) {
        if (bn == null) return;

        int xC = offsetX + x * descentItemSize;
        int yC = offsetY + y * descentItemSize;

        // Paint cell background
        g.setColor(colorMap.get(bn.getComponentName()));
        g.fillRect(xC, yC, descentItemSize, descentItemSize);

        String terrain = bn.getComponentName();
        Stroke s = g.getStroke();

        if (DescentTypes.TerrainType.isWalkableTerrain(terrain)) {
            g.setColor(Color.black);
            g.drawRect(xC, yC, descentItemSize, descentItemSize);
            g.setStroke(new BasicStroke(5));

            //TODO: Remove after testing, added for movement debugging - Marko
//            g.drawString("X:" + x + " Y:" + y, xC + 5, yC + 15);

            // Find connectivity in the graph and draw borders to the cell where connection doesn't exist
            List<Vector2D> neighbourCells = getNeighbourhood(x, y, gridWidth, gridHeight, false);
            // Explore all neighbourhood of this cell
            for (Vector2D n : neighbourCells) {

                // Check if this node is a connected neighbour
                boolean connected = false;
                for (int nnid : bn.getNeighbours().keySet()) {
                    BoardNode nn = (BoardNode) gameState.getComponentById(nnid);
                    if (nn == null) continue;
                    Vector2D location = ((PropertyVector2D) nn.getProperty(coordinateHash)).values;
                    if (location.equals(n)) {
                        connected = true;
                        break;
                    }
                }
                if (!connected) {
                    // Not a connection between these neighbours, drawing a thick black line on the edge to indicate this
                    if (n.getX() - x == 0) {
                        // Vertical neighbours, draw horizontal line
                        if (n.getY() > y) {
                            // Neighbour is below, separation line is y + cell size
                            g.drawLine(xC, yC + descentItemSize, xC + descentItemSize, yC + descentItemSize);
                        } else {
                            // Neighbour is above, separation line is y
                            g.drawLine(xC, yC, xC + descentItemSize, yC);
                        }
                    } else if (n.getY() - y == 0) {
                        // Horizontal neighbours, draw vertical line
                        if (n.getX() > x) {
                            // Neighbour is to the right, separation line is x + cell size
                            g.drawLine(xC + descentItemSize, yC, xC + descentItemSize, yC + descentItemSize);
                        } else {
                            // Neighbour is to the left, separation line is x
                            g.drawLine(xC, yC, xC, yC + descentItemSize);
                        }
                    }
                }
            }
        } else {
            // Bocked terrain can never be occupied, not connected to anything
            if (terrain.equals("block")) {
                g.setStroke(new BasicStroke(5));
                g.setColor(Color.black);
                g.drawRect(xC, yC, descentItemSize, descentItemSize);
            }
        }
        g.setStroke(s);
    }

    private void drawNeighbourConnections(Graphics2D g, BoardNode bn, int x, int y, int offsetX, int offsetY) {
        if (bn == null) return;
        int xC = offsetX + x * descentItemSize;
        int yC = offsetY + y * descentItemSize;

        // Draw underlying graph
        g.setColor(Color.green);
        Stroke s = g.getStroke();
        for (int nnid : bn.getNeighbours().keySet()) {
            BoardNode nn = (BoardNode) gameState.getComponentById(nnid);
            if (nn == null) continue;
            Vector2D location = ((PropertyVector2D) nn.getProperty(coordinateHash)).values;
            int xC2 = offsetX + location.getX() * descentItemSize;
            int yC2 = offsetY + location.getY() * descentItemSize;

            g.setStroke(new BasicStroke((float) bn.getNeighbourCost(nnid)));
            g.drawLine(xC + descentItemSize /2, yC + descentItemSize /2, xC2 + descentItemSize /2, yC2 + descentItemSize /2);
        }
        g.setColor(Color.black);
        g.setStroke(s);
    }

    @Override
    public void scrollRectToVisible(Rectangle rect) {
        // Disable autoscroll
//        super.scrollRectToVisible(rect);
    }

}
