package games.hanabi.gui;

import core.components.Deck;
import games.hanabi.*;
import gui.views.DeckView;
import org.davidmoten.text.utils.WordWrap;
import utilities.ImageIO;

import java.awt.*;
import static games.hanabi.gui.HanabiGUIManager.*;

public class HanabiDeckView extends DeckView<HanabiCard> {
    String dataPath;
    Image backOfCard;
    HanabiGameState hgs;

    /**
     * Constructor initialising information and adding key/mouse listener for card highlight (left click or ALT + hover
     * allows showing the highlighted card on top of all others).
     * @param d - deck to draw
     * @param visible - true if whole deck visible
     * @param dataPath - path to assets
     */
    public HanabiDeckView(HanabiGameState hgs, int human, Deck<HanabiCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(human, d, visible, hanabiCardWidth, hanabiCardHeight, rect);
        this.dataPath = dataPath;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
        this.hgs = hgs;
    }


    public void drawDeck(Graphics2D g) {
        @SuppressWarnings("unchecked") Deck<HanabiCard> deck = (Deck<HanabiCard>) component;
        if (deck != null && deck.getSize() > 0) {
            // Draw cards, 0 index on top
            int offset = Math.max((rect.width - itemWidth) / deck.getSize(), minCardOffset);
            rects = new Rectangle[deck.getSize()];
            for (int i = deck.getSize() - 1; i >= 0; i--) {
                if (i < deck.getSize()) {
                    HanabiCard card = deck.get(i);
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, itemWidth, itemHeight);
                    rects[i] = r;
                    drawComponent(g, r, card, front || componentVisibility(deck, i));
                }
            }
            if (cardHighlight != -1) {
                // Draw this one on top
                if (deck.getSize() > cardHighlight) {
                    HanabiCard card = deck.get(cardHighlight);
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
        }
    }

    /**
     * Draws the specified component at the specified place
     *
     * @param g         Graphics object
     * @param rect      Where the item is to be drawn
     * @param card The item itself
     * @param front     true if the item is visible (e.g. the card details); false if only the card-back
     */
    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, HanabiCard card, boolean front) {
        int currentPlayer = hgs.getCurrentPlayer();
        // Draw background
        if (!card.getColorStr(currentPlayer).contains("?")) g.setColor(card.color.color);
        else g.setColor(Color.lightGray);
        g.fillRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
        g.setColor(Color.black);

        // Draw card name and owner
        String value = "?";
        if (!card.getNumberStr(currentPlayer).contains("?")) value = "" + card.number;
        int w = (width * 2 - 10) / g.getFont().getSize();
        String wrapped =
                WordWrap.from(value)
                        .maxWidth(w)
                        .insertHyphens(true) // true is the default
                        .wrap();
        String[] wraps = wrapped.split("\n");
        int size = g.getFont().getSize();

        int i = 0;
        for (String s : wraps) {
            g.drawString(s, rect.x + rect.width - 20, rect.y + i * size + 20);
            i++;
        }

        // Draw border
        g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
    }
}
