package games.spades.gui;

import core.components.FrenchCard;
import gui.views.CardView;
import games.spades.SpadesGameState;
import utilities.ImageIO;
import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * GUI component for displaying the current trick being played in Spades.
 */
public class SpadesTrickView extends JPanel {
    
    private final String dataPath;
    private List<Pair<Integer, FrenchCard>> currentTrick;
    private Image backOfCard;
    private int leadPlayer = -1;
    
    public static final int CARD_WIDTH = 60;
    public static final int CARD_HEIGHT = 80;
    public static final int TRICK_AREA_WIDTH = 300;
    public static final int TRICK_AREA_HEIGHT = 200;
    
    public SpadesTrickView(String dataPath) {
        this.dataPath = dataPath;
        this.backOfCard = ImageIO.GetInstance().getImage(dataPath + "gray_back.png");
        
        setOpaque(false);
        setPreferredSize(new Dimension(TRICK_AREA_WIDTH, TRICK_AREA_HEIGHT));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw background area for trick
        g2d.setColor(new Color(0, 100, 0, 100)); // Dark green semi-transparent
        g2d.fillRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 20, 20);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 20, 20);
        
        // Draw label
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.WHITE);
        g2d.drawString("Current Trick", 20, 30);
        
        // Draw played cards
        if (currentTrick != null && !currentTrick.isEmpty()) {
            drawTrickCards(g2d);
        } else {
            // Draw placeholder text
            g2d.setFont(new Font("Arial", Font.ITALIC, 12));
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawString("No cards played yet", 60, getHeight() / 2);
        }
    }
    
    private void drawTrickCards(Graphics2D g) {
        if (currentTrick == null || currentTrick.isEmpty()) return;
        
        // Take a snapshot to avoid concurrent modification
        int cardCount = currentTrick.size();
        if (cardCount == 0) return;
        
        // Position cards in a cross pattern (North, East, South, West)
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int offset = 40;
        
        for (int i = 0; i < cardCount; i++) {
            // Defensive check to prevent IndexOutOfBoundsException
            if (i >= currentTrick.size()) break;
            
            Pair<Integer, FrenchCard> entry;
            try {
                entry = currentTrick.get(i);
            } catch (IndexOutOfBoundsException e) {
                // Trick was modified during painting, skip
                break;
            }
            
            if (entry == null) continue;
            
            int playerId = entry.a;
            FrenchCard card = entry.b;
            
            if (card == null) continue;
            
            // Calculate position based on player ID
            int x, y;
            switch (playerId) {
                case 0: // Bottom (South)
                    x = centerX - CARD_WIDTH / 2;
                    y = centerY + offset;
                    break;
                case 1: // Left (West)
                    x = centerX - offset - CARD_WIDTH;
                    y = centerY - CARD_HEIGHT / 2;
                    break;
                case 2: // Top (North)
                    x = centerX - CARD_WIDTH / 2;
                    y = centerY - offset - CARD_HEIGHT;
                    break;
                case 3: // Right (East)
                    x = centerX + offset;
                    y = centerY - CARD_HEIGHT / 2;
                    break;
                default:
                    x = centerX - CARD_WIDTH / 2;
                    y = centerY - CARD_HEIGHT / 2;
                    break;
            }
            
            // Highlight lead card
            if (playerId == leadPlayer) {
                g.setColor(new Color(255, 255, 0, 150)); // Yellow highlight
                g.fillRoundRect(x - 3, y - 3, CARD_WIDTH + 6, CARD_HEIGHT + 6, 10, 10);
            }
            
            // Draw the card
            Image cardImage = getCardImage(card);
            CardView.drawCard(g, x, y, CARD_WIDTH, CARD_HEIGHT, card, cardImage, backOfCard, true);
            
            // Draw card border
            g.setColor(Color.BLACK);
            g.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 6, 6);
            
            // Draw player ID
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString("P" + playerId, x + 2, y - 2);
        }
    }
    
    private Image getCardImage(FrenchCard card) {
        String imageName;
        if (card.type == FrenchCard.FrenchCardType.Number) {
            imageName = card.number + card.suite.name() + ".png";
        } else {
            imageName = card.type.name() + card.suite.name() + ".png";
        }
        return ImageIO.GetInstance().getImage(dataPath + imageName);
    }
    
    public void updateTrick(SpadesGameState gameState) {
        if (gameState != null) {
            try {
                this.currentTrick = gameState.getCurrentTrick();
                this.leadPlayer = gameState.getLeadPlayer();
            } catch (Exception e) {
                // If there's an error, clear the trick
                this.currentTrick = null;
                this.leadPlayer = -1;
            }
        } else {
            this.currentTrick = null;
            this.leadPlayer = -1;
        }
        repaint();
    }
    
    public void clearTrick() {
        this.currentTrick = null;
        this.leadPlayer = -1;
        repaint();
    }
} 