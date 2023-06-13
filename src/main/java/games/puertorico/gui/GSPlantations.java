package games.puertorico.gui;

import games.puertorico.PuertoRicoGameState;
import games.puertorico.components.Plantation;

import javax.swing.*;
import java.awt.*;

import static games.puertorico.gui.PRGUIUtils.*;

public class GSPlantations extends JComponent {
    PuertoRicoGameState gs;
    Dimension size = new Dimension(300, plantationSize + pad*2);

    public GSPlantations(PuertoRicoGameState gs) {
        this.gs = gs;
    }

    @Override
    protected void paintComponent(Graphics gg) {
        // Visible plantations + how many left in deck + how many discount things left
        Graphics2D g = (Graphics2D) gg;
        g.setFont(PRGUIUtils.textFontBold);
        g.setColor(Color.black);
        FontMetrics fm = g.getFontMetrics();
        String s1 = "Plantations left: " + gs.numberOfPlantationsInStack();
        g.drawString(s1, pad, pad + textFontSize);
//        String s2 = "Quarries left: " + gs.numberOfQuarries();  // TODO: not in state?
//        g.drawString(s2, pad, pad*2 + textFontSize*2);
//        int x = Math.max(fm.stringWidth(s1), fm.stringWidth(s2)) + pad;

        int x = fm.stringWidth(s1) + pad*4;
        for (Plantation p: gs.getAvailablePlantations().getComponents()) {
            drawPlantation(g, p, x, pad);
            x += plantationSize + pad/2;
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
}
