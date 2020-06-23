package games.descent.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import games.descent.DescentGameState;
import players.ActionController;
import players.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class DescentGUI extends AbstractGUI {
    DescentGridBoardView view;
    int width, height;
    int maxWidth = 800;
    int maxHeight = 600;

    public DescentGUI(AbstractGameState gameState, ActionController ac) {
        super(ac, 1);  // TODO: calculate/approximate max action space

        DescentGameState dgs = (DescentGameState) gameState;

        view = new DescentGridBoardView(dgs.getMasterBoard(), dgs);
        width = view.getPreferredSize().width;
        height = view.getPreferredSize().height;

        JPanel infoPanel = createGameStateInfoPanel("Descent", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new Collection[0], width, defaultActionPanelHeight);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(infoPanel);

        JScrollPane pane = new JScrollPane(view);
        pane.setPreferredSize(new Dimension(maxWidth, maxHeight));
        getContentPane().add(pane, BorderLayout.CENTER);
        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(actionPanel, BorderLayout.SOUTH);

        setFrameProperties();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
            view.updateGameState((DescentGameState) gameState);
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(maxWidth, maxHeight + defaultActionPanelHeight + defaultInfoPanelHeight);
    }
}
