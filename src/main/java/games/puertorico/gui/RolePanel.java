package games.puertorico.gui;

import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;

import javax.swing.*;
import java.awt.*;

import static games.puertorico.gui.PRGUIUtils.capitalize;

public class RolePanel extends JComponent {
    PuertoRicoGameState gs;
    Dimension size = new Dimension(150, 25);
    int pad = 4;

    public RolePanel(PuertoRicoGameState gs) {
        this.gs = gs;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // roles available + money

        int i = 0;
        for (PuertoRicoConstants.Role r: PuertoRicoConstants.Role.values()) {
            if (r == PuertoRicoConstants.Role.DISCARD) continue;
            boolean available = gs.isRoleAvailable(r);
            int money = gs.getMoneyOnRole(r);
            g.setFont(PRGUIUtils.roleFontAvailable);
            g.setColor(Color.black);
            g.drawString("(" + money + ")", pad, PRGUIUtils.roleFontSize *(i+1));

            if (available) {
                g.setColor(PRGUIUtils.secondaryColor);
            } else {
                g.setFont(PRGUIUtils.roleFontNotAvailable);
                g.setColor(PRGUIUtils.secondaryColorFaint);
            }
            String name = r.name();
            g.drawString(capitalize(name), pad + PRGUIUtils.roleFontSize *2, PRGUIUtils.roleFontSize *(i+1));

            i++;
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
