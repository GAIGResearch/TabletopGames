package games.whist.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.FrenchCard;
import games.tricktaking.gui.CardArt;
import games.tricktaking.gui.PlayerHandView;
import games.tricktaking.gui.TrickView;
import games.whist.WhistGameState;
import games.whist.WhistParameters;
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
 * <p>GUI for Whist: one area per player round the table, with the partnerships opposite each other, and the trick
 * in progress in the centre.</p>
 *
 * <p>This is a view only - it holds no rules and never changes the state. It is repainted for every player,
 * human or not, so a hand is only shown face-up when the viewer is entitled to see it (see showHand).</p>
 */
public class WhistGUIManager extends AbstractGUIManager {

    // 13 cards are dealt to each player, so a hand needs a wide area
    static final int playerAreaWidth = 420;
    // one player area: the cards, the status line underneath, and the titled border below that
    static final int playerAreaHeight = CardArt.cardHeight + 43;

    PlayerHandView[] playerViews;
    TrickView trickView;
    Border[] playerViewBorders;
    TitledBorder[] playerTitles;
    String[] agentNames;
    JLabel scoreLabel;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public WhistGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        WhistGameState state = (WhistGameState) gameState;
        int nPlayers = state.getNPlayers();

        // wide enough for the longest line: the suit led and what wins the trick
        trickView = new TrickView(nPlayers, 520);
        this.width = Math.max(playerAreaWidth + 2 * 40, trickView.getPreferredSize().width + 2 * playerAreaWidth);
        // three bands: the North player, the East/West players and the trick (the taller of the two), and the South player
        this.height = playerAreaHeight * 2 + Math.max(playerAreaHeight, trickView.getPreferredSize().height) + 60;

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
        tabs.add("Rules", createRulesPanel());

        // Player areas: player 0 at the bottom, then clockwise round the table, so partners face each other
        playerViews = new PlayerHandView[nPlayers];
        playerViewBorders = new Border[nPlayers];
        playerTitles = new TitledBorder[nPlayers];
        agentNames = new String[nPlayers];
        JPanel mainGameArea = new JPanel(new BorderLayout());
        mainGameArea.setOpaque(false);
        String[] locations = {BorderLayout.SOUTH, BorderLayout.WEST, BorderLayout.NORTH, BorderLayout.EAST};
        JPanel[] sides = new JPanel[locations.length];
        for (int s = 0; s < sides.length; s++) {
            sides[s] = new JPanel(new GridBagLayout());
            sides[s].setOpaque(false);   // an unused side would otherwise paint a grey block over the table
        }
        for (int i = 0; i < nPlayers; i++) {
            PlayerHandView playerView = new PlayerHandView(state.getPlayerHand(i), i, playerAreaWidth);
            playerView.setOpaque(false);
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i,
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            title.setTitleColor(Color.white);   // the table background is dark
            playerTitles[i] = title;
            playerViewBorders[i] = title;
            playerView.setBorder(title);
            sides[i % locations.length].add(playerView);
            playerViews[i] = playerView;
        }
        for (int s = 0; s < locations.length; s++)
            mainGameArea.add(sides[s], locations[s]);

