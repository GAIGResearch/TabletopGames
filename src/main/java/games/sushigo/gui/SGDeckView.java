package games.sushigo.gui;

import core.components.Deck;
import games.sushigo.cards.SGCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

import static games.sushigo.gui.SGGUI.*;

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
        CardView.drawCard(g, rect, card, cardFace, backOfCard, front);
    }

    private Image getCardImage(SGCard card)
    {
        return ImageIO.GetInstance().getImage(dataPath + card.type + ".png");
    }

}
