package games.explodingkittens.gui;

import core.components.Deck;
import games.explodingkittens.cards.ExplodingKittensCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static games.explodingkittens.gui.ExplodingKittensGUIManager.*;


public class ExplodingKittensDeckView extends DeckView<ExplodingKittensCard> {

    // Back of card image
    Image backOfCard;
    // Path to assets
    String dataPath;

    // Card images
    HashMap<Integer, Image> cardCatImageMapping;
    ArrayList<String> catImages;
    Random rnd = new Random();  // This doesn't need to use the game random seed

    /**
     * Constructor initialising information and adding key/mouse listener for card highlight (left click or ALT + hover
     * allows showing the highlighted card on top of all others).
     * @param d - deck to draw
     * @param visible - true if whole deck visible
     * @param dataPath - path to assets
     */
    public ExplodingKittensDeckView(int player, Deck<ExplodingKittensCard> d, boolean visible, String dataPath) {
        super(player, d, visible, ekCardWidth, ekCardHeight, new Rectangle(5, 5, playerAreaWidth, playerAreaHeight));
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
        this.dataPath = dataPath;
        cardCatImageMapping = new HashMap<>();

        // Get card Images
        File dir = new File(dataPath + "cats/");
        File[] files = dir.listFiles();
        catImages = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                catImages.add(f.getAbsolutePath());
            }
        }
    }

    /**
     * Draws the specified component at the specified place
     *
     * @param g         Graphics object
     * @param rect      Where the item is to be drawn
     * @param card The item itself
     * @param visible     true if the item is visible (e.g. the card details); false if only the card-back
     */
    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, ExplodingKittensCard card, boolean visible) {
        drawCat(g, card, rect, visible);
    }

    /**
     * Draws an Exploding Kittens card, with a random cat icon.
     * @param g - Graphics object
     * @param card - card to draw on
     * @param r - rectangle in which card is to be drawn
     * @param visible - if the card is visible or not
     */
    private void drawCat(Graphics2D g, ExplodingKittensCard card, Rectangle r, boolean visible) {
        Image cardFace = ImageIO.GetInstance().getImage(dataPath + card.cardType.name().toLowerCase() + ".png");
        CardView.drawCard(g, r, card, cardFace, backOfCard, visible);

        if (visible) {
            // Draw decorative cat image if card is visible
            Image catImg;
            if (cardCatImageMapping.containsKey(card.getComponentID())) {
                catImg = cardCatImageMapping.get(card.getComponentID());
            } else {
                // Random selection from those available
                int choice = rnd.nextInt(catImages.size());
                catImg = ImageIO.GetInstance().getImage(catImages.get(choice));
                catImages.remove(choice);
                cardCatImageMapping.put(card.getComponentID(), catImg);
            }
            double scaleW = 1.0 * defaultItemSize / catImg.getWidth(null);
            int height = (int) (catImg.getHeight(null) * scaleW);
            g.drawImage(catImg, r.x + ekCardWidth / 2 - defaultItemSize / 2, r.y + ekCardHeight / 2, defaultItemSize, height, null);
        }
    }

}
