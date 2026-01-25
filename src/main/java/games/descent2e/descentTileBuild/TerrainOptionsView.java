package games.descent2e.descentTileBuild;

import games.descent2e.DescentTypes;
import games.descent2e.gui.DescentGridBoardView;
import gui.IScreenHighlight;
import utilities.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static gui.AbstractGUIManager.defaultItemSize;

public class TerrainOptionsView extends JComponent implements IScreenHighlight {

    static int inARow = 2;
    static HashSet<String> terrains;

    HashMap<String, Rectangle> terrainLocations;
    Pair<String, Rectangle> highlight;

    public TerrainOptionsView() {
        terrains = DescentTypes.TerrainType.getWalkableStringTerrains();
        terrains.add("open");
        terrains.add("block");
        terrains.add("null");
        terrainLocations = new HashMap<>();

        int i = 0;
        for (String t: terrains) {
            int xC = i % 2 * defaultItemSize;
            int yC = i / 2 * defaultItemSize;
            i++;
            terrainLocations.put(t, new Rectangle(xC, yC, defaultItemSize, defaultItemSize));
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    for (Map.Entry<String, Rectangle> t : terrainLocations.entrySet()) {
                        if (t.getValue().contains(e.getPoint())) {
                            highlight = new Pair<>(t.getKey(), t.getValue());
                            break;
                        }
                    }
                } else highlight = null;
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        for (Map.Entry<String, Rectangle> t: terrainLocations.entrySet()) {
            int xC = t.getValue().x;
            int yC = t.getValue().y;

            // Paint cell background
            Color bg = DescentGridBoardView.colorMap.get(t.getKey());
            g.setColor(bg);
            g.fillRect(xC, yC, defaultItemSize, defaultItemSize);
            g.setColor(Color.black);
            g.drawRect(xC, yC, defaultItemSize, defaultItemSize);

            if (bg != null && (bg.equals(Color.black) || bg.equals(Color.blue) || bg.equals(Color.darkGray))) {
                g.setColor(Color.white);
            }
            g.drawString(t.getKey(), xC + 10, yC + defaultItemSize/2);
        }

        // Highlight
        if (highlight != null) {
            g.setColor(Color.cyan);
            Stroke s = ((Graphics2D) g).getStroke();
            ((Graphics2D) g).setStroke(new BasicStroke(3));
            g.drawRect(highlight.b.x - 1, highlight.b.y - 1, highlight.b.width + 2, highlight.b.height + 2);
            ((Graphics2D) g).setStroke(s);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(inARow * defaultItemSize, (terrains.size()/inARow+1) * defaultItemSize);
    }

    @Override
    public void clearHighlights() {
        highlight = null;
    }
}
