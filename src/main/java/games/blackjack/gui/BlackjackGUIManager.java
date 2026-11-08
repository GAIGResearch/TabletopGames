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
import games.GameType;
import gui.views.RulesView;
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
            agentNames[i] = game.getPlayers().get(i).toString();
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
        RulesView.addTabs(tabs, GameType.Blackjack, params, height);

        JPanel infoPanel = createGameStateInfoPanel("Blackjack", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);
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
}
