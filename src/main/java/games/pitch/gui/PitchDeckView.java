package games.pitch.gui;

import core.components.Deck;
import core.components.FrenchCard;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

/**
 * A player's hand, face up (sorted by suit, Aces high) or face down.
 */
public class PitchDeckView extends DeckView<FrenchCard> {

    static final String DATA_PATH = "data/FrenchCards/";
    private final Image backOfCard;
    private final Dimension size;

    public PitchDeckView(int humanId, Deck<FrenchCard> deck, boolean visible, int width, int height) {
        super(humanId, deck, visible, PitchGUIManager.CARD_WIDTH, PitchGUIManager.CARD_HEIGHT,
                new Rectangle(0, 0, width, height));
        backOfCard = ImageIO.GetInstance().getImage(DATA_PATH + "gray_back.png");
        size = new Dimension(width, height);
        setOpaque(false);
        setDisplayOrder(FrenchCard.HAND_DISPLAY_ORDER);
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, FrenchCard card, boolean front) {
        Image img = front ? cardImage(card) : backOfCard;
        g.drawImage(img, rect.x, rect.y, rect.width, rect.height, null);
    }

    static Image cardImage(FrenchCard card) {
        String rank = card.type == FrenchCard.FrenchCardType.Number ? String.valueOf(card.number) : card.type.name();
        return ImageIO.GetInstance().getImage(DATA_PATH + rank + card.suite + ".png");
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        return size;
    }
}
