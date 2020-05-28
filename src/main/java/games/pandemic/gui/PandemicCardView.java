package games.pandemic.gui;

import core.components.Card;
import core.content.Property;
import core.content.PropertyColor;
import core.content.PropertyLong;
import core.content.PropertyString;
import org.davidmoten.text.utils.WordWrap;
import utilities.Hash;
import utilities.ImageIO;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;

import static games.pandemic.PandemicConstants.countryHash;
import static games.pandemic.PandemicConstants.effectHash;
import static utilities.CoreConstants.colorHash;
import static utilities.CoreConstants.nameHash;

public class PandemicCardView extends JComponent {
    private Image background, secondaryBG;
    private boolean usingSecondary;
    private Card card;
    private int width;
    private int height;

    public static int offset = 10;
    public static final int cardWidth = 110;
    public static final int cardHeight = 80;

    public PandemicCardView(Card c) {
        width = cardWidth;
        height = cardHeight;
        setCard(c);
    }

    private void setCard(Card c) {
        this.card = c;
        String tooltip = "";

        String dataPath = "data/pandemic/img/";
        if (c != null) {
            Property country = c.getProperty(countryHash);
            Property pop = c.getProperty(Hash.GetInstance().hash("population"));
            Property act = c.getProperty(Hash.GetInstance().hash("action"));
            Property effect = c.getProperty(effectHash);
            if (country != null) {
                if (pop != null) {
                    // City card
                    this.background = ImageIO.GetInstance().getImage(dataPath + "citycardbg.png");
                    tooltip = "<html>Population: " + ((PropertyLong)pop).value +
                            "<br/>Country: " + ((PropertyString)country).value + "</html>";
                } else {
                    // Infection card
                    this.background = ImageIO.GetInstance().getImage(dataPath + "infectioncardbg.png");
                }
            } else if (act != null) {
                // Player role card
                this.background = ImageIO.GetInstance().getImage(dataPath + "playercardbg.png");
                this.secondaryBG = ImageIO.GetInstance().getImage(dataPath + "playeractivebg.png");
                tooltip = "Action: " + ((PropertyString)act).value;
            } else {
                // Event card
                this.background = ImageIO.GetInstance().getImage(dataPath + "eventcardbg.png");
                tooltip = "Effect: " + ((PropertyString)effect).value;
            }
        }

        if (!tooltip.equals("")) setToolTipText(tooltip);
    }

    private static Image findBackground(Card c) {
        String dataPath = "data/pandemic/img/";
        Image background = null;
        if (c != null) {
            Property country = c.getProperty(countryHash);
            Property pop = c.getProperty(Hash.GetInstance().hash("population"));
            Property act = c.getProperty(Hash.GetInstance().hash("action"));
            if (country != null) {
                if (pop != null) {
                    // City card
                    background = ImageIO.GetInstance().getImage(dataPath + "citycardbg.png");
                } else {
                    // Infection card
                    background = ImageIO.GetInstance().getImage(dataPath + "infectionbg.png");
                }
            } else if (act != null) {
                // Player role card
                background = ImageIO.GetInstance().getImage(dataPath + "playercardbg.png");
            } else {
                // Event card
                background = ImageIO.GetInstance().getImage(dataPath + "eventcardbg.png");
            }
        }
        return background;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (usingSecondary) {
            drawCard((Graphics2D) g, card, secondaryBG, 0, 0, width, height);
        } else {
            drawCard((Graphics2D) g, card, background, 0, 0, width, height);
        }
    }

    public static void drawCard(Graphics2D g, Card card, Image background, Rectangle rect) {
        drawCard(g, card, background, rect.x, rect.y, rect.width, rect.height);
    }

    public static void drawDeckBack(Graphics2D g, String name, Image background, Rectangle rect) {
        if (background != null) {
            g.drawImage(background, rect.x, rect.y, null, null);
        } else {
            g.setColor(Color.lightGray);
            g.fillRect(rect.x, rect.y, rect.width-1, rect.height-1);
            g.setColor(Color.black);
            g.drawLine(rect.x, rect.y, rect.x+rect.width-1, rect.y+rect.height-1);
        }

        int size = g.getFont().getSize();
        if (name != null && !name.equals("")) {
            g.drawString(name, rect.x + 10, rect.y + size + 20);
        }
        g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
    }

    public static void drawCard(Graphics2D g, Card card, Image background, int x, int y, int width, int height) {
        // Draw card background
        if (background == null) {
            background = findBackground(card);
        }
        if (background != null) {
            int w = background.getWidth(null);
            int h = background.getHeight(null);
            double scaleW = width*1.0/w;
            double scaleH = height*1.0/h;
            g.drawImage(background, x, y, (int) (w*scaleW), (int) (h*scaleH), null);
        } else {
            g.setColor(Color.lightGray);
            g.fillRect(x, y, width-1, height-1);
            g.setColor(Color.black);
        }

        if (card != null) {
            Property name = card.getProperty(nameHash);
            boolean event = false;
            if (card.getProperty(effectHash) != null) {
                // Event card
                event = true;
            }
            PropertyColor col = (PropertyColor)card.getProperty(colorHash);
            if (col != null) {
                g.setColor(Utils.stringToColor(col.valueStr));
            }
            Stroke st = g.getStroke();
            g.setStroke(new BasicStroke(2));
            x += offset;
            y += offset;
            width -= offset*2;
            height -= offset*2;
            g.drawRect(x, y, width, height);
            g.setStroke(st);

            g.setColor(Color.black);

            int w = (width * 2 - offset) / g.getFont().getSize();
            String wrapped =
                    WordWrap.from(((PropertyString) name).value)
                            .maxWidth(w)
                            .insertHyphens(true) // true is the default
                            .wrap();
            String[] wraps = wrapped.split("\n");
            int size = g.getFont().getSize();

            int i = 0;
            if (event) {
                Font f = g.getFont();
                g.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
                g.drawString("~ EVENT ~", x + 10, y + 17);
                g.setFont(f);
                i = 1;
            }

            for (String s : wraps) {
                g.drawString(s, x + 10, y + i * size + 20);
                i++;
            }
        }
    }

    public void updateCard(Card c) {
        setCard(c);
    }

    public void setUsingSecondary(boolean s) {
        this.usingSecondary = s;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width + offset/2, height + offset/2);
    }

    public Card getCard() {
        return card;
    }
}
