package games.cantstop.gui;

import core.*;
import games.cantstop.CantStopGameState;
import gui.*;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class CantStopGUIManager extends AbstractGUIManager {

    CantStopBoardView view;
    static int cantStopWidth = 600;
    static int cantStopHeight = 500;

    public CantStopGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        super(parent, game, ac, humanId);
        CantStopGameState state = (CantStopGameState) game.getGameState();
        view = new CantStopBoardView(state);

        width = cantStopWidth;
        height = cantStopHeight;

        JPanel infoPanel = createGameStateInfoPanel("Can't Stop", state, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 20));
    }

    @Override
    public int getMaxActionSpace() {
        return 6;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.update((CantStopGameState) gameState);
        }
    }

}
