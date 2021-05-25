package games.terraformingmars.gui;

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
import games.terraformingmars.rules.requirements.TagsPlayedRequirement;
import utilities.ImageIO;
import utilities.Vector2D;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import static core.AbstractGUI.defaultItemSize;
import static games.terraformingmars.gui.Utils.*;

public class TMCardView extends JComponent {
    Image production;
    Image actionArrow;
    Image reqMin, reqMax;

    Image pointBg;
    Image projCardBg;

    Color anyPlayerColor = new Color(234, 38, 38, 168);

    int index, width, height;
    TMCard card;
    TMGameState gs;

    TMCard updateCard;
    TMGameState updateGS;
    int updateIndex;
    boolean update;

    static int spacing = 10;
    Rectangle aboveRibbon;

    boolean clicked;
    TMCardView[] views;
    TMGUI gui;

    public TMCardView(TMGameState gs, TMCard card, int index, int width, int height) {
        this.gs = gs;
        this.card = card;
        this.index = index;
        this.width = width;
        this.height = height;

        pointBg = ImageIO.GetInstance().getImage("data/terraformingmars/images/cards/card-point-bg.png");
        projCardBg = ImageIO.GetInstance().getImage("data/terraformingmars/images/cards/proj-card-bg.png");
        production = ImageIO.GetInstance().getImage("data/terraformingmars/images/misc/production.png");
        actionArrow = ImageIO.GetInstance().getImage("data/terraformingmars/images/misc/arrow.png");
        reqMin = ImageIO.GetInstance().getImage("data/terraformingmars/images/requisites/min_big.png");
        reqMax = ImageIO.GetInstance().getImage("data/terraformingmars/images/requisites/max_big.png");

        aboveRibbon = new Rectangle(width/5, 0, width - width/5 - spacing/2, height/8);

        if (card != null) {
            setToolTipText(card.annotation);
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                for (TMCardView view : views) {
                    view.clicked = false;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    clicked = true;
                }
                gui.updateButtons = true;
            }
        });
    }

    void informGUI (TMGUI gui) {
        this.gui = gui;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                gui.stateChange = true;
            }
        });
    }
    void informOtherViews(TMCardView[] views) {
        this.views = views;
    }

    @Override
    protected void paintComponent(Graphics g1) {
        if (update) {
            card = updateCard;
            index = updateIndex;
            gs = updateGS;
            update = false;
            if (card != null) {
                setToolTipText(card.annotation);
            } else {
                setToolTipText(null);
            }
        }

        Graphics2D g = (Graphics2D) g1;
        if (clicked) {
            g.setStroke(new BasicStroke(3));
            setBorder(new SoftBevelBorder(BevelBorder.RAISED, Color.lightGray, Color.green, Color.lightGray, Color.darkGray));
        } else {
            setBorder(new EmptyBorder(0, 0, 0, 0));
        }

        if (card == null) return;

        g.setFont(TMGUI.defaultFont);

        if (card.cardType == TMTypes.CardType.Corporation) {
            drawCorporationCard(g);
        } else {
            drawProjectCard(g);
        }
    }

    private void drawProjectCard(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics();

        // Draw background
        drawImage(g, projCardBg, 0, 0, height);
        // Draw ribbon
        Image ribbon = ImageIO.GetInstance().getImage(card.cardType.getImagePath());
        Rectangle ribbonRect = drawImage(g, ribbon, 2, height/8 - 2, width - 4);
        // Draw name
        Font f = g.getFont();
        g.setFont(new Font("Arial", Font.BOLD, 14));
        drawStringCentered(g, card.getComponentName(), ribbonRect, Color.black, 14);
        g.setFont(f);
        // Draw cost
        drawStringCentered(g, "" + card.cost, new Rectangle(0, 0, (int)(width/5.5), (int)(width/5.5)), Color.darkGray, 14);
        // Draw points
        if (card.nPoints != 0) {
            Vector2D dim = scaleLargestDimImg(pointBg, defaultItemSize);
            drawImage(g, pointBg, width - dim.getX() - 2, height - dim.getY() - 2, dim.getX(), dim.getY());
            Rectangle contentRect = new Rectangle(width - dim.getX() - 2, height - dim.getY() - 2, dim.getX(), dim.getY());
            int size = contentRect.width/3;
            int nOther = (int)(1/card.nPoints);
            if (card.pointsResource != null) {
                drawShadowStringCentered(g, "1/" + (nOther > 1? nOther : ""),
                        new Rectangle(contentRect.x, contentRect.y, contentRect.width/2, contentRect.height),
                        Color.orange, Color.black, 14);
                drawImage(g, ImageIO.GetInstance().getImage(card.pointsResource.getImagePath()),
                        contentRect.x + contentRect.width/2 + contentRect.width/4 - size/2, contentRect.y + contentRect.height/2 - size/2,
                        size, size);
            } else if (card.pointsTile != null) {
                drawShadowStringCentered(g, "1/" + (nOther > 1? nOther : ""),
                        new Rectangle(contentRect.x, contentRect.y, contentRect.width/2, contentRect.height),
                        Color.orange, Color.black, 14);
                drawImage(g, ImageIO.GetInstance().getImage(card.pointsTile.getImagePath()),
                        contentRect.x + contentRect.width/2 + contentRect.width/4 - size/2, contentRect.y + contentRect.height/2 - size/2,
                        size, size);
            } else if (card.pointsTag != null) {
                drawShadowStringCentered(g, "1/" + (nOther > 1? nOther : ""),
                        new Rectangle(contentRect.x, contentRect.y, contentRect.width/2, contentRect.height),
                        Color.orange, Color.black, 14);
                drawImage(g, ImageIO.GetInstance().getImage(card.pointsTag.getImagePath()),
                        contentRect.x + contentRect.width/2 + contentRect.width/4 - size/2, contentRect.y + contentRect.height/2 - size/2,
                        size, size);
            } else {
                String text;
                if ((card.nPoints == Math.floor(card.nPoints)) && !Double.isInfinite(card.nPoints)) {
                    text = "" + (int) card.nPoints;
                } else {
                    text = "" + card.nPoints;
                }
                drawShadowStringCentered(g, text, contentRect, Color.orange);
            }
        }
        int size = defaultItemSize/3;

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
                    drawShadowStringCentered(g, text, new Rectangle(sX, sY, fm.stringWidth(text), tagSize));
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
        // Draw resources on card
        int yRC = ribbonRect.y + ribbonRect.height + spacing;
        if (card.resourceOnCard != null && card.nResourcesOnCard > 0) {
            int xRC = width/2 - size*3/2;
            drawImage(g, ImageIO.GetInstance().getImage(card.resourceOnCard.getImagePath()), xRC, yRC, size, size);
            drawShadowStringCentered(g, " : ", new Rectangle(xRC + size, yRC, size, size));
            drawShadowStringCentered(g, "" + card.nResourcesOnCard, new Rectangle(xRC + size*2, yRC, size, size));
            yRC += size;
        }
        // Draw actions
        int yA = yRC + spacing;
        for (TMAction a: card.actions) {
            int xA = width/2;
            yA += size + spacing / 5;
            drawAction(g, a, xA, yA, size);
        }
        // Draw discounts
        int yD = yA + spacing;
        for (Requirement r: card.discountEffects.keySet()) {
            if (r instanceof TagsPlayedRequirement) {
                int xD = width/2 - size*6/2;
                Image from = ImageIO.GetInstance().getImage(((TagsPlayedRequirement)r).tags[0].getImagePath());
                Image to = ImageIO.GetInstance().getImage(TMTypes.Resource.MegaCredit.getImagePath());
                int amount = card.discountEffects.get(r);
                drawImage(g, from, xD, yD, size, size);
                drawShadowStringCentered(g, ":", new Rectangle(xD + size, yD, size*2, size));
                drawShadowStringCentered(g, "-" + amount, new Rectangle(xD + size*3, yD, size*2, size));
                drawImage(g, to, xD + size * 5, yD, size, size);
            } else if (r instanceof CounterRequirement) {
                int xD = width/2 - size*4/2;
                TMTypes.GlobalParameter gp = utilities.Utils.searchEnum(TMTypes.GlobalParameter.class, ((CounterRequirement)r).counterCode);
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
            // todo tag on card req
            // todo ActionTypeRequirement (standard project)
            yD += size + spacing/2;
        }
        // Draw resource mappings
        int yRM = yD + spacing;
        int xRM = width/2 - size*7/2;
        for (TMGameState.ResourceMapping rm: card.resourceMappings) {
            Image from = ImageIO.GetInstance().getImage(rm.from.getImagePath());
            Image to = ImageIO.GetInstance().getImage(rm.to.getImagePath());
            drawImage(g, from, xRM, yRM, size, size);
            drawImage(g, to, xRM + size * 5, yRM, size, size);

            String text;
            if ((rm.rate == Math.floor(rm.rate)) && !Double.isInfinite(rm.rate)) {
                text = ": " + (int) rm.rate;
            } else {
                text = ": " + rm.rate;
            }
            drawShadowStringCentered(g, text, new Rectangle(xRM + size, yRM, size*4, size));
            yRM += size + spacing/2;
            // todo tag req
        }
        // Draw after-action effects
        int yEF = yRM + spacing;
        int xEF = width/2 - size*5/2;
        for (Effect e: card.persistingEffects) {
            int leftNumber = -1;
            Image[] from = null;
            if (e instanceof PayForActionEffect) {
                from = new Image[]{ImageIO.GetInstance().getImage(TMTypes.Resource.MegaCredit.getImagePath())};
                leftNumber = ((PayForActionEffect) e).minCost;
            } else if (e instanceof PlaceTileEffect) {
                if (((PlaceTileEffect) e).tile != null) {
                    from = new Image[]{ImageIO.GetInstance().getImage(((PlaceTileEffect) e).tile.getImagePath())};
                } else if (((PlaceTileEffect) e).resourceTypeGained != null) {
                    // several images separated by slash
                    int nRes = ((PlaceTileEffect) e).resourceTypeGained.length;
                    from = new Image[nRes];
                    int i = 0;
                    for (TMTypes.Resource r: ((PlaceTileEffect) e).resourceTypeGained) {
                        Image imgR = ImageIO.GetInstance().getImage(r.getImagePath());
                        from[i] = imgR;
                        i++;
                    }
                } else {
                    from = new Image[]{ImageIO.GetInstance().getImage(TMTypes.Tile.City.getImagePath())};
                }
            } else if (e instanceof PlayCardEffect) {
                // several images separated by slash
                HashSet<TMTypes.Tag> tags = ((PlayCardEffect) e).tagsOnCard;
                from = new Image[tags.size()];
                int i = 0;
                for (TMTypes.Tag t: tags) {
                    from[i] = ImageIO.GetInstance().getImage(t.getImagePath());
                    i++;
                }
            }

            if (!e.mustBeCurrentPlayer) {
                // draw red outline for the left part
                g.setColor(anyPlayerColor);
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
                int nImgs = from.length;
                int sectionSize = Math.max((nImgs-1) * size * 2, size);
                int resX = xEF + size - sectionSize;
                for (int i = 0; i < nImgs; i++) {
                    Image imgR = from[i];
                    drawImage(g, imgR, resX, yEF, size, size);
                    resX += size;
                    if (i != nImgs-1) {
                        drawShadowStringCentered(g, "/", new Rectangle(resX, yEF, size, size), Color.white, Color.black, 12);
                        resX += size;
                    }
                }
            }
            drawShadowStringCentered(g, " : ", new Rectangle(xEF + size*2, yEF, size, size));

            // "to" depends on the action applied as the effect
            TMAction action = e.effectAction;
            drawCardEffect(g, action, xEF + size * 4, yEF, size, -1);

            yEF += size + spacing/2;
        }
        // Draw card effects
        int yE = yEF + spacing;
        if (card.immediateEffects.length > 0) {
            int xE = width/2;
            for (TMAction a: card.immediateEffects) {
                yE = drawCardEffect(g, a, xE, yE, ribbonRect.width, -1);
            }
        }
    }

    private void drawCorporationCard(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics();

        Image img = ImageIO.GetInstance().getImage(TMTypes.CardType.Corporation.getImagePath());
        drawImage(g, img, 0, 0, height);
        // Draw name
        Font f = g.getFont();
        g.setFont(new Font("Arial", Font.BOLD, 14));
        Rectangle titleRect =  new Rectangle(2, height/8 - 2, width - 4, height/8);
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
            if (aa instanceof ModifyPlayerResource) {
                int xRes = width/2;
                drawModifyPlayerResourceAction(g, (ModifyPlayerResource) aa, xRes, yRes, size);
                yRes += size + spacing / 5;
            }
        }
        // Draw first action
        int yA = yRes + spacing;
        int xA = width / 2;
        if (card.firstAction != null) {
            drawAction(g, card.firstAction, xA, yA, size);
            drawShadowStringCentered(g, "*", new Rectangle(5, yA, size, size));
            yA += size + spacing / 5;
        }
        // Draw active actions
        for (TMAction a: card.actions) {
            drawAction(g, a, xA, yA, size);
            yA += size + spacing / 5;
        }
        // Draw discounts
        int yD = yA + spacing;
        for (Requirement r: card.discountEffects.keySet()) {
            if (r instanceof TagsPlayedRequirement) {
                int xD = width/2 - size*6/2;
                Image from = ImageIO.GetInstance().getImage(((TagsPlayedRequirement)r).tags[0].getImagePath());
                Image to = ImageIO.GetInstance().getImage(TMTypes.Resource.MegaCredit.getImagePath());
                int amount = card.discountEffects.get(r);
                drawImage(g, from, xD, yD, size, size);
                drawShadowStringCentered(g, ":", new Rectangle(xD + size, yD, size*2, size));
                drawShadowStringCentered(g, "-" + amount, new Rectangle(xD + size*3, yD, size*2, size));
                drawImage(g, to, xD + size * 5, yD, size, size);
            } else if (r instanceof CounterRequirement) {
                int xD = width/2 - size*4/2;
                TMTypes.GlobalParameter gp = utilities.Utils.searchEnum(TMTypes.GlobalParameter.class, ((CounterRequirement)r).counterCode);
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
            // todo ActionTypeRequirement (standard project)
            // todo TagsOnCard req
            yD += size + spacing/2;
        }
        // Draw resource mappings
        int yRM = yD + spacing;
        int xRM = width/2 - size*7/2;
        for (TMGameState.ResourceMapping rm: card.resourceMappings) {
            Image from = ImageIO.GetInstance().getImage(rm.from.getImagePath());
            Image to = ImageIO.GetInstance().getImage(rm.to.getImagePath());
            drawImage(g, from, xRM, yRM, size, size);
            drawImage(g, to, xRM + size * 5, yRM, size, size);

            String text;
            if ((rm.rate == Math.floor(rm.rate)) && !Double.isInfinite(rm.rate)) {
                text = ": " + (int) rm.rate;
            } else {
                text = ": " + rm.rate;
            }
            drawShadowStringCentered(g, text, new Rectangle(xRM + size, yRM, size*4, size));
            yRM += size + spacing/2;
            // todo TagOnCard requirement
        }
        // Draw after-action effects
        int yEF = yRM + spacing;
        int xEF = width/2 - size*5/2;
        for (Effect e: card.persistingEffects) {
            int leftNumber = -1;
            Image[] from = null;
            if (e instanceof PayForActionEffect) {
                from = new Image[]{ImageIO.GetInstance().getImage(TMTypes.Resource.MegaCredit.getImagePath())};
                leftNumber = ((PayForActionEffect) e).minCost;
            } else if (e instanceof PlaceTileEffect) {
                if (((PlaceTileEffect) e).tile != null) {
                    from = new Image[]{ImageIO.GetInstance().getImage(((PlaceTileEffect) e).tile.getImagePath())};
                } else if (((PlaceTileEffect) e).resourceTypeGained != null) {
                    // several images separated by slash
                    int nRes = ((PlaceTileEffect) e).resourceTypeGained.length;
                    from = new Image[nRes];
                    int i = 0;
                    for (TMTypes.Resource r: ((PlaceTileEffect) e).resourceTypeGained) {
                        Image imgR = ImageIO.GetInstance().getImage(r.getImagePath());
                        from[i] = imgR;
                        i++;
                    }
                } else {
                    from = new Image[]{ImageIO.GetInstance().getImage(TMTypes.Tile.City.getImagePath())};
                }
            } else if (e instanceof PlayCardEffect) {
                // several images separated by slash
                HashSet<TMTypes.Tag> tags = ((PlayCardEffect) e).tagsOnCard;
                from = new Image[tags.size()];
                int i = 0;
                for (TMTypes.Tag t: tags) {
                    from[i] = ImageIO.GetInstance().getImage(t.getImagePath());
                    i++;
                }
            }

            if (!e.mustBeCurrentPlayer) {
                // draw red outline for the left part
                g.setColor(anyPlayerColor);
                int widthRect = size * 2;
                if (leftNumber == -1) {
                    widthRect = size;
                }
                g.fillRoundRect(xEF - 2, yEF - 2, widthRect + 4, size + 4, spacing, spacing);
            }

            if (leftNumber != -1) {
                drawShadowStringCentered(g, "" + leftNumber, new Rectangle(xEF, yEF, size, size), Color.white, Color.black, 12);
            }
            if (from != null) {
                int nImgs = from.length;
                int sectionSize = Math.min(size*4, Math.max((nImgs-1) * size * 2, size));
                int oneSize = sectionSize/nImgs;
                int resX = xEF + size - sectionSize;
                for (int i = 0; i < nImgs; i++) {
                    Image imgR = from[i];
                    drawImage(g, imgR, resX, yEF, oneSize, oneSize);
                    resX += oneSize;
                    if (i != nImgs-1) {
                        drawShadowStringCentered(g, "/", new Rectangle(resX, yEF, oneSize, oneSize), Color.white, Color.black, 12);
                        resX += oneSize;
                    }
                }
            }
            drawShadowStringCentered(g, " : ", new Rectangle(xEF + size*2, yEF, size, size));

            // "to" depends on the action applied as the effect
            TMAction action = e.effectAction;
            drawCardEffect(g, action, xEF + size * 4, yEF, size, -1);

            yEF += size + spacing/2;
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

    private int drawCardEffect(Graphics2D g, TMAction a, int xE, int yE, int width, int height) {
        // xE is the middle

        if (a instanceof ModifyPlayerResource) {
            drawModifyPlayerResourceAction(g, (ModifyPlayerResource) a, xE, yE, defaultItemSize/3);
            yE += defaultItemSize/3 + spacing/5;
        } else if (a instanceof PlaceTile) {
            drawPlaceTileAction(g, (PlaceTile) a, xE, yE, defaultItemSize/2);
            yE += defaultItemSize/2 + spacing/5;
        } else if (a instanceof AddResourceOnCard) {
            drawAddResourceAction(g, (AddResourceOnCard) a, xE, yE, defaultItemSize/3);
            yE += defaultItemSize/3 + spacing/5;
        } else if (a instanceof ChoiceAction) {
            // Horizontal display, each action next to each other, separated by /
            int nActions = ((ChoiceAction) a).actions.length;
            int sepWidth = g.getFontMetrics().stringWidth(" / ");
            int widthOne = (width - sepWidth*(nActions-1))/nActions;
            xE = xE - width/2 + widthOne/2;
            for (int i = 0; i < nActions; i++) {
                drawCardEffect(g, ((ChoiceAction) a).actions[i], xE, yE, width, height);
                xE += widthOne;
                if (i != nActions-1) {
                    drawShadowStringCentered(g, " / ", new Rectangle(xE - widthOne/2, yE, sepWidth, defaultItemSize / 2));
                    xE += sepWidth;
                }
            }
            yE += defaultItemSize/2 + spacing/5;
        } else if (a instanceof CompoundAction) {
            // Vertical display, one action under the other
            int nActions = ((CompoundAction) a).actions.length;
            for (int i = 0; i < nActions; i++) {
                yE = drawCardEffect(g, ((CompoundAction) a).actions[i], xE, yE, width, height);
            }
        } else if (a instanceof DiscardCard) {
            int aaaa = 0;
        } else if (a instanceof DuplicateImmediateEffect) {
            drawDuplicateEffect(g, (DuplicateImmediateEffect) a, xE, yE, defaultItemSize/3);
            yE += defaultItemSize/3 + spacing/5;
        } else if (a instanceof ModifyGlobalParameter) {
            drawModifyGlobalParameter(g, (ModifyGlobalParameter) a, xE, yE, defaultItemSize/3);
            yE += defaultItemSize/3 + spacing/5;
        } else if (a instanceof  ReserveTile) {
            String text = "Reserve tile";
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(text);
            drawShadowStringCentered(g, text, new Rectangle(xE-w/2, yE, w, defaultItemSize/3));
        } else if (a instanceof TopCardDecision) {
            int nCardsLook = ((TopCardDecision) a).nCardsLook;
            int nCardsKeep = ((TopCardDecision) a).nCardsKeep;
            boolean buy = ((TopCardDecision) a).buy;
            String text = "Look at the top " + (nCardsLook > 1? nCardsLook + " cards." : "card.");
            if (buy) {
                text += " Buy " + (nCardsKeep > 1? nCardsKeep : "it") + " or discard all.";
            } else {
                text += " Take " + nCardsKeep + " of them into your hand and discard the rest.";
            }
            drawShadowStringCentered(g, text, new Rectangle(xE - width/2, yE, width, TMDeckDisplay.cardHeight/3));
        } else {
            int b = 0;
        }
        return yE;
    }

    private void drawAddResourceAction(Graphics2D g, AddResourceOnCard a, int x, int y, int size) {
        // x is the middle

        String amount = "" + a.amount;
        TMTypes.Resource res = a.resource;
        boolean any = a.chooseAny;
        boolean another = a.getCardID() == -1;
        TMTypes.Tag tagRequired = a.tagRequirement;
        int minResRequired = a.minResRequirement;

        // Calculate width of display
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(amount) + size + 4;
        if (another) {
            if (!any) {
                // *
                w += fm.stringWidth("* ");
            }
            if (tagRequired != null || minResRequired > 0) {
                w += fm.stringWidth("()");
                if (tagRequired != null) {
                    // (tag)
                    w += size;
                }
                if (minResRequired > 0) {
                    // (min: X)
                    w += fm.stringWidth("min: " + minResRequired);
                }
            }
        }
        x -= w/2;

        // Draw amount
        drawShadowStringCentered(g, amount, new Rectangle(x, y, fm.stringWidth(amount), size));
        x += fm.stringWidth(amount);
        if (any) {
            // red background
            g.setColor(anyPlayerColor);
            g.fillRoundRect(x, y - 2, size + 4, size + 4, spacing, spacing);
        }
        // Draw res
        drawImage(g, ImageIO.GetInstance().getImage(res.getImagePath()), x + 2, y, size, size);
        x += size + 4;
        // Draw other decorators
        if (another) {
            if (!any) {
                // *
                drawShadowStringCentered(g, "* ", new Rectangle(x, y, fm.stringWidth("* "), size));
                x += fm.stringWidth("* ");
            }
            if (tagRequired != null || minResRequired > 0) {
                // (something)
                drawShadowStringCentered(g, "(", new Rectangle(x, y, fm.stringWidth("("), size));
                x += fm.stringWidth("(");

                if (tagRequired != null) {
                    drawImage(g, ImageIO.GetInstance().getImage(tagRequired.getImagePath()), x, y, size, size);
                    x += size;
                }
                if (minResRequired > 0) {
                    // (min: X)
                    String minText = "min: " + minResRequired;
                    drawStringCentered(g, minText, new Rectangle(x, y, fm.stringWidth(minText), size), Color.gray);
                    x += fm.stringWidth(minText);
                }

                drawShadowStringCentered(g, ")", new Rectangle(x, y, fm.stringWidth(")"), size));
            }
        }
    }

    private void drawPlaceTileAction(Graphics2D g, PlaceTile a, int x, int y, int size) {
        // x is the middle

        TMTypes.Tile t = a.tile;
        TMTypes.MapTileType mt = a.mapType;
        String name = a.tileName;
        int w = size;
        String sep = " -> ";
        FontMetrics fm = g.getFontMetrics();
        int sepW = fm.stringWidth(sep);
        if (mt != null && t.getRegularLegalTileType() != mt) {
            // Draw special placement requirement
            int tileW = fm.stringWidth(mt.name());
            w += tileW + sepW + spacing*2/5;
        }
        if (name != null) {
            // Draw location name
            int nameW = fm.stringWidth(name);
            w += nameW + sepW + spacing*2/5;
        }
        // Do actual drawing
        int xE2 = x - w/2;
        drawImage(g, t.getImagePath(), xE2, y, size, size);
        if (mt != null && t.getRegularLegalTileType() != mt) {
            // Draw special placement requirement
            drawStringCentered(g, " -> " + mt.name(),
                    new Rectangle(xE2 + size, y, sepW + fm.stringWidth(mt.name()), size), Color.gray);
        }
        if (name != null) {
            // Draw location name
            drawStringCentered(g, " -> " + name,
                    new Rectangle(xE2 + size, y, sepW + fm.stringWidth(name), size), Color.gray);
        }
    }

    private void drawModifyPlayerResourceAction(Graphics2D g, ModifyPlayerResource aa, int x, int y, int size) {
        // x is the middle of this rectangle
        String amount;
        if ((aa.change == Math.floor(aa.change)) && !Double.isInfinite(aa.change)) {
            amount = "" + (int)aa.change;
        } else {
            amount = "" + aa.change;
        }
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(amount);
        int totalWidth = size + spacing/5 + textWidth;
        x -= totalWidth/2;

        TMTypes.Resource res = aa.resource;
        boolean prod = aa.production;
        Image resImg = ImageIO.GetInstance().getImage(res.getImagePath());

        if (aa.targetPlayer == -2) {
            g.setColor(anyPlayerColor);
            g.fillRoundRect(x - 2, y - 2, size + 4, size + 4, spacing, spacing);
        }

        drawResource(g, resImg, production, prod, x, y, size, 0.6);
        if (aa.opponents) {
            drawShadowStringCentered(g, "*", new Rectangle(x + size + 2, y, fm.stringWidth("*"), size));
            x += fm.stringWidth("*");
        }
        drawShadowStringCentered(g, amount, new Rectangle(x + size + spacing/5, y, textWidth, size));
    }

    private void drawModifyGlobalParameter(Graphics2D g, ModifyGlobalParameter aa, int x, int y, int size) {
        // x is the middle of this rectangle

        int amount = (int)aa.change;
        TMTypes.GlobalParameter param = aa.param;
        Image resImg = ImageIO.GetInstance().getImage(param.getImagePath());
        int totalWidth = size * amount + spacing/5 * (amount-1);
        x -= totalWidth/2;

        for (int i = 0; i < amount; i++) {
            drawImage(g, resImg, x, y, size);
            x += size + spacing/5;
        }
    }

    private void drawDuplicateEffect(Graphics2D g, DuplicateImmediateEffect aa, int x, int y, int size) {
        // x is the middle of this rectangle

        String text = "Copy a ";
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int totalWidth = size + spacing/5 + textWidth;
        x -= totalWidth/2;
        drawShadowStringCentered(g, text, new Rectangle(x, y, textWidth, size));

        TMTypes.Tag tag = aa.tagRequirement;
        Class<? extends TMAction> actionClass = aa.actionClass;
        if (actionClass.equals(ModifyPlayerResource.class)) {
            boolean prod = aa.production;
            Image resImg = ImageIO.GetInstance().getImage(tag.getImagePath());
            drawResource(g, resImg, production, prod, x + spacing/5 + textWidth, y, size, 0.6);
        }

    }

    private void drawAction(Graphics2D g, TMAction a, int x, int y, int size) {
        // x is center

        // Draw action arrow
        drawImage(g, actionArrow, x - defaultItemSize/4, y, defaultItemSize/2, size);

        TMTypes.Resource leftR = a.getCostResource();
        String left = leftR != null? leftR.getImagePath() : null;
        int leftNumber = Math.abs(a.getCost());

        if (leftNumber > 0) {
            drawShadowStringCentered(g, "" + leftNumber, new Rectangle(x - defaultItemSize/4 - size*2, y, size, size), Color.white, Color.black, 12);
        }
        if (left != null) {
            Image image = ImageIO.GetInstance().getImage(left);
            drawImage(g, image, x - defaultItemSize/4 - size, y, size, size);
        }

        int wR = TMDeckDisplay.cardWidth / 2 - defaultItemSize/4 - spacing;
        if (a instanceof CompoundAction) {
            // first part of the action is the left side of the arrow, unless a cost is defined
            if (left == null) {
                drawCardEffect(g, ((CompoundAction) a).actions[0], x - defaultItemSize/4 - wR/2, y, TMDeckDisplay.cardWidth / 2, -1);
            }

            // second part (right) is all other actions
            int nActions = ((CompoundAction) a).actions.length - 1;
            int widthOne = wR/nActions;
            int xStart = x + defaultItemSize/4 + spacing/5;
            for (int i = 1; i < ((CompoundAction) a).actions.length; i++) {
                drawCardEffect(g, ((CompoundAction) a).actions[i], xStart + widthOne/2, y, widthOne, -1);
                xStart += widthOne + spacing/5;
            }
        } else {
            // Otherwise this is the effect
            drawCardEffect(g, a, x + defaultItemSize/4 + spacing/5 + wR/2, y, wR, -1);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void update(TMGameState gs, TMCard card, int idx) {
        this.updateGS = gs;
        this.updateCard = card;
        this.updateIndex = idx;
        this.update = true;
    }
}
