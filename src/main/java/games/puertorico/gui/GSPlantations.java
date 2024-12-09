package games.puertorico.gui;

import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.PuertoRicoParameters;
import games.puertorico.actions.DrawPlantation;
import games.puertorico.components.Plantation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import static games.puertorico.gui.PRGUIUtils.*;

public class GSPlantations extends JComponent {
    PuertoRicoGameState gs;
    PuertoRicoGUI gui;
    Dimension size;
    Map<Rectangle, Plantation> rectToPlantationMap = new HashMap<>();

    public GSPlantations(PuertoRicoGUI gui, PuertoRicoGameState gs) {
        this.gs = gs;
        this.gui = gui;
        ToolTipManager.sharedInstance().registerComponent(this);
        size = new Dimension((int)(plantationSize * (((PuertoRicoParameters)gs.getGameParameters()).extraVisiblePlantations + gs.getNPlayers() + 3.5)), plantationSize + pad*2);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (Map.Entry<Rectangle, Plantation> entry: rectToPlantationMap.entrySet()) {
                    if (entry.getKey().contains(e.getPoint())) {
                        if (gs.getCurrentRole() == PuertoRicoConstants.Role.SETTLER
                                && gui.getHumanPlayerIds().contains(gs.getCurrentPlayer())
                                && gs.getPlayerBoard(gs.getCurrentPlayer()).getPlantations().size() < ((PuertoRicoParameters)gs.getGameParameters()).plantationSlotsOnBoard) {
                            if (entry.getValue() != null) {
                                gui.getAC().addAction(new DrawPlantation(entry.getValue().crop));
                            } else {
                                if (gs.numberOfPlantationsInStack() > 0 && gs.hasActiveBuilding(gs.getCurrentPlayer(), PuertoRicoConstants.BuildingType.HACIENDA))
                                    gui.getAC().addAction(new DrawPlantation());
                            }

                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics gg) {
        rectToPlantationMap.clear();
        boolean highlight = gs.getCurrentRole() == PuertoRicoConstants.Role.SETTLER && gui.getHumanPlayerIds().contains(gs.getCurrentPlayer()) && gs.getPlayerBoard(gs.getCurrentPlayer()).getPlantations().size() < ((PuertoRicoParameters)gs.getGameParameters()).plantationSlotsOnBoard;

        // Visible plantations + how many left in deck + how many discount things left
        Graphics2D g = (Graphics2D) gg;
        g.setFont(PRGUIUtils.textFontBold);
        g.setColor(Color.black);
        FontMetrics fm = g.getFontMetrics();
        String s1 = "Plantations left: " + gs.numberOfPlantationsInStack();
        g.drawString(s1, pad, pad + textFontSize);
        rectToPlantationMap.put(new Rectangle(pad, pad, fm.stringWidth(s1), textFontBold.getSize()), null);

        if (highlight) {
            if (gs.numberOfPlantationsInStack() > 0 && gs.hasActiveBuilding(gs.getCurrentPlayer(), PuertoRicoConstants.BuildingType.HACIENDA)) {
                Stroke s = g.getStroke();
                g.setColor(highlightColor);
                g.setStroke(new BasicStroke(2));
                g.drawRect(pad, pad, fm.stringWidth(s1), textFontBold.getSize());
                g.setStroke(s);
            }
        }

        int x = fm.stringWidth(s1) + pad*4;
        for (Plantation p: gs.getAvailablePlantations().getComponents()) {
            drawPlantation(g, p, x, pad);
            rectToPlantationMap.put(new Rectangle(x, pad, plantationSize, plantationSize), p);

            if (highlight) {
                Stroke s = g.getStroke();
                g.setColor(highlightColor);
                g.setStroke(new BasicStroke(2));
                g.drawRect(x, pad, plantationSize, plantationSize);
                g.setStroke(s);
            }

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

    @Override
    public String getToolTipText(MouseEvent event) {
        if (showTooltips) {
            for (Map.Entry<Rectangle, Plantation> e : rectToPlantationMap.entrySet()) {
                if (e.getKey().contains(event.getPoint())) {
                    if (e.getValue() != null) {
                        if (e.getValue().crop != null) {
                            return e.getValue().crop.name();
                        } else return "";
                    }
                    else return "Draw plantation from the deck (if you're Settler or you have the construction hut)";
                }
            }
        }
        return super.getToolTipText(event);
    }
}
