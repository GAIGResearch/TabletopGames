package games.terraformingmars.gui;

import core.components.Counter;
import core.components.GridBoard;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.*;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.effects.Bonus;
import gui.IScreenHighlight;
import gui.views.ComponentView;
import utilities.ImageIO;
import utilities.Vector2D;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import static gui.AbstractGUIManager.defaultItemSize;
import static games.terraformingmars.gui.TMCardView.drawResource;
import static utilities.GUIUtils.*;

public class TMBoardView extends ComponentView implements IScreenHighlight {

    TMGameState gs;
    public final static Color[] playerColors = new Color[]{Color.RED, Color.YELLOW, Color.BLUE, Color.GREEN};

    HashMap<Rectangle, String> rects;  // Used for highlights + action trimming
    ArrayList<Rectangle> highlight;

    Image background;
    Image counterTop, counterMid, counterBot, production, tr;
    Color mapTileBackground = new Color(97, 97, 97, 70);

    int fontSize = 16;
    int offsetX = 10;
    int spacing = 10;

    private boolean adjustedSize;

    public TMBoardView(TMGUI gui, TMGameState gs) {
        super(gs.getBoard(), 0, 0);
        this.gs = gs;

        width = 500;
        height = 500;

        rects = new HashMap<>();
        highlight = new ArrayList<>();
        background = ImageIO.GetInstance().getImage("data/terraformingmars/images/mars.png");
        counterTop = ImageIO.GetInstance().getImage("data/terraformingmars/images/misc/meter-scale-top.png");
        counterMid = ImageIO.GetInstance().getImage("data/terraformingmars/images/misc/meter-scale-mid.png");
        counterBot = ImageIO.GetInstance().getImage("data/terraformingmars/images/misc/meter-scale-bot.png");
        production = ImageIO.GetInstance().getImage("data/terraformingmars/images/misc/production.png");
        tr = ImageIO.GetInstance().getImage("data/terraformingmars/images/resources/TR.png");

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
                gui.updateButtons = true;
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;

        g.setFont(TMGUI.defaultFont);

        // Draw global parameters
        double height = 0;
        double width = 0;
        ArrayList<TMTypes.GlobalParameter> drawOrder = TMTypes.GlobalParameter.getDrawOrder((TMGameParameters)gs.getGameParameters());
        for (int i = 0; i < drawOrder.size(); i++) {
            TMTypes.GlobalParameter p = drawOrder.get(i);
            Rectangle rect = drawCounter(g, offsetX + i * defaultItemSize * 2, 0, gs.getGlobalParameters().get(p));
            if (rect.getHeight() + rect.getY() > height) {
                height = rect.getHeight() + rect.getY();
            }
            if (rect.getWidth() + rect.getX() > width) {
                width = rect.getWidth() + rect.getX();
            }
        }

        Rectangle rect = drawGridBoard(g, (GridBoard) component, offsetX + (gs.getGlobalParameters().size()+1) * defaultItemSize + 10, defaultItemSize);
        if (rect.getHeight() + rect.getY() > height) {
            height = rect.getHeight() + rect.getY();
        }
        if (rect.getWidth() + rect.getX() > width) {
            width = rect.getWidth() + rect.getX();
        }

        // Draw TR for all players
        int offsetY = (int)(height + spacing);
        int trWidth = 0;
        for (int i = 0; i < gs.getNPlayers(); i++) {
            Rectangle r = drawImage(g, tr, offsetX + i*defaultItemSize*3, offsetY, defaultItemSize);
            drawShadowStringCentered(g, "p" + i + ": " +gs.getPlayerResources()[i].get(TMTypes.Resource.TR).getValue(),
                    new Rectangle(offsetX + i*defaultItemSize*3 + defaultItemSize, offsetY, defaultItemSize*2, r.height),
                    playerColors[i], TMGUI.darkGrayColor);
            if (i == 0) {
                height += spacing + r.getHeight() + spacing;
            }
            trWidth += defaultItemSize*3;
        }
        if (offsetX + trWidth > width) {
            width = offsetX + trWidth;
        }

        if (!adjustedSize) {
            this.height = (int) height;
            this.width = (int) width + offsetX;
            adjustedSize = true;
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

    public Rectangle drawGridBoard(Graphics2D g, GridBoard gridBoard, int x, int y) {
        int offsetY = defaultItemSize/2;

        // Board background
        int width = gridBoard.getWidth() * defaultItemSize;
        int height = gridBoard.getHeight() * defaultItemSize;
        drawImage(g, background, x, y, width, height);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                int offsetX = defaultItemSize/2;
                if (i % 2 == 1) {
                    offsetX += defaultItemSize/2;
                }

                int xC = x + offsetX + j * defaultItemSize;
                int yC = y + offsetY + i * defaultItemSize;
                drawCell(g, (TMMapTile) gridBoard.getElement(j, i), xC, yC);

                // Save rect where cell is drawn
                rects.put(new Rectangle(xC - defaultItemSize/2, yC - defaultItemSize/2, defaultItemSize, defaultItemSize), "grid-" + j + "-" + i);
            }
        }

        // Draw extra cells
        int yC = y + offsetY + spacing + height;
        int xC = x + Math.max(0, width/2 - gs.getExtraTiles().size() * defaultItemSize/2);
        int i = 0;
        for (TMMapTile mt: gs.getExtraTiles()) {
            drawCell(g, mt, xC, yC);
            rects.put(new Rectangle(xC - defaultItemSize/2, yC - defaultItemSize/2, defaultItemSize, defaultItemSize), mt.getComponentName());
            i++;
            xC += defaultItemSize * 2;
            if (xC + defaultItemSize > x + width) {
                xC = x + Math.max(0, width/2 - (gs.getExtraTiles().size()-i) * defaultItemSize / 2 + defaultItemSize/2);
                yC += defaultItemSize;
            }
        }

        return new Rectangle(x, y, width, height);
    }

