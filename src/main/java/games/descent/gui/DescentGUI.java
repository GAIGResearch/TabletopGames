package games.descent.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import games.descent.DescentGameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class DescentGUI extends AbstractGUIManager {
    DescentGridBoardView view;
    int width, height;
    int maxWidth = 800;
    int maxHeight = 600;

    public DescentGUI(GamePanel panel, AbstractGameState gameState, ActionController ac) {
        super(panel, ac, 100);  // TODO: calculate/approximate max action space

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

        panel.setLayout(new BorderLayout());
        panel.add(pane, BorderLayout.CENTER);
        panel.add(north, BorderLayout.NORTH);
        panel.add(actionPanel, BorderLayout.SOUTH);
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
            view.updateGameState((DescentGameState) gameState);
        }
        parent.repaint();
    }

}
