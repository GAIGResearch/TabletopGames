package games.terraformingmars.gui;

import core.components.Deck;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;
import gui.views.ComponentView;
import utilities.ImageIO;
import utilities.Vector2D;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import static core.AbstractGUI.defaultItemSize;
import static games.terraformingmars.gui.Utils.*;

public class TMDeckDisplay extends ComponentView {

    Deck<TMCard> deck;
    TMGameState gs;

    Rectangle[] rects;  // Used for highlights + action trimming
    ArrayList<Rectangle> highlight;

    Image background;

    Image pointBg;
    Image projCardBg;

    static int fontSize = 16;
    static int offsetX = 10;
    static int spacing = 10;
    static int cardHeight = 200;
    static int cardWidth;

    public TMDeckDisplay(TMGameState gs, Deck<TMCard> deck) {
        super(gs.getBoard(), 0, 0);
        this.gs = gs;
        this.deck = deck;

        rects = new Rectangle[gs.getBoard().getWidth() * gs.getBoard().getHeight()];
        highlight = new ArrayList<>();

        pointBg = ImageIO.GetInstance().getImage("data/terraformingmars/images/cards/card-point-bg.png");
        projCardBg = ImageIO.GetInstance().getImage("data/terraformingmars/images/cards/proj-card-bg.png");


        Vector2D dim = scaleLargestDimImg(projCardBg, cardHeight);
        cardWidth = dim.getX();
        width = deck.getSize() * cardWidth + offsetX*2;
        height = cardHeight + offsetX*2;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Left click, highlight cell
                    for (Rectangle r: rects) {
                        if (r != null && r.contains(e.getPoint())) {
                            highlight.clear();
                            highlight.add(r);
                            break;
                        }
                    }
                } else {
                    // Remove highlight
                    highlight.clear();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;

        g.setFont(new Font("Prototype", Font.BOLD, fontSize));

        if (deck != null) {
            // Draw player hand
            for (int i = 0; i < deck.getSize(); i++) {
//            if (playerHand.isComponentVisible(i, gs.getCurrentPlayer())) {
                int cardX = offsetX + i * cardWidth;
                int cardY = offsetX;
                drawCard(g, deck.get(i), cardX, cardY, cardWidth, cardHeight);
//            }
            }
        }

        if (highlight.size() > 0) {
            g.setColor(Color.green);
            Stroke s = g.getStroke();
            g.setStroke(new BasicStroke(3));

            Rectangle r = highlight.get(0);
            g.drawRect(r.x, r.y, r.width, r.height);
            g.setStroke(s);
        }
    }

    private void drawCard(Graphics2D g, TMCard card, int x, int y, int width, int height) {
        if (card.cardType == TMTypes.CardType.Corporation) {
            Image img = ImageIO.GetInstance().getImage(TMTypes.CardType.Corporation.getImagePath());
            drawImage(g, img, x, y, height);
        } else {
            // Draw background
            drawImage(g, projCardBg, x, y, height);
            // Draw ribbon
            Image ribbon = ImageIO.GetInstance().getImage(card.cardType.getImagePath());
            Rectangle2D ribbonRect = drawImage(g, ribbon, x + 2, y + height/8 - 2, width - 4);
            Rectangle2D aboveRibbon = new Rectangle2D.Double(x + width/5., y, width - width/5. - spacing/2., height/8.);
            // Draw name
            Font f = g.getFont();
            g.setFont(new Font("Arial", Font.BOLD, 14));
            drawStringCentered(g, card.getComponentName(), ribbonRect, Color.black, 14);
            g.setFont(f);
            // Draw cost
            drawStringCentered(g, "" + card.cost, new Rectangle2D.Double(x, y, width/5.5, width/5.5), Color.darkGray, 14);
            // Draw points
            if (card.nPoints != 0) {
                Vector2D dim = scaleLargestDimImg(pointBg, defaultItemSize);
                drawImage(g, pointBg, x + width - dim.getX() - 2, y + height - dim.getY() - 2, dim.getX(), dim.getY());
                drawShadowStringCentered(g, "" + card.nPoints,
                        new Rectangle2D.Double(x + width - dim.getX() - 2, y + height - dim.getY() - 2, dim.getX(), dim.getY()),
                        Color.orange);
            }
            // Draw tags
            int tagSize = defaultItemSize/3;
            int tagsWidth = card.tags.length * tagSize;
            int startX = (int)(aboveRibbon.getX() + aboveRibbon.getWidth() - tagsWidth);
            int tagY = (int)(aboveRibbon.getY() + aboveRibbon.getHeight()/2 - tagSize/2);
            for (int i = 0; i < card.tags.length; i++) {
                TMTypes.Tag tag = card.tags[i];
                Image img = ImageIO.GetInstance().getImage(tag.getImagePath());
                drawImage(g, img, startX + i*tagSize, tagY, tagSize, tagSize);
            }
        }
    }

    public ArrayList<Rectangle> getHighlight() {
        return highlight;
    }

    public void update(TMGameState gs) {
        this.gs = gs;
        Deck<TMCard> update = (Deck<TMCard>) gs.getComponentById(deck.getComponentID());
        if (update != null) {
            this.deck = update;
            width = deck.getSize() * cardWidth + offsetX * 2;
        }
    }
}
