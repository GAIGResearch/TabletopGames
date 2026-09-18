package games.blackjack.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.FrenchCard;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.CardView;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Set;

/**
 * <p>GUI for Blackjack: the dealer at the top of the table, and the players in a row (or two) below.</p>
 *
 * <p>This is a view only - it holds no rules and never changes the state. Every player's cards are dealt face up,
 * so they are always shown; the dealer's hole card is face down until it is turned up (or in full-observability
 * mode).</p>
 */
public class BlackjackGUIManager extends AbstractGUIManager {

    static final String dataPath = "data/FrenchCards/";
    static final int cardWidth = 60;
    static final int cardHeight = 80;
    // the horizontal step between overlapping cards of one hand
    static final int cardOffset = 20;
    static final int playersPerRow = 4;

    BlackjackDealerView dealerView;
    BlackjackPlayerView[] playerViews;
    TitledBorder[] playerTitles;
    String[] agentNames;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public BlackjackGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        BlackjackGameState state = (BlackjackGameState) gameState;
        BlackjackParameters params = (BlackjackParameters) state.getGameParameters();
        int nPlayers = state.getNPlayers();
        // getMaxActionSpace is called by the super constructor, before the parameters are known
        maxActionSpace = maxActions(params);

        dealerView = new BlackjackDealerView();
        playerViews = new BlackjackPlayerView[nPlayers];
        playerTitles = new TitledBorder[nPlayers];
        agentNames = new String[nPlayers];

        JPanel players = new JPanel(new GridLayout(0, Math.min(nPlayers, playersPerRow), 6, 6));
        players.setOpaque(false);
        for (int i = 0; i < nPlayers; i++) {
            playerViews[i] = new BlackjackPlayerView(i, params.splitting);
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
            playerTitles[i] = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                    "Player " + i, TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            playerViews[i].setBorder(playerTitles[i]);
            players.add(playerViews[i]);
        }

        JPanel table = new JPanel();
        table.setOpaque(false);
        table.setLayout(new BoxLayout(table, BoxLayout.Y_AXIS));
        JPanel dealerRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        dealerRow.setOpaque(false);
        dealerRow.add(dealerView);
        JPanel playersRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        playersRow.setOpaque(false);
        playersRow.add(players);
        table.add(dealerRow);
        table.add(playersRow);

        int rows = (nPlayers + playersPerRow - 1) / playersPerRow;
        Dimension playerSize = playerViews[0].getPreferredSize();
        // each player area has its titled border below it, and each row the grid and flow gaps
        this.width = Math.max(dealerView.getPreferredSize().width,
                Math.min(nPlayers, playersPerRow) * (playerSize.width + 16)) + 40;
        this.height = dealerView.getPreferredSize().height + rows * (playerSize.height + 36) + 40;

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
        tabs.add("Rules", createRulesPanel(params));

