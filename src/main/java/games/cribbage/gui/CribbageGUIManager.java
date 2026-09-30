package games.cribbage.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.cribbage.CribbageGameState;
import games.cribbage.CribbageParameters;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
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
        tabs.add("Rules", new RulesView(rulesHtml((CribbageParameters) state.getGameParameters()), height));

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

    private static String rulesHtml(CribbageParameters params) {
        int handSize = params.nCardsDealt - params.nCardsToCrib;
        String row = "<tr><td>%s</td><td align=center>%s</td><td align=center>%s</td></tr>";
        String scoring = "<table border=1 cellpadding=4 cellspacing=0>" +
                "<tr><th align=left>Combination</th><th>The play</th><th>The show</th></tr>" +
                String.format(row, "Fifteen", params.playFifteenPoints, params.fifteenPoints + " each") +
                String.format(row, "Count of " + params.maxCount, params.thirtyOnePoints, "") +
                String.format(row, "Last card", params.lastCardPoints, "") +
                String.format(row, "Pair", params.pairPoints, params.pairPoints) +
                String.format(row, "Three of a kind", params.pairRoyalPoints, params.pairRoyalPoints) +
                String.format(row, "Four of a kind", params.doublePairRoyalPoints, params.doublePairRoyalPoints) +
                String.format(row, "Run of 3 or more", "1 per card", "1 per card") +
                String.format(row, "Flush", "", params.flushPoints + ", or " + (params.flushPoints + 1) +
                        " with the starter") +
                String.format(row, "His nobs", "", params.hisNobsPoints) +
                "</table>";
        String end = params.targetScore > 0
                ? "The game ends at once when a player reaches " + params.targetScore + ", or after " +
                params.nRounds + " rounds."
                : "The game ends after " + params.nRounds + " rounds.";
        return "<h2>Cribbage</h2>" +
                "<p>A game for two players, who deal in turn. Player 0 deals first. The dealer owns the crib.</p>" +
                "<p><b>Card values.</b> Ace counts 1 and court cards count 10. For pairs and runs the cards rank " +
                "from Ace (low) to King.</p>" +
                "<p><b>Each round.</b></p><ol>" +
                "<li>Each player is dealt " + params.nCardsDealt + " cards and discards " + params.nCardsToCrib +
                " of them face down to the crib. The non-dealer discards first.</li>" +
                "<li>The top card of the deck is turned up as the starter. If it is a Jack, the dealer scores " +
                params.hisHeelsPoints + " (his heels).</li>" +
                "<li>In the play, the non-dealer leads. The players then take turns to play a card, and each card " +
                "adds its value to the count. The count may not go over " + params.maxCount + ".</li>" +
                "<li>A player who cannot play is skipped (a go), and the other player plays on alone. When neither " +
                "can play, or the count reaches " + params.maxCount + ", the count starts again from 0. The " +
                "opponent of the player of the last card leads.</li>" +
                "<li>In the show, the " + handSize + " cards each player played are scored with the starter. The " +
                "non-dealer is scored first, then the dealer, then the crib (for the dealer).</li></ol>" +
                "<p><b>Scoring.</b></p>" + scoring +
                "<ul><li>In the play, a card scores for the count it makes, and for a pair or run it makes with the " +
                "cards just before it in the same count. A run may be in any order. The last card scores only if " +
                "the count is below " + params.maxCount + ".</li>" +
                "<li>In the show, every combination of cards that adds up to 15 scores. Only the longest runs score" +
                (params.runsIncludeStarter ? "" : ", and they are made without the starter") + ". Each different " +
                "set of cards making a run counts, so 6-7-7-8 is two runs.</li>" +
                "<li>A flush is all " + handSize + " cards of one suit" +
                (params.cribFlushNeedsStarter ? ". In the crib a flush scores only if the starter matches too" : "") +
                ". His nobs is the Jack of the starter's suit in the hand or crib.</li></ul>" +
                "<p><b>Winning.</b> " + end + " The higher score wins, and equal scores draw.</p>" +
                "<h3>Interface</h3>" +
                "<p>Choose a pair of cards to discard (\"Discard ... to crib\"), then a card to play, from the " +
                "action buttons at the bottom. The centre shows the Starter, the Crib (face down, except the cards " +
                "a human player discarded) and the Count with the cards played in it. The line below says whose " +
                "turn it is. Each player's area shows their Hand and the cards they have Played this round, with " +
                "their score and \"dealer (owns the crib)\" for the dealer. A blue outline shows whose turn it " +
                "is.</p>";
    }
}
