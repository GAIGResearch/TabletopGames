package games.rummy.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.rummy.RummyGameState;
import games.rummy.RummyParameters;
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
 * The players' hands in a column, in turn order from the top, beside the draw deck, the discard pile and the melds.
 */
public class RummyGUIManager extends AbstractGUIManager {

    static final int CARD_WIDTH = 45, CARD_HEIGHT = 63;
    static final int HAND_WIDTH = 440, SEAT_WIDTH = HAND_WIDTH + 20, SEAT_HEIGHT = CARD_HEIGHT + 32;
    static final int TABLE_WIDTH = 620, TABLE_HEIGHT = 420;

    private RummyDeckView[] hands;
    private TitledBorder[] seatTitles;
    private JPanel[] seats;
    private RummyTableView table;
    private final Border activeBorder = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public RummyGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null || game.getGameState() == null) return;
        RummyGameState state = (RummyGameState) game.getGameState();
        int nPlayers = state.getNPlayers();

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);
        parent.setBackground(ImageIO.GetInstance().getImage(RummyDeckView.DATA_PATH + "table-background.jpg"));

        hands = new RummyDeckView[nPlayers];
        seatTitles = new TitledBorder[nPlayers];
        seats = new JPanel[nPlayers];
        JPanel handColumn = new JPanel(new GridLayout(nPlayers, 1));
        handColumn.setOpaque(false);
        for (int p = 0; p < nPlayers; p++) {
            hands[p] = new RummyDeckView(p, state.getPlayerHand(p), false, HAND_WIDTH, CARD_HEIGHT + 16);
            seatTitles[p] = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                    "Player " + p, TitledBorder.LEFT, TitledBorder.TOP);
            seatTitles[p].setTitleColor(Color.WHITE);
            seats[p] = new JPanel(new GridBagLayout());
            seats[p].setOpaque(false);
            seats[p].add(hands[p]);
            seats[p].setBorder(seatTitles[p]);
            seats[p].setPreferredSize(new Dimension(SEAT_WIDTH, SEAT_HEIGHT));
            handColumn.add(seats[p]);
        }
        table = new RummyTableView(TABLE_WIDTH, TABLE_HEIGHT);
        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        middle.add(table);
        JPanel tableArea = new JPanel(new BorderLayout());
        tableArea.setOpaque(false);
        tableArea.add(handColumn, BorderLayout.WEST);
        tableArea.add(middle, BorderLayout.CENTER);

        width = SEAT_WIDTH + TABLE_WIDTH + 30;
        int tableHeight = Math.max(nPlayers * SEAT_HEIGHT, TABLE_HEIGHT) + 10;

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(createGameStateInfoPanel("Rummy", state, width, defaultInfoPanelHeight), BorderLayout.NORTH);
        main.add(tableArea, BorderLayout.CENTER);
        main.add(createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight), BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        tabs.add("Main", main);
        tabs.add("Rules", new RulesView(rulesHtml((RummyParameters) state.getGameParameters()), tableHeight));

        height = tableHeight + defaultInfoPanelHeight + defaultActionPanelHeight + 60;
        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height));
        parent.revalidate();
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // The largest hand is 11 cards (10 dealt to each of 2 players, plus the card drawn). It offers at most 11
        // discards; at most 45 melds, from an 11-card run (9 + 8 + ... + 1 segments of 3 or more cards), which beats
        // any mix of sets (5 from four of a kind) and shorter runs; and at most 3 lay-offs per card (SET, LOW, HIGH).
        return 11 + 45 + 3 * 11;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (hands == null) return;
        RummyGameState state = (RummyGameState) gameState;
        int current = state.getCurrentPlayer();
        boolean fullyObservable = state.getCoreGameParameters().alwaysDisplayFullObservable;
        boolean showCurrent = state.getCoreGameParameters().alwaysDisplayCurrentPlayer;
        for (int p = 0; p < state.getNPlayers(); p++) {
            hands[p].updateComponent(state.getPlayerHand(p));
            hands[p].setFront(fullyObservable || !state.isNotTerminal() || humanPlayerIds.contains(p)
                    || (showCurrent && p == current));
            seatTitles[p].setTitle(seatTitle(state, p));
            boolean active = state.isNotTerminal() && p == current;
            seats[p].setBorder(active ? BorderFactory.createCompoundBorder(activeBorder, seatTitles[p]) : seatTitles[p]);
        }
        table.update(state);
        parent.repaint();
    }

    @Override
    protected void updateGameStateInfo(AbstractGameState gameState) {
        super.updateGameStateInfo(gameState);
        // in a single deal the scores are the points in each hand, which the players cannot see until the end
        RummyParameters params = (RummyParameters) gameState.getGameParameters();
        if (params.targetScore == 0 && gameState.isNotTerminal()
                && !gameState.getCoreGameParameters().alwaysDisplayFullObservable)
            playerScores.setText("Player Scores: shown at the end");
    }

    private String seatTitle(RummyGameState state, int p) {
        String agent = game.getPlayers() == null ? "" : " [" + game.getPlayers().get(p).toString() + "]";
        RummyParameters params = (RummyParameters) state.getGameParameters();
        String score = params.targetScore > 0 ? " (score " + state.getPlayerScore(p) + ")" : "";
        return "Player " + p + agent + score;
    }

    private static String rulesHtml(RummyParameters params) {
        String length = params.targetScore == 0
                ? "<p>The game is a single deal. The player with the fewest points in hand wins, and players with "
                + "equal points share the place.</p>"
                : "<p>At the end of each deal one player scores the points left in the other hands. That player is "
                + "the one who went out or, if nobody did, the one player with the fewest points in hand. If the "
                + "fewest points are tied, nobody scores. The next deal will be started by the next player in turn "
                + "order. The game ends when a player's score reaches " + params.targetScore + ", and the highest "
                + "score wins.</p>";
        return "<h2>Rummy</h2>"
                + "<p>The aim is to get rid of your cards by forming melds. Each player is dealt a hand from a "
                + "52-card pack:</p>"
                + "<table border=1 cellpadding=4 cellspacing=0>"
                + "<tr><th align=left>Players</th><td align=center>2</td><td align=center>3-4</td>"
                + "<td align=center>5-6</td></tr>"
                + "<tr><th align=left>Cards</th><td align=center>" + params.cardsFor2Players + "</td>"
                + "<td align=center>" + params.cardsFor3To4Players + "</td><td align=center>"
                + params.cardsFor5To6Players + "</td></tr></table>"
                + "<p>The next card starts the discard pile, face up, and the rest form the draw deck. Player 0 "
                + "plays first.</p>"
                + "<h3>A turn</h3><ol>"
                + "<li>Draw the top card of the draw deck, or take the top card of the discard pile.</li>"
                + "<li>In any order, meld at most once and lay off any number of cards.</li>"
                + "<li>Discard one card, which ends the turn. A card taken from the discard pile this turn may be "
                + "discarded only if it is your last card.</li></ol>"
                + "<h3>Melds</h3>"
                + "<p>A set is 3 or 4 cards of one rank. A run is 3 or more cards of one suit in sequence. Aces are "
                + "low, so A-2-3 is a run but Q-K-A is not.</p>"
                + "<p>You may lay off a card onto any meld on the table, including another player's. A set takes "
                + "a card of its rank. A run takes the card just below or just above it in its suit.</p>"
                + "<h3>End of a deal</h3>"
                + "<p>A deal ends when a player's hand is empty, whether by melding, laying off or discarding. It "
                + "also ends after the turn in which the draw deck runs out, or after " + params.maxTurnsPerDeal
                + " turns. Each card left in hand is worth its number in points, with an Ace worth 1 and a court "
                + "card 10.</p>" + length
                + "<h3>Interface</h3>"
                + "<p>The hands are on the left, in turn order from the top. The player to act has a blue border. "
                + "A face-up hand is sorted by rank. A card taken from the discard pile is known to every player, "
                + "so it has a gold outline and is shown face up even in a face-down hand.</p>"
                + "<p>On the right are the draw deck and the discard pile with their sizes, and the melds below "
                + "them. The lines at the bottom give what the player to act must do, the card they took from "
                + "the discard pile, and the turn number in this deal.</p>"
                + "<p>A card is written as {Hearts 5}. The action button Lay off {Hearts 5} below a run places the "
                + "Five below the run whose lowest card is the Six of Hearts.</p>";
    }
}
