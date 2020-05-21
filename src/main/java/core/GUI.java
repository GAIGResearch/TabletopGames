package core;

import core.actions.IAction;
import players.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("rawtypes")
public abstract class GUI extends JFrame {
    protected ActionButton[] actionButtons;
    protected int maxActionSpace;
    protected ActionController ac;
    protected JLabel gameStatus, turnOwner, turn, currentPlayer, gamePhase;

    public GUI(ActionController ac, int maxActionSpace) {
        this.ac = ac;
        this.maxActionSpace = maxActionSpace;
        gameStatus = new JLabel();
        gamePhase = new JLabel();
        turnOwner = new JLabel();
        turn = new JLabel();
        currentPlayer = new JLabel();
    }

    /**
     * Updates all GUI elements. Must be implemented by subclass.
     * @param player - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    protected abstract void _update(AbstractPlayer player, AbstractGameState gameState);

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

    /**
     * Updates which action buttons should be visible to the players, and which should not.
     * By default all actions are transformed into visible buttons.
     * @param player - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        List<IAction> actions = gameState.getActions();
        for (int i = 0; i < actions.size(); i++) {
            actionButtons[i].setVisible(true);
            actionButtons[i].setButtonAction(actions.get(i));
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
        JPanel actionPanel = new JPanel();
        actionPanel.setPreferredSize(new Dimension(width, height));

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

        return actionPanel;
    }

    /**
     * Creates a JPanel containing labels with default game state information.
     * @param gameTitle - title of the game, displayed first at the top
     * @param gameState - initial game state.
     * @return - JPanel containing several JLabels with game state information.
     */
    protected JPanel createGameStateInfoPanel(String gameTitle, AbstractGameState gameState) {
        JPanel gameInfo = new JPanel();
        gameInfo.setLayout(new BoxLayout(gameInfo, BoxLayout.Y_AXIS));
        gameInfo.add(new JLabel(gameTitle));

        updateGameStateInfo(gameState);

        gameInfo.add(gameStatus);
        gameInfo.add(gamePhase);
        gameInfo.add(turnOwner);
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);

        return gameInfo;
    }

    /**
     * Updates the information stored in the JLabels with new game state information.
     * @param gameState - current game state to be used for the update.
     */
    protected void updateGameStateInfo(AbstractGameState gameState) {
        gameStatus.setText("Game status: " + gameState.getGameStatus());
        gamePhase.setText("Game phase: " + gameState.getGamePhase());
        turnOwner.setText("Turn owner: " + gameState.getTurnOrder().getTurnOwner());
        turn.setText("Turn: " + gameState.getTurnOrder().getTurnCounter() +
                "; Round: " + gameState.getTurnOrder().getRoundCounter());
        currentPlayer.setText("Current player: " + gameState.getTurnOrder().getCurrentPlayer(gameState));
    }

    /**
     * Helper class, JButton with an associated action. This action is added to the human agent action queue for execution
     * in the game when the button is clicked. Any associated highlights from the GUI are cleared.
     */
    @SuppressWarnings("rawtypes")
    protected static class ActionButton extends JButton {
        IAction action;
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

        public void setButtonAction(IAction action) {
            this.action = action;
            if (action != null) setText(action.toString());
            else setText("");
        }

        public void setButtonAction(IAction action, String actionText) {
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
                actionButton.setButtonAction(null);
            }
        }
    }
}
