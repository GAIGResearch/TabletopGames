package games.euchre.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.FrenchCard;
import games.euchre.EuchreGameState;
import games.euchre.EuchreParameters;
import games.tricktaking.gui.CardArt;
import games.tricktaking.gui.PlayerHandView;
import games.tricktaking.gui.TrickView;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import games.GameType;
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
 * <p>GUI for Euchre: one area per player round the table, with the partnerships opposite each other, and in the
 * centre the up-card and the trick in progress.</p>
 *
 * <p>This is a view only - it holds no rules and never changes the state. It is repainted for every player,
 * human or not, so a hand is only shown face-up when the viewer is entitled to see it (see showHand).</p>
 */
public class EuchreGUIManager extends AbstractGUIManager {

    // up to 6 cards (the dealer after taking the up-card)
    static final int playerAreaWidth = 360;
    // one player area: the cards, the status line underneath, and the titled border below that
    static final int playerAreaHeight = CardArt.cardHeight + 43;

    PlayerHandView[] playerViews;
    TrickView trickView;
    UpCardView upCardView;
    Border[] playerViewBorders;
    TitledBorder[] playerTitles;
    String[] agentNames;
    JLabel scoreLabel;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public EuchreGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        EuchreGameState state = (EuchreGameState) gameState;
        int nPlayers = state.getNPlayers();

