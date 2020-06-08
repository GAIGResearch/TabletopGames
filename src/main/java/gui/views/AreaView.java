package gui.views;

import core.components.*;
import core.components.Component;
import gui.TokenView;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static core.AbstractGUI.*;

// TODO: snap deck/area (add component to deck/area + dependency) + remove on extract
// Long press move deck/area, short press move component in deck/area
public class AreaView extends ComponentView {
    private HashMap<Integer, Rectangle> drawMap;
    private ArrayList<Map.Entry<Integer, Rectangle>> draggingDependent, dragging;
    private HashMap<Integer, Integer> dependencies;  // Components within other components

    private Deck<? extends Component> deckHighlight;
    private Point selectionStart, selectionEnd;
    private Point initialClick;
    private HashMap<Integer, Point> translation;

    public AreaView(Area area, int width, int height) {
        super(area, width, height);

        drawMap = new HashMap<>();
        dependencies = new HashMap<>();
        draggingDependent = new ArrayList<>();
        dragging = new ArrayList<>();
        translation = new HashMap<>();

        MouseAdapter ma = new MouseAdapter() {
            private java.util.Timer timer;
            private boolean selecting;

            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();

                if (e.getButton() == MouseEvent.BUTTON3) {
                    // On right click, reset the deck/area (all components to 0,0)
                    for (Map.Entry<Integer, Rectangle> en : drawMap.entrySet()) {
                        Rectangle r = new Rectangle(en.getValue());
                        if (translation.containsKey(en.getKey())) {
                            // Add translation
                            r.x += translation.get(en.getKey()).x;
                            r.y += translation.get(en.getKey()).y;
                        }
                        Component c = area.getComponents().get(en.getKey());
                        if ((c instanceof Deck || c instanceof Area) && r.contains(e.getPoint())) {
                            if (dependencies.containsValue(c.getComponentID())) {
                                for (Map.Entry<Integer, Integer> dep : dependencies.entrySet()) {
                                    if (dep.getValue() == c.getComponentID()) {
//                                        Rectangle r2 = drawMap.get(dep.getKey());
//                                        drawMap.put(dep.getKey(), new Rectangle(0, 0, r2.width, r2.height));
                                        translation.put(dep.getKey(), new Point());
                                    }
                                }
                            }
                            break;
                        }
                    }
                } else {
                    // On left click, select items and move them
                    if (selecting) {
                        selecting = false;
                    } else {
                        boolean intersect = false;
                        for (Map.Entry<Integer, Rectangle> en : drawMap.entrySet()) {
                            Rectangle r = new Rectangle(en.getValue());
                            // Add translation
                            if (translation.containsKey(en.getKey())) {
                                r.x += translation.get(en.getKey()).x;
                                r.y += translation.get(en.getKey()).y;
                            }
                            // Add parent dependency
                            if (dependencies.containsKey(en.getKey())) {
                                int parentId = dependencies.get(en.getKey());
                                r.x += drawMap.get(parentId).x;
                                r.y += drawMap.get(parentId).y;
                                // Add parent translation
                                if (translation.containsKey(parentId)) {
                                    r.x += translation.get(parentId).x;
                                    r.y += translation.get(parentId).y;
                                }
                            }
                            if (r.contains(e.getPoint())) {
                                intersect = true;

                                //Don't add decks and areas here.
                                Component c = area.getComponents().get(en.getKey());
                                if (c instanceof Deck) {
                                    deckHighlight = (Deck<? extends Component>) c;
                                    continue;
                                } else if (c instanceof Area) continue;

                                // clicked on this rectangle
                                if (dependencies.containsKey(en.getKey())) {
                                    draggingDependent.add(en);
                                } else {
                                    dragging.add(en);
                                }
                                break;
                            }
                        }
                        if (intersect) {
                            if (timer == null) {
                                timer = new java.util.Timer();
                            }
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    dragging.clear();
                                    for (Map.Entry<Integer, Rectangle> en : drawMap.entrySet()) {
                                        Component c = area.getComponents().get(en.getKey());
                                        Rectangle r = new Rectangle(en.getValue());
                                        // Add translation
                                        if (translation.containsKey(en.getKey())) {
                                            r.x += translation.get(en.getKey()).x;
                                            r.y += translation.get(en.getKey()).y;
                                        }
                                        if ((c instanceof Deck || c instanceof Area) && r.contains(e.getPoint())) {
                                            // long clicked on this rectangle of a collection
                                            dragging.add(en);
                                            if (c instanceof Deck) {
                                                deckHighlight = (Deck<? extends Component>) c;
                                            }
                                            draggingDependent.clear();
                                            break;
                                        }
                                    }
                                }
                            }, 600);
                        } else {
                            // Not intersecting rect, dragging selection for multiples instead.
                            selecting = true;
                            selectionStart = e.getPoint();
                            deckHighlight = null;
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging.clear();
                draggingDependent.clear();

                if (selectionEnd != null && selectionStart != null && !selectionStart.equals(selectionEnd)) {
                    int topX = Math.min(selectionEnd.x, selectionStart.x);
                    int topY = Math.min(selectionEnd.y, selectionStart.y);
                    int width = Math.abs(selectionEnd.x-selectionStart.x);
                    int height = Math.abs(selectionEnd.y-selectionStart.y);
                    Rectangle selection = new Rectangle(topX, topY, width, height);

                    for (Map.Entry<Integer, Rectangle> en : drawMap.entrySet()) {
                        Rectangle r = new Rectangle(en.getValue());
                        // Add translation
                        if (translation.containsKey(en.getKey())) {
                            r.x += translation.get(en.getKey()).x;
                            r.y += translation.get(en.getKey()).y;
                        }
                        // Add parent dependency
                        if (dependencies.containsKey(en.getKey())) {
                            int parentId = dependencies.get(en.getKey());
                            r.x += drawMap.get(parentId).x;
                            r.y += drawMap.get(parentId).y;
                            // Add parent translation
                            if (translation.containsKey(parentId)) {
                                r.x += translation.get(parentId).x;
                                r.y += translation.get(parentId).y;
                            }
                        }
                        if (selection.contains(new Point((int)r.getMinX(), (int)r.getMinY())) &&
                                selection.contains(new Point((int)r.getMinX(), (int)r.getMaxY())) &&
                                selection.contains(new Point((int)r.getMaxX(), (int)r.getMinY())) &&
                                selection.contains(new Point((int)r.getMaxX(), (int)r.getMaxY()))) {

                            Component c = area.getComponents().get(en.getKey());
                            if (c instanceof Deck) {
                                deckHighlight = (Deck<? extends Component>) c;
                            }

                            // Dragging this rectangle
                            if (dependencies.containsKey(en.getKey())) {
                                draggingDependent.add(en);
                            } else {
                                dragging.add(en);
                            }
                        }
                    }
                } else {
                    selecting = false;
                }

                selectionStart = null;
                selectionEnd = null;
                initialClick = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                if (selecting) {
                    selectionEnd = e.getPoint();
                } else {
                    ArrayList<Integer> parents = new ArrayList<>();
                    if (dragging != null && dragging.size() > 0) {
                        for (Map.Entry<Integer, Rectangle> dragged : dragging) {
                            Point old = translation.get(dragged.getKey());
                            if (old != null) {  // Add to previous translation
                                translation.put(dragged.getKey(),
                                        new Point(old.x + e.getPoint().x - initialClick.x, old.y + e.getPoint().y - initialClick.y));
                            } else {
                                translation.put(dragged.getKey(),
                                        new Point(e.getPoint().x - initialClick.x, e.getPoint().y - initialClick.y));
                            }
                            parents.add(dragged.getKey());
                        }
                        repaint();
                    }

                    // Only update translations of children if their parents not updated
                    if (draggingDependent != null && draggingDependent.size() > 0) {
                        for (Map.Entry<Integer, Rectangle> dragged : draggingDependent) {
                            
                            // Check if this one's parent was updated, skip if that's the case
                            boolean parentUpdated = parents.contains(dependencies.get(dragged.getKey()));
                            if (parentUpdated) continue;

                            Point old = translation.get(dragged.getKey());
                            if (old != null) {  // Add to previous translation
                                translation.put(dragged.getKey(),
                                        new Point(old.x + e.getPoint().x - initialClick.x, old.y + e.getPoint().y - initialClick.y));
                            } else {
                                translation.put(dragged.getKey(),
                                        new Point(e.getPoint().x - initialClick.x, e.getPoint().y - initialClick.y));
                            }
                        }
                        repaint();
                    }
                }
                initialClick = e.getPoint();
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);

    }

