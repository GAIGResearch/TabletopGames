package gui.views;

import core.AbstractGameState;
import core.components.*;
import core.components.Component;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static gui.GUI.*;

// TODO: snap component to deck (add component to deck + dependency) + remove on extract
// Long press move deck/area, short press move component in deck/area
public class AreaView extends ComponentView {
    private HashMap<Integer, Rectangle> drawMap;  // Holds references to all the rectangles to be drawn, from position (0,0)
    private HashMap<Integer, Integer> dependencies;  // Dependencies of components to other components (ID -> ID mapping), e.g. cards to parent decks

    private Deck<? extends Component> deckHighlight;  // Top deck being highlighted, contents can be displayed in GUI

    private ArrayList<Map.Entry<Integer, Rectangle>> dragging;  // A list of non-dependent rectangles currently getting dragged on the screen
    private ArrayList<Map.Entry<Integer, Rectangle>> draggingDependent;  // A list of dependent rectangles currently getting dragged on the screen
    private Point selectionStart, selectionEnd;  // If selecting an area on the screen, these define the points where the selection started, and where it ends
    private Point initialClick;  // Initial point clicked in a drag motion
    private HashMap<Integer, Point> translation;  // Translations applied to rectangles after dragging motions

    AbstractGameState gs;

    public AreaView(AbstractGameState gs, Area area, int width, int height) {
        super(area, width, height);
        this.gs = gs;

        // Initialise all maps and lists
        drawMap = new HashMap<>();
        dependencies = new HashMap<>();
        dragging = new ArrayList<>();
        draggingDependent = new ArrayList<>();
        translation = new HashMap<>();

        // Set up mouse adapter to handle clicks and drags
        MouseAdapter ma = new MouseAdapter() {
            private java.util.Timer timer;
            private boolean selecting;

            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();

                if (e.getButton() == MouseEvent.BUTTON3) {
                    // On right click, reset the deck/area (all components to 0,0)
                    for (Map.Entry<Integer, Rectangle> en : drawMap.entrySet()) {
                        Component c = area.getComponents().get(en.getKey());
                        if (c instanceof Deck || c instanceof Area) {
                            Rectangle r = new Rectangle(en.getValue());
                            if (translation.containsKey(en.getKey())) {
                                // Add translation
                                r.x += translation.get(en.getKey()).x;
                                r.y += translation.get(en.getKey()).y;
                            }
                            // Was this rectangle clicked on?
                            if (r.contains(e.getPoint())) {
                                // Are there others dependent on this?
                                if (dependencies.containsValue(c.getComponentID())) {
                                    // Reset translations for all dependents
                                    for (Map.Entry<Integer, Integer> dep : dependencies.entrySet()) {
                                        if (dep.getValue() == c.getComponentID()) {
                                            translation.put(dep.getKey(), new Point());
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                } else {
                    // On left click, select items and move them
                    if (selecting) {
                        selecting = false;
                    } else {
                        boolean intersect = false;

                        // Find rectangle clicked on
                        for (Map.Entry<Integer, Rectangle> en : drawMap.entrySet()) {

                            // Can't move areas
                            Component c = area.getComponents().get(en.getKey());
                            if (c instanceof Area) continue;

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
                            // Was this rectangle clicked on?
                            if (r.contains(e.getPoint())) {
                                intersect = true;

                                // Decks not moved with short clicks, skip for now
                                if (c instanceof Deck) {
                                    deckHighlight = (Deck<? extends Component>) c;
                                    continue;
                                }

                                // Add rectangle to list of draggable objects
                                if (dependencies.containsKey(en.getKey())) {
                                    draggingDependent.add(en);
                                } else {
                                    dragging.add(en);
                                }

                                // Only drag first object
                                break;
                            }
                        }

                        if (intersect) {
                            // If something was clicked on, start a timer for long clicks which move collections of components
                            if (timer == null) {
                                timer = new java.util.Timer();
                            }
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    for (Map.Entry<Integer, Rectangle> en : drawMap.entrySet()) {
                                        Component c = area.getComponents().get(en.getKey());

                                        // Only move collections on long clicks
                                        if (c instanceof Deck || c instanceof Area) {
                                            Rectangle r = new Rectangle(en.getValue());
                                            // Add translation
                                            if (translation.containsKey(en.getKey())) {
                                                r.x += translation.get(en.getKey()).x;
                                                r.y += translation.get(en.getKey()).y;
                                            }
                                            // Was this rectangle clicked on?
                                            if (r.contains(e.getPoint())) {
                                                // Remove any previously added elements
                                                dragging.clear();
                                                draggingDependent.clear();

                                                // Add this element and note highlight if a deck
                                                dragging.add(en);
                                                if (c instanceof Deck) {
                                                    deckHighlight = (Deck<? extends Component>) c;
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }, 600);
                        } else {
                            // Not intersecting rectangle, possibly dragging selection for multiples instead.
                            selecting = true;
                            selectionStart = e.getPoint();
                            deckHighlight = null;
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Nothing dragged anymore
                dragging.clear();
                draggingDependent.clear();

                // Check if there was something selected in the last motion
                if (selectionEnd != null && selectionStart != null && !selectionStart.equals(selectionEnd)) {

                    // Find rectangle drawn by selection start and end points
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
                        // Does the selection rectangle include ALL 4 points of this object?
                        if (selection.contains(new Point((int)r.getMinX(), (int)r.getMinY())) &&
                                selection.contains(new Point((int)r.getMinX(), (int)r.getMaxY())) &&
                                selection.contains(new Point((int)r.getMaxX(), (int)r.getMinY())) &&
                                selection.contains(new Point((int)r.getMaxX(), (int)r.getMaxY()))) {

                            // Mark deck selection
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
                    // Finished selecting
                    selecting = false;
                }

                // Reset variables
                selectionStart = null;
                selectionEnd = null;
                initialClick = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (timer != null) {
                    // Moved the mouse, stop waiting for long press
                    timer.cancel();
                    timer = null;
                }
                if (selecting) {
                    // Update selection end point, if selecting
                    selectionEnd = e.getPoint();
                } else {
                    // Update translations of parents with the difference of mouse moved since last press/drag function call
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

                    // Only update translations of children if their parents were not updated
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

                // Update last click point to here
                initialClick = e.getPoint();
            }
        };

        // Adding the mouse adapter defined as a listener, and also as a motion listener
        addMouseListener(ma);
        addMouseMotionListener(ma);

    }

    @Override
    protected void paintComponent(Graphics g) {
        drawArea((Graphics2D) g);
    }

    protected void drawArea(Graphics2D g) {
        // Draw components in area
        drawArea(g, gs, (Area) component, 0, 0, width, height, drawMap, dependencies, translation);

        // Draw highlight of deck
        if (deckHighlight != null) {
            Rectangle r = drawMap.get(deckHighlight.getComponentID());
            g.setColor(Color.green);
            g.drawRect(r.x - 1, r.y - 1, r.width + 2, r.height + 2);
            g.setColor(Color.black);
        }

        // Draw highlights of dragging components, dependent orange, non-dependent yellow
        drawHighlightDrag(g, dragging, Color.yellow);
        drawHighlightDrag(g, draggingDependent, Color.orange);

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

    /**
     * Draws highlights of a list of rectangles with given color
     * @param g - Graphics object to use for drawing
     * @param highlights - list of highlighted rectangles
     * @param color - color to highlight with
     */
    private void drawHighlightDrag(Graphics2D g, ArrayList<Map.Entry<Integer, Rectangle>> highlights, Color color) {
        g.setColor(color);
        for (Map.Entry<Integer,Rectangle> d: highlights) {
            Rectangle r = d.getValue();
            Rectangle toDraw = getRectangle(d.getKey(), r.width, r.height, drawMap, dependencies, translation);
            g.drawRect(toDraw.x - 1, toDraw.y - 1, toDraw.width + 2, toDraw.height + 2);
        }
        g.setColor(Color.black);
    }

    /**
     * Draws an area within a given rectangle, with a map of rectangles for components, a map of dependencies of
     * components, and a map of translations of component rectangles. Updates the dependencies and drawMap within the function.
     * @param g - Graphics2D object to use to draw
     * @param area - area containing components to draw
     * @param x - x position
     * @param y - y position
     * @param width - width of area
     * @param height - height of area
     * @param drawMap - mapping from component ID to rectangle drawn on the screen
     * @param dependencies - mapping from component ID to parent component ID (if applicable)
     * @param translation - mapping from component ID to Point representing its translation on the screen
     */
    public static void drawArea(Graphics2D g, AbstractGameState gs, Area area, int x, int y, int width, int height,
                                HashMap<Integer, Rectangle> drawMap,
                                HashMap<Integer, Integer> dependencies,
                                HashMap<Integer, Point> translation) {
        int fontSize = g.getFont().getSize();

        // Find all sub-collections in this area: decks and other areas. Just drawing a rectangle around them and name
        List<Deck<? extends Component>> decks = new ArrayList<>();
        List<Area> areas = new ArrayList<>();
        for (Map.Entry<Integer, Component> e: area.getComponentsMap().entrySet()) {
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
        for (Map.Entry<Integer, Component> e: area.getComponentsMap().entrySet()) {
            Component c = e.getValue();

            // Decks and areas already drawn
            if (e.getValue() instanceof Deck || e.getValue() instanceof Area) {
                continue;
            }

            // Check dependencies, draw relative to parents
            if (!dependencies.containsKey(e.getKey())) {
                // Check dependency
                for (Area a : areas) {
                    if (a.getComponentsMap().containsValue(c)) {
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
                Rectangle toDraw = getRectangle(e.getKey(),((GridBoard) c).getWidth() * defaultItemSize,
                        ((GridBoard) c).getHeight() * defaultItemSize, drawMap, dependencies, translation);
                GridBoardView.drawGridBoard(g, (GridBoard) c, toDraw);
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
                GraphBoardView.drawGraphBoard(g, gs, (GraphBoard) c, toDraw);
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

    /**
     * Retrieves the on-screen display of a component, given its base rectangle in drawMap and translations.
     * @param key - component ID
     * @param width - width of component display
     * @param height - height of component display
     * @param drawMap - mapping from component ID to base rectangle
     * @param dependencies - mapping from component ID to parent component ID, used to apply parent's base rectangle to this
     * @param translation - mapping from component ID to Point representing its translation on the screen, which is
     *                    applied to the base rectangle. If component has a parent, the parent's translation is applied
     *                    to the base rectangle as well.
     * @return - new Rectangle object representing this component's current location on the screen
     */
    private static Rectangle getRectangle(int key, int width, int height,
                                          HashMap<Integer, Rectangle> drawMap,
                                          HashMap<Integer, Integer> dependencies,
                                          HashMap<Integer, Point> translation) {
        // Find base rectangles and translations
        Rectangle r = drawMap.get(key);
        Point t = translation.get(key);
        Rectangle parent = null;
        Point pt = null;
        if (dependencies.containsKey(key)) {
            int parentId = dependencies.get(key);
            parent = drawMap.get(parentId);
            pt = translation.get(parentId);
        }

        // If null, update the draw map with this component's base rectangle
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

    /**
     * Retrieves the rectangle display on screen for a non-dependent component.
     */
    private static Rectangle getRectangle(int key, int width, int height,
                                          HashMap<Integer, Rectangle> drawMap) {
        Rectangle r = drawMap.get(key);
        if (r == null) {
            r = new Rectangle(0, 0, width, height);
            drawMap.put(key, r);
        }
        return new Rectangle(r);
    }

    /**
     * Get the currently highlighted deck.
     * @return deck highlighted.
     */
    public Deck<? extends Component> getDeckHighlight() {
        return deckHighlight;
    }
}
