package gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

public abstract class AbstractGUIManager {
    protected GamePanel parent;
    protected Game game;
    protected Set<Integer> humanPlayerIds;

    public static int defaultItemSize = 50;
    public static int defaultActionPanelHeight = 100;
    public static int defaultInfoPanelHeight = 180, defaultInfoPanelWidth = 300;
    public static int defaultCardWidth = 100, defaultCardHeight = 80;
    public static int defaultBoardWidth = 400, defaultBoardHeight = 300;
    public static int defaultDisplayWidth = 500, defaultDisplayHeight = 400;


    protected ActionButton[] actionButtons;
    protected int maxActionSpace;
    protected ActionController ac;
    // the actions a human player may choose by clicking on the game's views (see ClickableActions)
    protected final ClickableActions clickable;
    protected JLabel gameStatus, playerStatus, turn, currentPlayer, gamePhase, playerScores;
    protected JTextPane historyInfo;
    protected JScrollPane historyContainer;
    protected Set<Integer> historyPerspective = new HashSet<>();
    protected List<String> history = new ArrayList<>();

    private int actionsAtLastUpdate;

    // the panels made by createActionPanel and createGameStateInfoPanel, if the GUI uses them
    private JComponent actionPanel, infoPanel;

    protected int width, height;

    public AbstractGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        this.ac = ac;
        this.maxActionSpace = getMaxActionSpace();
        this.parent = parent;
        this.game = game;
        this.humanPlayerIds = human == null ? Set.of() : human;
        this.clickable = new ClickableActions(ac, humanPlayerIds);

