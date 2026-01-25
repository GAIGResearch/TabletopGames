package games.terraformingmars.gui;

import core.components.GridBoard;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMMapTile;
import gui.views.ComponentView;
import utilities.ImageIO;
import evaluation.summarisers.TAGOccurrenceStatSummary;

import java.awt.*;
import static utilities.GUIUtils.*;

public class TMBoardHeatMap extends ComponentView {

    TMGameState gs;

    Image background;
    Color mapTileBackground = new Color(97, 97, 97, 70);
    int spacing = 10;

    int tileSize;
    TAGOccurrenceStatSummary stats;
    int minCount = 0, maxCount = 0, nGames;

    public TMBoardHeatMap(TMGameState gs, TAGOccurrenceStatSummary stats, int nGames) {
        super(gs.getBoard(), 0, 0);
        this.gs = gs;
        this.stats = stats;
        for (int count: stats.getElements().values()) {
            if (count < minCount) minCount = count;
            if (count > maxCount) maxCount = count;
        }
        this.nGames = nGames;

        width = 200;
        height = 200;
        tileSize = Math.min(width/gs.getBoard().getWidth(), height/(gs.getBoard().getHeight()+2));

        background = ImageIO.GetInstance().getImage("data/terraformingmars/images/mars.png");
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        GridBoard gridBoard = gs.getBoard();
        int x = width/2 - gridBoard.getWidth() * tileSize/2;
        int y = tileSize;

        // Board background
        int width = gridBoard.getWidth() * tileSize;
        int height = gridBoard.getHeight() * tileSize;
        drawImage(g, background, x, y, width, height);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                int offsetX = tileSize/2;
                if (i % 2 == 1) {
                    offsetX += tileSize/2;
                }

                int xC = x + offsetX + j * tileSize;
                int yC = y + i * tileSize;
                drawCell(g, (TMMapTile) gridBoard.getElement(j, i), xC, yC);

                // Heatmap
                String toSearch = "(" + j + "-" + i + ")";
                if (stats.getElements().containsKey(toSearch)) {
                    int count = stats.getElements().get(toSearch);
                    double perc = count * 1.0 / nGames;
                    g.setColor(new Color(161, 64, 245, 30 + (int) (perc * 195)));
                    Polygon h = new Polygon();
                    for (int k = 0; k < 6; k++) {
                        h.addPoint((int) (xC + tileSize / 2 * Math.cos(Math.PI / 2 + k * 2 * Math.PI / 6)),
                                (int) (yC + tileSize / 2 * Math.sin(Math.PI / 2 + k * 2 * Math.PI / 6)));
                    }
                    g.fillPolygon(h);
                }
            }
        }

        // Draw extra cells
        int yC = y + spacing + height;
        int xC = x + Math.max(0, width/2 - gs.getExtraTiles().size() * tileSize/2);
        int i = 0;
        for (TMMapTile mt: gs.getExtraTiles()) {
            drawCell(g, mt, xC, yC);

            // Heatmap
            if (stats.getElements().containsKey(mt.getComponentName())) {
                int count = stats.getElements().get(mt.getComponentName());
                double perc = count * 1.0 / nGames;
                g.setColor(new Color(161, 64, 245, 30 + (int) (perc * 195)));
                Polygon h = new Polygon();
                for (int k = 0; k < 6; k++) {
                    h.addPoint((int) (xC + tileSize / 2 * Math.cos(Math.PI / 2 + k * 2 * Math.PI / 6)),
                            (int) (yC + tileSize / 2 * Math.sin(Math.PI / 2 + k * 2 * Math.PI / 6)));
                }
                g.fillPolygon(h);
            }

            i++;
            xC += tileSize * 2;
            if (xC + tileSize > x + width) {
                xC = x + Math.max(0, width/2 - (gs.getExtraTiles().size()-i) * tileSize / 2 + tileSize/2);
                yC += tileSize;
            }
        }

        new Rectangle(x, y, width, height);
    }

    private void drawCell(Graphics2D g, TMMapTile element, int x, int y) {
        if (element != null) {
            // Create hexagon
            Polygon h = new Polygon();
            for (int i = 0; i < 6; i++) {
                h.addPoint((int) (x + tileSize/2 * Math.cos(Math.PI/2 + i * 2 * Math.PI / 6)),
                        (int) (y + tileSize/2 * Math.sin(Math.PI/2 + i * 2 * Math.PI / 6)));
            }

            g.setColor(mapTileBackground);
            g.fillPolygon(h);

            g.setColor(element.getTileType().getOutlineColor());
            g.drawPolygon(h);

            // Draw resources
            boolean drew = drawElementName(g, x, y, element);
            TMTypes.Resource[] resources = element.getResources();
            int resSize = tileSize/4;
            for (int i = 0; i < resources.length; i++) {
                if (resources[i] != null) {
                    int yDraw = y + i * resSize - tileSize / 3;
                    int xDraw = x + i * resSize - tileSize / 3;
                    if (drew) {
                        yDraw = y - tileSize / 3;
                        xDraw = x + i * resSize + i * 2 - tileSize / 3;
                    }
                    drawImage(g, resources[i].getImagePath(), xDraw, yDraw, resSize, resSize);
                }
            }
        }
    }

    private boolean drawElementName(Graphics2D g, int x, int y, TMMapTile element) {
        if (element.getTileType() == TMTypes.MapTileType.City) {
            // Draw city name
            drawShadowStringCentered(g, element.getComponentName(),
                    new Rectangle(x-tileSize/2, y-tileSize/2, tileSize, tileSize),
                    TMGUI.lightGrayColor, null, 10);
            return true;
        }
        return false;
    }
}
