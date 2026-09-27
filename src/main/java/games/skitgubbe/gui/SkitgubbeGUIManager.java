package games.skitgubbe.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.skitgubbe.SkitgubbeGameState;
import games.skitgubbe.SkitgubbeParameters;
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
 * The players in a column, each with their phase-one hand and their face-up collected cards, beside the table: the
 * draw deck, the trump card, the trick and the bounced cards.
 */
public class SkitgubbeGUIManager extends AbstractGUIManager {

    static final int CARD_WIDTH = 40, CARD_HEIGHT = 56;
    static final int ROW_WIDTH = 460, ROW_HEIGHT = CARD_HEIGHT + 6;
    static final int SEAT_WIDTH = ROW_WIDTH + 20, SEAT_HEIGHT = 2 * ROW_HEIGHT + 34;
    static final int TABLE_WIDTH = 480, TABLE_HEIGHT = 480;

    private SkitgubbeDeckView[] hands, collected;
    private TitledBorder[] seatTitles;
    private JPanel[] seats;
    private SkitgubbeTableView table;
    private final Border activeBorder = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public SkitgubbeGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null || game.getGameState() == null) return;
        SkitgubbeGameState state = (SkitgubbeGameState) game.getGameState();
        int nPlayers = state.getNPlayers();

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);
        parent.setBackground(ImageIO.GetInstance().getImage(SkitgubbeDeckView.DATA_PATH + "table-background.jpg"));

        hands = new SkitgubbeDeckView[nPlayers];
        collected = new SkitgubbeDeckView[nPlayers];
        seatTitles = new TitledBorder[nPlayers];
        seats = new JPanel[nPlayers];
        JPanel seatColumn = new JPanel(new GridLayout(nPlayers, 1));
        seatColumn.setOpaque(false);
        for (int p = 0; p < nPlayers; p++) {
            hands[p] = new SkitgubbeDeckView(p, state.getPlayerHand(p), false, ROW_WIDTH, ROW_HEIGHT);
            collected[p] = new SkitgubbeDeckView(p, state.getCollectedCards(p), true, ROW_WIDTH, ROW_HEIGHT);
            seatTitles[p] = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                    "Player " + p, TitledBorder.LEFT, TitledBorder.TOP);
            seatTitles[p].setTitleColor(Color.WHITE);
            seats[p] = new JPanel();
            seats[p].setLayout(new BoxLayout(seats[p], BoxLayout.Y_AXIS));
            seats[p].setOpaque(false);
            seats[p].add(hands[p]);
            seats[p].add(collected[p]);
            seats[p].setBorder(seatTitles[p]);
            seats[p].setPreferredSize(new Dimension(SEAT_WIDTH, SEAT_HEIGHT));
            seatColumn.add(seats[p]);
        }
        table = new SkitgubbeTableView(TABLE_WIDTH, TABLE_HEIGHT);
        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        middle.add(table);
        JPanel tableArea = new JPanel(new BorderLayout());
        tableArea.setOpaque(false);
        tableArea.add(seatColumn, BorderLayout.WEST);
        tableArea.add(middle, BorderLayout.CENTER);

        width = SEAT_WIDTH + TABLE_WIDTH + 30;
        int tableHeight = Math.max(nPlayers * SEAT_HEIGHT, TABLE_HEIGHT) + 10;

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(createGameStateInfoPanel("Skitgubbe", state, width, defaultInfoPanelHeight), BorderLayout.NORTH);
        main.add(tableArea, BorderLayout.CENTER);
        main.add(createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, true), BorderLayout.SOUTH);

        JLabel rules = new JLabel(rulesText((SkitgubbeParameters) state.getGameParameters()));
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
        // phase one: a play per card in hand (SkitgubbeParameters.handSize, at most 4) and a turn-up.
        // Phase two: a leader may hold almost the whole pack - 52 plays - and a follower may also pick up.
        return 53;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (hands == null) return;
        SkitgubbeGameState state = (SkitgubbeGameState) gameState;
        int current = state.getCurrentPlayer();
        boolean fullyObservable = state.getCoreGameParameters().alwaysDisplayFullObservable;
        boolean showCurrent = state.getCoreGameParameters().alwaysDisplayCurrentPlayer;
        for (int p = 0; p < state.getNPlayers(); p++) {
            hands[p].updateComponent(state.getPlayerHand(p));
            hands[p].setFront(fullyObservable || humanPlayerIds.contains(p) || (showCurrent && p == current));
            // the hand is empty in phase two, when the collected cards are the cards to get rid of
            hands[p].setVisible(state.getGamePhase() == SkitgubbeGameState.Phase.PHASE_ONE);
            collected[p].updateComponent(state.getCollectedCards(p));
            seatTitles[p].setTitle(seatTitle(state, p));
            boolean active = state.isNotTerminal() && p == current;
            seats[p].setBorder(active ? BorderFactory.createCompoundBorder(activeBorder, seatTitles[p]) : seatTitles[p]);
        }
        int drawer = state.getTrumpPlayer();
        boolean trumpVisible = drawer >= 0 && (fullyObservable || humanPlayerIds.contains(drawer)
                || (showCurrent && drawer == current));
        table.update(state, trumpVisible);
        parent.repaint();
    }

    private String seatTitle(SkitgubbeGameState state, int p) {
        String agent = game.getPlayers() == null ? "" : " [" + game.getPlayers().get(p).toString() + "]";
        boolean phaseOne = state.getGamePhase() == SkitgubbeGameState.Phase.PHASE_ONE;
        String cards = phaseOne ? " - " + state.getCollectedCards(p).getSize() + " cards won"
                : " - " + state.getCollectedCards(p).getSize() + " cards to get rid of";
        String out = state.getExitScore(p) > 0 ? " - OUT, score " + state.getExitScore(p) : "";
        String trump = p == state.getTrumpPlayer() && phaseOne ? " - holds the trump card" : "";
        return "Player " + p + agent + (out.isEmpty() ? cards : out) + trump;
    }

    private static String rulesText(SkitgubbeParameters params) {
        String completer = params.completerLeads
                ? "the player who completed it leads the next trick (the next player holding cards, if they are out)"
                : "the next player holding cards leads the next trick";
        String ties = params.exitOrderTiebreak
                ? " Players on equal scores are ranked by the order in which they went out."
                : " Players on equal scores share their place.";
        return "<html><body style='width:600px'><h1>Skitgubbe</h1>"
                + "<p>A Swedish game for 3 or 4 players with a 52-card pack, ranked A K Q J 10 ... 2. In phase one you"
                + " win cards in tricks; in phase two you try to get rid of them. The last player left holding cards"
                + " loses (the <i>skitgubbe</i>).</p>"
                + "<h3>Phase one</h3><p>Each player has " + params.handSize + " cards, and the rest form the draw deck."
                + " A trick is between two players: the leader and the player on their left. Each plays any card -"
                + " suits do not matter - or, while the draw deck holds more than one card, turns up its top card and"
                + " plays that. After playing from hand you draw back up to " + params.handSize + " cards. The higher"
                + " card wins: the winner takes both cards (face up) and leads the next trick, to the player on their"
                + " left. Equal cards 'bounce': they are left aside and the same two play again, and the next winner"
                + " takes those cards too.</p>"
                + "<p>The last card of the draw deck is the trump card: whoever draws it keeps it face down, apart"
                + " from their hand. Phase one ends when the player due to play has no cards. Everyone then adds"
                + " their hand, any bounced cards and (for its drawer) the trump card to the cards they won. The trump"
                + " card's suit is trumps, and its drawer leads phase two.</p>"
                + "<h3>Phase two</h3><p>Lead any card. After that each player must beat the top card of the trick -"
                + " with a higher card of its suit, or with any trump if it is not a trump - or pick it up. You may"
                + " pick up even when you could beat. A trick is complete when it holds as many cards as there were"
                + " players holding cards when it began; its cards are then discarded and " + completer + ". If every"
                + " card of a trick is picked up, the next player leads.</p>"
                + "<h3>Scoring</h3><p>A player who plays their last card is out, and scores the number of players"
                + " who were holding cards when that trick began (3 players: usually 3 for the first out, 2 for the"
                + " second). The game ends as soon as only one player holds cards; they score 0." + ties
                + " As a safeguard the game also ends after " + params.maxPhaseTwoActions + " phase-two actions,"
                + " with everyone still holding cards scoring 0.</p>"
                + "<h3>Interface</h3><p>Each player's row shows their phase-one hand above their won cards, which are"
                + " face up and in phase two are the cards they must get rid of. The player to act has a blue border."
                + " On the table: the draw deck, the trump card, the trick (in phase two the top card, the one to"
                + " beat, is brightest) and any bounced cards.</p>"
                + "</body></html>";
    }
}
