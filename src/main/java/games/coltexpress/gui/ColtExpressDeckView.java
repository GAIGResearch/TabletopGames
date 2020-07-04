package games.coltexpress.gui;

import core.components.Deck;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.cards.ColtExpressCard;
import gui.views.CardView;
import gui.views.ComponentView;
import utilities.ImageIO;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import static games.coltexpress.gui.ColtExpressGUI.*;

public class ColtExpressDeckView extends ComponentView {
    protected boolean front;
    Image backOfCard;
    String dataPath;
    int minCardOffset = 5;

    Rectangle[] rects;
    int cardHighlight = -1;

    HashMap<Integer, ColtExpressTypes.CharacterType> characters;

    public ColtExpressDeckView(Deck<ColtExpressCard> d, boolean visible, String dataPath,
                               HashMap<Integer, ColtExpressTypes.CharacterType> characters) {
        super(d, playerAreaWidth, ceCardHeight);
        this.front = visible;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
        this.dataPath = dataPath;
        this.characters = characters;

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
        Deck<ColtExpressCard> deck = (Deck<ColtExpressCard>) component;

        if (deck != null && deck.getSize() > 0) {
            // Draw cards, 0 index on top
            int offset = Math.max((width-ceCardWidth) / deck.getSize(), minCardOffset);
            rects = new Rectangle[deck.getSize()];
            for (int i = deck.getSize()-1; i >= 0; i--) {
                ColtExpressCard card = deck.get(i);
                Rectangle r = new Rectangle(offset * i, 0, ceCardWidth, ceCardHeight);
                rects[i] = r;
                drawCard(g, card, r);
            }
            if (cardHighlight != -1) {
                // Draw this one on top
                ColtExpressCard card = deck.get(cardHighlight);
                Rectangle r = new Rectangle(offset * cardHighlight, 0, ceCardWidth, ceCardHeight);
                drawCard(g, card, r);
            }
            g.setColor(Color.black);
            g.drawString(""+deck.getSize(), 10, ceCardHeight - size);
        }
    }

    private void drawCard(Graphics2D g, ColtExpressCard card, Rectangle r) {
        Image cardFace = ImageIO.GetInstance().getImage(dataPath + "characters/deck/" + card.cardType.name() + ".png");
        CardView.drawCard(g, r.x, r.y, r.width, r.height, card, cardFace, backOfCard, front);
        // Draw border around card in player owner's color
        Stroke s = g.getStroke();
        int width = 6;
        int arc = 20;
        g.setStroke(new BasicStroke(width));
        g.setColor(characters.get(card.playerID).getColor());
        g.drawRoundRect(r.x + width/2, r.y + width/2, r.width - width, r.height - width, arc, arc);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
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
