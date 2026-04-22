package games.spades.gui;

import core.components.Deck;
import core.components.FrenchCard;
import gui.views.CardView;
import gui.views.ComponentView;
import utilities.ImageIO;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * GUI component for displaying a player's hand in Spades.
 */
public class SpadesPlayerView extends ComponentView {
    
    private final int playerId;
    private final String dataPath;
    private boolean isActivePlayer = false;
    private int highlightedCard = -1;
    private boolean isVisible = false;
    private Deck<FrenchCard> deck;
    private Image backOfCard;
    private Rectangle[] cardRects;
    
    public static final int CARD_WIDTH = 70;
    public static final int CARD_HEIGHT = 90;
    public static final int PLAYER_WIDTH = 400;
    public static final int PLAYER_HEIGHT = 120;
    
    public SpadesPlayerView(Deck<FrenchCard> deck, int playerId, String dataPath) {
        super(deck, PLAYER_WIDTH, PLAYER_HEIGHT);
        this.deck = deck;
        this.playerId = playerId;
        this.dataPath = dataPath;
        this.backOfCard = ImageIO.GetInstance().getImage(dataPath + "gray_back.png");
        
        setOpaque(false);
        
        // Add mouse listener for card selection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isVisible && cardRects != null && deck != null) {
                    for (int i = 0; i < cardRects.length && i < deck.getSize(); i++) {
                        if (cardRects[i] != null && cardRects[i].contains(e.getPoint())) {
                            setCardHighlight(i);
                            break;
                        }
                    }
                }
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw background for active player
        if (isActivePlayer) {
            g2d.setColor(new Color(255, 255, 0, 50)); // Light yellow highlight
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
        
        drawHand(g2d, new Rectangle(5, 10, getWidth() - 10, CARD_HEIGHT));
    }
    
    private void drawHand(Graphics2D g, Rectangle rect) {
        if (deck == null || deck.getSize() == 0) return;
        
        // Take a snapshot to avoid concurrent modification
        int cardCount = deck.getSize();
        if (cardCount == 0) return;
        
        cardRects = new Rectangle[cardCount];
        int spacing = Math.min(15, Math.max(5, (rect.width - CARD_WIDTH) / Math.max(1, cardCount - 1)));
        
        for (int i = 0; i < cardCount; i++) {
            // Defensive check to prevent IndexOutOfBoundsException
            if (i >= deck.getSize()) break;
            
            FrenchCard card;
            try {
                card = deck.get(i);
            } catch (IndexOutOfBoundsException e) {
                // Card was removed during painting, skip
                break;
            }
            
            if (card == null) continue;
            
            int x = rect.x + i * spacing;
            int y = rect.y;
            
            cardRects[i] = new Rectangle(x, y, CARD_WIDTH, CARD_HEIGHT);
            
            // Highlight selected card
            if (i == highlightedCard) {
                g.setColor(new Color(0, 255, 0, 150)); // Green highlight
                g.fillRoundRect(x - 2, y - 2, CARD_WIDTH + 4, CARD_HEIGHT + 4, 10, 10);
            }
            
            // Draw card
            Image cardImage = getCardImage(card);
            CardView.drawCard(g, x, y, CARD_WIDTH, CARD_HEIGHT, card, cardImage, backOfCard, isVisible);
            
            // Draw card border
            g.setColor(Color.BLACK);
            g.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 8, 8);
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
    
    public void setActivePlayer(boolean active) {
        this.isActivePlayer = active;
        repaint();
    }
    
    public void setCardHighlight(int cardIndex) {
        this.highlightedCard = cardIndex;
        repaint();
    }
    
    public void setVisible(boolean visible) {
        this.isVisible = visible;
        repaint();
    }
    
    public FrenchCard getHighlightedCard() {
        if (deck != null && highlightedCard >= 0 && highlightedCard < deck.getSize()) {
            try {
                return deck.get(highlightedCard);
            } catch (IndexOutOfBoundsException e) {
                // Card was removed, reset highlight
                highlightedCard = -1;
                return null;
            }
        }
        return null;
    }
    
    public void setDeck(Deck<FrenchCard> newDeck) {
        this.deck = newDeck;
        this.component = newDeck;
        repaint();
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PLAYER_WIDTH, PLAYER_HEIGHT);
    }
} 