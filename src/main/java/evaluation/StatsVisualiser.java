package evaluation;

import core.interfaces.AbstractGameListener;
import games.GameType;
import games.terraformingmars.stats.TMStatsVisualiser;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public abstract class StatsVisualiser extends JFrame {

    public void setFrameProperties(Dimension dimension) {
        // Frame properties
        setSize(dimension);
        setPreferredSize(dimension);
        setMinimumSize(dimension);
        revalidate();
        pack();
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//        setExtendedState(JFrame.MAXIMIZED_BOTH);
        repaint();
    }

    static StatsVisualiser getVisualiserForGame(GameType gameType, List<AbstractGameListener> listeners) {
        switch (gameType) {
            case TerraformingMars: return new TMStatsVisualiser(listeners);
            default: return null;
        }
    }
}
