package games.poker.gui;

import core.components.Deck;
import core.components.FrenchCard;
import gui.views.CardView;
import gui.views.ComponentView;
import utilities.ImageIO;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static games.poker.gui.PokerGUIManager.*;

public class PokerDeckView extends ComponentView {

    // Is deck visible?
    protected boolean front;
    // Back of card image
    Image backOfCard;
    // Path to assets
    String dataPath;
    // Minimum distance between cards drawn in deck area
    int minCardOffset = 5;

    // Rectangles where cards are drawn, used for highlighting
    Rectangle[] rects;
    // Index of card highlighted
    int cardHighlight = -1;  // left click (or ALT+hover) show card, right click back in deck
    // If currently highlighting (ALT)
    boolean highlighting;

    /**
     * Constructor initialising information and adding key/mouse listener for card highlight (left click or ALT + hover
     * allows showing the highlighted card on top of all others).
     * @param d - deck to draw
     * @param visible - true if whole deck visible
     * @param dataPath - path to assets
     */
    public PokerDeckView(Deck<FrenchCard> d, boolean visible, String dataPath) {
        super(d, playerAreaWidth, pokerCardHeight);
        this.front = visible;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
        this.dataPath = dataPath;

        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

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
                if (e.getButton() == 1) {
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
        drawDeck((Graphics2D) g, new Rectangle(0, 0, width, pokerCardHeight));
    }

    /**
     * Draws all cards in the deck, evenly spaced.
     * @param g - Graphics object
     */
    public void drawDeck(Graphics2D g, Rectangle rect) {
        int size = g.getFont().getSize();
        @SuppressWarnings("unchecked") Deck<FrenchCard> deck = (Deck<FrenchCard>) component;

        if (deck != null && deck.getSize() > 0) {
            // Draw cards, 0 index on top
            int offset = Math.max((rect.width-pokerCardWidth) / deck.getSize(), minCardOffset);
            rects = new Rectangle[deck.getSize()];
            for (int i = 0; i < deck.getSize(); i++) {
                FrenchCard card = deck.get(i);
                Image cardFace = getCardImage(card);
                Rectangle r = new Rectangle(rect.x + offset * i, rect.y, pokerCardWidth, pokerCardHeight);
                rects[i] = r;
                CardView.drawCard(g, r.x, r.y, r.width, r.height, card, cardFace, backOfCard, front);
                g.drawRoundRect(r.x, r.y, r.width, r.height, 15, 15);
            }
            if (cardHighlight != -1) {
                // Draw this one on top
                FrenchCard card = deck.get(cardHighlight);
                Image cardFace = getCardImage(card);
                Rectangle r = rects[cardHighlight];
                CardView.drawCard(g, r.x, r.y, r.width, r.height, card, cardFace, backOfCard, front);
                g.drawRoundRect(r.x, r.y, r.width, r.height, 15, 15);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Retrieves the image for a card from the asset folder.
     * @param card - card to retrieve image for
     * @return - image for card.
     */
    private Image getCardImage(FrenchCard card) {
        Image img = null;
        //String cardName = card.suite.substring(0, 1).toUpperCase() + card.suite.substring(1).toLowerCase();
        switch(card.type) {
            case Number:
                img = ImageIO.GetInstance().getImage(dataPath + card.number + card.suite + ".png");
                break;
            case Jack:
            case Queen:
            case King:
            case Ace:
                img = ImageIO.GetInstance().getImage(dataPath + card.type + card.suite + ".png");
                break;
        }
        return img;
    }

    // Getters, setters
    public int getCardHighlight() {
        return cardHighlight;
    }
    public void setCardHighlight(int cardHighlight) {
        this.cardHighlight = cardHighlight;
    }
    public Rectangle[] getRects() {
        return rects;
    }
    public void setFront(boolean visible) {
        this.front = visible;
    }
    public void flip() {
        front = !front;
    }
}
