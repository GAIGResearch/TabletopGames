package games.root.gui;

import core.components.Deck;
import games.root.components.cards.RootQuestCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

import static games.root.gui.RootGUIManager.cardHeight;
import static games.root.gui.RootGUIManager.cardWidth;

public class RootQuestDeckView extends DeckView<RootQuestCard> {
    String dataPath;
    Image backOfCard;

    public RootQuestDeckView(int player, Deck<RootQuestCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(player, d, visible, cardWidth, cardHeight, rect);
        this.dataPath = dataPath;
        this.front = visible;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "back.png");;
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, RootQuestCard card, boolean front) {
        Image cardFace = getCardImage(card);
        CardView.drawCard(g, rect, card, cardFace, backOfCard, front);

    }

    private Image getCardImage(RootQuestCard card)
    {
        String cardName = card.cardType.name().toLowerCase();
        String cardSuit = card.suit.name().toLowerCase();
        return ImageIO.GetInstance().getImage(dataPath + cardName + cardSuit + ".png");
    }
}
