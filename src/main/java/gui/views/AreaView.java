package gui.views;

import core.components.*;
import core.components.Component;
import gui.TokenView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static core.AbstractGUI.*;

// TODO: snap deck/area (add component to deck/area) + remove on extract + search?
// Long press move deck/area, short press move component in deck/area
public class AreaView extends JComponent {
    Area area;
    int width, height;

    private HashMap<Integer, Rectangle> drawMap;
    private Map.Entry<Integer, Rectangle> dragging, draggingLong;
    private HashMap<Integer, Integer> dependencies;  // Components within other components

    public AreaView(Area area, int width, int height) {
        updateArea(area);
        this.width = width;
        this.height = height;
        drawMap = new HashMap<>();
        dependencies = new HashMap<>();

        addMouseListener(new MouseAdapter() {
            private java.util.Timer timer;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    // On right click, reset the deck/area (all components to 0,0 + parent dependency)
                    for (Map.Entry<Integer, Rectangle> en : drawMap.entrySet()) {
                        Component c = area.getComponents().get(en.getKey());
                        if ((c instanceof Deck || c instanceof Area) && en.getValue().contains(e.getPoint())) {
                            if (dependencies.containsValue(c.getComponentID())) {
                                for (Map.Entry<Integer, Integer> dep : dependencies.entrySet()) {
                                    if (dep.getValue() == c.getComponentID()) {
                                        Rectangle r = drawMap.get(dep.getKey());
                                        drawMap.put(dep.getKey(), new Rectangle(0, 0, r.width, r.height));
                                    }
                                }
                            }
                            break;
                        }
                    }
                } else {
                    for (Map.Entry<Integer, Rectangle> en : drawMap.entrySet()) {
                        Component c = area.getComponents().get(en.getKey());
                        if (c instanceof Deck || c instanceof Area) continue;

                        Rectangle r = new Rectangle(en.getValue());
                        // Add parent dependency
                        if (dependencies.containsKey(en.getKey())) {
                            r.x += drawMap.get(dependencies.get(en.getKey())).x;
                            r.y += drawMap.get(dependencies.get(en.getKey())).y;
                        }
                        if (r.contains(e.getPoint())) {
                            // clicked on this rectangle
                            dragging = en;
                            break;
                        }
                    }
                    if (timer == null) {
                        timer = new java.util.Timer();
                    }
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            for (Map.Entry<Integer, Rectangle> en : drawMap.entrySet()) {
                                Component c = area.getComponents().get(en.getKey());
                                if ((c instanceof Deck || c instanceof Area) && en.getValue().contains(e.getPoint())) {
                                    // long clicked on this rectangle of a collection
                                    draggingLong = en;
                                    break;
                                }
                            }
                        }
                    }, 1000);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingLong != null) {
                    Rectangle r = draggingLong.getValue();
                    r.setLocation(e.getPoint().x - r.width/2, e.getPoint().y - r.height/2);
                    drawMap.put(draggingLong.getKey(), r);
                    draggingLong = null;
                    repaint();
                } else if (dragging != null) {
                    Rectangle r = dragging.getValue();
                    Point p = new Point(e.getPoint());
                    if (dependencies.containsKey(dragging.getKey())) {
                        // Remove parent dependency
                        p.x -= drawMap.get(dependencies.get(dragging.getKey())).x;
                        p.y -= drawMap.get(dependencies.get(dragging.getKey())).y;
                    }
                    r.setLocation(p.x - r.width/2, p.y - r.height/2);
                    drawMap.put(dragging.getKey(), r);
                    dragging = null;
                    repaint();
                }
                if(timer != null)
                {
                    timer.cancel();
                    timer = null;
                }
            }

