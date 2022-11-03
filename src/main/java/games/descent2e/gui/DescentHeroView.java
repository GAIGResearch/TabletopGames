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
import games.descent2e.components.Item;
import games.descent2e.components.tokens.DToken;
import gui.views.ComponentView;
import utilities.ImageIO;
import utilities.Utils;
import utilities.Vector2D;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    static Font descentFont, primaryAttributeF, secondaryAttributeF, titleFont;

    public DescentHeroView(DescentGameState dgs, Hero hero, int heroIdx, int width, int height) {
        super(hero, width, height);
        this.dgs = dgs;
        this.hero = hero;
        this.heroIdx = heroIdx;
        this.heroClass = Utils.searchEnum(DescentTypes.HeroClass.class, ((PropertyString) hero.getProperty("class")).value);
        this.imgPath = ((DescentParameters)dgs.getGameParameters()).dataPath + "/img/";

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

        // TODO draw character ability on card
        // TODO draw character heroic feat on card (indicate if executed)
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

        /* Draw abilities, equipment, other cards */
        // TODO

        int cardHeight = (int)(heroCardHeight * 0.6);
        int cardWidth = (int)(heroCardWidth * 0.5);
        int x = 0;
        int y = heroCardHeight + 5;
        Deck<Card> weapons = hero.getHandEquipment();
        for (Card card: weapons.getComponents()) {
            drawCard(g, card, x, y, cardWidth, cardHeight);
            x += cardWidth + 2;
            if (x + cardWidth >= width) {
                x = 0;
                y += cardHeight + 2;
            }
        }
    }

    public void drawCard(Graphics2D g, Card card, int x, int y, int width, int height) {
        g.setColor(Color.darkGray);
        g.fillRoundRect(x, y, width, height, 5, 5);
        g.setColor(Color.white);

        FontMetrics fm = g.getFontMetrics();
        int h = fm.getHeight();
        String name = card.getProperty("name").toString();
        y += h;
        g.drawString(name, x + width/2 - fm.stringWidth(name)/2, y);

        y += h;
        Property XP = card.getProperty("XP");
        Property fatigue = card.getProperty("fatigue");
        String text = "XP: " + XP.toString() + "; Fatigue: " + fatigue.toString();
        g.drawString(text, x + width/2 - fm.stringWidth(text)/2, y);

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

        /*
        "attackPower": ["String[]", ["blue", "red"]],
        "weaponSurges": ["String[]", ["DAMAGE_PLUS_1", "DAMAGE_PLUS_1"]],
        "action": ["String", "Attack"]
        "passive": ["String", "You gain +4 Health. Whenever you stand up or are revived by another hero, you recover 2 additional Heart."]
         */
    }
}
