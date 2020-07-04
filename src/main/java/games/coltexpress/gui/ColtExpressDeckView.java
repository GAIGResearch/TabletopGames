package games.coltexpress.gui;

import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Loot;
import gui.views.CardView;
import gui.views.ComponentView;
import utilities.ImageIO;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import static games.coltexpress.gui.ColtExpressGUI.*;

public class ColtExpressDeckView<T extends Component> extends ComponentView {
    protected boolean front;
    Image backOfCard;
    String dataPath;
    int minCardOffset = 5;

    Rectangle[] rects;
    int cardHighlight = -1;
    int activePlayer = -1;

    HashMap<Integer, ColtExpressTypes.CharacterType> characters;

    public ColtExpressDeckView(Deck<T> d, boolean visible, String dataPath,
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
        drawDeck((Graphics2D) g, new Rectangle(0, 0, width, height));
    }

    public void setFront(boolean visible) {
        this.front = visible;
    }

    public void flip() {
        front = !front;
    }

    public void drawDeck(Graphics2D g, Rectangle rect) {
        int size = g.getFont().getSize();
        Deck<T> deck = (Deck<T>) component;

        if (deck != null && deck.getSize() > 0) {
            // Draw cards, 0 index on top
            rects = new Rectangle[deck.getSize()];
            for (int i = deck.getSize()-1; i >= 0; i--) {
                if (deck.get(0) instanceof ColtExpressCard) {
                    int offset = (rect.width-ceCardWidth) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, ceCardWidth, ceCardHeight);
                    rects[i] = r;
                    drawCard(g, (ColtExpressCard) deck.get(i), r, deck instanceof PartialObservableDeck ?
                            activePlayer != -1 && ((PartialObservableDeck) deck).isComponentVisible(i, activePlayer) : front);
                } else  {
                    int offset = (rect.width-defaultItemSize) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, defaultItemSize, defaultItemSize);
                    rects[i] = r;
                    drawLoot(g, (Loot) deck.get(i), r, front);
                }
            }
            if (cardHighlight != -1) {
                // Draw this one on top
                if (deck.get(0) instanceof ColtExpressCard) {
                    int offset = (rect.width-ceCardWidth) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * cardHighlight, rect.y, ceCardWidth, ceCardHeight);
                    drawCard(g, (ColtExpressCard) deck.get(cardHighlight), r, (deck instanceof PartialObservableDeck ?
                            (activePlayer != -1 && cardHighlight != -1 && ((PartialObservableDeck) deck).isComponentVisible(cardHighlight, activePlayer)) : front));
                } else  {
                    int offset = (rect.width-defaultItemSize) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * cardHighlight, rect.y, defaultItemSize, defaultItemSize);
                    drawLoot(g, (Loot) deck.get(cardHighlight), r, front);
                }
            }
            g.setColor(Color.black);
            g.drawString(""+deck.getSize(), rect.x + 10, rect.y + rect.height - size);
        }
    }

    private void drawCard(Graphics2D g, ColtExpressCard card, Rectangle r, boolean visible) {
        Image cardFace = ImageIO.GetInstance().getImage(dataPath + "characters/deck/" + card.cardType.name() + ".png");
        CardView.drawCard(g, r.x, r.y, r.width, r.height, card, cardFace, backOfCard, visible);
        // Draw border around card in player owner's color
        if (visible) {
            Stroke s = g.getStroke();
            int width = 6;
            int arc = 20;
            g.setStroke(new BasicStroke(width));
            g.setColor(characters.get(card.playerID).getColor());
            g.drawRoundRect(r.x + width / 2, r.y + width / 2, r.width - width, r.height - width, arc, arc);
        }
    }

    private void drawLoot(Graphics2D g, Loot loot, Rectangle r, boolean visible) {
        Image lootFace = ImageIO.GetInstance().getImage(dataPath + loot.getLootType().name() + "_behind.png");
        g.drawImage(lootFace, r.x, r.y, r.width, r.height, null);
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

    public void informActivePlayer(int player) {
        this.activePlayer = player;
    }
}
