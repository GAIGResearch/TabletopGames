package games.klaverjassen.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.FrenchCard;
import games.klaverjassen.KlaverjassenGameState;
import games.klaverjassen.KlaverjassenParameters;
import games.tricktaking.gui.CardArt;
import games.tricktaking.gui.PlayerHandView;
import games.tricktaking.gui.TrickView;
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
 * <p>GUI for Klaverjassen: one area per player round the table, with the partnerships opposite each other, and the
 * trick in progress in the centre.</p>
 */
public class KlaverjassenGUIManager extends AbstractGUIManager {

    // 8 cards are dealt to each player
    static final int playerAreaWidth = 360;
    // one player area: the cards, the status line underneath, and the titled border below that
    static final int playerAreaHeight = CardArt.cardHeight + 43;

    PlayerHandView[] playerViews;
    TrickView trickView;
    Border[] playerViewBorders;
    TitledBorder[] playerTitles;
    String[] agentNames;
    JLabel scoreLabel;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public KlaverjassenGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        KlaverjassenGameState state = (KlaverjassenGameState) gameState;
        int nPlayers = state.getNPlayers();

        // wide enough for the longest line: each team's points, roem and tricks this hand
        trickView = new TrickView(nPlayers, 560);
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

        JPanel infoPanel = createGameStateInfoPanel("Klaverjassen", gameState, width, defaultInfoPanelHeight);
        // up to 8 cards can be legal at once, so the action panel is the vertical, scrolling list
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