    @Override
    protected void paintComponent(Graphics g) {
        drawArea((Graphics2D) g);
    }

    protected void drawArea(Graphics2D g) {
        drawArea(g, (Area) component, 0, 0, width, height, drawMap, dependencies, translation);

        // Draw highlighted deck
        if (deckHighlight != null) {
            Rectangle r = drawMap.get(deckHighlight.getComponentID());
            g.setColor(Color.green);
            g.drawRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
            g.setColor(Color.black);
        }

        // Draw dragging components
        ArrayList<Map.Entry<Integer, Rectangle>> highlights = new ArrayList<>();
        highlights.addAll(dragging);
        highlights.addAll(draggingDependent);
        for (Map.Entry<Integer, Rectangle> dragged: highlights) {
            Rectangle r = dragged.getValue();
            Rectangle toDraw = getRectangle(dragged.getKey(), r.width, r.height, drawMap, dependencies, translation);
            g.setColor(Color.yellow);
            g.drawRect(toDraw.x - 1, toDraw.y - 1, toDraw.width + 2, toDraw.height + 2);
            g.setColor(Color.black);
        }

        // Draw highlighted selection
        if (selectionStart != null && selectionEnd != null && !selectionStart.equals(selectionEnd)) {
            g.setColor(Color.green);
            int topX = Math.min(selectionEnd.x, selectionStart.x);
            int topY = Math.min(selectionEnd.y, selectionStart.y);
            int width = Math.abs(selectionEnd.x-selectionStart.x);
            int height = Math.abs(selectionEnd.y-selectionStart.y);
            g.drawRect(topX, topY, width, height);
            g.setColor(Color.black);
        }
    }

