package games.cribbage.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.cribbage.CribbageGameState;
import games.cribbage.CribbageParameters;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import games.GameType;
import gui.views.RulesView;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Set;

/**
 * <p>GUI for Cribbage: player 1 at the top, player 0 at the bottom, and the starter, crib and current count in the
 * centre.</p>
 *
 * <p>This is a view only - it holds no rules and never changes the state. It is repainted for every player,
 * human or not, so a hand is only shown face-up when the viewer is entitled to see it (see showHand), and a crib
 * card only to the human who discarded it.</p>
 */
public class CribbageGUIManager extends AbstractGUIManager {

    static final String dataPath = "data/FrenchCards/";
    static final int cardWidth = 70;
    static final int cardHeight = 92;
    static final int labelHeight = 16;

    CribbagePlayerView[] playerViews;
    CribbageTableView tableView;
    Border[] playerViewBorders;
    TitledBorder[] playerTitles;
    String[] agentNames;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public CribbageGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        CribbageGameState state = (CribbageGameState) gameState;
        int nPlayers = state.getNPlayers();
        // per-card crib visibility is shown for a single human player; otherwise the crib is face-down
        int humanId = human != null && human.size() == 1 ? human.iterator().next() : -1;

        tableView = new CribbageTableView(state, humanId);
        playerViews = new CribbagePlayerView[nPlayers];
        playerViewBorders = new Border[nPlayers];
        playerTitles = new TitledBorder[nPlayers];
        agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            playerViews[i] = new CribbagePlayerView(state, i, humanId);
            playerViews[i].setOpaque(false);
            agentNames[i] = game.getPlayers().get(i).toString();
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i,
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            title.setTitleColor(Color.white);
            playerTitles[i] = title;
            playerViewBorders[i] = title;
            playerViews[i].setBorder(title);
        }

        Dimension playerSize = playerViews[0].getPreferredSize();
        Dimension tableSize = tableView.getPreferredSize();
        this.width = Math.max(playerSize.width, tableSize.width) + 60;
        this.height = playerSize.height * 2 + tableSize.height + 60;

        parent.setBackground(ImageIO.GetInstance().getImage(dataPath + "table-background.jpg"));

        // without these the tabbed pane's content area paints over the parent's background image
        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        tabs.add("Game", main);
        RulesView.addTabs(tabs, GameType.Cribbage, state.getGameParameters(), height);

        JPanel mainGameArea = new JPanel(new BorderLayout());
        mainGameArea.setOpaque(false);
        mainGameArea.add(wrap(playerViews[1]), BorderLayout.NORTH);
        mainGameArea.add(wrap(tableView), BorderLayout.CENTER);
        mainGameArea.add(wrap(playerViews[0]), BorderLayout.SOUTH);

        JPanel infoPanel = createGameStateInfoPanel("Cribbage", gameState, width, defaultInfoPanelHeight);
        // a vertical list: the 15 discard options do not fit side by side
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

    /** Centre a view in a transparent panel, so it keeps its preferred size. */
    private static JPanel wrap(JComponent view) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.add(view);
        return panel;
    }

    /**
     * The most actions offered at once is in the discard: one DiscardToCrib per pair of the 6 cards dealt
     * (CribbageParameters.nCardsDealt), 6 x 5 / 2 = 15. The play offers at most one PlayCard per card in hand (4).
     * This is called before the game is known, so it cannot read the parameters.
     */
    @Override
    public int getMaxActionSpace() {
        return 15;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof CribbageGameState state)) return;
        int currentPlayer = state.getCurrentPlayer();
        for (int i = 0; i < playerViews.length; i++) {
            playerViews[i].update(state, showHand(state, i));
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]");
            playerViews[i].setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i])
                    : playerViewBorders[i]);
        }
        tableView.update(state);
        parent.repaint();
    }

    /**
     * A hand is face-up only for a human player, for the current player if the core parameters allow it,
     * or in full-observability mode.
     */
    private boolean showHand(CribbageGameState state, int playerId) {
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }
}
