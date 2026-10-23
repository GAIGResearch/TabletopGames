package games.descent2e.gui;

import core.components.BoardNode;
import core.components.GridBoard;
import core.properties.PropertyColor;
import core.properties.PropertyInt;
import core.properties.PropertyString;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.tokens.DToken;
import games.descent2e.components.Monster;
import gui.IScreenHighlight;
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
import static games.descent2e.gui.DescentGUI.foregroundColor;
import static games.descent2e.gui.DescentGUI.prettyVersion;
import static gui.AbstractGUIManager.defaultItemSize;
import static utilities.Utils.*;

public class DescentGridBoardView extends ComponentView implements IScreenHighlight {

    public static HashMap<String, Color> colorMap = new HashMap<>() {{
        put("null", Color.black);
        put(null, Color.black);
        put("edge", Color.black);
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
    double scale;
    Set<Vector2D> actionHighlights;
    int attackTarget = -1;
    Color highlightColor = Color.green; //new Color(207, 75, 220);
    Stroke highlightStroke = new BasicStroke(3);
    int maxHighlights = 3;

    HashMap<Vector2D, List<Vector2D>> notConnectedMap;
    HashMap<Vector2D, Pair<Image, Pair<Integer, Integer>>> tileImageTopLeftCorners;

    Vector2D cellHighlight;

    static String dataPath;
    int offset;
    Dimension maxSize;

    boolean debugDrawCellCoordinates = false;
    boolean debugDrawGridReferences = false;
    boolean debugDrawTileReferences = false;

    public DescentGridBoardView(GridBoard gridBoard, DescentGameState gameState, int offset, int width, int height) {
        super(gridBoard, width, height);
        this.gameState = gameState;
        this.offset = offset;
        this.maxSize = new Dimension(width+offset*2, height+offset*2);
        dataPath = ((DescentParameters) gameState.getGameParameters()).dataPath + "img/";
        notConnectedMap = new HashMap<>();
        double w = gridBoard.getWidth() * defaultItemSize;
        double h = gridBoard.getHeight() * defaultItemSize;
        double minScale = 1.0;
        if (width / w < minScale) minScale = width/w;
        if (height / h < minScale) minScale = height/h;
        scale = minScale;
        updateScale(scale);

        // Focus on hero characters
//        Hero hero = gameState.getHeroes().get(0);
//        Vector2D pos = hero.getPosition();
//        panX = pos.getX() * descentItemSize / 2;
//        panY = -pos.getY() * descentItemSize / 2 - descentItemSize * 3;

        // Cache the top-left corners of rotated tile images for quick drawing
        tileImageTopLeftCorners = new HashMap<>();
        for (String tile: gameState.getGridReferences().keySet()) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = 0;
            int maxY = 0;
            int compID = -1;
            for (Vector2D space: gameState.getGridReferences().get(tile).keySet()) {
                if (space.getX() < minX) minX = space.getX();
                if (space.getY() < minY) minY = space.getY();
                if (space.getX() > maxX) maxX = space.getX();
                if (space.getY() > maxY) maxY = space.getY();
                compID = gameState.getTileReferences()[space.getY()][space.getX()];
            }
            int orientation = ((PropertyInt)gameState.getTiles().get(compID).getProperty(orientationHash)).value;
            Image img = ImageIO.GetInstance().getImage(dataPath + "tiles/" + tile.split("-")[0] + ".png");
            Image img2 = rotateImage((BufferedImage) img, new Pair<>(img.getWidth(null), img.getHeight(null)), orientation);

            tileImageTopLeftCorners.put(new Vector2D(minX, minY), new Pair<>(img2, new Pair<>(maxX-minX+1, maxY-minY+1)));
        }

        actionHighlights = new HashSet<>();
        addMouseWheelListener(e -> {
            double amount = 0.2 * Math.abs(e.getPreciseWheelRotation());
            if (e.getPreciseWheelRotation() > 0) {
                // Rotated down, zoom out
                updateScale(scale - amount);
            } else {
                updateScale(scale + amount);
            }
            actionHighlights.clear();
            attackTarget = -1;
            cellHighlight = null;
        });
        addMouseListener(new MouseAdapter() {
            Point start;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    // Middle (wheel) click, pan around
                    start = e.getPoint();
                    cellHighlight = null;
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
                    actionHighlights.clear();
                    attackTarget = -1;
                    cellHighlight = null;
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 || actionHighlights.size() >= maxHighlights) {
                    actionHighlights.clear();
                    attackTarget = -1;
                    cellHighlight = null;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    cellHighlight = new Vector2D((e.getX()-offset-panX) / descentItemSize, (e.getY()-offset-panY)/descentItemSize);
                }
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
    protected void paintComponent(Graphics gg) {
        // Draw background
        Graphics2D g = (Graphics2D) gg;
        g.setColor(Color.black);
        g.fillRect(offset, offset, width - offset * 10, height);

        if (prettyVersion) {
            // Draw map tile images
            for (Map.Entry<Vector2D, Pair<Image, Pair<Integer, Integer>>> e : tileImageTopLeftCorners.entrySet()) {
                g.drawImage(e.getValue().a, offset + panX + e.getKey().getX() * descentItemSize, offset + panY + e.getKey().getY() * descentItemSize,
                        e.getValue().b.a * descentItemSize, e.getValue().b.b * descentItemSize, null);
            }
        } else {
            drawGridBoardWithGraphConnectivity(g, (GridBoard) component, offset + panX, offset + panY, gameState.getGridReferences(), gameState.getTileReferences());
        }

        // Draw tokens
        for (DToken dt : gameState.getTokens()) {
            if (dt.getPosition() != null) {
                String imgPath = dataPath + dt.getDescentTokenType().getImgPath(new Random(gameState.getGameParameters().getRandomSeed()));
                Image img = ImageIO.GetInstance().getImage(imgPath);
                g.drawImage(img, offset + panX + dt.getPosition().getX() * descentItemSize, offset + panY + dt.getPosition().getY() * descentItemSize, descentItemSize, descentItemSize, null);

                // TODO ugly version
            }
        }
        Stroke s = g.getStroke();

        // Draw heroes
        for (Hero f : gameState.getHeroes()) {
            Vector2D loc = f.getPosition();
            DescentTypes.Archetype archetype = DescentTypes.Archetype.valueOf(((PropertyString) f.getProperty("archetype")).value);

            // Color
            g.setColor(archetype.getColor());
            g.fillOval(offset + panX + loc.getX() * descentItemSize, offset + panY + loc.getY() * descentItemSize, descentItemSize, descentItemSize);

            if (gameState.getActingFigure().equals(f)) {
                g.setStroke(highlightStroke);
                g.setColor(highlightColor);
            } else {
                g.setColor(Color.black);
            }
            g.drawOval(offset + panX + loc.getX() * descentItemSize, offset + panY + loc.getY() * descentItemSize, descentItemSize, descentItemSize);
            g.setStroke(s);

            if (prettyVersion) {
                // Image
                int imgSize = descentItemSize * 2 / 3;
                Image img = ImageIO.GetInstance().getImage(dataPath + "heroes/" + archetype.name().toLowerCase() + ".png");
                g.drawImage(img, offset + panX + loc.getX() * descentItemSize + descentItemSize / 2 - imgSize / 2,
                        offset + panY + loc.getY() * descentItemSize + descentItemSize / 2 - imgSize / 2, imgSize, imgSize, null);
            }
        }

        // Draw monsters
        for (List<Monster> monsterGroup : gameState.getMonsters()) {

            // Only draws the monster group if there are monsters within it, to prevent out of index access attempts
            if(!monsterGroup.isEmpty())
            {
                String path = ((PropertyString) monsterGroup.get(0).getProperty(imgHash)).value;

                for (Monster m : monsterGroup) {
//                Vector2D loc = m.getPosition();
                    Vector2D loc = m.applyAnchorModifier();
                    if (loc == null) continue;
                    int orientation = m.getOrientation().ordinal();

                    // Get the size of the monster, and scale according to item size
                    Pair<Integer, Integer> size = m.getSize().copy();
                    size.a *= descentItemSize;
                    size.b *= descentItemSize;

                    // TODO ugly version

                    String imagePath = dataPath;
                    if (((PropertyColor) m.getProperty(colorHash)).valueStr.equals("red")) {
                        imagePath += path.replace(".png", "-master.png");
                    } else {
                        imagePath += path;
                    }
                    Image imgRaw = ImageIO.GetInstance().getImage(imagePath);
                    BufferedImage imgToDraw = rotateImage((BufferedImage) imgRaw, size, orientation);
                    g.drawImage(imgToDraw, offset + panX + loc.getX() * descentItemSize, offset + panY + loc.getY() * descentItemSize, null);

                    if (gameState.getActingFigure().equals(m)) {
                        g.setStroke(highlightStroke);
                        g.setColor(highlightColor);
                        // Check if facing Up/Down or Left/Right to draw correctly
                        if (m.getOrientation().ordinal() % 2 == 0) {
                            g.drawOval(offset + panX + loc.getX() * descentItemSize, offset + panY + loc.getY() * descentItemSize, size.a, size.b);
                        }
                        else {
                            g.drawOval(offset + panX + loc.getX() * descentItemSize, offset + panY + loc.getY() * descentItemSize, size.b, size.a);
                        }
                    } else {
                        g.setColor(Color.black);
                    }
                    g.setStroke(s);

                    int health = m.getAttributeValue(Figure.Attribute.Health);
                    int maxHealth = m.getAttributeMax(Figure.Attribute.Health);
                    g.setColor(Color.red);
                    g.fillRect(offset + panX + loc.getX() * descentItemSize, offset + panY + loc.getY() * descentItemSize, (int) (descentItemSize * health * 1.0 / maxHealth), 5);
                    g.setColor(Color.black);
                    g.drawRect(offset + panX + loc.getX() * descentItemSize, offset + panY + loc.getY() * descentItemSize, (int) (descentItemSize * health * 1.0 / maxHealth), 5);
                }
            }
        }

        // Draw action space highlights
        for (Vector2D pos: actionHighlights) {
            int xC = offset+panX + pos.getX() * descentItemSize;
            int yC = offset+ panY + pos.getY() * descentItemSize;
            g.setColor(highlightColor);
            g.fillRect(xC+ descentItemSize /4, yC+ descentItemSize /4, descentItemSize /2, descentItemSize /2);
            g.setColor(Color.black);
            g.drawRect(xC+ descentItemSize /4, yC+ descentItemSize /4, descentItemSize /2, descentItemSize /2);
        }

        // Draw attack target
        if (attackTarget != -1) {

            Figure target = (Figure) gameState.getComponentById(attackTarget);

            int xC = offset + panX + target.getPosition().getX() * descentItemSize;
            int yC = offset + panY + target.getPosition().getY() * descentItemSize;

            // Get the size of the monster, and scale according to item size
            Pair<Integer, Integer> size = target.getSize().copy();
            size.a *= descentItemSize;
            size.b *= descentItemSize;

            g.setColor(Color.red);
            if (target instanceof Monster && ((Monster) target).getOrientation().ordinal() % 2 == 1) {
                g.drawOval(xC, yC, size.b, size.a);
            }
            else {
                g.drawOval(xC, yC, size.a, size.b);
            }
        }

        // Draw selected cell highlight
        if (cellHighlight != null) {
            int xC = offset + panX + cellHighlight.getX() * descentItemSize;
            int yC = offset + panY + cellHighlight.getY() * descentItemSize;
            g.setColor(highlightColor);
            g.drawRect(xC, yC, descentItemSize, descentItemSize);
        }

        // Debug draw cell coordinates
        if (debugDrawCellCoordinates) {
            g.setColor(foregroundColor);
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.PLAIN, (int)(12*scale)));
            GridBoard gridBoard = (GridBoard) component;
            for (int i = 0; i < gridBoard.getHeight(); i++) {
                for (int j = 0; j < gridBoard.getWidth(); j++) {
                    int xC = panX + j * descentItemSize;
                    int yC = panY + i * descentItemSize;
                    g.drawString("X:" + j + " Y:" + i, xC + defaultItemSize/5, yC + defaultItemSize);
                }
            }
            g.setFont(f);
        }
    }

    public void drawGridBoardWithGraphConnectivity(Graphics2D g, GridBoard gridBoard, int x, int y,
                                                   Map<String, Map<Vector2D,Vector2D>> gridReferences,
                                                   int[][] tileReferences) {
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

        g.setColor(foregroundColor);

        // Draw grid references
        if (debugDrawGridReferences) {
            for (String tile: gridReferences.keySet()) {
                for (Vector2D t: gridReferences.get(tile).keySet()) {
                    g.drawString(tile, x+t.getX()*defaultItemSize + 10, y+t.getY()*defaultItemSize + 10);
                }
            }
        }

        // Draw tile references
        if (debugDrawTileReferences) {
            for (int i = 0; i < gridBoard.getHeight(); i++) {
                for (int j = 0; j < gridBoard.getWidth(); j++) {
                    g.drawString(""+tileReferences[i][j], x+j*defaultItemSize + 10, y+i*defaultItemSize + 10);
                }
            }
        }
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

            List<Vector2D> notConnectedList = notConnectedMap.get(new Vector2D(xC, yC));
            if (notConnectedList == null) {
                notConnectedList = new ArrayList<>();
                // Find connectivity in the graph and draw borders to the cell where connection doesn't exist
                List<Vector2D> neighbourCells = getNeighbourhood(x, y, gridWidth, gridHeight, false);
                // Explore all neighbourhood of this cell
                for (Vector2D n : neighbourCells) {

                    // Check if this node is a connected neighbour
                    boolean connected = false;
                    for (BoardNode nn : bn.getNeighbours().keySet()) {
                        if (nn == null) continue;
                        Vector2D location = ((PropertyVector2D) nn.getProperty(coordinateHash)).values;
                        if (location.equals(n)) {
                            connected = true;
                            break;
                        }
                    }
                    if (!connected) notConnectedList.add(n);
                }
                // Caching the not connected neighbours for faster drawing of thick lines between these
                notConnectedMap.put(new Vector2D(xC, yC), notConnectedList);
            }
            for (Vector2D n: notConnectedList) {
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
        for (BoardNode nn : bn.getNeighbours().keySet()) {
            if (nn == null) continue;
            Vector2D location = ((PropertyVector2D) nn.getProperty(coordinateHash)).values;
            int xC2 = offsetX + location.getX() * descentItemSize;
            int yC2 = offsetY + location.getY() * descentItemSize;

            g.setStroke(new BasicStroke((float) bn.getNeighbourCost(nn)));
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

    @Override
    public Dimension getMaximumSize() {
        return maxSize;
    }

    @Override
    public void clearHighlights() {
        actionHighlights.clear();
        attackTarget = -1;
        cellHighlight = null;
    }

    public int getAttackTarget() {
        return attackTarget;
    }

    public Vector2D getCellHighlight() {
        return cellHighlight;
    }
}
