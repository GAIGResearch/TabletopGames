package games.lawnandorder.gui;

import games.lawnandorder.components.LawnCard;
import games.lawnandorder.components.RuleCard;
import utilities.ImageIO;

import java.awt.*;

/**
 * Draws Lawn cards from their images (cut from the print-and-play sheets), and Rule cards as notices.
 */
public class LawnCardArt {

    public static final String dataPath = "data/lawnandorder/";
    public static final int cardWidth = 60, cardHeight = 84;

    private LawnCardArt() {
    }

    public static Image image(LawnCard card) {
        return ImageIO.GetInstance().getImage(dataPath + card.type.name() + "_" + card.colour.name() + "_"
                + card.feature.name() + ".jpg");
    }

    public static Image back() {
        return ImageIO.GetInstance().getImage(dataPath + "back.jpg");
    }

    public static void drawCard(Graphics2D g, LawnCard card, Rectangle r, boolean faceUp) {
        g.drawImage(faceUp ? image(card) : back(), r.x, r.y, r.width, r.height, null);
        g.setColor(Color.darkGray);
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
    }

    /**
     * The printed colour of a Colour attribute, as on the cards.
     */
    public static Color colour(LawnCard.Attribute attribute) {
        return switch (attribute) {
            case RED -> new Color(0xEF0404);
            case YELLOW -> new Color(0xFFF34B);
            case PINK -> new Color(0xFF3DFB);
            case BLUE -> new Color(0x0324FF);
            default -> new Color(235, 235, 225);
        };
    }

    /**
     * Draws a Rule card as a small notice, in the colour it condemns if it condemns a Colour.
     */
    public static void drawRule(Graphics2D g, RuleCard rule, Rectangle r, boolean faceUp) {
        if (!faceUp) {
            g.setColor(new Color(90, 70, 50));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(Color.white);
            drawCentred(g, "H.O.A.", r);
            return;
        }
        Color fill = rule.condemned != null ? colour(rule.condemned) : new Color(255, 236, 170);
        g.setColor(fill);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(Color.darkGray);
        g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
        boolean dark = rule.condemned == LawnCard.Attribute.BLUE || rule.condemned == LawnCard.Attribute.RED;
        g.setColor(dark ? Color.white : Color.black);
        drawCentred(g, rule.toString(), r);
    }

    /**
     * The text centred in the rectangle, broken onto two lines at the last space if it is too wide.
     */
    private static void drawCentred(Graphics2D g, String text, Rectangle r) {
        FontMetrics fm = g.getFontMetrics();
        String[] lines = {text};
        if (fm.stringWidth(text) > r.width - 6 && text.lastIndexOf(' ') > 0) {
            int split = text.lastIndexOf(' ');
            lines = new String[]{text.substring(0, split), text.substring(split + 1)};
        }
        int lineHeight = fm.getAscent();
        int y = r.y + (r.height - lineHeight * lines.length) / 2 + lineHeight - 2;
        for (String line : lines) {
            g.drawString(line, r.x + (r.width - fm.stringWidth(line)) / 2, y);
            y += lineHeight + 1;
        }
    }
}
