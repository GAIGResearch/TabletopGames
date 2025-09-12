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
    private static final int CARD_WIDTH = 100;
    private static final int CARD_HEIGHT = 100;
    private static final int GAP = 5; 
    
    public PowerGridDeckView(int player, Deck<PowerGridCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(player, d, visible, CARD_WIDTH, CARD_HEIGHT, rect);
        this.dataPath = dataPath.endsWith("/") ? dataPath : dataPath + "/";
        this.front = visible;
        this.backOfCard = ImageIO.GetInstance().getImage(this.dataPath + "back.png");
        this.minCardOffset = CARD_WIDTH + GAP;
        
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, PowerGridCard card, boolean front) {
        Image face = getCardImage(card);
        CardView.drawCard(g, rect, card, face, backOfCard, front);
    }

    private Image getCardImage(PowerGridCard card) {
        // STEP3 uses image card_0.png; plants use their plant number.
        int n = (card.type == PowerGridCard.Type.STEP3) ? 0 : card.number;
        String fileName = "card_" + n + ".png";
        return ImageIO.GetInstance().getImage(dataPath + fileName);
    }
    

    
}
