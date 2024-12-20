package games.resistance.gui;

import core.components.Deck;
import games.resistance.components.ResPlayerCards;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

import static games.resistance.gui.ResGUIManager.ResPlayerCardsHeight;
import static games.resistance.gui.ResGUIManager.ResPlayerCardsWidth;

public class ResDeckView extends DeckView<ResPlayerCards> {

    String dataPath;
    Image backOfCard;

    public ResDeckView(int player, Deck<ResPlayerCards> d, boolean visible, String dataPath, Rectangle rect) {
        super(player, d, visible, ResPlayerCardsWidth, ResPlayerCardsHeight, rect);
        this.dataPath = dataPath;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, ResPlayerCards card, boolean front) {
        Image cardFace = getCardImage(card);
        int fontSize = g.getFont().getSize();
        CardView.drawCard(g, new Rectangle(rect.x, rect.y, rect.width, rect.height-fontSize), card, cardFace, backOfCard, front);
        g.drawString(card.getType().name(), rect.x + 2, (int)(rect.y + rect.height - fontSize*1.5));
    }

    private Image getCardImage(ResPlayerCards card)
    {
        String cardName = card.cardType.name().toLowerCase();
        return ImageIO.GetInstance().getImage(dataPath + cardName + ".png");
    }

}
