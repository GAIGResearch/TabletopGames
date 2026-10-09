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
        tabs.add("Rules", new RulesView(rulesHtml((KlaverjassenParameters) state.getGameParameters()), height));

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

    private static String rulesHtml(KlaverjassenParameters params) {
        // the card numbers of each suit from high to low: Jack 11, Queen 12, King 13, Ace 14
        int[] trumpOrder = {11, 9, 14, 10, 13, 12, 8, 7};
        int[] otherOrder = {14, 10, 13, 12, 11, 9, 8, 7};
        String cardTable = "<table border=1 cellpadding=4 cellspacing=0>"
                + cardRows("Trumps", trumpOrder, FrenchCard.Suite.Hearts, params)
                + cardRows("Other suits", otherOrder, FrenchCard.Suite.Spades, params) + "</table>";
        int total = params.lastTrickBonus;
        for (int n : trumpOrder)
            total += points(n, FrenchCard.Suite.Hearts, params) + 3 * points(n, FrenchCard.Suite.Spades, params);
        String partnerTrump = switch (params.partnerTrumpRule) {
            case NO_UNDERTRUMP -> "you may play a card that is not a trump, or a trump higher than your " +
                    "partner's. You may play a lower trump only if you hold nothing else.";
            case DISCARD -> "you must play a card that is not a trump if you hold one.";
        };
        return "<h2>Klaverjassen</h2>" +
                "<p>Players 0 and 2 (team 0) play against players 1 and 3 (team 1). Partners sit opposite each " +
                "other. The game lasts " + params.nHands + (params.nHands == 1 ? " hand" : " hands") + ".</p>" +
                "<p><b>The deal.</b> The 32-card pack (Seven to Ace in each suit) is shuffled, and each player is " +
                "dealt " + params.handSize + " cards. The player on the dealer's left must choose trumps after " +
                "seeing their hand, and then leads the first trick. The deal passes to the left after each " +
                "hand.</p>" +
                "<p><b>Cards.</b> The cards of each suit rank from high to low as below, with their points.</p>" +
                cardTable +
                "<p>The team that wins the last trick of a hand scores " + params.lastTrickBonus + " points more, " +
                "so each hand has " + total + " points in all.</p>" +
                "<p><b>Play.</b> Any card may be led. The highest trump in a trick wins it. If the trick holds no " +
                "trump, the highest card of the suit led wins. The winner leads the next trick.</p><ul>" +
                "<li>If a suit other than trumps is led, you must follow suit if you can.</li>" +
                "<li>If trumps are led, you must play a trump higher than every trump in the trick if you can. " +
                "If you hold only lower trumps, you must play one of them.</li>" +
                "<li>If you cannot follow suit and an opponent is winning the trick, you must play a trump that " +
                "beats the winning card if you can. If you cannot, you must play a card that is not a trump. You " +
                "may play a lower trump only if you hold nothing else.</li>" +
                "<li>If you cannot follow suit and your partner is winning the trick, you may play any card. If " +
                "your partner is winning with a trump, " + partnerTrump + "</li></ul>" +
                "<p><b>Roem.</b> The team that wins a trick scores roem (bonus points) for the cards in it.</p>" +
                "<table border=1 cellpadding=4 cellspacing=0>" +
                "<tr><td>A run of three in one suit, in the order 7 8 9 10 J Q K A</td><td align=right>" +
                params.runOfThreeBonus + "</td></tr>" +
                "<tr><td>A run of four</td><td align=right>" + params.runOfFourBonus + "</td></tr>" +
                "<tr><td>The King and Queen of trumps (stuk), as well as any run</td><td align=right>" +
                params.stukBonus + "</td></tr>" +
                "<tr><td>Four Tens, Queens, Kings or Aces</td><td align=right>" + params.fourOfAKindBonus +
                "</td></tr>" +
                "<tr><td>Four Jacks</td><td align=right>" + params.fourJacksBonus + "</td></tr></table>" +
                "<p>A team that wins every trick of a hand scores " + params.pitBonus + " roem more.</p>" +
                "<p><b>Scoring a hand.</b> Each team's points for the hand are its card points plus its roem. If " +
                "the team that chose trumps has " + (params.tieIsFailure ? "the same or fewer points" : "fewer " +
                "points") + " than the other team, it scores nothing, and the other team scores the points of " +
                "both teams. Otherwise each team scores its own points.</p>" +
                "<p><b>Winning.</b> After the last hand the team with the higher score wins. Teams with the same " +
                "score draw.</p>" +
                "<h3>Interface</h3>" +
                "<p>Choose trumps or a card to play from the action buttons at the bottom. Player 0 sits at the " +
                "bottom, with players 1, 2 and 3 to the left, top and right. Below each player's cards are the " +
                "number of cards they hold, their team, and the suits they are known to be void in. The player " +
                "to act has a blue border. The title of the dealer shows \"dealer\", and the title of the player " +
                "who chose trumps shows \"chose trumps\".</p>" +
                "<p>The centre panel shows the hand and trick number, the trump suit and the team that chose it, " +
                "and the suit led. Below them are the cards of the trick, each with the player who played it. " +
                "The card winning the trick is outlined in orange. The bottom line gives each team's card " +
                "points, roem and tricks in this hand. The score of each team is shown under the panel.</p>";
    }

    /**
     * Two rows of the card table: the cards of one suit from high to low, and their points.
     */
    private static String cardRows(String title, int[] order, FrenchCard.Suite suit, KlaverjassenParameters params) {
        StringBuilder cards = new StringBuilder("<tr><th align=left>" + title + "</th>");
        StringBuilder pointRow = new StringBuilder("<tr><th align=left>Points</th>");
        for (int n : order) {
            cards.append("<td align=center>").append(n <= 10 ? String.valueOf(n) : "JQKA".substring(n - 11, n - 10))
                    .append("</td>");
            pointRow.append("<td align=center>").append(points(n, suit, params)).append("</td>");
        }
        return cards + "</tr>" + pointRow + "</tr>";
    }

    /**
     * The points of the card of the given number and suit, when Hearts are trumps.
     */
    private static int points(int number, FrenchCard.Suite suit, KlaverjassenParameters params) {
        return params.cardPoints(new FrenchCard(FrenchCard.FrenchCardType.Number, suit, number),
                FrenchCard.Suite.Hearts);
    }
}