        gameStatus = new JLabel();
        playerStatus = new JLabel();
        playerScores = new JLabel();
        gamePhase = new JLabel();
        turn = new JLabel();
        currentPlayer = new JLabel();
        historyInfo = new JTextPane();
    }

    /* Methods that should/can be implemented by subclass */

    /**
     * Defines how many action button objects will be created and cached for usage if needed. Less is better, but
     * should not be smaller than the number of actions available to players in any game state.
     *
     * @return maximum size of the action space (maximum actions available to a player for any decision point in the game)
     */
    public abstract int getMaxActionSpace();

    /**
     * Updates all GUI elements. Must be implemented by subclass.
     *
     * @param player    - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    protected abstract void _update(AbstractPlayer player, AbstractGameState gameState);

    /**
     * Updates which action buttons should be visible to the players, and which should not.
     * By default all actions are transformed into visible buttons.
     *
     * @param player    - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    protected void updateActionButtons(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState.getGameStatus() == CoreConstants.GameResult.GAME_ONGOING && !(actionButtons == null)) {
            List<AbstractAction> actions = player.getForwardModel().computeAvailableActions(gameState, gameState.getCoreGameParameters().actionSpace, player.getPlayerID());
            clickable.offer(gameState, actions, player.getPlayerID());
            for (int i = 0; i < actions.size() && i < maxActionSpace; i++) {
                actionButtons[i].setVisible(true);
                actionButtons[i].setButtonAction(actions.get(i), gameState);
                actionButtons[i].setBackground(Color.white);
            }
            for (int i = actions.size(); i < actionButtons.length; i++) {
                actionButtons[i].setVisible(false);
                actionButtons[i].setButtonAction(null, "");
            }
        }
    }

    /**
     * Creates a panel containing all action buttons; all not visible by default.
     *
     * @param highlights - when button is clicked, any GUI highlights are cleared. This array contains all lists of
     *                   highlights maintained by the GUI. Can be null if not used.
     * @param width      - width of this panel.
     * @param height     - height of this panel.
     * @param opaque     - true by default. if false, all panels created are not opaque (transparent).
     * @return - JComponent containing all action buttons.
     */
    protected JComponent createActionPanelOpaque(IScreenHighlight[] highlights, int width, int height, boolean opaque) {
        return createActionPanel(highlights, width, height, opaque, null, null, null);
    }

    protected JComponent createActionPanel(IScreenHighlight[] highlights, int width, int height) {
        return createActionPanel(highlights, width, height, true, null, null, null);
    }

    protected JComponent createActionPanel(IScreenHighlight[] highlights, int width, int height, Consumer<ActionButton> onActionSelected) {
        return createActionPanel(highlights, width, height, true, onActionSelected, null, null);
    }

    /**
     * The action buttons, as a grid of equal cells that fills each row across the panel and then wraps to the next,
     * scrolling vertically when there are more than fit (see FlowGridLayout).
     */
    protected JComponent createActionPanel(IScreenHighlight[] highlights, int width, int height, boolean opaque, Consumer<ActionButton> onActionSelected,
                                           Consumer<ActionButton> onMouseEnter,
                                           Consumer<ActionButton> onMouseExit) {
        JPanel actionPanel = new ActionGridPanel(width);

        actionButtons = new ActionButton[maxActionSpace];
        for (int i = 0; i < maxActionSpace; i++) {
            ActionButton ab = new ActionButton(ac, highlights, onActionSelected, onMouseEnter, onMouseExit);
            // once an action is chosen with a button, a click on the views cannot choose another
            ab.addActionListener(e -> clickable.withdraw());
            actionButtons[i] = ab;
            actionButtons[i].setVisible(false);
            actionPanel.add(actionButtons[i]);
        }
        for (ActionButton actionButton : actionButtons) {
            actionButton.informAllActionButtons(actionButtons);
        }

        JScrollPane pane = new JScrollPane(actionPanel);
        pane.setPreferredSize(new Dimension(width, height));

        pane.setMinimumSize(new Dimension(width, height));
        pane.setPreferredSize(new Dimension(width, height));

        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        actionPanel.setOpaque(opaque);
        pane.setOpaque(opaque);
        pane.getViewport().setOpaque(opaque);

        this.actionPanel = pane;
        return pane;
    }

    public ActionController getAC() {
        return ac;
    }

    public Set<Integer> getHumanPlayerIds() {
        return humanPlayerIds;
    }

    /* Read-only access to the standard parts of the GUI, for showing them elsewhere: the web package streams a GUI to
       a browser, hides these panels, and draws their contents natively in the page. Call on the Swing thread. */

    /**
     * The panel of action buttons made by createActionPanel, or null if this GUI does not use one.
     */
    public JComponent getActionPanel() {
        return actionPanel;
    }

    /**
     * The game state information and history panel made by createGameStateInfoPanel, or null if this GUI does not
     * use one.
     */
    public JComponent getInfoPanel() {
        return infoPanel;
    }

    /**
     * The action buttons (empty if this GUI has none). A button offers an action when it is visible and has text;
     * clicking it (doClick) chooses that action, as a click on the GUI would.
     */
    public JButton[] getActionButtons() {
        return actionButtons == null ? new JButton[0] : actionButtons.clone();
    }

    /**
     * The lines of game state information shown in the info panel (status, scores, phase, turn, current player).
     */
    public List<String> getGameStateInfo() {
        List<String> lines = new ArrayList<>();
        for (JLabel label : new JLabel[]{gameStatus, playerStatus, playerScores, gamePhase, turn, currentPlayer})
            if (label.getText() != null && !label.getText().isEmpty())
                lines.add(label.getText());
        return lines;
    }

    /**
     * The action history shown in the history panel.
     */
    public List<String> getHistory() {
        // the history is added to by a game listener on the game's thread, so copying it may meet a change
        for (int attempt = 0; ; attempt++) {
            try {
                return new ArrayList<>(history);
            } catch (ConcurrentModificationException e) {
                if (attempt == 5) throw e;
            }
        }
    }

    /**
     * Whether a player's hidden information (their hand, role, planned cards) is drawn face up: for a human player;
     * for the player to move if the core parameters say so, or when no player is human (so an AI-only game can be
     * watched); or for everyone in full-observability mode. Note that the player passed to _update is the player to
     * move, not the viewer, so it must not be used to decide this.
     */
    public boolean showHiddenInfo(AbstractGameState state, int playerId) {
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer()
                    && (humanPlayerIds.isEmpty() || state.getCoreGameParameters().alwaysDisplayCurrentPlayer));
    }

    /**
     * The player whose view of hidden information is drawn when only one can be (Hanabi's hints, Colt Express's
     * planned actions): the human player to move, else the first human player, else the player to move.
     */
    public int viewingPlayer(AbstractGameState state) {
        int current = state.getCurrentPlayer();
        if (humanPlayerIds.isEmpty() || humanPlayerIds.contains(current)
                || state.getCoreGameParameters().alwaysDisplayCurrentPlayer)
            return current;
        return humanPlayerIds.stream().min(Integer::compare).orElse(current);
    }

    /**
     * Creates a JPanel containing labels with default game state information.
     *
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
        gameInfo.add(turn);
        gameInfo.add(currentPlayer);

        gameInfo.setPreferredSize(new Dimension(width / 2 - 10, height));

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new FlowLayout());
        wrapper.add(gameInfo);

        createActionHistoryPanel(width / 2 - 10, height, humanPlayerIds);
        wrapper.add(historyContainer);
        this.infoPanel = wrapper;
        return wrapper;
    }

    protected void createActionHistoryPanel(int width, int height, Set<Integer> perspectiveSet) {
        this.historyPerspective = perspectiveSet;
        if (!perspectiveSet.isEmpty()) {
            // we need to create a GameListener for ACTION_CHOSEN events
            game.addListener(new IGameListener() {
                @Override
                public void onEvent(Event event) {
                    if (event.type == Event.GameEvent.ACTION_CHOSEN) {
                        history.add("Player " + event.playerID + " : " + event.action.getString(game.getGameState(), perspectiveSet));
                    } else if (event.type == Event.GameEvent.GAME_EVENT) {
                        history.add(event.action.toString());
                    } else if (event.type == Event.GameEvent.GAME_OVER) {
                        for (int i = 0; i < event.state.getNPlayers(); i++) {
                            history.add(String.format("Player %d finishes at position %d with score: %.0f", i, event.state.getOrdinalPosition(i), event.state.getGameScore(i)));
                        }
                    }
                }

                @Override
                public void report() {
                }

                @Override
                public void setGame(Game game) {
                }

                @Override
                public Game getGame() {
                    return null;
                }
            });
        }
        historyInfo.setPreferredSize(new Dimension(width, height));
        historyContainer = new JScrollPane(historyInfo);
        historyContainer.setPreferredSize(new Dimension(width - 15, height));
        historyContainer.setMinimumSize(new Dimension(width - 15, height));
        historyContainer.setMaximumSize(new Dimension(width - 15, height));
    }

    /**
     * Updates the information stored in the JLabels with new game state information.
     *
     * @param gameState - current game state to be used for the update.
     */
    protected void updateGameStateInfo(AbstractGameState gameState) {
        if (historyPerspective.size() == 0) {
            history = gameState.getHistoryAsText();
            // otherwise we populate history from ACTION_CHOSEN events
        }
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
        turn.setText("Turn: " + gameState.getTurnCounter() +
                "; Round: " + gameState.getRoundCounter());
        currentPlayer.setText("Current player: " + gameState.getCurrentPlayer());
    }


    /* Public API */

    /**
     * Updates the GUI, public method called from the Game class. Updates game state info panels, resets action buttons
     * and then calls the _update() method to allow subclasses to update their inner state.
     *
     * @param player      - current player acting.
     * @param gameState   - current game state to be used in updating visuals.
     * @param showActions - if false, action buttons not shown
     */
    public void update(AbstractPlayer player, AbstractGameState gameState, boolean showActions) {
        updateGameStateInfo(gameState);
        _update(player, gameState);
        if (showActions)
            updateActionButtons(player, gameState);
        else {
            resetActionButtons();
            clickable.withdraw();
        }
        //      parent.revalidate();
        //      parent.repaint();
    }

    protected void resetActionButtons() {
        if (actionButtons != null)
            for (ActionButton actionButton : actionButtons) {
                actionButton.setVisible(false);
                actionButton.setButtonAction(null, "");
            }
    }

    /* Helper class */

    /**
     * JButton with an associated action. This action is added to the human agent action queue for execution
     * in the game when the button is clicked. Any associated highlights from the GUI are cleared.
     */
    protected static class ActionButton extends JButton {
        AbstractAction action;
        ActionButton[] actionButtons;

        public ActionButton(ActionController ac, IScreenHighlight[] highlights) {
            this(ac, highlights, null, null, null);
        }

        public ActionButton(ActionController ac, IScreenHighlight[] highlights,
                            Consumer<ActionButton> onActionSelected,
                            Consumer<ActionButton> onMouseEnter,
                            Consumer<ActionButton> onMouseExit) {
            addActionListener(e -> {
                ac.addAction(action);
                if (highlights != null) {
                    for (IScreenHighlight c : highlights) {
                        c.clearHighlights();
                    }
                }
                resetActionButtons();
                if (onActionSelected != null)
                    onActionSelected.accept(this);
            });
            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (onMouseEnter != null) onMouseEnter.accept(ActionButton.this);
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (onMouseExit != null) onMouseExit.accept(ActionButton.this);
                }
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

        public AbstractAction getButtonAction() {
            return action;
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
}
