package games.uno.gui;

import core.components.Deck;
import games.uno.cards.UnoCard;
import gui.views.CardView;
import gui.views.ComponentView;
import utilities.ImageIO;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static games.uno.gui.UnoGUI.*;

public class UnoDeckView extends ComponentView {
    protected boolean front;
    Image backOfCard;
    String dataPath;
    int minCardOffset = 5;

    Rectangle[] rects;
    int cardHighlight = -1;  // left click show card, right click back in deck
    boolean highlighting;

    public UnoDeckView(Deck<UnoCard> d, boolean visible, String dataPath) {
        super(d, playerAreaWidth, unoCardHeight);
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
        drawDeck((Graphics2D) g);
    }

    public void setFront(boolean visible) {
        this.front = visible;
    }

    public void flip() {
        front = !front;
    }

    public void drawDeck(Graphics2D g) {
        int size = g.getFont().getSize();
        Deck<UnoCard> deck = (Deck<UnoCard>) component;

        if (deck != null) {
            // Draw cards, 0 index on top
            int offset = Math.max((width-unoCardWidth) / deck.getSize(), minCardOffset);
            rects = new Rectangle[deck.getSize()];
            for (int i = deck.getSize()-1; i >= 0; i--) {
                UnoCard card = deck.get(i);
                Image cardFace = getCardImage(card);
                Rectangle r = new Rectangle(offset * i, 0, unoCardWidth, unoCardHeight);
                rects[i] = r;
                CardView.drawCard(g, offset * i, 0, unoCardWidth, unoCardHeight, card, cardFace, backOfCard, front);
            }
            if (cardHighlight != -1) {
                // Draw this one on top
                UnoCard card = deck.get(cardHighlight);
                Image cardFace = getCardImage(card);
                CardView.drawCard(g, offset * cardHighlight, 0, unoCardWidth, unoCardHeight, card, cardFace, backOfCard, front);
            }
            g.drawString(""+deck.getSize(), 10, unoCardHeight - size);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    private Image getCardImage(UnoCard card) {
        Image img = null;
        String colorName = card.color.substring(0, 1).toUpperCase() + card.color.substring(1).toLowerCase();
        switch(card.type) {
            case Number:
                img = ImageIO.GetInstance().getImage(dataPath + colorName + card.number + ".png");
                break;
            case Skip:
                img = ImageIO.GetInstance().getImage(dataPath + colorName + "Skip.png");
                break;
            case Reverse:
                img = ImageIO.GetInstance().getImage(dataPath + colorName + "Reverse.png");
                break;
            case Draw:
                img = ImageIO.GetInstance().getImage(dataPath + colorName + "Draw" + card.drawN + ".png");
                break;
            case Wild:
                if (card.drawN > 0) {
                    img = ImageIO.GetInstance().getImage(dataPath + "WildDraw" + card.drawN + ".png");
                } else {
                    img = ImageIO.GetInstance().getImage(dataPath + "Wild.png");
                }
                break;
        }
        return img;
    }

    public int getCardHighlight() {
        return cardHighlight;
    }

    public void setCardHighlight(int cardHighlight) {
        this.cardHighlight = cardHighlight;
    }

    public Rectangle[] getRects() {
        return rects;
    }
}
