package gui.views;

import core.AbstractGameState;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.properties.PropertyBoolean;
import core.properties.PropertyColor;
import core.properties.PropertyString;
import core.properties.PropertyVector2D;
import utilities.Utils;
import utilities.Vector2D;

import java.awt.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static gui.GUI.defaultItemSize;
import static core.CoreConstants.*;
import static core.CoreConstants.sizeHash;
import static games.pandemic.PandemicConstants.*;

public class GraphBoardView extends ComponentView {
    AbstractGameState gs;

    public GraphBoardView(AbstractGameState gs, GraphBoard board, int width, int height) {
        super(board, width, height);
        this.gs = gs;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGraphBoard((Graphics2D)g, gs, (GraphBoard) component, 0, 0, width, height);
    }

    public static void drawGraphBoard(Graphics2D g, AbstractGameState gs, GraphBoard graphBoard, int x, int y, int width, int height) {
        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        double scaleW = 1.0;
        double scaleH = 1.0;
        PropertyVector2D boardSizeProp = (PropertyVector2D)graphBoard.getProperty(sizeHash);
        if (boardSizeProp != null) {
            Vector2D boardSize = boardSizeProp.values;
            scaleW = boardSize.getX()*1.0/width;
            scaleH = boardSize.getY()*1.0/height;
        }

        // Draw connections
        Collection<BoardNode> bList = graphBoard.getBoardNodes();
        for (BoardNode b: bList) {
            PropertyVector2D posProp = (PropertyVector2D) b.getProperty(coordinateHash);
            if (posProp != null) {
                Vector2D poss = posProp.values;
                Vector2D pos = new Vector2D((int) (poss.getX() * scaleW), (int) (poss.getY() * scaleH));
                PropertyBoolean edge = ((PropertyBoolean) b.getProperty(edgeHash));

                Set<BoardNode> neighbours = b.getNeighbours().keySet();
                for (BoardNode b2 : neighbours) {
                    PropertyVector2D posProp2 = (PropertyVector2D) b2.getProperty(coordinateHash);
                    if (posProp2 != null) {
                        Vector2D poss2 = posProp2.values;
                        Vector2D pos2 = new Vector2D((int) (poss2.getX() * scaleW), (int) (poss2.getY() * scaleH));
                        PropertyBoolean edge2 = ((PropertyBoolean) b2.getProperty(edgeHash));

                        if (edge != null && edge.value && edge2 != null && edge2.value) {
                            // Two edge nodes connected check if on opposite sides, draw connection as if b2 on the other side of map
                            if (pos2.getX() < width / 2 && pos.getX() > width / 2 || pos2.getX() > width / 2 && pos.getX() < width / 2) {
                                if (pos2.getX() > pos.getX()) pos2.setX(pos2.getX() - width);
                                else pos2.setX(width + pos2.getX());
                            }
                        }
                        g.setColor(Color.white);
                        g.drawLine(pos.getX(), pos.getY(), pos2.getX(), pos2.getY());
                    }
                }

                g.setColor(Color.black);
                g.drawString(((PropertyString) b.getProperty(nameHash)).value, pos.getX(), pos.getY() - defaultItemSize / 2);
            }
        }

        // Draw board nodes
        for (BoardNode bn: graphBoard.getBoardNodes()) {
            PropertyVector2D posProp = (PropertyVector2D) bn.getProperty(coordinateHash);
            if (posProp != null) {
                Vector2D poss = posProp.values;
                Vector2D pos = new Vector2D((int) (poss.getX() * scaleW), (int) (poss.getY() * scaleH));

                PropertyColor colorProp = (PropertyColor) bn.getProperty(colorHash);
                if (colorProp != null) {
                    g.setColor(Utils.stringToColor(colorProp.valueStr));
                }
                g.fillOval(pos.getX() - defaultItemSize / 2, pos.getY() - defaultItemSize / 2, defaultItemSize, defaultItemSize);
                g.setColor(new Color(30, 108, 47));
                g.drawOval(pos.getX() - defaultItemSize / 2, pos.getY() - defaultItemSize / 2, defaultItemSize, defaultItemSize);
                g.setColor(Color.black);
            }
        }
    }

    public static void drawGraphBoard(Graphics2D g, AbstractGameState gs, GraphBoard graphBoard, Rectangle rect) {
        drawGraphBoard(g, gs, graphBoard, rect.x, rect.y, rect.width, rect.height);
    }

}
