package games.pitch.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.pitch.PitchGameState;
import games.pitch.PitchParameters;
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
 * Player 0 sits at the bottom, then clockwise: player 1 on the left, 2 at the top, 3 on the right. The trick and the
 * deal's public information are in the middle.
 */
public class PitchGUIManager extends AbstractGUIManager {

    static final int CARD_WIDTH = 60, CARD_HEIGHT = 80;
    // six cards side by side, just overlapping
    static final int HAND_WIDTH = 6 * CARD_WIDTH + 20, SEAT_WIDTH = HAND_WIDTH + 20, SEAT_HEIGHT = CARD_HEIGHT + 35;
    static final int TABLE_WIDTH = 340, TABLE_HEIGHT = 300;

    private PitchDeckView[] hands;
    private TitledBorder[] seatTitles;
    private JPanel[] seats;
    private PitchTableView table;
    private final Border activeBorder = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public PitchGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null || game.getGameState() == null) return;
        PitchGameState state = (PitchGameState) game.getGameState();
        int nPlayers = state.getNPlayers();

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);
        parent.setBackground(ImageIO.GetInstance().getImage(PitchDeckView.DATA_PATH + "table-background.jpg"));

        hands = new PitchDeckView[nPlayers];
        seatTitles = new TitledBorder[nPlayers];
        seats = new JPanel[nPlayers];
        JPanel tableArea = new JPanel(new BorderLayout());
        tableArea.setOpaque(false);
        String[] places = {BorderLayout.SOUTH, BorderLayout.WEST, BorderLayout.NORTH, BorderLayout.EAST};
        for (int p = 0; p < nPlayers; p++) {
            hands[p] = new PitchDeckView(p, state.getPlayerHand(p), false, HAND_WIDTH, CARD_HEIGHT);
            seatTitles[p] = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                    "Player " + p, TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            seatTitles[p].setTitleColor(Color.WHITE);
            seats[p] = new JPanel(new GridBagLayout());
            seats[p].setOpaque(false);
            seats[p].add(hands[p]);
            seats[p].setBorder(seatTitles[p]);
            Dimension seatSize = new Dimension(SEAT_WIDTH, SEAT_HEIGHT);
            seats[p].setPreferredSize(seatSize);
            JPanel holder = new JPanel(new GridBagLayout());
            holder.setOpaque(false);
            holder.add(seats[p]);
            tableArea.add(holder, places[p]);
        }
        table = new PitchTableView(TABLE_WIDTH, TABLE_HEIGHT);
        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        middle.add(table);
        tableArea.add(middle, BorderLayout.CENTER);

        width = 2 * SEAT_WIDTH + TABLE_WIDTH + 20;
        int tableHeight = 2 * SEAT_HEIGHT + Math.max(TABLE_HEIGHT, SEAT_HEIGHT) + 20;

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(createGameStateInfoPanel("Pitch", state, width, defaultInfoPanelHeight), BorderLayout.NORTH);
        main.add(tableArea, BorderLayout.CENTER);
        main.add(createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false), BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        tabs.add("Main", main);
        tabs.add("Rules", new RulesView(rulesHtml((PitchParameters) state.getGameParameters()), tableHeight));

        height = tableHeight + defaultInfoPanelHeight + defaultActionPanelHeight + 60;
        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height));
        parent.revalidate();
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // bidding offers at most Pass and the bids 2 to 5 (5 actions); play at most the 6 cards in hand
        return 6;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (hands == null) return;
        PitchGameState state = (PitchGameState) gameState;
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

    private String seatTitle(PitchGameState state, int p) {
        String agent = game.getPlayers() == null ? "" : " [" + game.getPlayers().get(p).toString() + "]";
        String role = p == state.getDealer() ? ", dealer" : "";
        int bid = state.getPlayerBid(p);
        String bidText = bid < 0 ? "" : bid == 0 ? " - passed" : " - bid " + bid;
        return "Player " + p + agent + " (team " + state.getTeam(p) + role + ")" + bidText;
    }

    private static String rulesHtml(PitchParameters params) {
        String hlj = params.countHighLowSeparately
                ? "A card that is two or three of High, Low and Jack scores 1 for each of them."
                : "A card that is two or three of High, Low and Jack scores only 1.";
        String winning = params.targetScore <= 1
                ? "The game is a single deal. The team with the higher score wins, and teams with the same score " +
                  "draw."
                : "Deals continue until the pitching team makes its bid and ends the deal with a score of " +
                  params.targetScore + " or more. That team wins, whatever the other team's score.";
        return "<h2>Pitch</h2>" +
                "<p>Players 0 and 2 (team 0) play against players 1 and 3 (team 1). Each player is dealt " +
                params.handSize + " cards from a 52-card deck, and the other cards are not used. Aces are high. " +
                "The deal passes to the left after each deal.</p>" +
                "<p><b>Bidding.</b> There is one round of bidding, starting on the dealer's left. Each player " +
                "passes, or bids from " + params.minBid + " to " + params.smudgeBid + " points. A bid must be " +
                "higher than the highest bid so far. The dealer bids last, and may take the bid by equalling the " +
                "highest bid. If everyone else has passed, the dealer must bid. The highest bidder is the " +
                "pitcher.</p>" +
                "<p><b>Play.</b> The pitcher leads to the first trick, and the suit of that card is trumps for " +
                "the deal. If you hold a card of the suit led, you must play a card of that suit or a trump. " +
                "Otherwise you may play any card. The highest trump wins the trick. If the trick holds no trump, " +
                "the highest card of the suit led wins. The winner leads the next trick.</p>" +
                "<p><b>Points.</b> At the end of the deal each team takes these points from the cards in the " +
                "tricks it won.</p>" +
                "<table border=1 cellpadding=4 cellspacing=0>" +
                "<tr><td>High</td><td>1 for the highest trump played</td></tr>" +
                "<tr><td>Low</td><td>1 for the lowest trump played</td></tr>" +
                "<tr><td>Jack</td><td>1 for the Jack of trumps, if it was dealt</td></tr>" +
                "<tr><td>Game</td><td>1 for the higher total of card values below (nobody scores it when the " +
                "totals are equal)</td></tr></table>" +
                "<p>" + hlj + " The card values for Game are:</p>" +
                "<table border=1 cellpadding=4 cellspacing=0>" +
                "<tr><th align=left>Card</th><td align=center>Ace</td><td align=center>King</td>" +
                "<td align=center>Queen</td><td align=center>Jack</td><td align=center>Ten</td></tr>" +
                "<tr><th align=left>Value</th><td align=center>" + params.gameValueAce + "</td><td align=center>" +
                params.gameValueKing + "</td><td align=center>" + params.gameValueQueen + "</td><td align=center>" +
                params.gameValueJack + "</td><td align=center>" + params.gameValueTen + "</td></tr></table>" +
                "<p><b>Scoring a deal.</b> The other team scores its points. The pitching team scores its points " +
                "if they reach its bid, and otherwise loses the value of its bid. A bid of " + params.smudgeBid +
                " is a smudge. The pitching team will then score " + params.smudgePoints + " if it wins every " +
                "trick and all four points, and otherwise lose " + params.smudgePoints + ".</p>" +
                "<p><b>Winning.</b> " + winning + "</p>" +
                "<h3>Interface</h3>" +
                "<p>Choose Pass, a Bid or a card to Play from the action buttons at the bottom. Player 0 sits at " +
                "the bottom, with players 1, 2 and 3 to the left, top and right. Each seat's title gives the " +
                "player's team, marks the dealer, and shows their bid or \"passed\". The player to act has a " +
                "blue border.</p>" +
                "<p>The middle of the table shows the current trick, each card on the side of the player who " +
                "played it. Below the trick are the trump suit, the pitcher and their bid, and each team's score " +
                "and tricks won in this deal. When the game ends it also shows the points each team took in the " +
                "last deal.</p>";
    }
}
