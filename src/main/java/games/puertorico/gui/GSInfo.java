package games.puertorico.gui;

import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;

import javax.swing.*;
import java.awt.*;

import static games.puertorico.gui.PRGUIUtils.*;

public class GSInfo extends JComponent {
    PuertoRicoGameState gs;
    Dimension size = new Dimension(300, 80);

    public GSInfo(PuertoRicoGameState gs) {
        this.gs = gs;
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
            PRGUIUtils.drawBarrel(g, x, pad, PRGUIUtils.cropColorMap.get(crop));
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
}
