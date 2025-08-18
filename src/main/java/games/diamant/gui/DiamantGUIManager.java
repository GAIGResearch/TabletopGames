package games.diamant.gui;

import core.*;
import games.diamant.DiamantGameState;
import gui.*;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class DiamantGUIManager extends AbstractGUIManager {

    DiamantBoardView view;
    static int diamantWidth = 800;
    static int diamantHeight = 400;

    public DiamantGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        super(parent, game, ac, humanId);
        DiamantGameState state = (DiamantGameState) game.getGameState();
        view = new DiamantBoardView(state);

        width = diamantWidth;
        height = diamantHeight;

        JPanel infoPanel = createGameStateInfoPanel("Diamant", state, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 20));
    }

    /**
     * Override to remove the history panel from the info panel.
     */
    @Override
    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        JPanel retValue = super.createGameStateInfoPanel(gameTitle, gameState, width, height);
        // Remove the history panel
        retValue.remove(historyContainer);
        return retValue;
    }

    @Override
    public int getMaxActionSpace() {
        return 3;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.update((DiamantGameState) gameState);
        }
    }
}
