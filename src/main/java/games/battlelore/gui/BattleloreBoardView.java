package games.battlelore.gui;

import core.components.GridBoard;
import games.battlelore.BattleloreGameState;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;
import gui.views.ComponentView;
import utilities.ImageIO;
import java.awt.*;
import java.awt.font.TextLayout;
import static gui.GUI.defaultItemSize;

public class BattleloreBoardView extends ComponentView {
    BattleloreGameState gameState;
    Image background;

    public BattleloreBoardView(GridBoard hexBoard) {
        super(hexBoard, hexBoard.getWidth() * defaultItemSize, hexBoard.getHeight() * defaultItemSize);
        background = ImageIO.GetInstance().getImage("data/battlelore/board.png");
    }

    public void drawGridBoard(Graphics2D g, GridBoard gridBoard, int x, int y) {
        int offSetX = 25;
        int offSetY = 60;

        // Draw hexes
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; ((i % 2 == 0 && j < gridBoard.getWidth()) || (i % 2 == 1 && j < gridBoard.getWidth()-1)); j++) {

                int xC = x + 35 + j * defaultItemSize;
                int yC = (int) (y + offSetY + i * Math.round(defaultItemSize / 1.2));

                if (i % 2 == 1) {
                    xC += offSetX;
                }

                drawCell(g, (MapTile) gridBoard.getElement(j, i), xC, yC);
            }
        }

        // Draw the rect perimeter
        g.drawLine(10, 31, 610, 31); //top
        g.drawLine(10, 423, 610, 423);//down
        g.drawLine(10, 31, 10, 423); //left
        g.drawLine(610, 31, 610, 423);//right

        //Draw inline
        g.setColor(Color.red);
        g.drawLine(209, 31, 209, 423);
        g.drawLine(409, 31, 409, 423);
        g.setColor(Color.black);

    }

    public static void drawImage(Graphics2D g, Image background, int x, int y, int width, int height) {
        int w = background.getWidth(null);
        int h = background.getHeight(null);
        double scaleW = width * 1.0/w;
        double scaleH = height * 1.0/h;
        g.drawImage(background, x, y, (int) (w*scaleW), (int) (h*scaleH), null);
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        drawGridBoard(g, (GridBoard) component, 0, 0);
    }

    private void drawCell(Graphics2D g, MapTile tile, int x, int y) {
        // Paint element in cell
        if (tile != null) {
            // Create a hexagon
            Polygon h = new Polygon();
            for (int i = 0; i < 6; i++) {
                h.addPoint((int) (x + defaultItemSize/1.75 * Math.cos(Math.PI/2 + i * 2 * Math.PI / 6)),
                        (int) (y + defaultItemSize/1.75 * Math.sin(Math.PI/2 + i * 2 * Math.PI / 6)));
            }

            g.setColor(Color.green);
            g.fillPolygon(h);
            g.setColor(Color.white);
            g.drawPolygon(h);

            drawElementName(g,x - defaultItemSize/3, y, tile);
        }
    }

    private boolean drawElementName(Graphics2D g, int x, int y, MapTile element) {
            Color factionColor = Color.black;

            if (element.GetFaction() == Unit.Faction.Uthuk_Yllan)
            {
                factionColor = Color.red;
            }
            else if (element.GetFaction() == Unit.Faction.Dakhan_Lords)
            {
                factionColor = Color.blue;
            }

            drawShadowString(g, element.GetUnitNames(), x, y, factionColor, Color.white);
            return true;
    }

    public static void drawShadowString(Graphics2D g, String text, int x, int y, Color color, Color shadow) {
        TextLayout textLayout = new TextLayout(text, g.getFont(), g.getFontRenderContext());

        if (shadow == null) {
            shadow = Color.black;
        }

        g.setPaint(shadow);
        textLayout.draw(g, x, y);

        if (color == null) {
            color = Color.white;  // white default
        }

        g.setPaint(color);
        textLayout.draw(g, x, y);
    }
}
