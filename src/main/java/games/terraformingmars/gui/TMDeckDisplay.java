package games.terraformingmars.gui;

import core.components.Deck;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.PlaceholderModifyCounter;
import games.terraformingmars.components.TMCard;
import utilities.ImageIO;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static core.AbstractGUI.defaultItemSize;
import static games.terraformingmars.gui.Utils.*;

public class TMDeckDisplay extends JComponent {

    Deck<TMCard> deck;
    TMGameState gs;

    HashMap<Rectangle, String> rects;  // Used for highlights + action trimming
    ArrayList<Rectangle> highlight;

    Image background;
    Image production;

    Image pointBg;
    Image projCardBg;
    int width, height;

    static int offsetX = 10;
    static int spacing = 10;
    static int cardHeight = 200;
    static int cardWidth;

    public TMDeckDisplay(TMGameState gs, Deck<TMCard> deck) {
        this.gs = gs;
        this.deck = deck;

        rects = new HashMap<>();
        highlight = new ArrayList<>();

        pointBg = ImageIO.GetInstance().getImage("data/terraformingmars/images/cards/card-point-bg.png");
        projCardBg = ImageIO.GetInstance().getImage("data/terraformingmars/images/cards/proj-card-bg.png");
        production = ImageIO.GetInstance().getImage("data/terraformingmars/images/misc/production.png");

        Vector2D dim = scaleLargestDimImg(projCardBg, cardHeight);
        cardWidth = dim.getX();
        if (deck != null) {
            width = deck.getSize() * cardWidth + offsetX * 2;
        } else {
            width = cardWidth + offsetX * 2;
        }
        height = cardHeight + offsetX*2;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Left click, highlight cell
                    for (Rectangle r: rects.keySet()) {
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

        g.setFont(TMGUI.defaultFont);

        if (deck != null) {
            // Draw player hand
            for (int i = 0; i < deck.getSize(); i++) {
//            if (playerHand.isComponentVisible(i, gs.getCurrentPlayer())) {
                if (deck.get(i) != null) {
                    int cardX = offsetX + i * cardWidth;
                    int cardY = offsetX;
                    drawCard(g, deck.get(i), cardX, cardY, cardWidth, cardHeight);
                    rects.put(new Rectangle(cardX, cardY, cardWidth, cardHeight), ""+i);
                }
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
        Rectangle aboveRibbon = new Rectangle(x + width/5, y, width - width/5 - spacing/2, height/8);
        if (card.cardType == TMTypes.CardType.Corporation) {
            Image img = ImageIO.GetInstance().getImage(TMTypes.CardType.Corporation.getImagePath());
            drawImage(g, img, x, y, height);
            // Draw name
            Font f = g.getFont();
            g.setFont(new Font("Arial", Font.BOLD, 14));
            Rectangle titleRect =  new Rectangle(x + 2, y + height/8 - 2, width - 4, height/8);
            drawStringCentered(g, card.getComponentName(), titleRect, Color.black, 14);
            g.setFont(f);
            // Draw tags
            int tagSize = defaultItemSize/3;
            int tagsWidth = card.tags.length * tagSize;
            int startX = (int)(aboveRibbon.getX() + aboveRibbon.getWidth() - tagsWidth);
            int tagY = (int)(aboveRibbon.getY() + aboveRibbon.getHeight()/2 - tagSize/2);
            for (int i = 0; i < card.tags.length; i++) {
                TMTypes.Tag tag = card.tags[i];
                Image img2 = ImageIO.GetInstance().getImage(tag.getImagePath());
                drawImage(g, img2, startX + i*tagSize, tagY, tagSize, tagSize);
            }
            // Draw starting resources
            int k = 0;
            for (AbstractAction aa: card.effects) {
                if (aa instanceof PlaceholderModifyCounter) {
                    TMTypes.Resource res = ((PlaceholderModifyCounter) aa).resource;
                    int amount = ((PlaceholderModifyCounter) aa).change;
                    boolean prod = ((PlaceholderModifyCounter) aa).production;
                    Image resImg = ImageIO.GetInstance().getImage(res.getImagePath());

                    int size = defaultItemSize/3;
                    int xRes = width/2 - (size + defaultItemSize)/2;
                    int yRes = (int)(titleRect.getY() + titleRect.getHeight() + k * size + k * spacing / 5);
                    drawResource(g, resImg, production, prod, xRes, yRes, size, 0.6);
                    drawShadowStringCentered(g, "" + amount, new Rectangle.Double(xRes + size, yRes, defaultItemSize, size));
                    k++;
                }
            }
        } else {
            // Draw background
            drawImage(g, projCardBg, x, y, height);
            // Draw ribbon
            Image ribbon = ImageIO.GetInstance().getImage(card.cardType.getImagePath());
            Rectangle ribbonRect = drawImage(g, ribbon, x + 2, y + height/8 - 2, width - 4);
            // Draw name
            Font f = g.getFont();
            g.setFont(new Font("Arial", Font.BOLD, 14));
            drawStringCentered(g, card.getComponentName(), ribbonRect, Color.black, 14);
            g.setFont(f);
            // Draw cost
            drawStringCentered(g, "" + card.cost, new Rectangle.Double(x, y, width/5.5, width/5.5), Color.darkGray, 14);
            // Draw points
            if (card.nPoints != 0) {
                Vector2D dim = scaleLargestDimImg(pointBg, defaultItemSize);
                drawImage(g, pointBg, x + width - dim.getX() - 2, y + height - dim.getY() - 2, dim.getX(), dim.getY());
                drawShadowStringCentered(g, "" + card.nPoints,
                        new Rectangle.Double(x + width - dim.getX() - 2, y + height - dim.getY() - 2, dim.getX(), dim.getY()),
                        Color.orange);
                // Draw different for points per resource TODO
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
            // Draw requirements TODO
        }
    }

    static void drawResource(Graphics2D g, Image resImg, Image production, boolean prod, int x, int y, int size, double scaleResIfProd) {
        if (prod) {
            drawImage(g, production, x, y, size);
            int newSize = (int)(size * scaleResIfProd);
            x += size/2 - newSize/2;
            y += size/2 - newSize/2;
            size = newSize;
        }
        drawImage(g, resImg, x, y, size);
    }

    public ArrayList<Rectangle> getHighlight() {
        return highlight;
    }

    public void update(Deck<TMCard> deck) {
        this.deck = deck;
        if (deck != null) {
            width = deck.getSize() * cardWidth + offsetX * 2;
            revalidate();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