    private void drawCell(Graphics2D g, TMMapTile element, int x, int y) {
        if (element != null) {
            // Create hexagon
            Polygon h = new Polygon();
            for (int i = 0; i < 6; i++) {
                h.addPoint((int) (x + defaultItemSize/2 * Math.cos(Math.PI/2 + i * 2 * Math.PI / 6)),
                        (int) (y + defaultItemSize/2 * Math.sin(Math.PI/2 + i * 2 * Math.PI / 6)));
            }

            g.setColor(mapTileBackground);
            g.fillPolygon(h);

            g.setColor(element.getTileType().getOutlineColor());
            g.drawPolygon(h);

            // Draw resources
            if (element.getTilePlaced() == null) {
                boolean drew = drawElementName(g, x, y, element);
                TMTypes.Resource[] resources = element.getResources();
                int resSize = defaultItemSize/4;
                for (int i = 0; i < resources.length; i++) {
                    if (resources[i] != null) {
                        int yDraw = y + i * resSize - defaultItemSize / 3;
                        int xDraw = x + i * resSize - defaultItemSize / 3;
                        if (drew) {
                            yDraw = y - defaultItemSize / 3;
                            xDraw = x + i * resSize + i * 2 - defaultItemSize / 3;
                        }
                        drawImage(g, resources[i].getImagePath(), xDraw, yDraw, resSize, resSize);
                    }
                }
            } else {
                // Draw tile here
                drawImage(g, element.getTilePlaced().getImagePath(), x-defaultItemSize/2, y-defaultItemSize/2, defaultItemSize, defaultItemSize);
                drawElementName(g, x, y, element);
                if (element.getOwnerId() >= 0) {
                    // Draw owner
                    g.setColor(playerColors[element.getOwnerId()]);
                    g.fillRect(x - defaultItemSize/6, y - defaultItemSize/6, defaultItemSize/3, defaultItemSize/3);
                    g.setColor(Color.black);
                    g.drawRect(x - defaultItemSize/6, y - defaultItemSize/6, defaultItemSize/3, defaultItemSize/3);
                }
            }
        }

    }

    private boolean drawElementName(Graphics2D g, int x, int y, TMMapTile element) {
        if (element.getTileType() == TMTypes.MapTileType.City) {
            // Draw city name
            drawShadowStringCentered(g, element.getComponentName(),
                    new Rectangle(x-defaultItemSize/2, y-defaultItemSize/2, defaultItemSize, defaultItemSize),
                    TMGUI.lightGrayColor, null, 10);
            return true;
        }
        return false;
    }

