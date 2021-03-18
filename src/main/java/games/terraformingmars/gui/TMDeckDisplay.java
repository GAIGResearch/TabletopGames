package games.terraformingmars.gui;

import core.components.Deck;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.*;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.effects.Effect;
import games.terraformingmars.rules.effects.PayForActionEffect;
import games.terraformingmars.rules.effects.PlaceTileEffect;
import games.terraformingmars.rules.effects.PlayCardEffect;
import games.terraformingmars.rules.requirements.CounterRequirement;
import games.terraformingmars.rules.requirements.Requirement;
import games.terraformingmars.rules.requirements.TagRequirement;
import utilities.ImageIO;
import utilities.Vector2D;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;

import static core.AbstractGUI.defaultItemSize;
import static games.terraformingmars.gui.Utils.*;

public class TMDeckDisplay extends JComponent {

    Deck<TMCard> deck;
    TMGameState gs;

    HashMap<Rectangle, String> rects;  // Used for highlights + action trimming
    ArrayList<Rectangle> highlight;

    Image background;
    Image production;
    Image actionArrow;

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
        actionArrow = ImageIO.GetInstance().getImage("data/terraformingmars/images/misc/arrow.png");

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
            int size = defaultItemSize/3;
            int yRes = titleRect.y + titleRect.height;
            for (TMAction aa: card.immediateEffects) {
                if (aa instanceof PlaceholderModifyCounter) {
                    TMTypes.Resource res = ((PlaceholderModifyCounter) aa).resource;
                    int amount = ((PlaceholderModifyCounter) aa).change;
                    boolean prod = ((PlaceholderModifyCounter) aa).production;
                    Image resImg = ImageIO.GetInstance().getImage(res.getImagePath());

                    int xRes = x + width/2 - (size + defaultItemSize)/2;
                    drawResource(g, resImg, production, prod, xRes, yRes, size, 0.6);
                    drawShadowStringCentered(g, "" + amount, new Rectangle.Double(xRes + size, yRes, defaultItemSize, size));
                    yRes += size + spacing / 5;
                }
            }
            // Draw actions
            int yA = yRes + spacing;
            for (TMAction a: card.actions) {
                int leftNumber = -1;
                String left = null;
                String right = null;
                int rightNumber = -1;
                if (a instanceof PayForAction) {
                    PayForAction aa = (PayForAction) a;
                    TMTypes.Resource leftR = aa.resourceToPay;
                    left = leftR.getImagePath();
                    leftNumber = Math.abs(aa.costTotal);
                    boolean played = aa.played;
                    if (aa.action instanceof PlaceTile) {
                        // get the tile image
                        TMTypes.Tile t = ((PlaceTile)aa.action).tile;
                        right = t.getImagePath();
                    } else if (aa.action instanceof ResourceTransaction) {
                        // get resource image
                        TMTypes.Resource rightR = ((ResourceTransaction)aa.action).res;
                        right = rightR.getImagePath();
                    }
                }
                // Draw left + arrow + right
                int xA = x + width/2 - defaultItemSize/4 - size;
                if (leftNumber != -1) xA -= size/2;
                if (rightNumber != -1) xA -= size/2;
                if (left != null) xA -= size/2;
                if (right != null) xA -= size/2;

                yA += size + spacing / 5;
                if (leftNumber != -1) {
                    drawShadowStringCentered(g, "" + leftNumber, new Rectangle(xA, yA, size, size), Color.white, Color.black, 12);
                    xA += size;
                }
                if (left != null) {
                    Image image = ImageIO.GetInstance().getImage(left);
                    drawImage(g, image, xA, yA, size, size);
                    xA += size;
                }
                drawImage(g, actionArrow, xA + size, yA, defaultItemSize/2, size);
                xA += defaultItemSize/2 + size*2;
                if (rightNumber != -1) {
                    drawShadowStringCentered(g, "" + rightNumber, new Rectangle(xA, yA, size, size), Color.white, Color.black, 12);
                    xA += size;
                }
                if (right != null) {
                    Image image = ImageIO.GetInstance().getImage(right);
                    drawImage(g, image, xA, yA, size, size);
                }
            }
            // Draw discounts
            int yD = yA + spacing;
            for (Requirement r: card.discountEffects.keySet()) {
                if (r instanceof TagRequirement) {
                    int xD = x + width/2 - size*6/2;
                    Image from = ImageIO.GetInstance().getImage(((TagRequirement)r).tags[0].getImagePath());
                    Image to = ImageIO.GetInstance().getImage(TMTypes.Resource.MegaCredit.getImagePath());
                    int amount = card.discountEffects.get(r);
                    drawImage(g, from, xD, yD, size, size);
                    drawShadowStringCentered(g, ":", new Rectangle(xD + size, yD, size*2, size));
                    drawShadowStringCentered(g, "-" + amount, new Rectangle(xD + size*3, yD, size*2, size));
                    drawImage(g, to, xD + size * 5, yD, size, size);
                } else if (r instanceof CounterRequirement) {
                    int xD = x + width/2 - size*4/2;
                    TMTypes.GlobalParameter gp = Utils.searchEnum(TMTypes.GlobalParameter.class, ((CounterRequirement)r).counterCode);
                    String imgStr;
                    if (gp == null) {
                        // A resource or production instead
                        TMTypes.Resource res = TMTypes.Resource.valueOf(((CounterRequirement)r).counterCode.split("prod")[0]);
                        imgStr = res.getImagePath();
                    } else {
                        imgStr = gp.getImagePath();
                    }
                    Image from = ImageIO.GetInstance().getImage(imgStr);
                    drawImage(g, from, xD, yD, size);
                    drawShadowStringCentered(g, ": +/-" + card.discountEffects.get(r), new Rectangle(xD + size, yD, size*3, size));
                }
                yD += size + spacing/2;
            }
            // Draw resource mappings
            int yRM = yD + spacing;
            int xRM = x + width/2 - size*7/2;
            for (TMGameState.ResourceMapping rm: card.resourceMappings) {
                Image from = ImageIO.GetInstance().getImage(rm.from.getImagePath());
                Image to = ImageIO.GetInstance().getImage(rm.to.getImagePath());
                drawImage(g, from, xRM, yRM, size, size);
                drawImage(g, to, xRM + size * 5, yRM, size, size);
                drawShadowStringCentered(g, ": " + rm.rate, new Rectangle(xRM + size, yRM, size*4, size));
                yRM += size + spacing/2;
            }
            // Draw after-action effects
            int yEF = yRM + spacing;
            int xEF = x + width/2 - size*5/2;
            for (Effect e: card.persistingEffects) {
                int leftNumber = -1;
                int rightNumber = -1;
                Image from = null;
                Image to = null;
                if (e instanceof PayForActionEffect) {
                    from = ImageIO.GetInstance().getImage(TMTypes.Resource.MegaCredit.getImagePath());
                    leftNumber = ((PayForActionEffect) e).minCost;
                } else if (e instanceof PlaceTileEffect) {
                    if (((PlaceTileEffect) e).tile != null) {
                        from = ImageIO.GetInstance().getImage(((PlaceTileEffect) e).tile.getImagePath());
                    } else if (((PlaceTileEffect) e).resourceTypeGained != null) {
                        // several images separated by slash
                        int nRes = ((PlaceTileEffect) e).resourceTypeGained.length;
                        int sectionSize = (nRes-1) * size * 2;
                        int resX = xEF + size - sectionSize;
                        int count = 0;
                        for (TMTypes.Resource r: ((PlaceTileEffect) e).resourceTypeGained) {
                            Image imgR = ImageIO.GetInstance().getImage(r.getImagePath());
                            drawResource(g, imgR, production, false, resX, yEF, size, 0.6);
                            resX += size;
                            if (count != nRes-1) {
                                drawShadowStringCentered(g, "/", new Rectangle(resX, yEF, size, size), Color.white, Color.black, 12);
                                resX += size;
                            }
                            count++;
                        }
                    } else {
                        from = ImageIO.GetInstance().getImage(TMTypes.Tile.City.getImagePath());
                    }
                } else if (e instanceof PlayCardEffect) {
                    from = ImageIO.GetInstance().getImage(((PlayCardEffect) e).tagOnCard.getImagePath());
                }
                // "to" depends on the action applied as the effect
                TMAction action = e.effectAction;
                if (action == null) {
                    action = TMAction.parseAction(gs, e.effectEncoding).a;
                }
                if (action instanceof PlaceholderModifyCounter) {
                    Image resImg = ImageIO.GetInstance().getImage(((PlaceholderModifyCounter) action).resource.getImagePath());
                    boolean prod = ((PlaceholderModifyCounter) action).production;
                    rightNumber = ((PlaceholderModifyCounter) action).change;
                    drawResource(g, resImg, production, prod, xEF + size * 4, yEF, size, 0.6);
                }

                if (!e.mustBeCurrentPlayer) {
                    // draw red outline for the left part
                    g.setColor(new Color(234, 38, 38, 168));
                    int xRect = xEF;
                    int widthRect = size * 2;
                    if (leftNumber == -1) {
                        xRect = xEF + size;
                        widthRect = size;
                    }
                    g.fillRoundRect(xRect - 2, yEF - 2, widthRect + 4, size + 4, spacing, spacing);
                }

                if (leftNumber != -1) {
                    drawShadowStringCentered(g, "" + leftNumber, new Rectangle(xEF, yEF, size, size), Color.white, Color.black, 12);
                }
                if (from != null) {
                    drawImage(g, from, xEF + size, yEF, size, size);
                }
                drawShadowStringCentered(g, " : ", new Rectangle(xEF + size*2, yEF, size, size));
                if (rightNumber != -1) {
                    drawShadowStringCentered(g, "" + rightNumber, new Rectangle(xEF + size * 3, yEF, size, size), Color.white, Color.black, 12);
                }
                if (to != null) {
                    drawImage(g, to, xEF + size * 4, yEF, size, size);
                }
                yEF += size + spacing/2;
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
