package core;

import core.actions.AbstractAction;
import gui.WindowInput;
import players.human.ActionController;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;

@SuppressWarnings("rawtypes")
public abstract class AbstractGUI extends JFrame {
    public static int defaultItemSize = 50;
    public static int defaultActionPanelHeight = 100;
    public static int defaultInfoPanelHeight = 160;
    public static int defaultCardWidth = 100, defaultCardHeight = 80;
    public static int defaultBoardWidth = 400, defaultBoardHeight = 300;
    public static int defaultDisplayWidth = 500, defaultDisplayHeight = 400;

    protected ActionButton[] actionButtons;
    protected int maxActionSpace;
    protected ActionController ac;
    protected JLabel gameStatus, playerStatus, turnOwner, turn, currentPlayer, gamePhase, playerScores;
    protected JTextPane historyInfo;
    protected JScrollPane historyContainer;
    private int actionsAtLastUpdate;
    private WindowInput wi;

    protected int width, height;

    public AbstractGUI(ActionController ac, int maxActionSpace) {
        this.ac = ac;
        this.maxActionSpace = maxActionSpace;
        gameStatus = new JLabel();
        playerStatus = new JLabel();
        playerScores = new JLabel();
        gamePhase = new JLabel();
        turnOwner = new JLabel();
        turn = new JLabel();
        currentPlayer = new JLabel();
        historyInfo = new JTextPane();

        this.wi = new WindowInput();
        addWindowListener(wi);
    }

    protected void setFrameProperties() {
        // Frame properties
        pack();
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
//        setDefaultCloseOperation(EXIT_ON_CLOSE);
        repaint();
    }

    /* Methods that should/can be implemented by subclass */

    /**
     * Updates all GUI elements. Must be implemented by subclass.
     * @param player - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    protected abstract void _update(AbstractPlayer player, AbstractGameState gameState);

    /**
     * Updates which action buttons should be visible to the players, and which should not.
     * By default all actions are transformed into visible buttons.
     * @param player - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.gameStatus == Utils.GameResult.GAME_ONGOING) {
            List<AbstractAction> actions = player.forwardModel.computeAvailableActions(gameState);
            for (int i = 0; i < actions.size(); i++) {
                actionButtons[i].setVisible(true);
                actionButtons[i].setButtonAction(actions.get(i), gameState);
            }
            for (int i = actions.size(); i < actionButtons.length; i++) {
                actionButtons[i].setVisible(false);
                actionButtons[i].setButtonAction(null, "");
            }
        }
    }

    /**
     * Creates a panel containing all action buttons; all not visible by default.
     * @param highlights - when button is clicked, any GUI highlights are cleared. This array contains all lists of
     *                   highlights maintained by the GUI. Can be null if not used.
     * @param width - width of this panel.
     * @param height - height of this panel.
     * @return - JComponent containing all action buttons.
     */
    protected JComponent createActionPanel(Collection[] highlights, int width, int height) {
        return createActionPanel(highlights, width, height, true);
    }
    protected JComponent createActionPanel(Collection[] highlights, int width, int height, boolean boxLayout) {
        JPanel actionPanel = new JPanel();
        if (boxLayout) {
            actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        }

        actionButtons = new ActionButton[maxActionSpace];
        for (int i = 0; i < maxActionSpace; i++) {
            ActionButton ab = new ActionButton(ac, highlights);
            actionButtons[i] = ab;
            actionButtons[i].setVisible(false);
            actionPanel.add(actionButtons[i]);
        }
        for (ActionButton actionButton : actionButtons) {
            actionButton.informAllActionButtons(actionButtons);
        }

        JScrollPane pane = new JScrollPane(actionPanel);
        pane.setPreferredSize(new Dimension(width, height));
        if (boxLayout) {
            pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }
        return pane;
    }

