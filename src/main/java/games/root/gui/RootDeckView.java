package games.root.gui;

import core.components.Deck;
import games.root.components.cards.RootCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

import static games.root.gui.RootGUIManager.cardHeight;
import static games.root.gui.RootGUIManager.cardWidth;


public class RootDeckView extends DeckView<RootCard> {
    String dataPath;
    Image backOfCard;

    public RootDeckView(int player, Deck<RootCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(player, d, visible, cardWidth, cardHeight, rect);
        this.dataPath = dataPath;
        this.front = visible;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "back.png");;
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, RootCard card, boolean front) {
        Image cardFace = getCardImage(card);
        CardView.drawCard(g, rect, card, cardFace, backOfCard, front);

    }

    private Image getCardImage(RootCard card)
    {
        String cardName = card.cardType.name().toLowerCase();
        String cardSuit = card.suit.name().toLowerCase();
        return ImageIO.GetInstance().getImage(dataPath + cardName + cardSuit + ".png");
    }
}
