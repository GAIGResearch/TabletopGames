package games.puertorico.gui;

import games.puertorico.actions.BuildQuarry;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import static games.puertorico.PuertoRicoConstants.BuildingType.CONSTRUCTION_HUT;

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

        FontMetrics fm = getFontMetrics(PRGUIUtils.textFontBold);
        int fontSize = PRGUIUtils.textFontBold.getSize();
        String s1 = "Crop Supply: ";
        int x = PRGUIUtils.pad + fm.stringWidth(s1);
        for (PuertoRicoConstants.Crop crop: PuertoRicoConstants.Crop.values()) {
            int n;
            if (crop == PuertoRicoConstants.Crop.QUARRY) n = gs.getQuarriesLeft();
            else n = gs.getSupplyOf(crop);
            s1 = n + " ";
            x += fm.stringWidth(s1);
            cropToRectMap.put(new Rectangle(x, PRGUIUtils.pad, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight), crop);
            x += PRGUIUtils.barrelWidth + PRGUIUtils.pad*2;
        }
        colonistIcon = new Rectangle(PRGUIUtils.pad, fontSize + PRGUIUtils.pad*2, PRGUIUtils.colonistRadius, PRGUIUtils.colonistRadius);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (gs.getCurrentRole() == PuertoRicoConstants.Role.SETTLER
                        && gui.getHumanPlayerId().contains(gs.getCurrentPlayer())
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
        g.setFont(PRGUIUtils.textFontBold);
        int fontSize = g.getFont().getSize();
        FontMetrics fm = g.getFontMetrics();

        String s1 = "Crop Supply: ";
        g.drawString(s1, PRGUIUtils.pad, PRGUIUtils.pad + fontSize);
        int x = PRGUIUtils.pad + fm.stringWidth(s1);
        for (PuertoRicoConstants.Crop crop: PuertoRicoConstants.Crop.values()) {
            int n;
            if (crop == PuertoRicoConstants.Crop.QUARRY) n = gs.getQuarriesLeft();
            else n = gs.getSupplyOf(crop);
            s1 = n + " ";
            g.drawString(s1, x, PRGUIUtils.pad + fontSize);
            x += fm.stringWidth(s1);
            if (crop == PuertoRicoConstants.Crop.QUARRY) {
                PRGUIUtils.drawHexagon(g, x, PRGUIUtils.pad + PRGUIUtils.barrelHeight/2 - PRGUIUtils.barrelWidth/2, PRGUIUtils.cropColorMap.get(crop), PRGUIUtils.barrelWidth, PRGUIUtils.barrelWidth);
                if (gs.getCurrentRole() == PuertoRicoConstants.Role.SETTLER
                        && gui.getHumanPlayerId().contains(gs.getCurrentPlayer())
                        && gs.getQuarriesLeft() > 0 && (gs.getCurrentPlayer() == gs.getRoleOwner() || gs.hasActiveBuilding(gs.getCurrentPlayer(), CONSTRUCTION_HUT))) {
                    g.setColor(PRGUIUtils.highlightColor);
                    Stroke s = g.getStroke();
                    g.setStroke(new BasicStroke(2));
                    g.drawOval(x, PRGUIUtils.pad, PRGUIUtils.barrelWidth, PRGUIUtils.barrelHeight);
                    g.setStroke(s);
                }
            } else {
                PRGUIUtils.drawBarrel(g, x, PRGUIUtils.pad, PRGUIUtils.cropColorMap.get(crop));
            }
            x += PRGUIUtils.barrelWidth + PRGUIUtils.pad*2;
            g.setColor(Color.black);
        }

        String s2 = " : " + gs.getColonistsInSupply() + " (" + gs.getColonistsOnShip() + " on ship)";
        PRGUIUtils.drawColonist(g, PRGUIUtils.pad, fontSize + PRGUIUtils.pad*2);
        g.drawString(s2, PRGUIUtils.pad*2 + PRGUIUtils.colonistRadius, fontSize*2 + PRGUIUtils.pad*2);

        g.setColor(PRGUIUtils.secondaryColor);
        String s3 = "VP Supply: " + gs.getVPSupply();
        g.drawString(s3, PRGUIUtils.pad, fontSize*2 + PRGUIUtils.colonistRadius + PRGUIUtils.pad*3);
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
        if (PRGUIUtils.showTooltips) {
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
