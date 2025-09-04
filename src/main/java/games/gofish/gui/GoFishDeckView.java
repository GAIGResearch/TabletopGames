package games.gofish.gui;

import core.components.Deck;
import core.components.FrenchCard;
import gui.views.DeckView;

import java.awt.*;

public class GoFishDeckView extends DeckView<FrenchCard> {

    public GoFishDeckView(int playerID, Deck<FrenchCard> deck, boolean visible, String dataPath, Rectangle rect) {
        super(playerID, deck, visible, GoFishGUIManager.cardWidth, GoFishGUIManager.cardHeight, rect);
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, FrenchCard card, boolean front) {
        if (front) {
            drawCardFront(g, rect, card);
        } else {
            drawCardBack(g, rect);
        }
    }

    private void drawCardFront(Graphics2D g, Rectangle rect, FrenchCard card) {
        // Draw card background
        g.setColor(Color.WHITE);
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);

        // Draw border
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);

        // Set color based on suit
        Color suitColor = (card.suite == FrenchCard.Suite.Hearts || card.suite == FrenchCard.Suite.Diamonds)
                ? Color.RED : Color.BLACK;
        g.setColor(suitColor);

        // Draw rank in top-left corner
        g.setFont(new Font("Arial", Font.BOLD, 14));
        String rankStr = getRankString(card);
        g.drawString(rankStr, rect.x + 5, rect.y + 18);

        // Draw suit symbol
        String suitSymbol = getSuitSymbol(card.suite);
        g.drawString(suitSymbol, rect.x + 5, rect.y + 35);

        // Draw large centered rank and suit
        g.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics fm = g.getFontMetrics();
        String centerText = rankStr + suitSymbol;
        int centerX = rect.x + (rect.width - fm.stringWidth(centerText)) / 2;
        int centerY = rect.y + (rect.height + fm.getHeight()) / 2;
        g.drawString(centerText, centerX, centerY);
    }

    private void drawCardBack(Graphics2D g, Rectangle rect) {
        // Draw card back with blue pattern
        g.setColor(new Color(0, 50, 150));
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);

        // Draw border
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 10, 10);

        // Draw pattern
        g.setColor(new Color(100, 150, 255));
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 6; j++) {
                int x = rect.x + 10 + i * 15;
                int y = rect.y + 10 + j * 15;
                g.fillOval(x, y, 8, 8);
            }
        }
    }

    private String getRankString(FrenchCard card) {
        switch (card.type) {
            case Number:
                return String.valueOf(card.number);
            case Jack:
                return "J";
            case Queen:
                return "Q";
            case King:
                return "K";
            case Ace:
                return "A";
            default:
                return "?";
        }
    }

    private String getSuitSymbol(FrenchCard.Suite suite) {
        switch (suite) {
            case Hearts:
                return "♥";
            case Diamonds:
                return "♦";
            case Clubs:
                return "♣";
            case Spades:
                return "♠";
            default:
                return "?";
        }
    }
}