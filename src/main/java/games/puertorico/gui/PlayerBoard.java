package games.puertorico.gui;

import games.puertorico.PRPlayerBoard;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.PuertoRicoParameters;
import games.puertorico.components.Building;
import games.puertorico.components.Plantation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import static games.puertorico.gui.PRGUIUtils.*;

public class PlayerBoard extends JComponent {
    PuertoRicoGameState gs;
    PRPlayerBoard playerBoard;
    int playerId;
    Dimension size = new Dimension(250, 315);
    int borderPadY = 20, borderPadX = 5;
    int nPlantationSlots, nTownSlots, townGridWidth, townGridHeight;
    int[][] townGrid;
    Map<Rectangle, Building> buildingRectMap = new HashMap<>();

    public PlayerBoard(PuertoRicoGameState gs, int playerId) {
        this.gs = gs;
        this.playerId = playerId;
        this.playerBoard = gs.getPlayerBoard(playerId);
        this.nPlantationSlots = ((PuertoRicoParameters)gs.getGameParameters()).plantationSlotsOnBoard;
        this.nTownSlots = ((PuertoRicoParameters)gs.getGameParameters()).townSlotsOnBoard;
        this.townGridWidth = ((PuertoRicoParameters)gs.getGameParameters()).townGridWidth;
        this.townGridHeight = ((PuertoRicoParameters)gs.getGameParameters()).townGridHeight;
        this.townGrid = new int[townGridHeight][townGridWidth];

        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    protected void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D) gg;

        int width = size.width-borderPadX*2;
        int height = size.height-borderPadY;
        g.drawRect(borderPadX, borderPadY, width, height);
        g.setFont(PRGUIUtils.textFontBold);
        FontMetrics fm = g.getFontMetrics();

        // VP, doubloons, # colonists, crops
        g.setColor(secondaryColor);
        g.drawString("VP: " + playerBoard.getVP(), pad + borderPadX, textFontSize + pad + borderPadY);
        g.setColor(Color.black);
        g.drawString("Doubloons: " + playerBoard.getDoubloons(), pad + borderPadX, textFontSize*2 + pad*2 + borderPadY);
        g.drawString("Colonists: " + playerBoard.getUnassignedColonists(), pad + borderPadX, textFontSize*3 + pad*3 + borderPadY);

        // Crops in supply
        int x = pad + borderPadX;
        int y = textFontSize*4 + pad*6 + borderPadY;
        for (PuertoRicoConstants.Crop crop: PuertoRicoConstants.Crop.values()) {
            int n = playerBoard.getStoresOf(crop);
            String s1 = n + " ";
            g.drawString(s1, x, y);
            x += fm.stringWidth(s1);
            PRGUIUtils.drawBarrel(g, x, y-textFontSize, PRGUIUtils.cropColorMap.get(crop));
            x += PRGUIUtils.barrelWidth + PRGUIUtils.pad*2;
            g.setColor(Color.black);
        }

        // Buildings

        // Adjust building slot width and height if needed
        if (width / townGridWidth < buildingWidth) {
            buildingWidth = width / townGridWidth;
        }
        if (height / townGridHeight < buildingHeight) {
            buildingHeight = height / townGridHeight;
        }

        TreeMap<Integer, List<Integer>> sizeToBuildings = new TreeMap<>();
        for (int i = 0; i < playerBoard.getBuildings().size(); i++) {
            Building b = playerBoard.getBuildings().get(i);
            if (!sizeToBuildings.containsKey(b.buildingType.size)) {
                sizeToBuildings.put(b.buildingType.size, new ArrayList<>());
            }
            sizeToBuildings.get(b.buildingType.size).add(i);
        }

        // Place buildings in town grid, starting from the largest
        for (int[] row: townGrid) {
            Arrays.fill(row, -1);
        }
        int i = 0;
        int j = 0;
        for (int size: sizeToBuildings.descendingKeySet()) {
            List<Integer> buildings = sizeToBuildings.get(size);
            for (int b: buildings) {
                // Find an empty spot to start drawing this building from
                boolean fail = false;
                while (townGrid[i][j] != -1) {
                    j++;
                    if (j % townGridWidth == 0) {
                        i++;
                        j = 0;
                    }
                    if (i >= townGridHeight) {
                        fail = true;
                        break;
                    }
                }
                if (fail) {
                    System.out.println("fail");
                    break;
                }
                for (int k = 0; k < size; k++) {
                    townGrid[i+k][j] = b;
                }
                j++;
                if (j % townGridWidth == 0) {
                    i += size;
                    j = 0;
                }
            }
        }

  //      int y = textFontSize*4 + pad*4 + borderPadY;
        // Draw the town grid
        y = textFontSize*4 + pad*6 + barrelHeight + borderPadY;
        for (i = 0; i < townGridHeight; i++) {
            x = borderPadX;
            for (j = 0; j < townGridWidth; j++) {
                if (townGrid[i][j] != -1 && i > 0 && townGrid[i-1][j] == townGrid[i][j]) {
                    // Skip this square
                    x += buildingWidth;
                    continue;
                }
                Building b = null;
                int h = buildingHeight;
                if (townGrid[i][j] != -1) {
                    b = playerBoard.getBuildings().get(townGrid[i][j]);
                    h *= b.buildingType.size;
                }
                drawBuilding(g, b, x, y, 0);
                buildingRectMap.put(new Rectangle(x, y, buildingWidth, h), b);
                x += buildingWidth;
            }
            y += buildingHeight;
        }

        // Plantations
        y += pad - plantationSize;
        int nColsP = width / plantationSize;
        x = borderPadX;
        for (i = 0; i < nPlantationSlots; i++) {
            if (i % nColsP == 0) {
                x = borderPadX;
                y += plantationSize;
            }
            Plantation p = null;
            if (i < playerBoard.getPlantations().size()) {
                p = playerBoard.getPlantations().get(i);
            }
            drawPlantation(g, p, x, y);
            x += plantationSize;
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        for (Rectangle r: buildingRectMap.keySet()) {
            if (r.contains(event.getPoint())) {
                Building b = buildingRectMap.get(r);
                if (b != null) {
                    return b.buildingType.tooltip;
                }
            }
        }
        return super.getToolTipText(event);
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }
}
