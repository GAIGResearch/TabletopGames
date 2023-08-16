package games.puertorico.gui;

import games.puertorico.PuertoRicoConstants;
import games.puertorico.components.Building;
import games.puertorico.components.Plantation;
import games.puertorico.components.ProductionBuilding;
import games.puertorico.components.Ship;
import utilities.Pair;

import javax.swing.plaf.metal.MetalButtonUI;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.GeneralPath;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class PRGUIUtils {
    public static boolean showTooltips = true;

    // Sizes
    public static int pad = 4;
    public static int barrelWidth = 12;
    public static int barrelHeight = 15;
    public static int barrelBend = 3;
    public static int colonistRadius = 20;
    public static int colonistRadiusSmall = 10;
    public static int colonistRadiusSmallDetail = 7;
    public static int colonistRadiusDetail = 13;
    public static int shipCurve = 3;
    public static int shipSpaceSize = 15;
    public static int nSpacesOnLine = 2;
    public static int plantationSize = 40;
    public static int buildingWidth = 60;
    public static int buildingHeight = 35;

    public static boolean outline = true;
    public static boolean barrelDetail = true;
    public static boolean barrelLinesHorizontal = false;
    public static int alphaColonistSpace = 50;
    public static Stroke stroke2 = new BasicStroke(2);

    public static PRButtonUI buttonUI = new PRButtonUI();

    // Colors
    public static Color colonistColor = new Color(65, 44, 44);
    public static Color colonistColorLight = new Color(114, 79, 79);
    public static double cropColorFade = 0.2;
    public static Map<PuertoRicoConstants.Crop, Color> cropColorMap = new HashMap<PuertoRicoConstants.Crop, Color>() {{
        put(PuertoRicoConstants.Crop.CORN, new Color(229, 190, 81));
        put(PuertoRicoConstants.Crop.INDIGO, new Color(47, 115, 164));
        put(PuertoRicoConstants.Crop.SUGAR, new Color(232, 208, 181));
        put(PuertoRicoConstants.Crop.TOBACCO, new Color(138, 73, 57));
        put(PuertoRicoConstants.Crop.COFFEE, new Color(42, 38, 38));
        put(PuertoRicoConstants.Crop.QUARRY, new Color(178, 197, 197));
    }};
    public static Map<PuertoRicoConstants.Crop, Color> cropContrastColorMap = new HashMap<PuertoRicoConstants.Crop, Color>() {{
        put(PuertoRicoConstants.Crop.CORN, new Color(56, 46, 19));
        put(PuertoRicoConstants.Crop.INDIGO, new Color(211, 230, 245));
        put(PuertoRicoConstants.Crop.SUGAR, new Color(59, 53, 46));
        put(PuertoRicoConstants.Crop.TOBACCO, new Color(215, 206, 204));
        put(PuertoRicoConstants.Crop.COFFEE, new Color(225, 219, 219));
        put(PuertoRicoConstants.Crop.QUARRY, new Color(64, 86, 85));
    }};
    public static Color backgroundColor = new Color(205, 231, 248);
    public static Color titleColor = new Color(73, 200, 232);
    public static Color highlightColor = new Color(6, 169, 145);
    public static Color secondaryColor = new Color(42, 91, 154);
    public static Color secondaryColorFaint = new Color(106, 130, 161);
    public static Color shipColor = new Color(204, 167, 120, 240);
    public static Color spaceColor = new Color(227, 214, 184, 103);
    public static Color plantationSpaceColor = new Color(118, 187, 97);
    public static Color buildingSpaceColor = new Color(238, 234, 227);
    public static Color commercialBuildingColor = new Color(195, 174, 243);

    // Fonts
    public static int roleFontSize = 16;
    public static Font roleFontAvailable = new Font("Book Antiqua", Font.BOLD, roleFontSize);
    public static Font roleFontAvailableButNotChosen = new Font("Book Antiqua", Font.PLAIN, roleFontSize);
    public static int titleFontSize = 30;
    public static Font titleFont = new Font("Book Antiqua", Font.BOLD, titleFontSize);
    public static int textFontSize = 14;
    public static int smallTextSize = 10;
    public static int smallestTextSize = 8;
    public static Font textFontPlain = new Font("Calibri", Font.PLAIN, textFontSize);
    public static Font textFontBold = new Font("Calibri", Font.BOLD, textFontSize);
    public static Font smallTextFont = new Font("Calibri", Font.PLAIN, smallTextSize);
    public static Font smallestTextFont = new Font("Calibri", Font.BOLD, smallestTextSize);
    public static Font roleFontNotAvailable;
    static {
        // Custom font registering
//        try {
//            GraphicsEnvironment ge =
//                    GraphicsEnvironment.getLocalGraphicsEnvironment();
//            roleFontAvailable = Font.createFont(Font.TRUETYPE_FONT, new File("data/puertorico/fonts/vinque rg.otf")).deriveFont((float)roleFontSize);
//            ge.registerFont(roleFontAvailable);
//        } catch (IOException | FontFormatException e) {
////            Handle exception
//        }

        Map<TextAttribute, Boolean> attributes = (Map<TextAttribute, Boolean>) PRGUIUtils.roleFontAvailable.getAttributes();
        attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        PRGUIUtils.roleFontNotAvailable = new Font(attributes);
    }

    public static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
    public static String clearUnderscore(String s) {
        return s.replace("_", " ");
    }

    public static void drawHexagon(Graphics2D g, int x, int y, Color color, int width, int height) {
        GeneralPath gp = new GeneralPath();
        gp.moveTo(x + width/2., y);
        gp.lineTo(x + width, y + height/4.);
        gp.lineTo(x + width, y + height*3./4);
        gp.lineTo(x + width/2., y + height);
        gp.lineTo(x, y + height*3./4);
        gp.lineTo(x, y + height/4.);
        gp.closePath();

        g.setColor(color);
        g.fill(gp);
        if (outline) {
            g.setColor(Color.BLACK);
            g.draw(gp);
        }
    }

    public static void drawBarrel(Graphics2D g, int x, int y, Color color) {
        drawBarrel(g, x, y, color, Color.BLACK);
    }

    /**
     * Returns simple barrel shape.
     * @param g - graphics object
     * @param x - top-left X
     * @param y - top-left Y
     * @param color - color of barrel
     */
    public static void drawBarrel(Graphics2D g, int x, int y, Color color, Color outlineColor) {
        GeneralPath gp = new GeneralPath();
        gp.moveTo(x + barrelBend, y);
        gp.lineTo(x + barrelWidth - barrelBend, y);
        gp.curveTo(x + barrelWidth, y + barrelHeight/3.0, x + barrelWidth, y + barrelHeight*2.0/3, x + barrelWidth - barrelBend, y + barrelHeight);
        gp.lineTo(x + barrelBend, y + barrelHeight);
        gp.curveTo(x, y + barrelHeight*2/3.0, x, y + barrelHeight/3.0, x + barrelBend, y);

        g.setColor(color);
        g.fill(gp);
        if (outline) {
            g.setColor(outlineColor);
            g.draw(gp);
        }
        if (barrelDetail) {
            int startX = x + barrelBend;
            int endX = x + barrelWidth - barrelBend;
            double middleX = x + barrelWidth / 2.;
            double quarter1X = (startX + middleX) / 2.;
            double quarter2X = (middleX + endX) / 2.;

            GeneralPath detailGP = new GeneralPath();
            if (!barrelLinesHorizontal) {
                // 3 vertical lines
                detailGP.moveTo(quarter1X, y);
                detailGP.curveTo(quarter1X - barrelBend / 2., y + barrelHeight / 3., quarter1X - barrelBend / 2., y + 2 * barrelHeight / 3., quarter1X, y + barrelHeight);
                detailGP.moveTo(middleX, y);
                detailGP.lineTo(middleX, y + barrelHeight);
                detailGP.moveTo(quarter2X, y);
                detailGP.curveTo(quarter2X + barrelBend / 2., y + barrelHeight / 3., quarter2X + barrelBend / 2., y + 2 * barrelHeight / 3., quarter2X, y + barrelHeight);
            } else {
                // 3 horizontal lines
                detailGP.moveTo(x + barrelBend/2., y + barrelHeight / 4.);
                detailGP.curveTo(quarter1X, y + barrelHeight / 4. + barrelBend/2., quarter2X, y + barrelHeight / 4. + barrelBend/2., x + barrelWidth - barrelBend/2., y + barrelHeight / 4.);
                detailGP.moveTo(x, y + barrelHeight/2.);
                detailGP.curveTo(quarter1X, y + barrelHeight / 2. + barrelBend/2., quarter2X, y + barrelHeight / 2. + barrelBend/2., x + barrelWidth - barrelBend/2., y + barrelHeight / 2.);
                detailGP.moveTo(x + barrelBend/2., y + barrelHeight*3/4.);
                detailGP.curveTo(quarter1X, y + barrelHeight*3/4. + barrelBend/2., quarter2X, y + barrelHeight*3/4. + barrelBend/2., x + barrelWidth - barrelBend/2., y + barrelHeight*3/4.);
            }

            g.setColor(outlineColor);
            g.draw(detailGP);
        }
    }

    public static void drawColonist(Graphics2D g, int x, int y) {
        drawColonist(g, x, y, colonistRadius, colonistRadiusDetail);
    }

    /**
     * Simple drawing of puck shape with 2 circles, one lighter on top
     * @param g - graphics object
     * @param x - top-left x
     * @param y - top-left y
     */
    public static void drawColonist(Graphics2D g, int x, int y, int radius, int detailRadius) {
        g.setColor(colonistColor);
        g.fillOval(x, y, radius, radius);
        g.setColor(colonistColorLight);
        g.fillOval(x, y, radius, detailRadius);
        if (outline) {
            g.setColor(Color.black);
            g.drawOval(x, y, radius, radius);
            g.drawOval(x, y, radius, detailRadius);
        }
    }

    public static Pair<Integer, Integer> drawShip(Graphics2D g, Ship ship, int x, int y) {
        return drawShip(g, ship, x, y, ship.getCurrentCargo(), ship.getSpacesFilled());
    }

    /**
     * Draws a Puerto Rico ship with indicated dimensions and properties.
     * @param g - graphics object
     * @param ship - {@link Ship} object with information on capacity {@link Ship#capacity}, spaces filled {@link Ship#getSpacesFilled} and cargo {@link Ship#getCurrentCargo}
     * @param x - top-left x
     * @param y - top-left y
     * @return - width and height of ship drawn
     */
    public static Pair<Integer, Integer> drawShip(Graphics2D g, Ship ship, int x, int y, PuertoRicoConstants.Crop crop, int nFilled) {
        int nLines = ship.capacity / nSpacesOnLine;
        int nSpacesLastRow = ship.capacity % nSpacesOnLine;
        if (nSpacesLastRow == 0) {
            nSpacesLastRow = nSpacesOnLine;
        } else {
            nLines++;
        }

        // General ship shape
        int width = shipSpaceSize * (nSpacesOnLine+1);
        int height = shipSpaceSize * (nLines+3);
        GeneralPath gp = new GeneralPath();
        gp.moveTo(x + shipCurve, y);
        gp.lineTo(x + width - shipCurve, y);
        gp.curveTo(x + width, y + height/3.0 - shipSpaceSize, x + width - shipCurve/2., y + height*2.0/3 - shipSpaceSize, x + width/2., y + height - shipSpaceSize);
        gp.lineTo(x + width/2., y + height);
        gp.moveTo(x + width/2., y + height - shipSpaceSize);
        gp.curveTo(x + shipCurve/2., y + height*2/3.0 - shipSpaceSize, x, y + height/3.0 - shipSpaceSize, x + shipCurve, y);

        g.setColor(shipColor);
        g.fill(gp);
        if (outline) {
            g.setColor(Color.black);
            g.draw(gp);
        }

        // Draw spaces (and cargo), n on a line
        int startX = x + shipSpaceSize/2;
        int startY = y + shipSpaceSize/2;
        for (int i = 0; i < nLines; i++) {
            if (i != nLines-1 || nSpacesLastRow == nSpacesOnLine) {
                drawSpaces(g, ship, outline, nSpacesOnLine, startX, startY, i, crop, nFilled);
            } else {
                // Last row has fewer spaces, first calculate offset in order to center spaces
                startX = x + width/2 - nSpacesLastRow*shipSpaceSize/2;
                drawSpaces(g, ship, outline, nSpacesLastRow, startX, startY, i, crop, nFilled);
            }
        }
        return new Pair<>(width, height);
    }

    /**
     * Helper method to draw spaces on a line in the ship
     * @param g - graphics object
     * @param outline - if true, black outline is drawn
     * @param nSpacesOnLine - how many spaces drawn in the line
     * @param startX - where the row starts, top-left X
     * @param startY - top-left Y
     * @param i - row index
     * @param crop - cargo for ship
     * @param nFilled - how many spaces on the ship are filled
     */
    private static void drawSpaces(Graphics2D g, Ship ship, boolean outline, int nSpacesOnLine, int startX, int startY, int i, PuertoRicoConstants.Crop crop, int nFilled) {
        Color fadeColor = cropColorMap.get(crop);
        Color fadeOutlineColor = Color.black;
        if (fadeColor != null) {
            fadeColor = new Color(cropColorMap.get(crop).getRed(), cropColorMap.get(crop).getGreen(), cropColorMap.get(crop).getBlue(), (int) (255 * cropColorFade));
            fadeOutlineColor = new Color(0,0,0, (int) (255 * cropColorFade));
        }
        for (int j = 0; j < nSpacesOnLine; j++) {
            g.setColor(spaceColor);
            g.fillRect(startX + shipSpaceSize*j, startY + shipSpaceSize * i, shipSpaceSize, shipSpaceSize);
            if (outline) {
                g.setColor(Color.black);
                g.drawRect(startX + shipSpaceSize * j, startY + shipSpaceSize * i, shipSpaceSize, shipSpaceSize);
            }
            if (i*nSpacesOnLine + j < ship.getSpacesFilled()) {
                drawBarrel(g, startX + shipSpaceSize * j + shipSpaceSize / 2 - barrelWidth / 2,
                        startY + shipSpaceSize * i + shipSpaceSize / 2 - barrelHeight / 2,
                        cropColorMap.get(crop));
            } else if (i * nSpacesOnLine + j < nFilled) {
                // Shadow barrel
                drawBarrel(g, startX + shipSpaceSize * j + shipSpaceSize / 2 - barrelWidth / 2 + 1,
                        startY + shipSpaceSize * i + shipSpaceSize / 2 - barrelHeight / 2 + 1,
                        fadeColor, fadeOutlineColor);
            }
        }
    }

    /**
     * Draws a plantation tile
     * @param g - graphics object
     * @param p - plantation object {@link Plantation}
     * @param x - top-left x
     * @param y - top-left y
     */
    public static void drawPlantation(Graphics2D g, Plantation p, int x, int y) {
        g.setFont(smallTextFont);
        Color color = plantationSpaceColor;
        if (p != null) color = cropColorMap.get(p.crop);
        g.setColor(color);
        g.fillRect(x, y, plantationSize, plantationSize);
        if (outline) {
            g.setColor(Color.black);
            g.drawRect(x, y, plantationSize, plantationSize);
        }

        if (p != null) {
            Color contrastColor = cropContrastColorMap.get(p.crop);
            g.setColor(contrastColor);
            g.drawString(capitalize(p.crop.name()), x + pad / 2, y + smallTextSize + pad / 2);

            // Draw worker space
            drawColonistSpace(g, x + pad / 2, y + plantationSize - colonistRadius - pad / 2, contrastColor, p.isOccupied());
        }
        g.setFont(textFontPlain);
    }

    public static void drawBuilding(Graphics2D g, Building b, int x, int y, int nAvailable) {
        g.setFont(smallestTextFont);
        Color color = buildingSpaceColor;
        Color contrastColor = Color.black;
        if (b != null) {
            if (b instanceof ProductionBuilding) {
                color = cropColorMap.get(((ProductionBuilding) b).cropType);
                contrastColor = cropContrastColorMap.get(((ProductionBuilding) b).cropType);
            } else {
                color = commercialBuildingColor;
            }
        }
        g.setColor(color);

        int h = buildingHeight;
        if (b != null) h *= b.buildingType.size;

        g.fillRect(x, y, buildingWidth, h);
        if (outline) {
            g.setColor(Color.black);
            g.drawRect(x, y, buildingWidth, h);
        }

        if (b != null) {
            g.setColor(contrastColor);
            g.drawString(clearUnderscore(capitalize(b.getComponentName())), x + pad / 2, y + smallestTextSize);
            g.drawString("Cost: " + b.buildingType.cost + "      VP: " + b.buildingType.vp, x + pad / 2, y + smallestTextSize + pad / 2 + smallestTextSize + pad / 2);

            // Draw worker spaces, occupied if someone is working there
            for (int i = 0; i < b.buildingType.capacity; i++) {
                drawColonistSpace(g, x + pad / 2 + i * (colonistRadiusSmall + pad / 2), y + h - colonistRadiusSmall - pad / 2, colonistRadiusSmall, colonistRadiusSmallDetail, contrastColor, b.getOccupation() > i);
            }

            // Draw nAvailable in the bottom-right corner
            if (nAvailable > 0) {
                g.drawString("x" + nAvailable, x + buildingWidth - pad / 2 - g.getFontMetrics().stringWidth("x" + nAvailable), y + h - pad / 2);
            }
        }
        g.setFont(textFontPlain);
    }

    private static void drawColonistSpace(Graphics2D g, int x, int y, int radius, int radiusDetail, Color color, boolean occupied) {
        // Draw worker spaces
        Stroke s = g.getStroke();
        g.setStroke(stroke2);
        Color transparent = new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaColonistSpace);
        g.setColor(transparent);
        g.fillOval(x, y, radius, radius);
        g.setColor(color);
        g.drawOval(x, y, radius, radius);
        g.setStroke(s);

        // Draw colonist if occupied
        if (occupied) {
            drawColonist(g, x, y, radius, radiusDetail);
        }

    }

    private static void drawColonistSpace(Graphics2D g, int x, int y, Color color, boolean occupied) {
        drawColonistSpace(g, x, y, colonistRadius, colonistRadiusDetail, color, occupied);
    }

    public static class PRButtonUI extends MetalButtonUI {
        protected Color getDisabledTextColor() {
            return secondaryColor;
        }
    }
}
