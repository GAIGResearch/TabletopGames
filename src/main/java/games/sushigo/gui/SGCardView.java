package games.sushigo.gui;

import core.components.Deck;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;

import static games.sushigo.gui.SGGUI.*;

public class SGCardView extends DeckView<SGCard> {

    String dataPath;
    Image backOfCard;

    public SGCardView(int player, Deck<SGCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(player, d, visible, SGCardWidth, SGCardHeight, rect);
        this.dataPath = dataPath;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, SGCard card, boolean front) {
        Image cardFace = getCardImage(card);
        CardView.drawCard(g, rect, card, cardFace, backOfCard, front);
    }

    public void drawFieldDeck(Graphics2D g, Rectangle rect, boolean firstOnTop){
        int size = g.getFont().getSize();
        Deck<SGCard> deck = (Deck<SGCard>) component;

        if (deck != null && deck.getSize() > 0){
            rects = new Rectangle[deck.getSize()];
            int i = deck.getSize()-1;
            if (firstOnTop) i = 0;
            while ( i>= 0 && !firstOnTop || i < deck.getSize() && firstOnTop){
                if (deck.get(0) instanceof SGCard){
                    int offset = (rect.width-60) / deck.getSize();
                    Rectangle r = new Rectangle(rect.x + offset * i, rect.y, 60, 85);
                    rects[i] = r;
                    drawComponent(g, r, (SGCard) deck.get(i), true);
                }
            }
        }
    }

    private Image getCardImage(SGCard card)
    {
        return ImageIO.GetInstance().getImage(dataPath + card.type + ".png");
    }
}
