package games.terraformingmars.gui;

import core.components.Deck;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.requirements.Requirement;
import utilities.ImageIO;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;

import static core.AbstractGUI.defaultItemSize;
import static games.terraformingmars.gui.Utils.*;

public class TMDeckDisplay2 extends JComponent {

    Deck<TMCard> deck;
    TMGameState gs;

    HashMap<Rectangle, String> rects;  // Used for highlights + action trimming
    ArrayList<Rectangle> highlight;

    Image reqMin, reqMax;
    Image pointBg;
    Image projCardBg;
    int width, height;

    static int spacing = 10;
    static int cardHeight = 200;
    static int cardWidth;

    public TMDeckDisplay2(TMGUI gui, TMGameState gs, Deck<TMCard> deck) {
        this.gs = gs;
        this.deck = deck;

        rects = new HashMap<>();
        highlight = new ArrayList<>();

        pointBg = ImageIO.GetInstance().getImage("data/terraformingmars/images/cards/card-point-bg.png");
        projCardBg = ImageIO.GetInstance().getImage("data/terraformingmars/images/cards/proj-card-bg.png");
        reqMin = ImageIO.GetInstance().getImage("data/terraformingmars/images/requisites/min_big.png");
        reqMax = ImageIO.GetInstance().getImage("data/terraformingmars/images/requisites/max_big.png");

        Vector2D dim = scaleLargestDimImg(projCardBg, cardHeight);
        cardWidth = dim.getX();
        if (deck != null) {
            height = deck.getSize() * cardHeight;
        } else {
            height = cardHeight;
        }
        width = cardWidth;

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
                    gui.updateButtons = true;
                } else {
                    // Remove highlight
                    highlight.clear();
                }
                gui.updateButtons = true;
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;

        g.setFont(TMGUI.defaultFont);

        if (deck != null) {
            for (int i = 0; i < deck.getSize(); i++) {
                if (deck.get(i) != null) {
                    int cardX = 0;
                    int cardY = i * cardHeight;
                    drawCard(g, deck.get(i), cardX, cardY, cardWidth, cardHeight);
                    rects.put(new Rectangle(cardX, cardY, cardWidth, cardHeight), ""+i);
                }
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
        drawStringCentered(g, "" + card.cost, new Rectangle(x, y, (int)(width/5.5), (int)(width/5.5)), Color.darkGray, 14);
        // Draw points
        if (card.nPoints != 0) {
            Vector2D dim = scaleLargestDimImg(pointBg, defaultItemSize);
            drawImage(g, pointBg, x + width - dim.getX() - 2, y + height - dim.getY() - 2, dim.getX(), dim.getY());
            Rectangle contentRect = new Rectangle(x + width - dim.getX() - 2, y + height - dim.getY() - 2, dim.getX(), dim.getY());
            int size = contentRect.width/3;
            int nOther = (int)(1/card.nPoints);
            if (card.pointsResource != null) {
                drawShadowStringCentered(g, "1/" + (nOther != 1? nOther : ""),
                        new Rectangle(contentRect.x, contentRect.y, contentRect.width/2, contentRect.height),
                        Color.orange, Color.black, 14);
                drawImage(g, ImageIO.GetInstance().getImage(card.pointsResource.getImagePath()),
                        contentRect.x + contentRect.width/2 + contentRect.width/4 - size/2, contentRect.y + contentRect.height/2 - size/2,
                        size, size);
            } else if (card.pointsTile != null) {
                drawShadowStringCentered(g, "1/" + (nOther != 1? nOther : ""),
                        new Rectangle(contentRect.x, contentRect.y, contentRect.width/2, contentRect.height),
                        Color.orange, Color.black, 14);
                drawImage(g, ImageIO.GetInstance().getImage(card.pointsTile.getImagePath()),
                        contentRect.x + contentRect.width/2 + contentRect.width/4 - size/2, contentRect.y + contentRect.height/2 - size/2,
                        size, size);
            } else if (card.pointsTag != null) {
                drawShadowStringCentered(g, "1/" + (nOther != 1? nOther : ""),
                        new Rectangle(contentRect.x, contentRect.y, contentRect.width/2, contentRect.height),
                        Color.orange, Color.black, 14);
                drawImage(g, ImageIO.GetInstance().getImage(card.pointsTag.getImagePath()),
                        contentRect.x + contentRect.width/2 + contentRect.width/4 - size/2, contentRect.y + contentRect.height/2 - size/2,
                        size, size);
            } else {
                drawShadowStringCentered(g, "" + (int)card.nPoints, contentRect, Color.orange);
            }
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
        // Draw requirements
        if (card.requirements.size() > 0) {
            Rectangle reqRect = new Rectangle(aboveRibbon.x + spacing/2, aboveRibbon.y + 2, aboveRibbon.width-tagsWidth - spacing, aboveRibbon.height - 4);
            boolean max = false;
            for (Requirement r: card.requirements) {
                if (r.isMax()) max = true;
            }
            if (max) {
                drawImage(g, reqMax, reqRect.x, reqRect.y, reqRect.width, reqRect.height);
            } else {
                drawImage(g, reqMin, reqRect.x, reqRect.y, reqRect.width, reqRect.height);
            }
            int sX = reqRect.x + spacing;
            int sY = reqRect.y + spacing/5;
            for (Requirement r: card.requirements) {
                String text = r.getDisplayText(gs);
                Image[] imgs = r.getDisplayImages();
                if (text != null) {
                    drawShadowString(g, text, sX, sY);
                    FontMetrics fm = g.getFontMetrics();
                    sX += fm.stringWidth(text);
                }
                if (imgs != null) {
                    for (Image img: imgs) {
                        drawImage(g, img, sX, sY, tagSize, tagSize);
                        sX += tagSize;
                    }
                }
            }
        }
        // Draw resources
        if (card.resourceOnCard != null) {
            int size = defaultItemSize;
            int yR = y + cardHeight / 2 - size / 2;
            String nRes = "On card:  " + card.nResourcesOnCard + " ";
            Image img = ImageIO.GetInstance().getImage(card.resourceOnCard.getImagePath());
            FontMetrics fm = g.getFontMetrics();
            int nResW = fm.stringWidth(nRes);
            int totW = size + nResW;
            int startX2 = x + cardWidth/2 - totW/2;
            drawShadowStringCentered(g, nRes, new Rectangle(startX2, yR, nResW, size));
            drawImage(g, img, startX2 + nResW, yR, size, size);
        }
    }

    public ArrayList<Rectangle> getHighlight() {
        return highlight;
    }

    public void update(Deck<TMCard> deck) {
        this.deck = deck;
        if (deck != null) {
            height = deck.getSize() * cardHeight;
            revalidate();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
