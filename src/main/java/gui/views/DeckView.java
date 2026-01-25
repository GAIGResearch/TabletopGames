package gui.views;

import core.components.*;
import core.components.Component;

import java.awt.*;
import java.awt.event.*;


public abstract class DeckView<T extends Component> extends ComponentView {
    protected boolean front;

    // Minimum distance between cards drawn in deck area
    public int minCardOffset = 5;
    // Rectangles where cards are drawn, used for highlighting
    protected Rectangle[] rects;
    // Rectangle containing the DeckView
    protected Rectangle rect;
    // Index of card highlighted
    protected int cardHighlight = -1;  // left click (or ALT+hover) show card, right click back in deck
    // If currently highlighting (ALT)
    protected boolean highlighting;
    // ID of player showing
    int humanId = -1;

    // card and display sizes
    protected int itemWidth, itemHeight;

    public DeckView(int humanPlayer, Deck<T> d, boolean visible, int componentWidth, int componentHeight) {
        this(humanPlayer, d, visible, componentWidth, componentHeight, new Rectangle(0, 0, componentWidth, componentHeight));
    }

    public DeckView(int humanPlayer, Deck<T> d, boolean visible, int componentWidth, int componentHeight, Rectangle display) {
        super(d, display.width, display.height);
        this.humanId = humanPlayer;
        this.itemHeight = componentHeight;
        this.itemWidth = componentWidth;
        this.rect = display;
        this.front = visible;

        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT) {
                    highlighting = true;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ALT) {
                    highlighting = false;
                    cardHighlight = -1;
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (highlighting) {
                    for (int i = 0; i < rects.length; i++) {
                        if (rects[i].contains(e.getPoint())) {
                            cardHighlight = i;
                            break;
                        }
                    }
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 1 && rects != null) {
                    // Left click, highlight
                    for (int i = 0; i < rects.length; i++) {
                        if (rects[i].contains(e.getPoint())) {
                            cardHighlight = i;
                            break;
                        }
                    }
                } else {
                    // Other click, reset highlight
                    cardHighlight = -1;
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawDeck((Graphics2D) g);
    }

    public void drawDeck(Graphics2D g) {
        @SuppressWarnings("unchecked") Deck<T> deck = (Deck<T>) component;
        if (deck != null && deck.getSize() > 0) {
            // Draw cards, 0 index on top
            int offset = Math.max((rect.width - itemWidth) / deck.getSize(), minCardOffset);
            rects = new Rectangle[deck.getSize()];
            for (int i = deck.getSize() - 1; i >= 0; i--) {
                if (i < deck.getSize()) {
                    T card = deck.get(i);
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, itemWidth, itemHeight);
                    rects[i] = r;
                    drawComponent(g, r, card, front || componentVisibility(deck, i));
                }
            }
            if (cardHighlight != -1) {
                // Draw this one on top
                if (deck.getSize() > cardHighlight) {
                    T card = deck.get(cardHighlight);
                    Rectangle r = rects[cardHighlight];
                    drawComponent(g, r, card, front || componentVisibility(deck, cardHighlight));
                } else {
                    cardHighlight = -1;
                }
            }
            int size = g.getFont().getSize();
//            String name = deck.getComponentName();
//            if (name != null && !name.equals("")) {
//                g.drawString(name, rect.x + 10, rect.y + size + 20);
//            }
            if (!front) g.drawString("" + deck.getSize(), rect.x + 10, rect.y + rect.height - size);
        }
    }

    public boolean componentVisibility(Deck<T> deck, int index) {
        if (deck instanceof PartialObservableDeck && humanId != -1) {
            return ((PartialObservableDeck<T>) deck).isComponentVisible(index, humanId);
        }
        return false; //
    }

    /**
     * Draws the specified component at the specified place
     *
     * @param g         Graphics object
     * @param rect      Where the item is to be drawn
     * @param component The item itself
     * @param front     true if the item is visible (e.g. the card details); false if only the card-back
     *                  (or equivalent) is to be shown
     */
    public abstract void drawComponent(Graphics2D g, Rectangle rect, T component, boolean front);

    public void setFront(boolean visible) {
        this.front = visible;
    }

    public void flip() {
        front = !front;
    }

    /**
     * Draws the provided deck
     *
     * @param g          Graphics object
     * @param deck       The deck to draw
     * @param name       The name of the deck (drawn underneath)
     * @param background The background image (e.g. the card back)
     * @param rect       The Rectangle in which the deck should be drawn
     * @param front      Whether the deck is visible or not (this is ignored if a non-null background image is provided)
     * @param <T>        The type of Component in the Deck
     */
    public static <T extends Component> void drawDeck(Graphics2D g, Deck<T> deck, String name, Image background,
                                                      Rectangle rect, boolean front) {
        if (background != null) {
            g.drawImage(background, rect.x, rect.y, rect.width, rect.height, null, null);
        } else {
            if (front && deck != null && deck.getSize() > 0) {
                Component c = deck.peek();
                if (c instanceof Card) {
                    // Draw cards, 0 index on top
                    for (int i = deck.getSize() - 1; i >= 0; i--) {
                        Card card = (Card) deck.getComponents().get(i);
                        CardView.drawCard(g, rect.x, rect.y, rect.width, rect.height, card, null, null, true);
                    }
                }
            } else {
                g.setColor(Color.lightGray);
                g.fillRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
                g.setColor(Color.black);
                g.drawLine(rect.x, rect.y, rect.x + rect.width - 1, rect.y + rect.height - 1);
            }
            g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
        }

        int size = g.getFont().getSize();
        if (name != null && !name.equals("")) {
            g.drawString(name, rect.x + 10, rect.y + size + 20);
        }
        if (deck != null) {
            g.drawString("" + deck.getSize(), rect.x + 10, rect.y + rect.height - size);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(rect.width, rect.height);
    }

    // Getters, setters
    public int getCardHighlight() {
        return cardHighlight;
    }

    public void setCardHighlight(int cardHighlight) {
        this.cardHighlight = cardHighlight;
    }
}
