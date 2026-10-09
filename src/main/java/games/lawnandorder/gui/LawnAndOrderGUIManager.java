package games.lawnandorder.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
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
        tabs.add("Rules", new RulesView(rulesHtml(params), height));

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

    private static String rulesHtml(LawnAndOrderParameters params) {
        StringBuilder groups = new StringBuilder("<tr><th align=left>Cards</th>");
        StringBuilder points = new StringBuilder("<tr><th align=left>Points</th>");
        int[] table = LawnAndOrderParameters.GROUP_POINTS;
        for (int n = 2; n < table.length; n++) {
            groups.append("<td align=center>").append(n).append(n == table.length - 1 ? "+" : "").append("</td>");
            points.append("<td align=center>").append(table[n]).append("</td>");
        }
        return "<h2>Lawn &amp; Order</h2>" +
                "<p>Build matching sets on your lawn while the Homeowners Association condemns attributes one by " +
                "one.</p>" +
                "<p><b>Cards.</b> Each Lawn card has a Type (Ornament, Furniture, Structure, Water Feature), " +
                "a Colour (Red, Yellow, Pink, Blue) and a Feature (Oversized, Illuminated, Plastic, Repurposed). " +
                "The Agenda holds " + params.ruleCards().size() + " Rule cards. " +
                "Each Standard Rule condemns one attribute. The others are " + params.nAdministrativeError + " Administrative Errors, " +
                params.nEmergencySession + " Emergency Session and " + params.nZeroTolerance +
                " Zero Tolerance Policy.</p>" +
                "<p><b>Each round</b> starts with " + params.handSize + " Lawn cards dealt to each player. One Rule " +
                "card is dealt face down between each pair of neighbours as an Insider Tip. You may look at the " +
                "two beside you.</p>" +
                "<p><b>Each turn</b> all active players act together:</p><ol>" +
                "<li>Each chooses a card from their hand face down. The cards are then revealed onto the lawns.</li>" +
                "<li>Each gains 1 Citation for each attribute of their new card that is already condemned.</li>" +
                "<li>The top Agenda card is revealed, and stays in force for the round. An Administrative Error does " +
                "nothing. An Emergency Session reveals the next " + params.emergencySessionReveals + " cards too. " +
                "Zero Tolerance lowers every Citation limit by " + params.zeroToleranceReduction + ".</li>" +
                "<li>Each active player gains 1 Citation for each card on their lawn with an attribute condemned " +
                "this turn. Players who have passed are immune.</li>" +
                "<li>A player with more Citations than lawn cards receives a Cease &amp; Desist. Their lawn and hand " +
                "are cleared, and they score nothing this round.</li>" +
                "<li>Each active player chooses in secret to <b>Continue</b> (draw a card and play again) or to " +
                "<b>Pass</b> (keep their lawn, safe from later rules).</li></ol>" +
                "<p><b>Scoring.</b> The round ends when no player is active, or when the Agenda runs out. Each " +
                "category scores on its own track: Type on Improvements, Colour on Colour, Feature on Character. " +
                "In each category, every group of lawn cards that share an attribute scores:</p>" +
                "<table border=1 cellpadding=4 cellspacing=0>" + groups + "</tr>" + points + "</tr></table>" +
                "<p><b>Goodwill.</b> A player who received a Cease &amp; Desist has a Citation limit " +
                params.goodwillBonus + " higher in the next round.</p>" +
                "<p><b>Winning.</b> The first player with " + params.targetScore + " or more on all three tracks " +
                "wins. If several reach it in the same round, the highest total of the three tracks wins. After " +
                params.getMaxRounds() + " rounds the highest total wins.</p>" +
                "<h3>Interface</h3>" +
                "<p>Choose from the action buttons at the bottom. The top panel shows the rules revealed this round " +
                "and the Insider Tips (face up only to the players beside them). Each player's area shows their " +
                "status and Citations, their hand, the card they have chosen this turn (face down), their lawn, " +
                "the attributes with two or more cards on it, and their three tracks (★ = target reached).</p>";
    }
}