    /**
     * Creates a JPanel containing labels with default game state information.
     * @param gameTitle - title of the game, displayed first at the top
     * @param gameState - initial game state.
     * @return - JPanel containing several JLabels with game state information.
     */
    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState, int width, int height) {
        JPanel gameInfo = new JPanel();
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel("<html><h1>" + gameTitle + "</h1></html>"));

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
        gameInfo.add(playerStatus);
        gameInfo.add(playerScores);
        gameInfo.add(gamePhase);
        gameInfo.add(turnOwner);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);

        gameInfo.setPreferredSize(new Dimension(width/2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        historyInfo.setPreferredSize(new Dimension(width/2 - 10, height));
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setPreferredSize(new Dimension(width/2 - 25, height));
        wrapper.add(historyContainer);
        return wrapper;
    }

    /**
     * Updates the information stored in the JLabels with new game state information.
     * @param gameState - current game state to be used for the update.
     */
    protected void updateGameStateInfo(AbstractGameState gameState) {
        List<String> history = gameState.getHistoryAsText();
        if (history.size() > actionsAtLastUpdate) {
            // this is to stop the panel updating on every tick during one's own turn
            actionsAtLastUpdate = history.size();
            historyInfo.setText(String.join("\n", history));
            historyInfo.setCaretPosition(historyInfo.getDocument().getLength());
        }
        gameStatus.setText("Game status: " + gameState.getGameStatus());
        playerStatus.setText(Arrays.toString(gameState.getPlayerResults()));
        playerScores.setText("Player Scores: " + IntStream.range(0, gameState.getNPlayers())
                .mapToObj(p -> String.format("%.0f", gameState.getGameScore(p)))
                .collect(joining(", ")));
        gamePhase.setText("Game phase: " + gameState.getGamePhase());
        turnOwner.setText("Turn owner: " + gameState.getTurnOrder().getTurnOwner());
        turn.setText("Turn: " + gameState.getTurnOrder().getTurnCounter() +
                "; Round: " + gameState.getTurnOrder().getRoundCounter());
        currentPlayer.setText("Current player: " + gameState.getTurnOrder().getCurrentPlayer(gameState));
    }


    /* Public API */

    /**
     * Updates the GUI, public method called from the Game class. Updates game state info panels, resets action buttons
     * and then calls the _update() method to allow subclasses to update their inner state.
     * @param player - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    public void update(AbstractPlayer player, AbstractGameState gameState){
        updateGameStateInfo(gameState);
//        resetActionButtons();
        _update(player, gameState);
    }

    protected void resetActionButtons() {
        for (ActionButton actionButton : actionButtons) {
            actionButton.setVisible(false);
            actionButton.setButtonAction(null, "");
        }
    }

    /**
     * Checks if the window is open.
     * @return true if open, false otherwise
     */
    public final boolean isWindowOpen() {
        return !wi.windowClosed;
    }

    /* Helper class */

    /**
     * JButton with an associated action. This action is added to the human agent action queue for execution
     * in the game when the button is clicked. Any associated highlights from the GUI are cleared.
     */
    @SuppressWarnings("rawtypes")
    protected static class ActionButton extends JButton {
        AbstractAction action;
        ActionButton[] actionButtons;

        public ActionButton(ActionController ac, Collection[] highlights) {
            addActionListener(e -> {
                ac.addAction(action);
                if (highlights != null) {
                    for (Collection c : highlights) {
                        c.clear();
                    }
                }
                resetActionButtons();
            });
        }

        public void setButtonAction(AbstractAction action, AbstractGameState gameState) {
            this.action = action;
            if (action != null) setText(action.getString(gameState));
            else setText("");
        }

        public void setButtonAction(AbstractAction action, String actionText) {
            this.action = action;
            setText(actionText);
        }

        public void informAllActionButtons(ActionButton[] actionButtons) {
            this.actionButtons = actionButtons;
        }

        /**
         * Resets all action buttons
         */
        private void resetActionButtons() {
            for (ActionButton actionButton : actionButtons) {
                actionButton.setVisible(false);
                actionButton.setButtonAction(null, "");
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20);
    }
}
