package games.conquest.gui;

import core.components.Deck;
import games.conquest.components.Command;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

public class CQCommandView extends DeckView<Command> {
    String dataPath; // Path to assets
    Image unknownCommand; // image of an unknown command
    private static final Color GREEN = new Color(58, 216, 66);
    Command highlight;

    /**
     * Constructor initialising information and adding key/mouse listener for card highlight (left click or ALT + hover
     * allows showing the highlighted card on top of all others).
     * @param human - the id of the human player, who is not supposed to see the other deck
     * @param d - deck to draw
     * @param visible - true if whole deck visible
     * @param rect - the location of the Deck
     */
    public CQCommandView(int human, Deck<Command> d, boolean visible, String path, Rectangle rect) {
        super(human, d, visible, rect.width, rect.height, rect);
        // increase dimensions to allow the highlighted command to get a full outline
        this.rect.x += 2;
        this.rect.y += 2;
        this.rect.width += 4;
        this.rect.height += 4;
        dataPath = path;
        unknownCommand = ImageIO.GetInstance().getImage(dataPath + "Stoicism.png");
    }
    public CQCommandView(int human, Deck<Command> d, boolean visible, String path) {
        this(human, d, visible, path, new Rectangle(0, 0, CQGUIManager.commandWidth, CQGUIManager.commandHeight));
    }

    public Command getHighlight() {
        return highlight;
    }

    /**
     * Draws the specified component at the specified place
     *
     * @param g         Graphics object
     * @param rect      Where the item is to be drawn
     * @param cmd       The item itself
     * @param front     true if the item is visible (e.g. the card details); false if only the card-back
     */
    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, Command cmd, boolean front) {
        Image cardFace = ImageIO.GetInstance().getImage(dataPath + cmd.getCommandType().name + ".png");
        if (cardHighlight >= 0 && rects[cardHighlight] == rect) {
            // current card is highlighted
            highlight = cmd;
            Stroke s = g.getStroke();
            g.setStroke(new BasicStroke(4));
            g.setColor(GREEN);
            g.drawRect(rect.x, rect.y, rect.width, rect.height);
            g.setStroke(s);
        } else if (cardHighlight < 0) {
            highlight = null;
        }
        CardView.drawCard(g, rect, cmd, cardFace, unknownCommand, front);
    }
}
