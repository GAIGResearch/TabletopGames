package games.mastermind.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.SetGridValueAction;
import core.components.Token;
import games.mastermind.MMConstants;
import games.mastermind.MMGameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class MMGUIManager extends AbstractGUIManager {

    MMBoardView guessView;
    MMBoardView resultView;

    public MMGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;

        MMGameState gameState = (MMGameState) game.getGameState();
        guessView = new MMBoardView(gameState.getGuessBoard());
        resultView = new MMBoardView(gameState.getResultBoard());

        // Set width/height of display
        this.width = Math.max(defaultDisplayWidth, defaultItemSize*gameState.getGuessBoard().getWidth());
        this.height = defaultItemSize*gameState.getGuessBoard().getHeight();

        // Add sections to the GUI (Game stats, grids, action buttons)
        parent.setLayout(new BorderLayout());

        JLabel guessBoardLabel = new JLabel("Guesses");
        JPanel guessPane = new JPanel();
        guessPane.setLayout(new BoxLayout(guessPane, BoxLayout.Y_AXIS));
        guessPane.add(guessBoardLabel);
        guessPane.add(guessView);
        parent.add(guessPane, BorderLayout.LINE_START);

        JLabel resultBoardLabel = new JLabel("Results");
        JPanel resultPane = new JPanel();
        resultPane.setLayout(new BoxLayout(resultPane, BoxLayout.Y_AXIS));
        resultPane.add(resultBoardLabel);
        resultPane.add(resultView);
        parent.add(resultPane, BorderLayout.LINE_END);

        JPanel infoPanel = createGameStateInfoPanel("Mastermind", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[]{guessView}, width, defaultActionPanelHeight, false);

        parent.add(infoPanel, BorderLayout.PAGE_START);
        parent.add(actionPanel, BorderLayout.PAGE_END);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();

    }

    @Override
    public int getMaxActionSpace() {
        return MMConstants.guessColours.size();
    }

    @Override
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING) {
            List<core.actions.AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState);

            for (int i = 0; i < actions.size(); i++) {
                SetGridValueAction action = (SetGridValueAction) actions.get(i);
                actionButtons[i].setVisible(true);
                actionButtons[i].setButtonAction(action, action.getValue(gameState).getComponentName());
            }
        }
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            guessView.updateComponent(((MMGameState)gameState).getGuessBoard());
        }
    }
}
