package games.cribbage.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.cribbage.CribbageGameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
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
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
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
        tabs.add("Rules", createRulesPanel());

        JPanel mainGameArea = new JPanel(new BorderLayout());
        mainGameArea.setOpaque(false);
        mainGameArea.add(wrap(playerViews[1]), BorderLayout.NORTH);
        mainGameArea.add(wrap(tableView), BorderLayout.CENTER);
        mainGameArea.add(wrap(playerViews[0]), BorderLayout.SOUTH);

        JPanel infoPanel = createGameStateInfoPanel("Cribbage", gameState, width, defaultInfoPanelHeight);
        // a vertical list: the 15 discard options do not fit side by side
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, true);

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

    private JPanel createRulesPanel() {
        JPanel rules = new JPanel();
        rules.setBackground(new Color(43, 108, 25, 111));
        JLabel text = new JLabel("<html><center><h1>Cribbage</h1></center><hr>" +
                "<p>Two players, two rounds: each player deals once. The higher score wins.</p><ul>" +
                "<li><b>Deal and discard:</b> each player is dealt 6 cards and discards 2 of them face-down to the " +
                "<b>crib</b>, which belongs to the dealer. The non-dealer discards first.</li>" +
                "<li><b>Starter:</b> the top card of the deck is turned up. If it is a Jack the dealer scores 2 " +
                "(his heels).</li>" +
                "<li><b>The play:</b> the non-dealer leads, then players take turns playing a card, adding its value " +
                "to the count (Ace 1, court cards 10). The count may not pass 31. A player who cannot play says " +
                "<i>go</i> (automatic here) and the other carries on alone. When neither can play, the player of " +
                "the last card scores 1 and the count starts again from 0.</li>" +
                "<li>Scoring in the play, for the player of the card: count of 15 - 1; count of 31 - 2; a pair " +
                "with the card before - 2 (three in a row 6, four 12); a run of 3 or more among the last cards, in " +
                "any order - 1 per card.</li>" +
                "<li><b>The show:</b> when all the cards are played, each player scores their 4 cards with the " +
                "starter - non-dealer first, then the dealer, then the dealer's crib: every combination making " +
                "15 - 2; each pair - 2; runs (without the starter) - 1 per card, each run counted; 4 cards of one " +
                "suit - 4, 5 if the starter matches; the Jack of the starter's suit - 1 (his nobs).</li>" +
                "<li>If a player reaches 121 the game ends at once and they win.</li>" +
                "</ul><hr><p><b>INTERFACE:</b> choose a pair of cards to discard, then a card to play, from the " +
                "action buttons at the bottom of the screen. The centre shows the starter, the crib and the current " +
                "count. Each player's played cards are shown next to their hand.</p></html>");
        text.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(width * 2 / 3 + 60, height * 2 / 3 + 100));
        rules.add(scroll);
        return rules;
    }
}
