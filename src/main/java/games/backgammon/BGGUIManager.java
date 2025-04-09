package games.backgammon;

import core.*;
import gui.*;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class BGGUIManager extends AbstractGUIManager {

    BackgammonBoardView view;
    static int backgammonWidth = 800;
    static int backgammonHeight = 600;

    public BGGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        super(parent, game, ac, humanId);
        BGGameState state = (BGGameState) game.getGameState();
        view = new BackgammonBoardView();

        width = backgammonWidth;
        height = backgammonHeight;

        JPanel infoPanel = createGameStateInfoPanel("Backgammon", state, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 20));
    }

    @Override
    public int getMaxActionSpace() {
        return 10; // Adjust based on the maximum number of actions in Backgammon
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.update((BGGameState) gameState);
        }
    }
}