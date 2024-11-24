package games.seasaltpaper.gui;

import core.components.Deck;
import games.seasaltpaper.cards.CardType;
import games.seasaltpaper.cards.SeaSaltPaperCard;
import games.sushigo.cards.SGCard;
import gui.views.CardView;
import gui.views.DeckView;
import games.seasaltpaper.gui.SSPGUIManager;
import utilities.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;


public class SSPDeckView extends DeckView<SeaSaltPaperCard> {

    final String dataPath = "data/seasaltpaper/";
    Image backOfCard;

    public SSPDeckView(int player, Deck<SeaSaltPaperCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(player, d, visible, SSPGUIManager.cardWidth, SSPGUIManager.cardHeight, rect);

//        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, SeaSaltPaperCard card, boolean front) {
        Image cardFace = getCardImage(card);
        int fontSize = g.getFont().getSize();
        CardView.drawCard(g, new Rectangle(rect.x, rect.y, rect.width, rect.height-fontSize), card, cardFace, backOfCard, front);
        if (front) {
            g.drawString(card.toString(), rect.x + 2, (int) (rect.y + rect.height - fontSize * 1.5));
        }
    }

    private Image getCardImage(SeaSaltPaperCard card)
    {
        BufferedImage cardFront = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cardFront.createGraphics();

        // Set background color
        Color cardColor = card.getCardColor().getColor();
        g.setColor(cardColor);
        g.fillRect(0, 0, cardFront.getWidth(), cardFront.getHeight());

        // Draw suite icon
        String suiteName = card.getCardSuite().name().toLowerCase();
        Image iconSuite = ImageIO.GetInstance().getImage(dataPath + suiteName + ".png");
        g.drawImage(iconSuite.getScaledInstance(width/2, height/2, Image.SCALE_DEFAULT), width/4, width/4, null);

        // Draw text info
        g.setColor(Color.BLACK);
        g.setFont(new Font( "SansSerif", Font.BOLD, 12 ));

        if (card.getCardType() == CardType.COLLECTOR) {
            String collectorBonus = card.getCardSuite().getCollectorBonus().toString();
            g.drawString(collectorBonus, 5, 15);
        }
        if (card.getCardType() == CardType.DUO) {
            String duoBonus = String.valueOf(card.getCardSuite().getDuoBonus());
            g.drawString("DUO | " + duoBonus, 5, 15);
        }
        if (card.getCardType() == CardType.MULTIPLIER) {
            String multiplier = String.valueOf(card.getCardSuite().getMultiplier());
            g.drawString("X " + multiplier, 5, 15);
        }
        //TODO Mermaid cards

        g.dispose();

        return cardFront;
    }
}
