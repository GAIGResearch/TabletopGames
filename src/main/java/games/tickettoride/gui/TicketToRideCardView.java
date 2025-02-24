package games.tickettoride.gui;

import core.components.Card;
import gui.views.CardView;
import core.properties.Property;
import core.properties.PropertyColor;
import core.properties.PropertyLong;
import core.properties.PropertyString;
import org.davidmoten.text.utils.WordWrap;
import utilities.Hash;
import utilities.ImageIO;
import utilities.Utils;


import java.awt.*;


import static core.CoreConstants.colorHash;
import static core.CoreConstants.nameHash;
import static games.tickettoride.TicketToRideConstants.*;

public class TicketToRideCardView extends CardView {
    private Image background, secondaryBG;
    private boolean usingSecondary;

    public static int offset = 10;
    public static final int cardWidth = 160;
    public static final int cardHeight = 90;



    public TicketToRideCardView(Card c) {
        super(c);
        width = cardWidth;
        height = cardHeight;
        setCard(c);
    }

    private void setCard(Card c) {
        this.component = c;
        String tooltip = "";

        String dataPath = "data/tickettoride/img/";
        if (c != null) {
            Property location1Test = c.getProperty(location1Hash);
            if (location1Test != null) { //train car card
                this.background = ImageIO.GetInstance().getImage(dataPath + "destinationCardBg.png");
                String location1 = String.valueOf(c.getProperty(location1Hash));
                String location2 = String.valueOf(c.getProperty(location2Hash));
                tooltip = location1 + " to " + location2;
            }
        }

        if (!tooltip.equals("")) {
            setToolTipText(tooltip);
        }
    }

    private static Image findBackground(Card c) {
        String dataPath = "data/tickettoride/img/";
        Image background = null;
        if (c != null) {
            Property location1Test = c.getProperty(location1Hash);
            if (location1Test != null) { //train car card
                background = ImageIO.GetInstance().getImage(dataPath + "destinationCardBg.png");
            }

        }
        return background;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (usingSecondary) {
            drawCard((Graphics2D) g, (Card) component, secondaryBG, 0, 0, width, height);
        } else {
            drawCard((Graphics2D) g, (Card) component, background, 0, 0, width, height);
        }
    }

    public static void drawCard(Graphics2D g, Card card, Image background, Rectangle rect) {
        drawCard(g, card, background, rect.x, rect.y, rect.width, rect.height);
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
        }
//        else {
//            if (card == null || card.getProperty(Hash.GetInstance().hash("action")) == null) {
//                g.setColor(Color.lightGray);
//                g.fillRect(x, y, width - 1, height - 1);
//                g.setColor(Color.black);
//            }
//        }

        if (card != null) {

            String location1 = String.valueOf(card.getProperty(location1Hash));
            String location2 = String.valueOf(card.getProperty(location2Hash));
            String points = String.valueOf(card.getProperty(pointsHash));

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
                    WordWrap.from(location1 + " to " + location2 )
                            .maxWidth(w)
                            .insertHyphens(true) // true is the default
                            .wrap();
            String[] wraps = wrapped.split("\n");


            int size = g.getFont().getSize();

            int i = 0;
            Font f = g.getFont();
            g.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
            g.drawString("Points: " +  points, x + 10, y + 17);
            g.setFont(f);
            i = 1;

            Font fontForDescription = g.getFont();
            g.setFont(new Font(fontForDescription.getName(), Font.PLAIN , 10));
            for (String s : wraps) {
                g.drawString(s, x + 10, y + i * size + 20);
                i++;
            }


        }
    }

    @Override
    public void updateComponent(core.components.Component c) {
        setCard((Card) c);
    }

    public void setUsingSecondary(boolean s) {
        this.usingSecondary = s;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width + offset/2, height + offset/2);
    }
}
