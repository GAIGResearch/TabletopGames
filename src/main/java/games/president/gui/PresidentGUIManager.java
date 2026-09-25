package games.president.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.president.PresidentGameState;
import games.president.PresidentParameters;
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

        JLabel rules = new JLabel(rulesText((PresidentParameters) state.getGameParameters()));
        rules.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane rulesPane = new JScrollPane(rules);
        rulesPane.setPreferredSize(new Dimension(width * 3 / 4, tableHeight));
        JPanel rulesTab = new JPanel();
        rulesTab.setBackground(new Color(43, 108, 25, 111));
        rulesTab.add(rulesPane);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        tabs.add("Main", main);
        tabs.add("Rules", rulesTab);

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

    private static String rulesText(PresidentParameters params) {
        String length = params.targetScore <= 1
                ? "The game is a single deal: the player who goes out first wins."
                : "Deals continue until a player's score reaches " + params.targetScore + "; the highest score"
                + " wins, and equal scores are ranked by the order of going out in the last deal.";
        String exchange = params.targetScore <= 1 || params.exchangeCards == 0 ? ""
                : "<h3>The exchange</h3><p>Each later deal is dealt starting with the President of the last deal."
                + " The Scum then gives the President their " + params.exchangeCards + " highest card(s), and the"
                + " President gives back any " + params.exchangeCards + " card(s). The President leads.</p>";
        return "<html><body style='width:600px'><h1>President</h1>"
                + "<p>The whole 52-card pack is dealt. Suits do not matter; the ranks from high to low are"
                + " 2 A K Q J 10 9 8 7 6 5 4 3. The aim is to empty your hand first.</p>"
                + "<h3>Play</h3><p>Player 0 leads first. The leader plays any set: one or more cards of the same rank."
                + " Each following player either passes or plays a set of the same number of cards and a higher"
                + " rank. A player who has passed may still play later in the same trick. When every other player"
                + " still holding cards has passed in a row, the cards are discarded and the player who played last"
                + " leads again (if they have gone out, the next player holding cards leads). Players who are out"
                + " are skipped.</p>"
                + "<h3>Scoring</h3><p>The first player out (the President) scores " + params.presidentPoints
                + ", the second (the Vice-President) " + params.vicePresidentPoints + ", the others 0. The deal ends"
                + " when only one player holds cards (the Scum). " + length + "</p>" + exchange
                + "<h3>Interface</h3><p>Hands are listed in turn order, sorted from the lowest rank; the player to"
                + " act has a blue border. The current trick is on the right: the set to beat is the brightest, the"
                + " earlier sets are dimmed. 'Play 2 x 7' plays a pair of Sevens.</p>"
                + "</body></html>";
    }
}
