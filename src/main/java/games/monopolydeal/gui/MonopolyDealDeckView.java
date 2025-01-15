package games.monopolydeal.gui;

import core.components.Deck;
import games.monopolydeal.cards.MonopolyDealCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

public class MonopolyDealDeckView extends DeckView<MonopolyDealCard> {

    String dataPath;
    Image backOfCard;

    public MonopolyDealDeckView(int player, Deck<MonopolyDealCard> d, boolean visible, String dataPath, Rectangle rect, int cardWidth, int cardHeight) {
        super(player, d, visible, cardWidth, cardHeight, rect);
        this.dataPath = dataPath;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, MonopolyDealCard card, boolean front) {
        Image cardFace = getCardImage(card);
        CardView.drawCard(g, rect, card, cardFace, backOfCard, front);
    }

    private Image getCardImage(MonopolyDealCard card){
        return ImageIO.GetInstance().getImage(dataPath + card.cardType() + ".png");
    }
}