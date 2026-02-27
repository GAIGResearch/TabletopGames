package games.seasaltpaper.gui;

import core.components.Deck;
import games.seasaltpaper.cards.CardColor;
import games.seasaltpaper.cards.CardType;
import games.seasaltpaper.cards.SeaSaltPaperCard;
import games.sushigo.cards.SGCard;
import gui.views.CardView;
import gui.views.DeckView;
import games.seasaltpaper.gui.SSPGUIManager;
import utilities.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;


public class SSPDeckView extends DeckView<SeaSaltPaperCard> {

    final String dataPath;
    BufferedImage backOfCard;

    public SSPDeckView(int player, Deck<SeaSaltPaperCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(player, d, visible, SSPGUIManager.cardWidth, SSPGUIManager.cardHeight, rect);
        this.dataPath = dataPath;

        // TODO make a proper card back image
        // Generate card back
        backOfCard = new BufferedImage(SSPGUIManager.cardWidth, SSPGUIManager.cardHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = backOfCard.createGraphics();
        // Set background color
        g.setColor(new Color(94, 92, 92));
        g.fillRect(0, 0, backOfCard.getWidth(), backOfCard.getHeight());

        // Draw outline
        g.setStroke(new BasicStroke(5));
        g.setColor(Color.WHITE);
        g.drawRect(0, 0, backOfCard.getWidth(), backOfCard.getHeight());

        g.dispose();

//        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, SeaSaltPaperCard card, boolean front) {
        Image cardFace = getCardImage(card, rect);
        int fontSize = g.getFont().getSize();
        CardView.drawCard(g, new Rectangle(rect.x, rect.y, rect.width, rect.height-fontSize), card, cardFace, backOfCard, front);
    }

    private Image getCardImage(SeaSaltPaperCard card, Rectangle rect)
    {
        BufferedImage cardFront = new BufferedImage(SSPGUIManager.cardWidth, SSPGUIManager.cardHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = cardFront.createGraphics();

        // Set background color
        Color cardColor = card.getCardColor().getColor();
        g.setColor(cardColor);
        g.fillRect(0, 0, rect.width, rect.height);

        // Draw suite icon
        String suiteName = card.getCardSuite().name().toLowerCase();
        BufferedImage iconSuite;

        // Change suiteIcon color to white
        // and text color to white if cardColor==BLACK
        if (card.getCardColor() == CardColor.BLACK) {
            g.setColor(Color.white);
            iconSuite = (BufferedImage) ImageIO.GetInstance().getImage(dataPath + suiteName + ".png", Color.black, Color.white);
        } else {
            iconSuite = (BufferedImage) ImageIO.GetInstance().getImage(dataPath + suiteName + ".png");
            g.setColor(Color.black);
        }
        g.drawImage(iconSuite.getScaledInstance(SSPGUIManager.cardWidth/2, SSPGUIManager.cardHeight/2, Image.SCALE_DEFAULT), SSPGUIManager.cardWidth/4, SSPGUIManager.cardHeight/4, null);

        // Draw text info
        g.setFont(new Font( "SansSerif", Font.BOLD, 12 ));

        if (card.getCardType() == CardType.COLLECTOR) {
            String collectorBonus = Arrays.toString(card.getCardSuite().getCollectorBonus());
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

        // Draw outline
        g.setStroke(new BasicStroke(5));
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, rect.width, rect.height);

        g.dispose();

        return cardFront;
    }
}