    public static void drawArea(Graphics2D g, Area area, int x, int y, int width, int height,
                                HashMap<Integer, Rectangle> drawMap,
                                HashMap<Integer, Integer> dependencies,
                                HashMap<Integer, Point> translation) {
        int fontSize = g.getFont().getSize();

        // Find all sub-collections in this area: decks and other areas. just draw a rectangle around them
        List<Deck<? extends Component>> decks = new ArrayList<>();
        List<Area> areas = new ArrayList<>();
        for (Map.Entry<Integer, Component> e: area.getComponents().entrySet()) {
            Rectangle r = drawMap.get(e.getKey());
            Point t = translation.get(e.getKey());

            if (e.getValue() instanceof Deck) {
                decks.add((Deck<? extends Component>) e.getValue());

                Rectangle toDraw = getRectangle(e.getKey(), defaultCardWidth, defaultCardHeight, drawMap);
                g.setColor(Color.lightGray);
                g.fillRect(toDraw.x, toDraw.y, toDraw.width, toDraw.height);
                g.setColor(Color.black);
                g.drawRect(toDraw.x, toDraw.y, toDraw.width, toDraw.height);
                g.drawString(e.getValue().getComponentName(), toDraw.x, toDraw.y + toDraw.height + fontSize);
            } else if (e.getValue() instanceof Area) {
                areas.add((Area) e.getValue());

                Rectangle toDraw = getRectangle(e.getKey(),width/2, height/2, drawMap);
                g.setColor(Color.black);
                g.drawRect(toDraw.x, toDraw.y, toDraw.width, toDraw.height);
                g.drawString(e.getValue().getComponentName(), toDraw.x, toDraw.y + toDraw.height + fontSize);
            }
        }

        // Draw components
        for (Map.Entry<Integer, Component> e: area.getComponents().entrySet()) {
            Component c = e.getValue();

            // Decks and areas already drawn
            if (e.getValue() instanceof Deck || e.getValue() instanceof Area) {
                continue;
            }

            // Check dependencies, draw relative to parents
            if (!dependencies.containsKey(e.getKey())) {
                // Check dependency
                for (Area a : areas) {
                    if (a.getComponents().containsValue(c)) {
                        dependencies.put(e.getKey(), a.getComponentID());
                        break;
                    }
                }
                for (Deck<? extends Component> d : decks) {
                    if (d.getComponents().contains(c)) {
                        dependencies.put(e.getKey(), d.getComponentID());
                        break;
                    }
                }
            }

            // Draw component itself (decks and areas already drawn)
            if (c instanceof GridBoard) {
                Rectangle toDraw = getRectangle(e.getKey(),((GridBoard<?>) c).getWidth() * defaultItemSize,
                        ((GridBoard<?>) c).getHeight() * defaultItemSize, drawMap, dependencies, translation);
                GridBoardView.drawGridBoard(g, (GridBoard<?>) c, toDraw);
            } else if (c instanceof Counter) {
                Rectangle toDraw = getRectangle(e.getKey(), defaultItemSize, defaultItemSize, drawMap, dependencies, translation);
                CounterView.drawCounter(g, (Counter) c, toDraw);
            } else if (c instanceof Card) {
                Rectangle toDraw = getRectangle(e.getKey(), defaultCardWidth, defaultCardHeight, drawMap, dependencies, translation);
                CardView.drawCard(g, (Card) c, toDraw);
            } else if (c instanceof Dice) {
                Rectangle toDraw = getRectangle(e.getKey(), defaultItemSize, defaultItemSize, drawMap, dependencies, translation);
                DieView.drawDie(g, (Dice) c, toDraw);
            } else if (c instanceof GraphBoard) {
                Rectangle toDraw = getRectangle(e.getKey(), defaultBoardWidth, defaultBoardHeight, drawMap, dependencies, translation);
                GraphBoardView.drawGraphBoard(g, (GraphBoard) c, toDraw);
            } else if (c instanceof Token) {
                Rectangle toDraw = getRectangle(e.getKey(), defaultItemSize, defaultItemSize, drawMap, dependencies, translation);
                TokenView.drawToken(g, (Token) c, toDraw);
            } else {
                System.out.println("Component unknown");
            }
        }

        g.setColor(Color.black);
        g.drawRect(x, y, width, height);
    }

