package games.powergrid.gui;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import core.components.Deck;
import games.powergrid.components.PowerGridCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

@SuppressWarnings("serial")
public class PowerGridDeckView extends DeckView<PowerGridCard> {
    private final String dataPath;
    private final Image backOfCard;
    private final Image hammerIcon; 
    private final Image discountIcon; 
    private Integer auctionPlantNumber = null; 
    private Integer discountPlantNumber = null;
    private static final int CARD_WIDTH = 100;
    private static final int CARD_HEIGHT = 100;
    private static final int GAP = 5; 
    
    public PowerGridDeckView(int player, Deck<PowerGridCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(player, d, visible, CARD_WIDTH, CARD_HEIGHT, rect);
        this.dataPath = dataPath.endsWith("/") ? dataPath : dataPath + "/";
        this.front = visible;
        this.backOfCard = ImageIO.GetInstance().getImage(this.dataPath + "back.png");
        this.hammerIcon = ImageIO.GetInstance().getImage(this.dataPath + "auction_hammer.png");
        this.discountIcon = ImageIO.GetInstance().getImage(this.dataPath + "1_bid.png");
        this.minCardOffset = CARD_WIDTH + GAP;
        
    }
    public void setAuctionPlantNumber(Integer plantNumber) {
        this.auctionPlantNumber = plantNumber; 
        repaint();
    }
    
    public void setDiscountPlantNumber(Integer plantNumber) {
        this.discountPlantNumber = plantNumber;
        repaint();
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, PowerGridCard card, boolean front) {
        Image face = getCardImage(card);
        CardView.drawCard(g, rect, card, face, backOfCard, front);

        if (front && auctionPlantNumber != null && isAuctionTarget(card)) {
            final int pad = 6;
            final int hammerW = 60;
            final int hammerH = 60;
            int x = rect.x + rect.width - hammerW - pad;
            int y = rect.y + pad;
            g.drawImage(hammerIcon, x, y, hammerW, hammerH, null);
        }
        if (discountPlantNumber != null && card.number == discountPlantNumber.intValue()) {
            final int pad = -2;
            final int w = 50, h = 50;
            int x = rect.x + pad;
            int y = rect.y + rect.height - h - pad - 22;
            g.drawImage(discountIcon, x, y, w, h, null);
        }
    }

    private Image getCardImage(PowerGridCard card) {
        // STEP3 uses image card_0.png; plants use their plant number.
        int n = (card.type == PowerGridCard.Type.STEP3) ? 2 : card.number;
        String fileName = "card_" + n + ".png";
        return ImageIO.GetInstance().getImage(dataPath + fileName);
    }
    
    private boolean isAuctionTarget(PowerGridCard card) {
        return card != null && auctionPlantNumber != null && card.number == auctionPlantNumber.intValue();
    }
    
}