    private Rectangle drawCounter(Graphics2D g, int x, int y, Counter c) {
        int border = 3;
        int counterWidth = defaultItemSize;
        Rectangle rect = new Rectangle(x, y, 0, 0);

        TMTypes.GlobalParameter p = TMGameState.counterToGP(c);
        if (p != null) {
            Image symbol = ImageIO.GetInstance().getImage(p.getImagePath());
            if (p == TMTypes.GlobalParameter.OceanTiles) {
                // Draw symbol and value
                drawImage(g, p.getImagePath(), x, y, counterWidth, counterWidth);
                String text = "" + c.getValue() + "/" + c.getMaximum();
                drawShadowStringCentered(g, text, new Rectangle(x, y, counterWidth, counterWidth), Color.yellow, Color.black);
                rect.setRect(x, y, counterWidth, counterWidth);
            } else {
                // Some vars
                int steps = c.getValues().length;
                int topHeight = (int)((counterWidth*1.0/counterTop.getWidth(null)) * counterTop.getHeight(null));
                int stepSize = counterWidth/2;
                int midHeight = steps * stepSize;
                int botHeight = (int)((counterWidth*1.0/counterBot.getWidth(null)) * counterBot.getHeight(null));

                rect.setRect(x, y, counterWidth*2, topHeight + midHeight + botHeight);

                // Draw background
                drawImage(g, counterTop, x, y, counterWidth, topHeight);
                drawImage(g, counterMid, x, topHeight + y, counterWidth, midHeight);
                drawImage(g, counterBot, x, y + topHeight + midHeight, counterWidth, botHeight);

                // Draw scale: values array or range(min, max)
                for (int i = 0; i < c.getValues().length; i++) {
                    int yStep = y + topHeight + midHeight - stepSize * (i+1);
                    if (c.getValueIdx() >= i) {
                        // This step has been reached, paint background
                        BufferedImage bg = (BufferedImage) ImageIO.GetInstance().getImage(p.getImagePath().replace(".png", "-texture.png"));
                        TexturePaint paint = new TexturePaint(bg, new Rectangle.Float(0,0, bg.getWidth(), bg.getHeight()));
                        g.setPaint(paint);
//                        g.setColor(p.getColor());
                        g.fillRect(x + border, yStep, counterWidth - border*2 - 1, stepSize);
                        g.setColor(Color.black);
                        g.drawRect(x + border, yStep, counterWidth - border*2 - 1, stepSize);
                    }
                    String text = "" + c.getValues()[i];
                    // Get the FontMetrics
                    FontMetrics metrics = g.getFontMetrics(g.getFont());
                    // Determine the X coordinate for the text
                    int xText = x + (counterWidth - metrics.stringWidth(text)) / 2;
                    drawShadowString(g, text, xText, (int)(yStep + stepSize*0.8), Color.white, Color.black);
                }

                // Draw symbol
                Vector2D scaledDim = scaleLargestDimImg(symbol, counterWidth);
                g.drawImage(symbol, x + counterWidth/2 - scaledDim.getX()/2,
                        y + topHeight + midHeight + spacing/5,
                        scaledDim.getX(), scaledDim.getY(), null);

                // Draw +/- action symbols
                Rectangle rMinus = new Rectangle(x + border, y + topHeight + midHeight + botHeight/2 + spacing/2, fontSize, fontSize);
                Rectangle rPlus = new Rectangle(x + counterWidth - border - fontSize, y + topHeight + midHeight + botHeight/2 + spacing/2, fontSize, fontSize);
                drawShadowStringCentered(g, "-", rMinus, Color.red, null, 28);
                drawShadowStringCentered(g, "+", rPlus, new Color(44, 150, 43), null, 28);
                rects.put(rMinus, p.name() + "+");
                rects.put(rPlus, p.name() + "-");

                // Draw bonus related to this counter
                for (Bonus b: gs.getBonuses()) {
                    if (b.param == p) {
                        int yDisplay = y + topHeight + midHeight - stepSize * (b.threshold+1) + stepSize/2;
                        int displayHeight = 5;
                        int displayWidth = defaultItemSize/2 - spacing/2;
                        g.fillRect(x + counterWidth, yDisplay, displayWidth, displayHeight);

                        // Positioning
                        int size = defaultItemSize/2;
                        int imgX = x + counterWidth + displayWidth + spacing/5;
                        int imgY = yDisplay + displayHeight/2 - size/2;

                        // Find image to display, for a global counter, resource/production, or tile
                        TMAction effect = b.getEffect();
                        String imgPath = null;
                        // A resource or production?
                        if (effect instanceof ModifyPlayerResource) {
                            Image resImg = ImageIO.GetInstance().getImage(((ModifyPlayerResource) effect).resource.getImagePath());
                            drawResource(g, resImg, production, ((ModifyPlayerResource) effect).production, imgX, imgY, size, 0.8);
                        }
                        else if (effect instanceof PlaceTile) {
                            // A tile to place?
                            imgPath = ((PlaceTile) effect).tile.getImagePath();
                        } else if (effect instanceof ModifyGlobalParameter) {
                            imgPath = ((ModifyGlobalParameter) effect).param.getImagePath();
                        }

                        if (imgPath != null) {
                            Image image = ImageIO.GetInstance().getImage(imgPath);
                            drawImage(g, image, imgX, imgY, size);
                        }
                    }
                }
            }
        }

        return rect;
    }

    public ArrayList<Rectangle> getHighlight() {
        return highlight;
    }

    public void update(TMGameState gs) {
        this.gs = gs;
    }

    @Override
    public void clearHighlights() {
        highlight.clear();
    }
}
