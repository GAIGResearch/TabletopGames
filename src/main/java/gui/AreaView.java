package gui;

import core.components.*;
import core.components.Component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import static core.AbstractGUI.*;

// todo: long press move deck/area, short press move component in deck/area
public class AreaView extends JComponent {
    Area area;
    int width, height;
    HashMap<Integer, Rectangle> drawMap;

    private Map.Entry<Integer, Rectangle> dragging;

    public AreaView(Area area, int width, int height) {
        updateArea(area);
        this.width = width;
        this.height = height;
        drawMap = new HashMap<>();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                for (Map.Entry<Integer, Rectangle> en: drawMap.entrySet()) {
                    if (en.getValue().contains(e.getPoint())) {
                        // clicked on this rectangle
                        dragging = en;
                        break;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging != null) {
                    dragging.getValue().setLocation(e.getPoint());
                    drawMap.put(dragging.getKey(), dragging.getValue());
                    repaint();
                    dragging = null;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging != null) {
                    dragging.getValue().setLocation(e.getPoint());
                    drawMap.put(dragging.getKey(), dragging.getValue());
                    repaint();
                    dragging = null;
                }
            }
        });
    }

    public void updateArea(Area area) {
        this.area = area;
        if (area != null) {
            setToolTipText("Component ID: " + area.getComponentID());
        }
    }

    public Area getArea() {
        return area;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawArea((Graphics2D) g);
    }

    protected void drawArea(Graphics2D g) {
        drawArea(g, area, 0, 0, width, height, drawMap);
    }


    public static void drawArea(Graphics2D g, Area area, int x, int y, int width, int height,
                                                      HashMap<Integer, Rectangle> drawMap) {

        // Draw components at (x, y)
        for (Map.Entry<Integer, Component> e: area.getComponents().entrySet()) {
            Component c = e.getValue();
            Rectangle r = drawMap.get(e.getKey());
            if (c instanceof GridBoard) {
                if (r == null) {
                    drawMap.put(e.getKey(), new Rectangle(0, 0,
                            ((GridBoard) c).getWidth() * defaultItemSize,
                            ((GridBoard) c).getHeight() * defaultItemSize));
                    GridBoardView.drawGridBoard(g, (GridBoard) c, 0, 0);
                } else {
                    GridBoardView.drawGridBoard(g, (GridBoard) c, r);
                }
            } else if (c instanceof Area) {
                if (r == null) {
                    drawMap.put(e.getKey(), new Rectangle(0, 0, width/2, height/2));
                    drawArea(g, (Area)c, 0, 0, width/2, height/2, drawMap);
                } else {
                    drawArea(g, (Area) c, r.x, r.y, r.width, r.height, drawMap);
                }
            } else if (c instanceof Card) {
                if (r == null) {
                    drawMap.put(e.getKey(), new Rectangle(0, 0, defaultCardWidth, defaultCardHeight));
                    CardView.drawCard(g, 0, 0, defaultCardWidth, defaultCardHeight, (Card) c);
                } else {
                    CardView.drawCard(g, r.x, r.y, r.width, r.height, (Card) c);
                }
            } else if (c instanceof Counter) {
                if (r == null) {
                    drawMap.put(e.getKey(), new Rectangle(0, 0, defaultItemSize, defaultItemSize));
                    CounterView.drawCounter(g, 0, 0, defaultItemSize, (Counter) c);
                } else {
                    CounterView.drawCounter(g, r.x, r.y, r.width, (Counter) c);
                }
            } else if (c instanceof Deck) {
                if (r == null) {
                    Rectangle rect = new Rectangle(0, 0, defaultCardWidth, defaultCardHeight);
                    drawMap.put(e.getKey(), rect);
                    DeckView.drawDeck(g, (Deck) c, c.getComponentName(), null, rect, true);
                } else {
                    DeckView.drawDeck(g, (Deck) c, c.getComponentName(), null, r, true);
                }
            } else if (c instanceof Dice) {
                if (r == null) {
                    drawMap.put(e.getKey(), new Rectangle(0, 0, defaultItemSize, defaultItemSize));
                    DieView.drawDie(g, 0, 0, defaultItemSize, (Dice) c);
                } else {
                    DieView.drawDie(g, r.x, r.y, r.width, (Dice) c);
                }
            } else if (c instanceof GraphBoard) {
                if (r == null) {
                    drawMap.put(e.getKey(), new Rectangle(0, 0, defaultBoardWidth, defaultBoardHeight));
                    GraphBoardView.drawGraphBoard(g, (GraphBoard) c,  0, 0, defaultBoardWidth, defaultBoardHeight);
                } else {
                    GraphBoardView.drawGraphBoard(g, (GraphBoard) c, r);
                }
            } else if (c instanceof Token) {
                if (r == null) {
                    drawMap.put(e.getKey(), new Rectangle(0, 0, defaultItemSize, defaultItemSize));
                    TokenView.drawToken(g, 0, 0, defaultItemSize, (Token) c);
                } else {
                    TokenView.drawToken(g, r.x, r.y, r.width, (Token) c);
                }
            } else {
                System.out.println("Component unknown");
            }
        }

        g.setColor(Color.black);
        g.drawRect(x, y, width, height);
    }
}