    private static Rectangle getRectangle(int key, int width, int height,
                                          HashMap<Integer, Rectangle> drawMap,
                                          HashMap<Integer, Integer> dependencies,
                                          HashMap<Integer, Point> translation) {
        Rectangle r = drawMap.get(key);
        Point t = translation.get(key);
        Rectangle parent = null;
        Point pt = null;
        if (dependencies.containsKey(key)) {
            int parentId = dependencies.get(key);
            parent = drawMap.get(parentId);
            pt = translation.get(parentId);
        }

        if (r == null) {
            r = new Rectangle(0, 0, width, height);
            drawMap.put(key, r);
        }
        Rectangle drawn = new Rectangle(r);
        // Add point translation
        if (t != null) {
            drawn.x += t.x;
            drawn.y += t.y;
        }
        // Add parent and parent translation
        if (parent != null) {
            drawn.x += parent.x;
            drawn.y += parent.y;
            if (pt != null) {
                drawn.x += pt.x;
                drawn.y += pt.y;
            }
        }
        return drawn;
    }

    private static Rectangle getRectangle(int key, int width, int height,
                                          HashMap<Integer, Rectangle> drawMap) {
        Rectangle r = drawMap.get(key);
        if (r == null) {
            r = new Rectangle(0, 0, width, height);
            drawMap.put(key, r);
        }
        return new Rectangle(r);
    }

    public Deck<? extends Component> getDeckHighlight() {
        return deckHighlight;
    }
}