        JPanel infoPanel = createGameStateInfoPanel("Blackjack", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false);
        main.add(infoPanel, BorderLayout.NORTH);
        main.add(table, BorderLayout.CENTER);
        main.add(actionPanel, BorderLayout.SOUTH);

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 40));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    /**
     * The most actions at one decision: Betting offers every even amount from minBet to maxBet (if the player has
     * the chips), Insurance offers 2, and Play at most 4 (Hit, Stand, DoubleDown, Split).
     */
    static int maxActions(BlackjackParameters params) {
        return Math.max(4, (params.maxBet - params.minBet) / 2 + 1);
    }

    /**
     * Called by the super constructor before the parameters are available, so this covers the largest maxBet that
     * BlackjackParameters offers (50); the constructor then sets the exact figure from the game's parameters.
     */
    @Override
    public int getMaxActionSpace() {
        return (50 - 2) / 2 + 1;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof BlackjackGameState state)) return;
        int currentPlayer = state.getCurrentPlayer();
        dealerView.update(state, state.getCoreGameParameters().alwaysDisplayFullObservable);
        for (int i = 0; i < playerViews.length; i++) {
            playerViews[i].update(state);
            String result = state.isNotTerminal() ? "" : " - " + state.getPlayerResults()[i];
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]" + result);
            playerViews[i].setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerTitles[i])
                    : playerTitles[i]);
        }
        parent.repaint();
    }

    /**
     * Draw a hand in the order the cards were dealt, overlapping left to right. Deck.add puts each new card at
     * index 0, so the first card dealt is the last in the Deck. A {@code hole} card (the dealer's), if given,
     * follows them, face up only if {@code showHole}. Cards are {@code maxStep} apart, or closer if needed to fit
     * {@code maxWidth}.
     */
    static void drawHand(Graphics2D g, Deck<FrenchCard> hand, int x, int y, int maxWidth, int maxStep,
                         FrenchCard hole, boolean showHole) {
        int n = hand.getSize() + (hole == null ? 0 : 1);
        if (n == 0) return;
        int step = n == 1 ? 0 : Math.min(maxStep, (maxWidth - cardWidth) / (n - 1));
        int i = 0;
        for (int c = hand.getSize() - 1; c >= 0; c--, i++)
            drawCard(g, hand.get(c), new Rectangle(x + i * step, y, cardWidth, cardHeight), true);
        if (hole != null)
            drawCard(g, hole, new Rectangle(x + i * step, y, cardWidth, cardHeight), showHole);
    }

    static void drawCard(Graphics2D g, FrenchCard card, Rectangle rect, boolean faceUp) {
        Image back = ImageIO.GetInstance().getImage(dataPath + "gray_back.png");
        CardView.drawCard(g, rect, card, cardImage(card), back, faceUp);
    }

    /**
     * Image file names are &lt;number&gt;&lt;suit&gt;.png for spot cards and &lt;type&gt;&lt;suit&gt;.png for the
     * others - so an Ace is "AceHearts.png", not "14Hearts.png".
     */
    static Image cardImage(FrenchCard card) {
        String name = card.type == FrenchCard.FrenchCardType.Number
                ? card.number + card.suite.name()
                : card.type.name() + card.suite.name();
        return ImageIO.GetInstance().getImage(dataPath + name + ".png");
    }

    /**
     * A hand's total as shown to players: "soft" when an Ace counts 11, "bust" over 21.
     */
    static String describeTotal(java.util.List<FrenchCard> cards) {
        int total = BlackjackGameState.handValue(cards);
        if (total > BlackjackGameState.BLACKJACK) return "bust (" + total + ")";
        return (BlackjackGameState.isSoft(cards) ? "soft " : "") + total;
    }

    private JPanel createRulesPanel(BlackjackParameters params) {
        JPanel rules = new JPanel();
        rules.setBackground(new Color(43, 108, 25, 111));
        String payout = params.payout21NaturalOnly
                ? "A <b>natural</b> (an Ace and a ten-value card as your first two cards) is paid " + params.payout21
                + " : 1 at once; other wins pay 1 : 1."
                : "A winning hand of <b>21</b> (any number of cards) pays " + params.payout21 + " : 1; other wins pay 1 : 1.";
        JLabel text = new JLabel("<html><center><h1>Blackjack</h1></center><hr>" +
                "<p>Each player plays against the dealer (the bank), not against each other. Try to finish with more " +
                "chips than you started with (" + params.startingChips + ").</p><ul>" +
                "<li>Cards count their face value; J, Q and K count 10; an Ace counts 11, or 1 if 11 would take the " +
                "total over 21 (a hand with an Ace counted 11 is <i>soft</i>). Over 21 is <b>bust</b> and loses.</li>" +
                "<li><b>Bet</b> an even number of chips from " + params.minBet + " to " + params.maxBet +
                ". You get two cards face up; the dealer one face up and one face down (the hole card).</li>" +
                "<li><b>Insurance</b>: if the dealer shows an Ace or a ten-value card, you may pay half your bet as " +
                "insurance. If the dealer has Blackjack it pays 2 : 1; otherwise it is lost.</li>" +
                "<li>The dealer then checks the hole card. A dealer Blackjack ends the hand: a player natural is a " +
                "push (bet returned), everyone else loses.</li>" +
                "<li><b>Hit</b> to take a card, <b>Stand</b> to stop." +
                (params.doubleDown ? " <b>Double down</b>: double your bet on your first two cards, take exactly " +
                        "one more card and stop." : "") +
                (params.splitting ? " <b>Split</b> a pair of the same rank into two hands, each with your bet " +
                        "(up to " + params.maxHandsAfterSplit + " hands; split Aces get one card each)." : "") +
                "</li>" +
                "<li>The dealer then turns the hole card up and draws until 17 or more" +
                (params.dealerHitsSoft17 ? ", drawing on a soft 17" : ", standing on a soft 17") + ".</li>" +
                "<li>Beat the dealer's total (or have the dealer bust) to win; an equal total is a push. " + payout +
                "</li>" +
                (params.nHands > 1 ? "<li>The game lasts " + params.nHands + " hands. A player without the " +
                        "chips for the minimum bet sits out.</li>" : "") +
                "</ul><hr><p><b>INTERFACE:</b> choose from the action buttons at the bottom of the screen. The hand " +
                "being played is outlined in yellow.</p></html>");
        text.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(width * 2 / 3 + 60, height * 2 / 3 + 100));
        rules.add(scroll);
        return rules;
    }
}