        // wide enough for the longest line: who is deciding on trumps, or the suit led and what wins the trick
        trickView = new TrickView(nPlayers, 440);
        upCardView = new UpCardView();
        int centreWidth = upCardView.getPreferredSize().width + 10 + trickView.getPreferredSize().width;
        int centreHeight = Math.max(upCardView.getPreferredSize().height, trickView.getPreferredSize().height);
        this.width = Math.max(playerAreaWidth + 2 * 40, centreWidth + 2 * playerAreaWidth);
        // three bands: the North player, the East/West players and the centre (the taller), and the South player
        this.height = playerAreaHeight * 2 + Math.max(playerAreaHeight, centreHeight) + 60;

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
        RulesView.addTabs(tabs, GameType.Euchre, state.getGameParameters(), height);

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
            agentNames[i] = game.getPlayers().get(i).toString();
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
        GridBagConstraints gap = new GridBagConstraints();
        gap.insets = new Insets(0, 0, 0, 10);
        centreWrapper.add(upCardView, gap);
        centreWrapper.add(trickView);
        centre.add(centreWrapper, BorderLayout.CENTER);
        scoreLabel = new JLabel("", SwingConstants.CENTER);
        scoreLabel.setForeground(Color.white);   // the table background is dark
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 14f));
        centre.add(scoreLabel, BorderLayout.SOUTH);
        mainGameArea.add(centre, BorderLayout.CENTER);

        JPanel infoPanel = createGameStateInfoPanel("Euchre", gameState, width, defaultInfoPanelHeight);
        // up to 7 actions at once, so the action panel is the vertical, scrolling list, and taller than usual
        int actionPanelHeight = defaultActionPanelHeight * 2;
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, actionPanelHeight);

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

    @Override
    public int getMaxActionSpace() {
        // the second round of choosing trumps: Pass, and a CallTrump for each of the 3 suits other than the
        // up-card's, with and without going alone. The dealer's Discard offers 6 cards, and a play at most 5.
        return 7;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof EuchreGameState state)) return;
        int currentPlayer = state.getCurrentPlayer();
        int dealer = state.getDealer();

        for (int i = 0; i < playerViews.length; i++) {
            String extra = i == state.getSittingOut() ? "sitting out"
                    : "team " + state.getTeam(i) + ",  tricks " + state.getTricksTaken(i);
            playerViews[i].update(state.getPlayerHand(i), showHand(state, i), state.getKnownVoids().get(i), extra);
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]"
                    + (i == dealer ? " - dealer" : "") + (i == state.getMaker() ? " - maker" : ""));
            playerViews[i].setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i])
                    : playerViewBorders[i]);
        }
        updateUpCardView(state);
        updateTrickView(state);
        scoreLabel.setText("Points - team 0: " + state.getTeamPoints(0) + "    team 1: " + state.getTeamPoints(1)
                + "    (target " + ((EuchreParameters) state.getGameParameters()).targetScore + ")");
        parent.repaint();
    }

    /**
     * Face-up while its suit is on offer and once the dealer has taken it; face-down once turned down.
     */
    private void updateUpCardView(EuchreGameState state) {
        int handSize = ((EuchreParameters) state.getGameParameters()).handSize;
        FrenchCard upCard = state.getUpCard();
        if (state.getTrumpSuit() == null) {
            if (state.isFirstBiddingRound())
                upCardView.update(upCard, true, CardArt.suitText(upCard.suite) + " as trumps?");
            else
                upCardView.update(upCard, false, "Turned down");
        } else if (state.getTrumpSuit() != upCard.suite) {
            upCardView.update(upCard, false, "Turned down");   // trumps were named in the second round
        } else if (state.getDealerDiscard() != null || state.getPlayerHand(state.getDealer()).getSize() > handSize) {
            upCardView.update(upCard, true, "Taken by the dealer");
        } else {
            // the dealer sat out and did not take it (EuchreParameters.sittingOutDealerPicksUp is false)
            upCardView.update(upCard, true, "Left in the kitty");
        }
    }

    /**
     * The centre panel: what is trumps and who made them, who is deciding or to lead, the suit led, and the tricks
     * each side has won.
     */
    private void updateTrickView(EuchreGameState state) {
        int handSize = ((EuchreParameters) state.getGameParameters()).handSize;
        int tricksDone = state.getTricksTaken(0) + state.getTricksTaken(1) + state.getTricksTaken(2)
                + state.getTricksTaken(3);
        String header;
        String leadText;
        FrenchCard.Suite lead = state.getCurrentTrick().getLeadSuit();
        if (!state.isNotTerminal()) {
            header = trumpText(state);
            int points0 = state.getTeamPoints(0), points1 = state.getTeamPoints(1);
            leadText = points0 == points1 ? "Game over - the teams are level on " + points0 + " points"
                    : "Game over - team " + (points0 > points1 ? 0 : 1) + " wins, "
                      + Math.max(points0, points1) + " points to " + Math.min(points0, points1);
        } else if (state.getTrumpSuit() == null) {
            header = "Deal " + (state.getRoundCounter() + 1) + "   -   choosing trumps, "
                    + (state.isFirstBiddingRound() ? "first round" : "second round");
            leadText = "Player " + state.getCurrentPlayer() + " to decide"
                    + (state.isFirstBiddingRound() ? " on " + state.getUpCard().suite.name()
                       : state.getCurrentPlayer() == state.getDealer() ? " - the dealer must name a suit"
                       : " - any suit but " + state.getUpCard().suite.name());
        } else if (state.getPlayerHand(state.getDealer()).getSize() > handSize) {
            header = trumpText(state);
            leadText = "Player " + state.getDealer() + " (the dealer) to discard";
        } else {
            header = "Trick " + Math.min(tricksDone + 1, handSize) + " of " + handSize + "   -   " + trumpText(state);
            leadText = lead == null ? "Player " + state.getCurrentTrick().getLeader() + " to lead"
                    : "Suit led: " + CardArt.suitText(lead) + "   -   highest trump wins";
        }
        String footer = "Tricks this deal - team 0: " + state.getTeamTricks(0)
                + "     team 1: " + state.getTeamTricks(1) + "     (the makers need 3)";
        trickView.update(state.getCurrentTrick(), state.getTrumpSuit(), header, leadText, footer,
                state.isNotTerminal() && state.getTrumpSuit() != null);
    }

    /**
     * What is trumps, and who made them.
     */
    private String trumpText(EuchreGameState state) {
        if (state.getTrumpSuit() == null)
            return "No trumps chosen";
        return "Trumps: " + CardArt.suitText(state.getTrumpSuit()) + " (player " + state.getMaker()
                + (state.isAlone() ? ", alone)" : ")");
    }

    /**
     * A hand is face-up only for a human player, for the current player if the core parameters allow it,
     * or in full-observability mode.
     */
    private boolean showHand(EuchreGameState state, int playerId) {
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }
}
