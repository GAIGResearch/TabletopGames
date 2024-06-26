package games.sushigo.gui;

import core.components.Deck;
import games.sushigo.cards.SGCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

import static games.sushigo.gui.SGGUIManager.*;

public class SGDeckView extends DeckView<SGCard> {

    String dataPath;
    Image backOfCard;

    public SGDeckView(int player, Deck<SGCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(player, d, visible, SGCardWidth, SGCardHeight, rect);
        this.dataPath = dataPath;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, SGCard card, boolean front) {
        Image cardFace = getCardImage(card);
        int fontSize = g.getFont().getSize();
        CardView.drawCard(g, new Rectangle(rect.x, rect.y, rect.width, rect.height-fontSize), card, cardFace, backOfCard, front);
        if (front) {
            g.drawString(card.type.name(), rect.x + 2, (int) (rect.y + rect.height - fontSize * 1.5));
        }
    }

    private Image getCardImage(SGCard card)
    {
        String cardName = card.type.name().toLowerCase();
        if (card.type == SGCard.SGCardType.Maki) {
            cardName = cardName + "_" + card.count;
        }
        return ImageIO.GetInstance().getImage(dataPath + cardName + ".png");
    }

}
