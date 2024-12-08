package games.puertorico.gui;

import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.actions.BuildQuarry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import static games.puertorico.PuertoRicoConstants.BuildingType.CONSTRUCTION_HUT;
import static games.puertorico.gui.PRGUIUtils.*;

public class GSInfo extends JComponent {
    PuertoRicoGameState gs;
    PuertoRicoGUI gui;
    Dimension size = new Dimension(300, 80);
    Map<Rectangle, PuertoRicoConstants.Crop> cropToRectMap = new HashMap<>();
    Rectangle colonistIcon;

    public GSInfo(PuertoRicoGUI gui, PuertoRicoGameState gs) {
        this.gs = gs;
        this.gui = gui;
        ToolTipManager.sharedInstance().registerComponent(this);

        FontMetrics fm = getFontMetrics(textFontBold);
        int fontSize = textFontBold.getSize();
        String s1 = "Crop Supply: ";
        int x = pad + fm.stringWidth(s1);
        for (PuertoRicoConstants.Crop crop: PuertoRicoConstants.Crop.values()) {
            int n;
            if (crop == PuertoRicoConstants.Crop.QUARRY) n = gs.getQuarriesLeft();
            else n = gs.getSupplyOf(crop);
            s1 = n + " ";
            x += fm.stringWidth(s1);
            cropToRectMap.put(new Rectangle(x, pad, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), crop);
            x += PRGUIUtils.barrelWidth + pad*2;
        }
        colonistIcon = new Rectangle(pad, fontSize + pad*2, PRGUIUtils.colonistRadius, PRGUIUtils.colonistRadius);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gs.getCurrentRole() == PuertoRicoConstants.Role.SETTLER
                        && gui.getHumanPlayerIds().contains(gs.getCurrentPlayer())
                        && gs.getQuarriesLeft() > 0 && (gs.getCurrentPlayer() == gs.getRoleOwner() || gs.hasActiveBuilding(gs.getCurrentPlayer(), CONSTRUCTION_HUT))) {
                    gui.getAC().addAction(new BuildQuarry());
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics gg) {
        // crop supply + colonists in supply + colonists on ship + VP supply
        Graphics2D g = (Graphics2D)gg;
        g.setFont(textFontBold);
        int fontSize = g.getFont().getSize();
        FontMetrics fm = g.getFontMetrics();

        String s1 = "Crop Supply: ";
        g.drawString(s1, pad, pad + fontSize);
        int x = pad + fm.stringWidth(s1);
        for (PuertoRicoConstants.Crop crop: PuertoRicoConstants.Crop.values()) {
            int n;
            if (crop == PuertoRicoConstants.Crop.QUARRY) n = gs.getQuarriesLeft();
            else n = gs.getSupplyOf(crop);
            s1 = n + " ";
            g.drawString(s1, x, pad + fontSize);
            x += fm.stringWidth(s1);
            if (crop == PuertoRicoConstants.Crop.QUARRY) {
                PRGUIUtils.drawHexagon(g, x, pad + barrelHeight/2 - barrelWidth/2, PRGUIUtils.cropColorMap.get(crop), barrelWidth, barrelWidth);
                if (gs.getCurrentRole() == PuertoRicoConstants.Role.SETTLER
                        && gui.getHumanPlayerIds().contains(gs.getCurrentPlayer())
                        && gs.getQuarriesLeft() > 0 && (gs.getCurrentPlayer() == gs.getRoleOwner() || gs.hasActiveBuilding(gs.getCurrentPlayer(), CONSTRUCTION_HUT))) {
                    g.setColor(highlightColor);
                    Stroke s = g.getStroke();
                    g.setStroke(new BasicStroke(2));
                    g.drawOval(x, pad, barrelWidth, barrelHeight);
                    g.setStroke(s);
                }
            } else {
                PRGUIUtils.drawBarrel(g, x, pad, PRGUIUtils.cropColorMap.get(crop));
            }
            x += PRGUIUtils.barrelWidth + pad*2;
            g.setColor(Color.black);
        }

        String s2 = " : " + gs.getColonistsInSupply() + " (" + gs.getColonistsOnShip() + " on ship)";
        PRGUIUtils.drawColonist(g, pad, fontSize + pad*2);
        g.drawString(s2, pad*2 + PRGUIUtils.colonistRadius, fontSize*2 + pad*2);

        g.setColor(secondaryColor);
        String s3 = "VP Supply: " + gs.getVPSupply();
        g.drawString(s3, pad, fontSize*2 + PRGUIUtils.colonistRadius + pad*3);
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
        if (showTooltips) {
            for (Map.Entry<Rectangle, PuertoRicoConstants.Crop> e : cropToRectMap.entrySet()) {
                if (e.getKey().contains(event.getPoint())) {
                    return e.getValue().name();
                }
            }
            if (colonistIcon.contains(event.getPoint())) {
                return "Colonists";
            }
        }
        return super.getToolTipText(event);
    }
}
