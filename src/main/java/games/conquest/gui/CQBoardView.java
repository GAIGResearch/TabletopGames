package games.conquest.gui;

import core.components.GridBoard;
import games.conquest.CQGameState;
import games.conquest.CQUtility;
import games.conquest.components.Cell;
import games.conquest.components.Troop;
import gui.IScreenHighlight;
import gui.views.ComponentView;
import utilities.Vector2D;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CQBoardView extends ComponentView implements IScreenHighlight {
    Rectangle[] rects;
    ArrayList<Rectangle> highlight;
    HashMap<Vector2D, Troop> locationToTroopMap;
    private int movementRange = 0;
    private int attackRange = 0;
    private Vector2D selection;
    int[][] distancesFromSelection;
    int defaultCellSize = 35;
    private static final Color GREEN = new Color(58, 216, 66);
    private static final Color BROWN = new Color(165, 42, 42);
    private static final Color BLUE = new Color(100, 149, 237);

    public CQBoardView(GridBoard<Cell> gridBoard) {
        super(gridBoard, gridBoard.getWidth(), gridBoard.getHeight());

        locationToTroopMap = new HashMap<>();
        distancesFromSelection = new int[gridBoard.getWidth()][gridBoard.getHeight()];
        rects = new Rectangle[gridBoard.getWidth() * gridBoard.getHeight()];
        highlight = new ArrayList<>();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Left click, highlight cell
                    for (Rectangle r: rects) {
                        if (r != null && r.contains(e.getPoint())) {
                            highlight.clear();
                            highlight.add(r);
                            break;
                        }
                    }
                } else {
                    // Remove highlight
                    highlight.clear();
                }
            }
        });
    }

    @Override
    public void clearHighlights() {
        highlight.clear();
    }

    public ArrayList<Rectangle> getHighlight() {
        return highlight;
    }
    @Override
    protected void paintComponent(Graphics g) {
        drawGridBoard((Graphics2D)g, (GridBoard<Cell>) component, 0, 0);

        if (!highlight.isEmpty()) {
            g.setColor(Color.blue);
            Stroke s = ((Graphics2D) g).getStroke();
            ((Graphics2D) g).setStroke(new BasicStroke(3));
            Rectangle r = highlight.get(0);
            g.drawRect(r.x, r.y, r.width, r.height);
            ((Graphics2D) g).setStroke(s);
        }
    }

    public void drawGridBoard(Graphics2D g, GridBoard<Cell> gridBoard, int x, int y) {
        int width = gridBoard.getWidth();
        int height = gridBoard.getHeight();
        rects = new Rectangle[width * height];

        // draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width * defaultCellSize - 1, height * defaultCellSize - 1);
        g.setColor(Color.black);

        // draw cells
        for (int i=0; i<height; i++) {
            for (int j=0; j<width; j++) {
                int xC = x + j * defaultCellSize;
                int yC = y + i * defaultCellSize;
                drawCell(g, gridBoard.getElement(j, i), xC, yC);

                int idx = i * width + j;
                if (rects[idx] == null) {
                    rects[idx] = new Rectangle(xC, yC, defaultCellSize, defaultCellSize);
                }
            }
        }
    }

    private void drawCell(Graphics2D g, Cell cell, int x, int y) {
        g.setColor(Color.lightGray);
        g.fillRect(x, y, defaultCellSize, defaultCellSize);
        g.setColor(Color.black);
        g.drawRect(x, y, defaultCellSize, defaultCellSize);
        Troop troop;
        String troopID = "";

        if (cell != null && locationToTroopMap != null) {
            Vector2D cellPos = cell.position;
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.BOLD, defaultCellSize));
            troop = locationToTroopMap.get(cellPos);
            Troop selected = locationToTroopMap.get(selection);
            if (selected != null && distancesFromSelection[cell.position.getX()][cell.position.getY()] <= movementRange) {
                if (selected.getLocation().equals(cell.position)) {
                    g.setColor(GREEN);
                } else {
                    g.setColor(BLUE);
                }
                g.fillRect(x, y, defaultCellSize, defaultCellSize);
                g.setColor(Color.black);
                g.drawRect(x, y, defaultCellSize, defaultCellSize);
            }
            if (troop != null) {
                troopID = String.valueOf(troop.getTroopID());
                if (selected != null &&
                    selected.getOwnerId() != troop.getOwnerId() &&
                    cell.getChebyshev(selected.getLocation()) <= attackRange
                ) {
                    // current cell contains an enemy troop that can be attacked from the current position
                    g.setColor(BROWN);
                    g.fillRect(x, y, defaultCellSize, defaultCellSize);
                    g.setColor(Color.black);
                    g.drawRect(x, y, defaultCellSize, defaultCellSize);
                }
            }
            // Center alignment:
            Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(troopID, g);
            int padLeft = (int) ((defaultCellSize - stringBounds.getWidth())/2 + 1);
            // add to board
            g.drawString(troopID,
                    x + padLeft,
                    y + defaultCellSize - defaultCellSize / 10);
            g.setFont(f);
        }
    }

    public synchronized void update(CQGameState cqgs) {
        // Function based on approach used in CantStopBoardView::update.
        HashMap<Vector2D, Integer> map = cqgs.getLocationToTroopMap();
        locationToTroopMap = new HashMap<>(); // reset list
        for (Map.Entry<Vector2D, Integer> entry : map.entrySet()) {
            Troop troop = (Troop) cqgs.getComponentById(entry.getValue());
            if (troop.getHealth() > 0) {
                locationToTroopMap.put(entry.getKey(), troop);
            }
        }
        Troop selected = cqgs.getSelectedTroop();
        if (selected != null && selected.getHealth() > 0) {
            selection = selected.getLocation();
            movementRange = selected.getMovement();
            attackRange = selected.getRange();
            distancesFromSelection = CQUtility.floodFill(cqgs, cqgs.getCell(selection));
        } else {
            selection = null;
            movementRange = 0;
            attackRange = 0;
        }
    }
}