        JPanel centre = new JPanel(new BorderLayout());
        centre.setOpaque(false);
        JPanel centreWrapper = new JPanel(new GridBagLayout());
        centreWrapper.setOpaque(false);
        centreWrapper.add(trickView);
        centre.add(centreWrapper, BorderLayout.CENTER);
        scoreLabel = new JLabel("", SwingConstants.CENTER);
        scoreLabel.setForeground(Color.white);   // the table background is dark
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 14f));
        centre.add(scoreLabel, BorderLayout.SOUTH);
        mainGameArea.add(centre, BorderLayout.CENTER);

        JPanel infoPanel = createGameStateInfoPanel("Whist", gameState, width, defaultInfoPanelHeight);
        // 13 cards can be legal at once, so the action panel is the vertical, scrolling list, and taller than usual
        int actionPanelHeight = defaultActionPanelHeight * 2;
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, actionPanelHeight, true);

        main.add(infoPanel, BorderLayout.NORTH);
        main.add(mainGameArea, BorderLayout.CENTER);
        main.add(actionPanel, BorderLayout.SOUTH);

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + actionPanelHeight + defaultInfoPanelHeight + 40));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    /**
     * The only action is PlayCard, one per card the player may legally play, and a player is dealt 13 cards - so a
     * leader with a full hand has 13 to choose from.
     */
    @Override
    public int getMaxActionSpace() {
        return 13;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof WhistGameState state)) return;
        WhistParameters params = (WhistParameters) state.getGameParameters();
        int currentPlayer = state.getCurrentPlayer();
        int dealer = state.getDealer();

        for (int i = 0; i < playerViews.length; i++) {
            String extra = "team " + state.getTeam(i) + ",  tricks " + state.getTricksTaken(i);
            playerViews[i].update(state.getPlayerHand(i), showHand(state, i), state.getKnownVoids().get(i), extra);
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]"
                    + (i == dealer ? " - dealer" : "") + (i == state.getCurrentTrick().getLeader() ? " - led" : ""));
            playerViews[i].setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i])
                    : playerViewBorders[i]);
        }
        updateTrickView(state, params);
        scoreLabel.setText("Points - team 0: " + state.getTeamPoints(0)
                + "    team 1: " + state.getTeamPoints(1));
        parent.repaint();
    }

    /**
     * The centre panel: the deal and trick numbers, what is trumps, the suit led, and the tricks each side has won.
     */
    private void updateTrickView(WhistGameState state, WhistParameters params) {
        // the whole pack is dealt, so there are as many tricks in a deal as cards in a hand
        int tricksInDeal = 52 / state.getNPlayers();
        int tricksDone = state.getDiscardPile().getSize() / state.getNPlayers();
        int trick = Math.min(tricksDone + 1, tricksInDeal);
        String header = (params.nDeals > 1 ? "Deal " + (state.getRoundCounter() + 1) + " of " + params.nDeals + "   " : "")
                + "Trick " + trick + " of " + tricksInDeal + "   -   " + trumpText(state);
        FrenchCard.Suite lead = state.getCurrentTrick().getLeadSuit();
        String leadText;
        if (!state.isNotTerminal()) {
            header = trumpText(state);
            int points0 = state.getTeamPoints(0), points1 = state.getTeamPoints(1);
            leadText = points0 == points1 ? "Game over - the sides are level on " + points0 + " points"
                    : "Game over - team " + (points0 > points1 ? 0 : 1) + " wins, "
                      + Math.max(points0, points1) + " points to " + Math.min(points0, points1);
        } else leadText = lead == null
                ? "Player " + state.getCurrentTrick().getLeader() + " to lead"
                : "Suit led: " + CardArt.suitText(lead) + "   -   "
                  + (state.getTrumpSuit() == null ? "highest " + lead.name() : "highest trump") + " wins";
        String footer = "Tricks this deal - team 0: " + state.getTeamTricks(0)
                + "     team 1: " + state.getTeamTricks(1) + "     (each trick over six scores a point)";
        // a turned-up trump card is named in the header, which is bold and short
        trickView.update(state.getCurrentTrick(), state.getTrumpSuit(), header, leadText, footer, state.isNotTerminal());
    }

    /**
     * What is trumps, and the turned-up card while the dealer still holds it.
     */
    private String trumpText(WhistGameState state) {
        if (state.getTrumpSuit() == null)
            return "No trumps";
        String text = "Trumps: " + CardArt.suitText(state.getTrumpSuit());
        FrenchCard turnUp = state.getTrumpCard();
        if (turnUp != null && state.getPlayerHand(state.getDealer()).contains(turnUp))
            return text + " (" + CardArt.shortName(turnUp) + " turned up)";
        return text;
    }

    /**
     * A hand is face-up only for a human player, for the current player if the core parameters allow it,
     * or in full-observability mode.
     */
    private boolean showHand(WhistGameState state, int playerId) {
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }

    private JPanel createRulesPanel() {
        JPanel rules = new JPanel();
        rules.setBackground(new Color(43, 108, 25, 111));
        JLabel text = new JLabel("<html><center><h1>Whist</h1></center><hr>" +
                "<p>The classic English trick-taking game, for four players in two fixed partnerships: " +
                "<b>players 0 and 2 against players 1 and 3</b>, sitting opposite each other.</p><ul>" +
                "<li>All 52 cards are dealt, 13 each, one at a time from the dealer's left. Aces are high.</li>" +
                "<li>The dealer's last card is turned face up and its suit is trumps. The dealer keeps it and " +
                "may play it like any other card. (With the rotation option instead, trumps run Hearts, Diamonds, " +
                "Spades, Clubs through successive deals.)</li>" +
                "<li>The player on the dealer's left leads to the first trick, and may lead any card.</li>" +
                "<li>You must follow the suit led if you can; otherwise play any card - trumping is never " +
                "compulsory. Failing to follow tells everyone you hold none of that suit.</li>" +
                "<li>The highest trump wins the trick, or if no trump was played, the highest card of the suit " +
                "led. The winner leads the next trick.</li>" +
                "<li>After all 13 tricks, the side with more tricks scores <b>one point for each trick over six</b> " +
                "(7 tricks = 1 point, 13 = 7). The other side scores nothing.</li>" +
                "<li>The deal passes to the left. After the agreed number of deals the side with more points wins; " +
                "level points are a draw. By default a game is a single deal.</li>" +
                "</ul><hr><p><b>INTERFACE:</b> choose a card from the action buttons at the bottom of the screen. " +
                "The centre shows the trick so far, with the winning card outlined, what is trumps, and the tricks " +
                "each side has won. A player's area shows the suits they are known to be void in.</p></html>");
        text.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(width * 2 / 3 + 60, height * 2 / 3 + 100));
        rules.add(scroll);
        return rules;
    }
}
