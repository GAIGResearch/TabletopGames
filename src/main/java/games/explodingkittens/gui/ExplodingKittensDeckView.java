package games.explodingkittens.gui;

import core.components.Deck;
import games.explodingkittens.cards.ExplodingKittensCard;
import gui.views.CardView;
import gui.views.ComponentView;
import utilities.ImageIO;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static games.explodingkittens.gui.ExplodingKittensGUI.*;


public class ExplodingKittensDeckView extends ComponentView {

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

    HashMap<Integer, Image> cardCatImageMapping;
    ArrayList<String> catImages;
    Random rnd = new Random();  // This doesn't need to use the game random seed

    int border = 5;
    int borderBottom = 20;

    /**
     * Constructor initialising information and adding key/mouse listener for card highlight (left click or ALT + hover
     * allows showing the highlighted card on top of all others).
     * @param d - deck to draw
     * @param visible - true if whole deck visible
     * @param dataPath - path to assets
     */
    public ExplodingKittensDeckView(Deck<ExplodingKittensCard> d, boolean visible, String dataPath) {
        super(d, playerAreaWidth, ekCardHeight + 25);
        this.front = visible;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
        this.dataPath = dataPath;
        cardCatImageMapping = new HashMap<>();

        // Get card Images
        File dir = new File(dataPath + "cats/");
        File[] files = dir.listFiles();
        catImages = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                catImages.add(f.getAbsolutePath());
            }
        }

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
        drawDeck((Graphics2D) g);
    }

    /**
     * Draws all cards in the deck, evenly spaced.
     * @param g - Graphics object
     */
    public void drawDeck(Graphics2D g) {
        int size = g.getFont().getSize();
        Deck<ExplodingKittensCard> deck = (Deck<ExplodingKittensCard>) component;

        if (deck != null && deck.getSize() > 0) {
            // Draw cards, 0 index on top
            int offset = Math.max((width-ekCardWidth) / deck.getSize(), minCardOffset);
            rects = new Rectangle[deck.getSize()];
            for (int i = deck.getSize()-1; i >= 0; i--) {
                ExplodingKittensCard card = deck.get(i);
                Rectangle r = new Rectangle(offset * i + border, border, ekCardWidth, ekCardHeight);
                rects[i] = r;
                drawCat(g, card, r);
            }
            if (cardHighlight != -1) {
                // Draw this one on top
                ExplodingKittensCard card = deck.get(cardHighlight);
                Rectangle r = rects[cardHighlight];
                drawCat(g, card, r);
            }
            g.drawString(""+deck.getSize(), 10, ekCardHeight - size);
        }
    }

    /**
     * Draws an Exploding Kittens card, with a random cat icon.
     * @param g - Graphics object
     * @param card - card to draw on
     * @param r - rectangle in which card is to be drawn
     */
    private void drawCat(Graphics2D g, ExplodingKittensCard card, Rectangle r) {
        Image cardFace = ImageIO.GetInstance().getImage(dataPath + card.cardType.name().toLowerCase() + ".png");
        CardView.drawCard(g, r.x, r.y, r.width, r.height, card, cardFace, backOfCard, front);

        if (front) {
            // Draw decorative cat image if card is visible
            Image catImg;
            if (cardCatImageMapping.containsKey(card.getComponentID())) {
                catImg = cardCatImageMapping.get(card.getComponentID());
            } else {
                // Random selection from those available
                int choice = rnd.nextInt(catImages.size());
                catImg = ImageIO.GetInstance().getImage(catImages.get(choice));
                catImages.remove(choice);
                cardCatImageMapping.put(card.getComponentID(), catImg);
            }
            double scaleW = 1.0 * defaultItemSize / catImg.getWidth(null);
            int height = (int) (catImg.getHeight(null) * scaleW);
            g.drawImage(catImg, r.x + ekCardWidth / 2 - defaultItemSize / 2, r.y + ekCardHeight / 2, defaultItemSize, height, null);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
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