//            @Override
//            public void mouseDragged(MouseEvent e) {
//                if (dragging != null) {
//                    dragging.getValue().setLocation(e.getPoint());
//                    drawMap.put(dragging.getKey(), dragging.getValue());
//                    repaint();
//                    dragging = null;
//                }
//            }
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
        drawArea(g, area, 0, 0, width, height, drawMap, dependencies);
    }

    public static void drawArea(Graphics2D g, Area area, int x, int y, int width, int height,
                                HashMap<Integer, Rectangle> drawMap,
                                HashMap<Integer, Integer> dependencies) {
        int fontSize = g.getFont().getSize();

        // Find all sub-collections in this area: decks and other areas. just draw a rectangle around them
        List<Deck<? extends Component>> decks = new ArrayList<>();
        List<Area> areas = new ArrayList<>();
        for (Map.Entry<Integer, Component> e: area.getComponents().entrySet()) {
            Rectangle r = drawMap.get(e.getKey());
            if (e.getValue() instanceof Deck) {
                decks.add((Deck<? extends Component>) e.getValue());
                if (r == null) {
                    r = new Rectangle(0, 0, defaultCardWidth, defaultCardHeight);
                    drawMap.put(e.getKey(), r);
                }
                g.setColor(Color.lightGray);
                g.fillRect(r.x, r.y, r.width, r.height);
                g.setColor(Color.black);
                g.drawRect(r.x, r.y, r.width, r.height);
                g.drawString(e.getValue().getComponentName(), r.x, r.y + r.height + fontSize);
            } else if (e.getValue() instanceof Area) {
                areas.add((Area) e.getValue());
                if (r == null) {
                    r = new Rectangle(0, 0, width/2, height/2);
                    drawMap.put(e.getKey(), r);
                }
                g.setColor(Color.black);
                g.drawRect(r.x, r.y, r.width, r.height);
                g.drawString(e.getValue().getComponentName(), r.x, r.y + r.height + fontSize);
            }
        }

        // Draw components
        for (Map.Entry<Integer, Component> e: area.getComponents().entrySet()) {
            Component c = e.getValue();
            Rectangle r = drawMap.get(e.getKey());
            Rectangle parent = null;

            if (e.getValue() instanceof Deck || e.getValue() instanceof Area) {
                continue;  // todo: can have decks in areas
            }

            // Check dependencies, draw relative to parents
            if (dependencies.containsKey(e.getKey())) {
                parent = drawMap.get(dependencies.get(e.getKey()));
            } else {  // TODO: enough to do just once?
                // Check dependency
                for (Area a : areas) {
                    if (a.getComponents().containsValue(c)) {
                        parent = drawMap.get(a.getComponentID());
                        dependencies.put(e.getKey(), a.getComponentID());
                        break;
                    }
                }
                for (Deck<? extends Component> d : decks) {
                    if (d.getComponents().contains(c)) {
                        parent = drawMap.get(d.getComponentID());
                        dependencies.put(e.getKey(), d.getComponentID());
                        break;
                    }
                }
            }

            // Draw component itself (decks and areas already drawn)
            if (c instanceof GridBoard) {
                r = getRectangle(drawMap, e, r, parent, ((GridBoard<?>) c).getWidth() * defaultItemSize,
                        ((GridBoard<?>) c).getHeight() * defaultItemSize);
                GridBoardView.drawGridBoard(g, (GridBoard<?>) c, r);
            } else if (c instanceof Counter) {
                r = getRectangle(drawMap, e, r, parent, defaultItemSize, defaultItemSize);
                CounterView.drawCounter(g, r.x, r.y, r.width, (Counter) c);
            } else if (c instanceof Card) {
                r = getRectangle(drawMap, e, r, parent, defaultCardWidth, defaultCardHeight);
                CardView.drawCard(g, r.x, r.y, r.width, r.height, (Card) c);
            } else if (c instanceof Dice) {
                r = getRectangle(drawMap, e, r, parent, defaultItemSize, defaultItemSize);
                DieView.drawDie(g, r.x, r.y, r.width, (Dice) c);
            } else if (c instanceof GraphBoard) {
                r = getRectangle(drawMap, e, r, parent, defaultBoardWidth, defaultBoardHeight);
                GraphBoardView.drawGraphBoard(g, (GraphBoard) c, r);
            } else if (c instanceof Token) {
                r = getRectangle(drawMap, e, r, parent, defaultItemSize, defaultItemSize);
                TokenView.drawToken(g, r.x, r.y, r.width, (Token) c);
            } else {
                System.out.println("Component unknown");
            }
        }

        g.setColor(Color.black);
        g.drawRect(x, y, width, height);
    }

    private static Rectangle getRectangle(HashMap<Integer, Rectangle> drawMap, Map.Entry<Integer, Component> e, Rectangle r, Rectangle parent, int width, int height) {
        if (r == null) {
            r = new Rectangle(0, 0, width, height);
            drawMap.put(e.getKey(), r);
        }
        if (parent != null) {
            Rectangle drawn = new Rectangle(r.x, r.y, r.width, r.height);
            drawn.x += parent.x;
            drawn.y += parent.y;
            return drawn;
        }
        return r;
    }
}
