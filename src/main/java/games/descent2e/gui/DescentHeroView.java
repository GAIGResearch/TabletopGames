package games.descent2e.gui;

import core.components.Card;
import core.components.Deck;
import core.properties.Property;
import core.properties.PropertyBoolean;
import core.properties.PropertyString;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.DescentTypes;
import games.descent2e.components.DescentDice;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.DescentCard;
import games.descent2e.components.tokens.DToken;
import gui.views.ComponentView;
import utilities.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import static games.descent2e.gui.DescentGUI.foregroundColor;
import static gui.AbstractGUIManager.defaultItemSize;

public class DescentHeroView extends ComponentView {
    Hero hero;
    int heroIdx;
    DescentTypes.HeroClass heroClass;
    String imgPath;

    DescentGameState dgs;

    HashMap<Figure.Attribute, Vector2D> maxValuesPositionMap;
    HashMap<Figure.Attribute, Vector2D> valuesPositionMap;

    Image characterCard;
    static double characterCardScale;
    static Font descentFont, primaryAttributeF, secondaryAttributeF, labelFont, titleFont;

    HashMap<Rectangle, DescentCard> rectToCardMap;

    public DescentHeroView(DescentGameState dgs, Hero hero, int heroIdx, int width, int height) {
        super(hero, width, height);
        this.dgs = dgs;
        this.hero = hero;
        this.heroIdx = heroIdx;
        this.heroClass = Utils.searchEnum(DescentTypes.HeroClass.class, ((PropertyString) hero.getProperty("class")).value);
        this.imgPath = ((DescentParameters)dgs.getGameParameters()).dataPath + "/img/";

        rectToCardMap = new HashMap<>();

        maxValuesPositionMap = new HashMap<Figure.Attribute, Vector2D>() {{
            put(Figure.Attribute.MovePoints, new Vector2D(290, 122));
            put(Figure.Attribute.Health, new Vector2D(305, 212));
            put(Figure.Attribute.Fatigue, new Vector2D(315, 300));
            put(Figure.Attribute.Willpower, new Vector2D(65, 462));
            put(Figure.Attribute.Might, new Vector2D(85, 416));
            put(Figure.Attribute.Knowledge, new Vector2D(175, 416));
            put(Figure.Attribute.Awareness, new Vector2D(193, 462));
        }};

        characterCard = ImageIO.GetInstance().getImage(imgPath + "heroes/" + heroClass.getArchetype() + "-card.png");
        characterCardScale = Math.max((width/2.) / characterCard.getWidth(null), 1.0 * height / characterCard.getHeight(null));

        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            descentFont = Font.createFont(Font.TRUETYPE_FONT, new File(((DescentParameters)dgs.getGameParameters()).dataPath + "fonts/VAFTHRUD.ttf"));
            ge.registerFont(descentFont);

            primaryAttributeF = new Font(descentFont.getName(), Font.BOLD, (int)(characterCardScale *45));
            secondaryAttributeF = new Font(descentFont.getName(), Font.BOLD, (int)(characterCardScale *30));
            labelFont = new Font("Arial", Font.BOLD, 10);

            titleFont = Font.createFont(Font.TRUETYPE_FONT, new File(((DescentParameters)dgs.getGameParameters()).dataPath + "fonts/Windlass.ttf"));
            ge.registerFont(titleFont);
            titleFont = new Font("Arial", Font.BOLD, (int)(characterCardScale *18));
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g1) {
        /* draw blank character card and fill in information */
        Graphics2D g = (Graphics2D) g1;

        // TODO ugly version

        // Draw character card
        int heroCardWidth = (int)(characterCardScale * characterCard.getWidth(null));
        int heroCardHeight = (int)(characterCardScale * characterCard.getHeight(null));
        g.drawImage(characterCard, 0, 0, heroCardWidth, heroCardHeight, null);

        Font f = g.getFont();

        // Draw name
        g.setColor(Color.black);
        g.setFont(titleFont);
        g.drawString(hero.getComponentName().split(":")[1].trim(), (int)(characterCardScale*20), (int)(characterCardScale*40));

        // Draw max attribute values
        g.setColor(foregroundColor);
        for (Figure.Attribute a: maxValuesPositionMap.keySet()) {
            if (a.isSecondary()) g.setFont(secondaryAttributeF);
            else g.setFont(primaryAttributeF);
            Vector2D where = maxValuesPositionMap.get(a);
            g.drawString("" + hero.getAttributeMax(a), (int)(where.getX() * characterCardScale), (int)(where.getY() * characterCardScale));
        }
        g.setFont(f);

        // Draw defence dice TODO: if more than 1 die, this will draw all on top of each other
        for (DescentDice die: hero.getDefenceDice().getComponents()) {
            Image toDraw = ImageIO.GetInstance().getImage(imgPath + "dice/" + die.getColour().name().toLowerCase() + ".png");
            g.drawImage(toDraw, (int)(characterCardScale*285), (int)(characterCardScale*353), defaultItemSize/3, defaultItemSize/3,null);
        }

        // Character ability // TODO: symbolic version?
        GUIUtils.drawStringCentered(g, hero.getAbilityStr(), new Rectangle((int)(characterCardScale*380), (int)(characterCardScale*70), (int)(characterCardScale*220), (int)(characterCardScale*190)), Color.black, 12, true);

        // Draw character heroic feat on card (indicate if executed)  // TODO symbolic?
        GUIUtils.drawStringCentered(g, hero.getHeroicFeatStr(), new Rectangle((int)(characterCardScale*380), (int)(characterCardScale*290), (int)(characterCardScale*220),
                (int)(characterCardScale*190)), hero.isFeatAvailable() ? Color.black : Color.lightGray, 12, true);
        if (!hero.isFeatAvailable()) {
            GUIUtils.drawStringCentered(g, "Executed", new Rectangle((int)(characterCardScale*380), (int)(characterCardScale*380), (int)(characterCardScale*220),
                    (int)(characterCardScale*190)), Color.black, 16, true);
        }

        // TODO character image?

        /* Draw other data stored on the figure */

        // Draw attribute current values
        g.setColor(foregroundColor);
        int n = 0;
        for (Figure.Attribute a: Figure.Attribute.values()) {
            if (!a.isSecondary()) {
                g.drawString(a.name() + " : " + hero.getAttributeValue(a), 5 + width/2, 15 + 15 * (n++));
            }
        }

        // Draw conditions
        g.drawString("Conditions: " + hero.getConditions().toString(), 5 + width/2, 15 + 15 * n);

        // Draw tokens player owns
        int i = 0;
        int j = 0;
        int maxCols = 2;
        int gap = 5;
        for (DToken t: dgs.getTokens()) {
            if (t.getOwnerId() == heroIdx) {
                String path = imgPath + t.getDescentTokenType().getImgPath(new Random(dgs.getGameParameters().getRandomSeed()));
                Image img = ImageIO.GetInstance().getImage(path);
                g.drawImage(img, 90 + width/2 + j*defaultItemSize/2 + gap*j, 5 + i * defaultItemSize/2 + i * gap, defaultItemSize/2, defaultItemSize/2, null);
                j++;
                if (j > maxCols) {
                    i++;
                    j = 0;
                }
            }
        }

        /* Draw cards: skills, equipment, others */
        int cardHeight = (int)(heroCardHeight * 0.8);
        int cardWidth = (int)(heroCardWidth * 0.6);
        Pair<Integer, Integer> xy = new Pair<>(0, heroCardHeight + 5);
        xy = drawDeckOfCards(g, hero.getHandEquipment(), xy.a, xy.b, cardWidth, cardHeight);
        Card armor = hero.getArmor();
        if (armor != null) {
            drawCard(g, hero.getArmor(), xy.a, xy.b, cardWidth, cardHeight);
            rectToCardMap.put(new Rectangle(xy.a, xy.b, cardWidth, cardHeight), hero.getArmor());
            xy.a += cardWidth + 2;
            if (xy.a + cardWidth + 5 >= width) {
                xy.a = 0;
                xy.b += cardHeight + 2;
            }
        }
        xy = drawDeckOfCards(g, hero.getOtherEquipment(), xy.a, xy.b, cardWidth, cardHeight);
        xy = drawDeckOfCards(g, hero.getSkills(), xy.a, xy.b, cardWidth, cardHeight);
    }

