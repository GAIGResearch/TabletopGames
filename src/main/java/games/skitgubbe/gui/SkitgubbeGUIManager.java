package games.skitgubbe.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.skitgubbe.SkitgubbeGameState;
import games.skitgubbe.SkitgubbeParameters;
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
        main.add(createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight), BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        tabs.add("Main", main);
        tabs.add("Rules", new RulesView(rulesHtml((SkitgubbeParameters) state.getGameParameters()), tableHeight));

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

    private static String rulesHtml(SkitgubbeParameters params) {
        int n = params.handSize;
        String completer = params.completerLeads
                ? "The player who completed it leads the next trick. If they are out, the next player holding " +
                "cards leads."
                : "The next player holding cards leads the next trick.";
        String ties = params.exitOrderTiebreak
                ? "Of players with the same score, the one who went out first ranks higher."
                : "Players with the same score share their place.";
        return "<h2>Skitgubbe</h2>" +
                "<p>In phase one you win cards in tricks. In phase two you try to get rid of them. The last player " +
                "holding cards loses (the skitgubbe).</p>" +
                "<p><b>Cards.</b> The pack has 52 cards, and Ace is high.</p>" +
                "<h3>Phase one</h3>" +
                "<p>Each player is dealt " + n + " cards, and the rest form the draw deck. Each trick is played " +
                "by two players, the leader and the player on their left. Suits do not matter.</p><ul>" +
                "<li>Play any card from your hand, or turn up the top card of the draw deck and play it. You may " +
                "turn up a card while the draw deck holds more than one card.</li>" +
                "<li>After playing from your hand you draw a card, to keep " + n + " in hand while the draw deck " +
                "lasts.</li>" +
                "<li>The higher card wins the trick. The winner takes both cards face up, and leads the next " +
                "trick.</li>" +
                "<li>Cards of the same rank bounce. They are set aside, and the same two players play again. The " +
                "winner of the next trick will take the bounced cards too.</li></ul>" +
                "<p>The last card of the draw deck is the trump card. The player who draws it keeps it face down, " +
                "apart from their hand.</p>" +
                "<p>Phase one ends when the player due to play has no cards. A card led and not answered goes back " +
                "to its leader. Each player then adds their hand and their bounced cards to the cards they won, and " +
                "the player who drew the trump card adds it too. The trump card's suit is trumps.</p>" +
                "<h3>Phase two</h3>" +
                "<p>The player who drew the trump card leads first.</p><ul>" +
                "<li>The leader plays any card.</li>" +
                "<li>Each later player must beat the top card of the trick or pick it up. A card beats it if it is " +
                "a higher card of the same suit, or a trump when the top card is not a trump. You may pick up even " +
                "when you could beat.</li>" +
                "<li>A trick is complete when it holds as many cards as there were players holding cards when it " +
                "began. Its cards are then discarded. " + completer + "</li>" +
                "<li>If every card of a trick is picked up, the next player leads.</li></ul>" +
                "<h3>Scoring</h3>" +
                "<p>A player who plays their last card is out. They score the number of players who were holding " +
                "cards when that trick began. A player with no cards at the start of phase two scores as if out in " +
                "its first trick. The game ends as soon as only one player holds cards, and that player scores 0. " +
                "The highest score wins. " + ties + " The game also ends after " + params.maxPhaseTwoActions +
                " actions in phase two, and every player still holding cards then scores 0.</p>" +
                "<h3>Interface</h3>" +
                "<p>The players are in a column on the left. Each title shows the player's number and agent, the " +
                "cards won in phase one or the cards to get rid of in phase two, \"OUT\" and the score once out, " +
                "and \"holds the trump card\" for the player who drew it. In phase one a player's hand is above " +
                "the cards they have won. The player to act has a blue border.</p>" +
                "<p>The table shows the draw deck, the trump card, the discarded cards in phase two, and the " +
                "trick. The trump card is face down except to the player who drew it, and in phase two its suit " +
                "is shown in its place. In phase one the led card is labelled with its leader (P0, P1 and so on) " +
                "until it is answered. In phase two the top card is the brightest, and it is the one to " +
                "beat. The bounced cards are shown beside the number of the player who played them. The lines " +
                "below say whose turn it is and what they are to do, how many cards the trick needs, and the " +
                "players who are out with their scores.</p>" +
                "<p>The action buttons are Play (a card), Turn up the top card of the draw deck, and Pick up.</p>";
    }
}