    @Override
    public int getMaxActionSpace() {
        // a leader with a full hand of KlaverjassenParameters.handSize (8) cards may play any of them; the trump
        // chooser has only 4 actions
        return 8;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof KlaverjassenGameState state)) return;
        KlaverjassenParameters params = (KlaverjassenParameters) state.getGameParameters();
        int currentPlayer = state.getCurrentPlayer();
        int dealer = state.getDealer();
        int chooser = state.getTrumpChooser();

        for (int i = 0; i < playerViews.length; i++) {
            playerViews[i].update(state.getPlayerHand(i), showHand(state, i), state.getKnownVoids().get(i),
                    "team " + state.getTeam(i));
            String role = i == dealer ? " - dealer" : i == chooser && state.getTrumpSuit() != null ? " - chose trumps" : "";
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]" + role);
            playerViews[i].setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i])
                    : playerViewBorders[i]);
        }
        updateTrickView(state, params);
        scoreLabel.setText("Score - team 0: " + state.getTeamScore(0) + "    team 1: " + state.getTeamScore(1));
        parent.repaint();
    }

    private void updateTrickView(KlaverjassenGameState state, KlaverjassenParameters params) {
        int tricksDone = state.getTricksWon(0) + state.getTricksWon(1);
        int trick = Math.min(tricksDone + 1, params.handSize);
        FrenchCard.Suite trumps = state.getTrumpSuit();
        String trumpText = trumps == null ? "Trumps not yet chosen"
                : "Trumps: " + CardArt.suitText(trumps) + " (team " + state.getTeam(state.getTrumpChooser()) + ")";
        String header = (params.nHands > 1 ? "Hand " + (state.getRoundCounter() + 1) + " of " + params.nHands + "   " : "")
                + "Trick " + trick + " of " + params.handSize + "   -   " + trumpText;
        FrenchCard.Suite lead = state.getCurrentTrick().getLeadSuit();
        String leadText;
        if (!state.isNotTerminal()) {
            header = "Game over";
            int score0 = state.getTeamScore(0), score1 = state.getTeamScore(1);
            leadText = score0 == score1 ? "The teams are level on " + score0 + " points"
                    : "Team " + (score0 > score1 ? 0 : 1) + " wins, "
                      + Math.max(score0, score1) + " points to " + Math.min(score0, score1);
        } else if (trumps == null) {
            leadText = "Player " + state.getTrumpChooser() + " to choose trumps, then lead";
        } else {
            leadText = lead == null
                    ? "Player " + state.getCurrentTrick().getLeader() + " to lead"
                    : "Suit led: " + CardArt.suitText(lead) + "   -   highest trump wins"
                      + (lead == trumps ? "" : ", or if none, the highest " + lead.name());
        }
        String footer = teamLine(state, 0) + "     " + teamLine(state, 1);
        trickView.update(state.getCurrentTrick(), trumps, header, leadText, footer, state.isNotTerminal());
    }

    private String teamLine(KlaverjassenGameState state, int team) {
        return "Team " + team + ": " + state.getHandPoints(team) + " + " + state.getHandRoem(team) + " roem, "
                + state.getTricksWon(team) + " tricks";
    }

    private boolean showHand(KlaverjassenGameState state, int playerId) {
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }

    private JPanel createRulesPanel() {
        JPanel rules = new JPanel();
        rules.setBackground(new Color(43, 108, 25, 111));
        JLabel text = new JLabel("<html><center><h1>Klaverjassen</h1></center><hr>" +
                "<p>The Dutch partnership trick-taking game, for four players: <b>players 0 and 2 against players " +
                "1 and 3</b>, sitting opposite each other. Rules as the Utrecht (compulsory trumps) and Amsterdam " +
                "variants.</p><ul>" +
                "<li>A 32-card pack (Seven to Ace in each suit) is dealt, 8 cards each.</li>" +
                "<li>The player on the dealer's left <b>must choose trumps</b> after seeing their hand, and then " +
                "leads the first trick. Their team must score more points than the other team (see below).</li>" +
                "<li>Trumps rank <b>J 9 A 10 K Q 8 7</b>; other suits rank <b>A 10 K Q J 9 8 7</b>. The highest " +
                "trump wins the trick, or the highest card of the suit led. The winner leads the next trick.</li>" +
                "<li>Follow the suit led if you can. When trumps are led you must beat the highest trump played " +
                "if you can.</li>" +
                "<li>If you cannot follow suit and an opponent is winning the trick, you must trump (beating any " +
                "trump already played) if you can; you may not play a lower trump unless you hold nothing else.</li>" +
                "<li>If you cannot follow suit and your partner is winning, you may play anything - except that " +
                "if partner is winning with a trump you must discard a non-trump if you hold one. (Option: you " +
                "may also overtrump partner, but never undertrump.)</li>" +
                "<li><b>Card points:</b> trumps J 20, 9 14, A 11, 10 10, K 4, Q 3; other suits A 11, 10 10, K 4, " +
                "Q 3, J 2. The last trick scores 10 more: 162 in all.</li>" +
                "<li><b>Roem</b> (bonus points), scored by the team winning the trick, for the cards in it: " +
                "a run of three in one suit (in the order A K Q J 10 9 8 7) 20, a run of four 50; the King and " +
                "Queen of trumps (stuk) 20 more; four Kings, Queens, Aces or Tens 100; four Jacks 200. A team " +
                "taking all 8 tricks scores 100 roem more.</li>" +
                "<li>If the team that chose trumps has fewer points (card points plus roem) than the other team, " +
                "it scores nothing and the other team scores all the points of the hand. (Option: a tie also " +
                "counts as failing.) Otherwise each team scores its own points.</li>" +
                "<li>By default a game is a single hand (an option plays more, with the deal passing to the " +
                "left). The team with more points wins; level points are a draw.</li>" +
                "</ul><hr><p><b>INTERFACE:</b> choose trumps or a card from the action buttons at the bottom of " +
                "the screen. The centre shows the trick so far, with the winning card outlined, what is trumps, " +
                "and each team's card points, roem and tricks this hand. A player's area shows the suits they are " +
                "known to be void in.</p></html>");
        text.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(width * 2 / 3 + 60, height * 2 / 3 + 100));
        rules.add(scroll);
        return rules;
    }
}
