package games.lawnandorder.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.lawnandorder.LawnAndOrderGameState;
import games.lawnandorder.LawnAndOrderParameters;
import games.lawnandorder.components.RuleCard;
import games.tricktaking.gui.CardArt;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * GUI for Lawn and Order: the HOA's Rule cards at the top, and one area per player below.
 */
public class LawnAndOrderGUIManager extends AbstractGUIManager {

    LawnAndOrderTableView tableView;
    LawnAndOrderPlayerView[] playerViews;
    JPanel[] playerCells;
    String[] agentNames;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);
    final Border noHighlight = BorderFactory.createEmptyBorder(3, 3, 3, 3);

    public LawnAndOrderGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        LawnAndOrderGameState state = (LawnAndOrderGameState) gameState;
        int nPlayers = state.getNPlayers();

        // one column for 2 players, two for 3-6
        int columns = nPlayers <= 2 ? 1 : 2;
        int rows = (nPlayers + columns - 1) / columns;
        int playerAreaWidth = LawnAndOrderPlayerView.width + 14;
        int playerAreaHeight = LawnAndOrderPlayerView.height + 10;
        this.width = columns * playerAreaWidth + 20;
        LawnAndOrderParameters params = (LawnAndOrderParameters) state.getGameParameters();
        tableView = new LawnAndOrderTableView(width - 20, params.ruleCards().size() - nPlayers);
        this.height = tableView.getPreferredSize().height + rows * playerAreaHeight + 20;

        parent.setBackground(ImageIO.GetInstance().getImage(CardArt.dataPath + "table-background.jpg"));
        // without these the tabbed pane's content area paints over the parent's background image
        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        tabs.add("Game", main);
        RulesView.addTabs(tabs, GameType.LawnAndOrder, params, height);

        JPanel tableWrapper = new JPanel(new GridBagLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.add(tableView);

        JPanel players = new JPanel(new GridLayout(rows, columns, 4, 4));
        players.setOpaque(false);
        playerViews = new LawnAndOrderPlayerView[nPlayers];
        playerCells = new JPanel[nPlayers];
        agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            agentNames[i] = game.getPlayers().get(i).toString();
            playerViews[i] = new LawnAndOrderPlayerView(i);
            JPanel cell = new JPanel(new BorderLayout());
            cell.setOpaque(false);
            cell.setBorder(noHighlight);
            cell.add(playerViews[i], BorderLayout.CENTER);
            playerCells[i] = cell;
            JPanel slot = new JPanel(new GridBagLayout());
            slot.setOpaque(false);
            slot.add(cell);
            players.add(slot);
        }

        JPanel mainGameArea = new JPanel(new BorderLayout());
        mainGameArea.setOpaque(false);
        mainGameArea.add(tableWrapper, BorderLayout.NORTH);
        mainGameArea.add(players, BorderLayout.CENTER);

        JPanel infoPanel = createGameStateInfoPanel("Lawn and Order", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

        main.add(infoPanel, BorderLayout.NORTH);
        main.add(mainGameArea, BorderLayout.CENTER);
        main.add(actionPanel, BorderLayout.SOUTH);

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 40));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // one PlayObject per card in hand: a hand never holds more than LawnAndOrderParameters.handSize (at most 7),
        // as a card is played before one is drawn
        return 7;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof LawnAndOrderGameState state)) return;
        int nPlayers = state.getNPlayers();
        int currentPlayer = state.getCurrentPlayer();

        for (int i = 0; i < nPlayers; i++) {
            playerViews[i].update(state, agentNames[i], showCards(state, i));
            playerCells[i].setBorder(i == currentPlayer && state.isNotTerminal() ? highlightActive : noHighlight);
        }

        String status;
        if (state.isNotTerminal()) {
            status = "Round " + (state.getRoundCounter() + 1)
                    + (state.getGamePhase() == LawnAndOrderGameState.Phase.PLAY_OBJECT
                    ? "   -   Choose a card" : "   -   Continue or pass")
                    + "   -   Draw deck " + state.getDrawDeck().getSize()
                    + "   -   Agenda " + state.getAgenda().getSize()
                    + (state.isZeroTolerance() ? "   -   ZERO TOLERANCE" : "");
        } else {
            status = "Game over after " + state.getRoundCounter() + " rounds   -   " + resultText(state);
        }
        List<RuleCard> tips = state.getInsiderTips().getComponents();
        List<Boolean> tipsVisible = new ArrayList<>();
        for (int i = 0; i < tips.size(); i++) {
            // tip i lies between players i and i+1
            tipsVisible.add(!state.isNotTerminal() || showCards(state, i) || showCards(state, (i + 1) % nPlayers));
        }
        tableView.update(status, state.getRevealedRules().getComponents(), tips, tipsVisible, nPlayers);
        parent.repaint();
    }

    /**
     * Who won, or who drew, and with what total.
     */
    private static String resultText(LawnAndOrderGameState state) {
        Set<Integer> winners = state.getWinners();
        Set<Integer> drawn = state.getTied();
        Set<Integer> top = winners.isEmpty() ? drawn : winners;
        if (top.isEmpty()) return "";
        int score = (int) state.getGameScore(top.iterator().next());
        StringBuilder names = new StringBuilder();
        for (int p : top)
            names.append(names.isEmpty() ? "" : " and ").append("Player ").append(p);
        return names + (winners.isEmpty() ? " draw" : " wins") + " with a total of " + score;
    }

    /**
     * A hand and a face-down chosen card are face up only for a human player, for the current player if the core
     * parameters allow it, or in full-observability mode.
     */
    private boolean showCards(LawnAndOrderGameState state, int playerId) {
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }
}
