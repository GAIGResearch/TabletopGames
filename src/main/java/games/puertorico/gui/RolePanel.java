package games.puertorico.gui;

import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.actions.SelectRole;
import gui.IScreenHighlight;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import static games.puertorico.gui.PRGUIUtils.*;

public class RolePanel extends JComponent implements IScreenHighlight {
    PuertoRicoGameState gs;
    PuertoRicoGUI gui;

    Dimension size = new Dimension(150, 25);
    Map<Rectangle, PuertoRicoConstants.Role> rectangleRoleMap = new HashMap<>();
    Rectangle hover = null;
    PuertoRicoConstants.Role selected = null;

    public RolePanel(PuertoRicoGUI gui, PuertoRicoGameState gs) {
        this.gui = gui;
        this.gs = gs;
        ToolTipManager.sharedInstance().registerComponent(this);

        this.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                for (Map.Entry<Rectangle, PuertoRicoConstants.Role> entry: rectangleRoleMap.entrySet()) {
                    if (entry.getKey().contains(e.getPoint())) {
                        hover = entry.getKey();
                        break;
                    }
                }
            }
        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                PuertoRicoConstants.Role activeRole = gs.getCurrentRole();
                if (activeRole != null && !gui.getHumanPlayerIds().contains(gs.getCurrentPlayer())) return;

                for (Map.Entry<Rectangle, PuertoRicoConstants.Role> entry: rectangleRoleMap.entrySet()) {
                    if (entry.getKey().contains(e.getPoint()) && gs.isRoleAvailable(entry.getValue())) {
                        selected = entry.getValue();
                        gui.getAC().addAction(new SelectRole(selected));
                        break;
                    }
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                hover = null;
            }
        });
    }

    @Override
    protected void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D) gg;
        // roles available + money
        rectangleRoleMap.clear();
        PuertoRicoConstants.Role activeRole = gs.getCurrentRole();

        int i = 0;
        for (PuertoRicoConstants.Role r: PuertoRicoConstants.Role.values()) {
            if (r == PuertoRicoConstants.Role.DISCARD) continue;
            boolean available = gs.isRoleAvailable(r);
            if (available) {
                if (activeRole != null && activeRole != r) {
                    g.setFont(PRGUIUtils.roleFontAvailableButNotChosen);
                    g.setColor(PRGUIUtils.secondaryColorFaint);
                } else {
                    g.setFont(PRGUIUtils.roleFontAvailable);
                    g.setColor(PRGUIUtils.secondaryColor);
                }
            } else if (activeRole != null && activeRole == r) {
                g.setFont(PRGUIUtils.roleFontAvailable);
                g.setColor(PRGUIUtils.highlightColor);
            } else {
                g.setFont(PRGUIUtils.roleFontNotAvailable);
                g.setColor(PRGUIUtils.secondaryColorFaint);
            }

            int money = gs.getMoneyOnRole(r);
            g.drawString("(" + money + ")", pad, PRGUIUtils.roleFontSize *(i+1));

            String name = r.name();
            g.drawString(capitalize(name), pad + PRGUIUtils.roleFontSize *2, PRGUIUtils.roleFontSize *(i+1));

            rectangleRoleMap.put(new Rectangle(0, pad + PRGUIUtils.roleFontSize * i, size.width, PRGUIUtils.roleFontSize), r);

            i++;
        }

        if (activeRole == null && hover != null) {
            Stroke s = g.getStroke();
            g.setColor(PRGUIUtils.highlightColor);
            g.setStroke(new BasicStroke(2));
            g.drawRect(hover.x, hover.y, hover.width, hover.height);
            g.setStroke(s);
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
    public void clearHighlights() {
        selected = null;
        hover = null;
    }


    @Override
    public String getToolTipText(MouseEvent event) {
        if (showTooltips) {
            for (Map.Entry<Rectangle, PuertoRicoConstants.Role> entry: rectangleRoleMap.entrySet()) {
                if (entry.getKey().contains(event.getPoint())) {
                    return "<html>" + entry.getValue().description.replace("\n", "<br/>") + "</html>";
                }
            }
        }
        return super.getToolTipText(event);
    }
}
