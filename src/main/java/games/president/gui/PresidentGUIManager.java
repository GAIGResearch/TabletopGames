package games.president.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.president.PresidentGameState;
import games.president.PresidentParameters;
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
 * The players' hands in a column, in turn order from the top, beside the current trick and the deal's information.
 */
public class PresidentGUIManager extends AbstractGUIManager {

    static final int CARD_WIDTH = 45, CARD_HEIGHT = 63;
    static final int HAND_WIDTH = 440, SEAT_WIDTH = HAND_WIDTH + 20, SEAT_HEIGHT = CARD_HEIGHT + 32;
    static final int TABLE_WIDTH = 560, TABLE_HEIGHT = 300;

    private PresidentDeckView[] hands;
    private TitledBorder[] seatTitles;
    private JPanel[] seats;
    private PresidentTableView table;
    private final Border activeBorder = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public PresidentGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null || game.getGameState() == null) return;
        PresidentGameState state = (PresidentGameState) game.getGameState();
        int nPlayers = state.getNPlayers();

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);
        parent.setBackground(ImageIO.GetInstance().getImage(PresidentDeckView.DATA_PATH + "table-background.jpg"));

        hands = new PresidentDeckView[nPlayers];
        seatTitles = new TitledBorder[nPlayers];
        seats = new JPanel[nPlayers];
        JPanel handColumn = new JPanel(new GridLayout(nPlayers, 1));
        handColumn.setOpaque(false);
        for (int p = 0; p < nPlayers; p++) {
            hands[p] = new PresidentDeckView(p, state.getPlayerHand(p), false, HAND_WIDTH, CARD_HEIGHT + 16);
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
        table = new PresidentTableView(TABLE_WIDTH, TABLE_HEIGHT);
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
        main.add(createGameStateInfoPanel("President", state, width, defaultInfoPanelHeight), BorderLayout.NORTH);
        main.add(tableArea, BorderLayout.CENTER);
        main.add(createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, true), BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        tabs.add("Main", main);
        tabs.add("Rules", new RulesView(rulesHtml((PresidentParameters) state.getGameParameters()), tableHeight));

        height = tableHeight + defaultInfoPanelHeight + defaultActionPanelHeight + 60;
        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height));
        parent.revalidate();
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // A leader has one play for each card held (a rank held k times gives sets of 1..k), and in the exchange the
        // President has one GiveCard per card. The largest hand is 13 cards (4 players) plus the 2 cards the Scum
        // gives with the largest PresidentParameters.exchangeCards.
        return 15;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (hands == null) return;
        PresidentGameState state = (PresidentGameState) gameState;
        int current = state.getCurrentPlayer();
        boolean fullyObservable = state.getCoreGameParameters().alwaysDisplayFullObservable;
        boolean showCurrent = state.getCoreGameParameters().alwaysDisplayCurrentPlayer;
        for (int p = 0; p < state.getNPlayers(); p++) {
            hands[p].updateComponent(state.getPlayerHand(p));
            hands[p].setFront(fullyObservable || humanPlayerIds.contains(p) || (showCurrent && p == current));
            seatTitles[p].setTitle(seatTitle(state, p));
            boolean active = state.isNotTerminal() && p == current;
            seats[p].setBorder(active ? BorderFactory.createCompoundBorder(activeBorder, seatTitles[p]) : seatTitles[p]);
        }
        table.update(state);
        parent.repaint();
    }

    private String seatTitle(PresidentGameState state, int p) {
        String agent = game.getPlayers() == null ? "" : " [" + game.getPlayers().get(p).toString() + "]";
        int place = state.getFinishingOrder().indexOf(p);
        String out = place < 0 ? "" : place == 0 ? " - President" : place == 1 ? " - Vice-President"
                : place == state.getNPlayers() - 1 ? " - Scum" : " - out in place " + (place + 1);
        return "Player " + p + agent + " (score " + state.getPlayerScore(p) + ")" + out;
    }

    private static String rulesHtml(PresidentParameters params) {
        int n = params.exchangeCards;
        String cards = n == 1 ? "card" : "cards";
        String length = params.targetScore <= 1
                ? "<p>The game is a single deal, and the player with the most points wins.</p>"
                : "<p>Deals continue until a player has " + params.targetScore + " points or more. The highest "
                + "score then wins. Players with equal scores are ranked by the order in which they went out in "
                + "the last deal.</p>";
        String exchange = params.targetScore <= 1 ? ""
                : n == 0
                ? "<h3>Later deals</h3><p>Each later deal is dealt starting with the President of the last deal, "
                + "who leads the first trick.</p>"
                : "<h3>The exchange</h3><p>Each later deal is dealt starting with the President of the last deal. "
                + "The Scum of the last deal then gives the President their " + n + " highest " + cards + ". "
                + "The President gives back any " + n + " " + cards + ", and leads the first trick.</p>";
        return "<h2>President</h2>"
                + "<p>The aim is to be the first to play all your cards. The whole 52-card pack is dealt. Suits "
                + "do not matter. The ranks from high to low are 2 A K Q J 10 9 8 7 6 5 4 3.</p>"
                + "<h3>Play</h3>"
                + "<p>A set is one or more cards of the same rank. Player 0 leads the first trick.</p><ul>"
                + "<li>The leader plays any set.</li>"
                + "<li>Each following player either passes or plays a set of the same number of cards and a "
                + "higher rank.</li>"
                + "<li>A player who has passed may still play later in the same trick.</li>"
                + "<li>Players who are out are skipped.</li>"
                + "<li>When all the other players still holding cards have passed since the last set, the trick "
                + "is discarded. The player who played that set leads the next trick. If they are out, the next "
                + "player holding cards leads.</li></ul>"
                + "<h3>Scoring</h3>"
                + "<p>The deal ends when only one player holds cards, and that player is the Scum. Points go by "
                + "the order in which the players went out:</p>"
                + "<table border=1 cellpadding=4 cellspacing=0>"
                + "<tr><th align=left>Place</th><th>Points</th></tr>"
                + "<tr><td>1st (President)</td><td align=center>" + params.presidentPoints + "</td></tr>"
                + "<tr><td>2nd (Vice-President)</td><td align=center>" + params.vicePresidentPoints + "</td></tr>"
                + "<tr><td>Others</td><td align=center>0</td></tr></table>"
                + length + exchange
                + "<h3>Interface</h3>"
                + "<p>The hands are on the left, in turn order from the top, with the lowest rank on the left. "
                + "A hand you cannot see is face down, with its number of cards. The title of each hand gives "
                + "the player's score, and their place once they are out. The player to act has a blue "
                + "border.</p>"
                + "<p>The current trick is on the right. The set to beat is the brightest, and the earlier sets "
                + "are dimmed. The lines below the trick give the set to beat, the passes since it was played, "
                + "the players out this deal and the scores.</p>"
                + "<p>The action buttons are Play, Pass and (in the exchange) Give. For example, Play 2 x 7 "
                + "plays a pair of Sevens.</p>";
    }
}
