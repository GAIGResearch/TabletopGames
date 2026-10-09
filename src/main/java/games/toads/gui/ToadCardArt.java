package games.toads.gui;

import games.toads.components.ToadCard;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Draws War of the Toads cards: the name, the Strength, and the text of the Rulebook 3 Special Attribute and Tactic.
 */
public class ToadCardArt {

    public static final int cardWidth = 96, cardHeight = 132;

    static final Color back = new Color(94, 60, 120);
    static final Color outline = new Color(40, 30, 20);

    // the printed text of the Rulebook 3 cards, by ability class; legacy abilities show no text
    static final Map<String, String> specialAttribute = Map.of(
            "AssassinII", "Wins against General",
            "SaboteurIII", "Wins against Siege Cannon",
            "GeneralHostages", "Loses against Assassin",
            "GeneralFlags", "Loses against Assassin",
            "SiegeCannon", "Wins in Attack, except v Saboteur. Loses in Defence");
    static final Map<String, String> tactic = Map.of(
            "AssassinII", "+2.5 to your Ally or their Foe, whoever is lower",
            "Scout", "+1 to your Ally. Opponent shows you 3 cards",
            "SaboteurIII", "Your Ally breaks ties",
            "TricksterII", "Switch lanes with your Ally. Cannot be blocked",
            "BerserkerII", "You become Angry",
            "Bodyguard", "Block your opponent's Tactic",
            "GeneralHostages", "+1 to your Ally per Hostage your opponent has",
            "GeneralFlags", "+1 to your Ally per Flag you have",
            "SiegeCannon", "Guess a card in your opponent's hand");

    private ToadCardArt() {
    }

    /**
     * Draws a card face-up, or its back when faceUp is false. A dimmed card is drawn half transparent.
     */
    public static void draw(Graphics2D g, ToadCard card, Rectangle r, boolean faceUp, boolean dimmed) {
        Composite oldComposite = g.getComposite();
        if (dimmed)
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        if (!faceUp) {
            g.setColor(back);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
            g.setColor(new Color(255, 255, 255, 70));
            g.drawRoundRect(r.x + 6, r.y + 6, r.width - 12, r.height - 12, 8, 8);
            g.setColor(outline);
            g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 12, 12);
        } else {
            g.setColor(faceColour(card.value));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
            g.setColor(outline);
            g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 12, 12);

            // the Strength in a circle at the top left, and the name beside it
            g.setColor(Color.white);
            g.fillOval(r.x + 5, r.y + 5, 26, 26);
            g.setColor(outline);
            g.drawOval(r.x + 5, r.y + 5, 26, 26);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 15f));
            String strength = String.valueOf(card.value);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(strength, r.x + 18 - fm.stringWidth(strength) / 2, r.y + 24);

            g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
            int y = drawWrapped(g, card.getComponentName(), r.x + 35, r.y + 16, r.width - 38, 12, 2);

            String ability = card.tactics == null ? "" : card.tactics.getClass().getSimpleName();
            g.setFont(g.getFont().deriveFont(Font.ITALIC, 9f));
            y = Math.max(y, r.y + 40);
            if (specialAttribute.containsKey(ability))
                y = drawWrapped(g, specialAttribute.get(ability), r.x + 6, y + 4, r.width - 12, 10, 3);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 9f));
            if (tactic.containsKey(ability)) {
                g.setColor(new Color(255, 255, 255, 150));
                int top = r.y + r.height - 56;
                g.fillRoundRect(r.x + 4, top, r.width - 8, 52, 8, 8);
                g.setColor(outline);
                drawWrapped(g, tactic.get(ability), r.x + 8, top + 11, r.width - 16, 10, 5);
            }
        }
        g.setComposite(oldComposite);
    }

    /**
     * An empty outline where a card could be.
     */
    public static void drawSlot(Graphics2D g, Rectangle r) {
        g.setColor(new Color(255, 255, 255, 90));
        g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 12, 12);
    }

    // a colour for each Strength, from cool (low) to warm (high); the Siege Cannon (0) is grey
    private static Color faceColour(int value) {
        if (value <= 0)
            return new Color(190, 190, 185);
        float hue = 0.55f - 0.08f * Math.min(value, 7);
        return Color.getHSBColor(hue, 0.35f, 0.97f);
    }

    /**
     * Draws the text word-wrapped to the width, at most maxLines lines, and returns the y below the last line.
     */
    private static int drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, int maxLines) {
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty())
            lines.add(line.toString());
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++)
            g.drawString(lines.get(i), x, y + i * lineHeight);
        return y + Math.min(lines.size(), maxLines) * lineHeight;
    }
}