    private Pair<Integer, Integer> drawDeckOfCards(Graphics2D g, Deck<DescentCard> deck, int x, int y, int cardWidth, int cardHeight) {
        for (DescentCard card: deck.getComponents()) {
            drawCard(g, card, x, y, cardWidth, cardHeight);
            rectToCardMap.put(new Rectangle(x, y, cardWidth, cardHeight), card);
            x += cardWidth + 2;
            if (x + cardWidth + 5 >= width) {
                x = 0;
                y += cardHeight + 2;
            }
        }
        return new Pair<>(x, y);
    }

    public void drawCard(Graphics2D g, DescentCard card, int x, int y, int width, int height) {
        g.setColor(Color.darkGray);
        g.fillRoundRect(x, y, width, height, 5, 5);
        g.setColor(Color.white);

        FontMetrics fm = g.getFontMetrics();
        int h = fm.getHeight();
        Property nameProp = card.getProperty("name");
        String name = nameProp.toString();
        y += h;
        g.drawString(name, x + width/2 - fm.stringWidth(name)/2, y);

        String text = "";

        Property XP = card.getProperty("XP");
        Property fatigue = card.getProperty("fatigue");
        if (XP != null && fatigue != null) {  // TODO: could a card have only XP, or only fatigue? probably
            y += h;
            text = "XP: " + XP + "; Fatigue: " + fatigue;
            g.drawString(text, x + width / 2 - fm.stringWidth(text) / 2, y);
        }

        Property equipmentType = card.getProperty("equipmentType");
        if (equipmentType != null) {
            y += h;
            text = "Type: " + equipmentType;
            g.drawString(text, x + width / 2 - fm.stringWidth(text) / 2, y);
        }
        Property equipSlots = card.getProperty("equipSlots");
        if (equipmentType != null) {
            y += h;
            String[] slots = ((PropertyStringArray)equipSlots).getValues();
            text = Arrays.toString(slots);
            g.drawString(text, x + width / 2 - fm.stringWidth(text) / 2, y);
        }
        Property cost = card.getProperty("cost");
        if (equipmentType != null) {
            int w = fm.stringWidth(text);
            text = " " + cost + "G";
            g.drawString(text, x + w/2 + width / 2, y);
        }

        Property exhaust = card.getProperty("exhaust");
        if (exhaust != null) {
            boolean ex = ((PropertyBoolean)card.getProperty("exhaust")).value;
            if (ex) {
                y += h;
                text = "Exhausts on use";
                g.drawString(text, x + width / 2 - fm.stringWidth(text) / 2, y);
            }
        }

        Property attackType = card.getProperty("attackType");
        if (attackType != null) {
            y += h;
            text = "Attack: " + attackType;
            g.drawString(text, x + width / 2 - fm.stringWidth(text) / 2, y);
        }

        Property attackPower = card.getProperty("attackPower");
        if (attackPower != null) {
            y += h;
            int idx = 0;
            int pad = 2;
            int startX = x + width/2 - hero.getAttackDice().getComponents().size() * (h+pad) / 2;
            String[] colors = ((PropertyStringArray)attackPower).getValues();
            for (String col: colors) {
                Image toDraw = ImageIO.GetInstance().getImage(imgPath + "dice/" + col + ".png");
                g.drawImage(toDraw, startX + idx*(h+pad), y-h*2/3, h, h,null);
                idx++;
            }
        }

        Property weaponSurges = card.getProperty("weaponSurges");
        if (weaponSurges != null) {
            int pad = 5;
            y += pad;
            String[] surges = ((PropertyStringArray)weaponSurges).getValues();
            for (String surge: surges) {
                y += h;
                Image toDraw = ImageIO.GetInstance().getImage(imgPath + "tokens/Surge_L.png");
                int startX = x + width/2 - (h + pad + fm.stringWidth(surge)) / 2;
                g.drawImage(toDraw, startX, y-h*2/3, h, h,null);
                g.drawString(surge, startX + h + pad, y);
            }
        }

        // Action and passive text are put as tooltips to not crowd view, below.

    }

    @Override
    public String getToolTipText(MouseEvent event) {
        Point p = new Point(event.getX(), event.getY());
        for (Rectangle r: rectToCardMap.keySet()) {
            if (r.contains(p)) {
                String toolTip = "";

                Property action = rectToCardMap.get(r).getProperty("action");
                if (action != null) {
                    toolTip += "Action: " + action;
                }
                Property passive = rectToCardMap.get(r).getProperty("passive");
                if (passive != null) {
                    toolTip += "Passive: " + passive;
                }

                return toolTip;
            }
        }
        return super.getToolTipText(event);
    }
}
