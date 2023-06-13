package games.puertorico.gui;

import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.components.Building;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static games.puertorico.gui.PRGUIUtils.*;

public class GSBuildings extends JComponent {
    PuertoRicoGameState gs;
    Set<Integer> discounts = PuertoRicoConstants.BuildingType.getQuarryDiscounts();
    Dimension size = new Dimension(pad*2+(buildingWidth + pad)*(discounts.size()), 300);
    Map<Rectangle, Building> buildingRectMap = new HashMap<>();

    public GSBuildings(PuertoRicoGameState gs) {
        this.gs = gs;
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    protected void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D) gg;

        // Buildings available for purchase
        int col = 0;
        FontMetrics fm = g.getFontMetrics();
        for (int discount : discounts) {
            int x = pad + col * (buildingWidth + pad);
            List<PuertoRicoConstants.BuildingType> buildings = PuertoRicoConstants.BuildingType.getBuildingTypesWithQuarryDiscount(discount);
            int y = pad;

            // Draw discount at top of column
            g.setColor(secondaryColor);
            g.setFont(PRGUIUtils.textFontBold);
            g.drawString(String.valueOf(discount), x + buildingWidth/2 - fm.stringWidth(String.valueOf(discount))/2, y + textFontSize);
            g.setColor(Color.black);
            y += textFontSize + pad;

            // Draw buildings
            for (PuertoRicoConstants.BuildingType buildingType: buildings) {
                int nAvailable = gs.getBuildingsOfType(buildingType);  // TODO draw n available
                Building b = null;
                if (nAvailable > 0) b = buildingType.instantiate();
                drawBuilding(g, b, x, y, nAvailable);
                buildingRectMap.put(new Rectangle(x, y, buildingWidth, buildingHeight), b);
                y += buildingHeight;
            }
            col++;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }
    @Override
    public Dimension getMinimumSize() {
        return size;
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
}
