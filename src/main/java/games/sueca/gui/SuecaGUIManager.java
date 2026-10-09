package games.sueca.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.FrenchCard;
import games.sueca.SuecaGameState;
import games.sueca.SuecaParameters;
import games.sueca.SuecaUtils;
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
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * <p>GUI for Sueca: one area per player round the table, with the partnerships opposite each other, and the trick in
 * progress in the centre, with the trump card and each team's card points.</p>
 */
public class SuecaGUIManager extends AbstractGUIManager {

    // 10 cards are dealt to each player
    static final int playerAreaWidth = 400;
    // one player area: the cards, the status line underneath, and the titled border below that
    static final int playerAreaHeight = CardArt.cardHeight + 43;

    // a face-up hand is sorted by suit (Spades, Hearts, Diamonds, Clubs), then in Sueca's order A 7 K J Q 6 5 4 3 2
    static final List<FrenchCard.Suite> SUIT_ORDER = List.of(FrenchCard.Suite.Spades, FrenchCard.Suite.Hearts,
            FrenchCard.Suite.Diamonds, FrenchCard.Suite.Clubs);
    static final Comparator<FrenchCard> HAND_ORDER = Comparator.<FrenchCard>comparingInt(c -> SUIT_ORDER.indexOf(c.suite))
            .thenComparing(c -> -SuecaUtils.CARD_ORDER.rank(c));

    PlayerHandView[] playerViews;
    TrickView trickView;
    Border[] playerViewBorders;
    TitledBorder[] playerTitles;
    String[] agentNames;
    JLabel scoreLabel;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public SuecaGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        SuecaGameState state = (SuecaGameState) gameState;
        int nPlayers = state.getNPlayers();

        // wide enough for the longest line: the trump card and who holds it
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
        tabs.add("Rules", new RulesView(rulesHtml((SuecaParameters) state.getGameParameters()), height));

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
            playerView.getHandView().setDisplayOrder(HAND_ORDER);
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

        JPanel infoPanel = createGameStateInfoPanel("Sueca", gameState, width, defaultInfoPanelHeight);
        // up to 10 cards can be legal at once, so the action panel is the vertical, scrolling list
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
        // a leader with a full hand of SuecaParameters.handSize (10) cards may play any of them
        return 10;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof SuecaGameState state)) return;
        SuecaParameters params = (SuecaParameters) state.getGameParameters();
        int currentPlayer = state.getCurrentPlayer();
        int dealer = state.getDealer();

        for (int i = 0; i < playerViews.length; i++) {
            String extra = "team " + state.getTeam(i);
            // everyone saw the trump card turned up, so it is shown while the dealer holds it
            if (i == dealer && state.isTrumpCardHeld())
                extra += ",  holds " + CardArt.shortName(state.getTrumpCard());
            playerViews[i].update(state.getPlayerHand(i), showHand(state, i), state.getKnownVoids().get(i), extra);
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]" + (i == dealer ? " - dealer" : ""));
            playerViews[i].setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i])
                    : playerViewBorders[i]);
        }
        updateTrickView(state, params);
        scoreLabel.setText(params.playRubber
                ? "Games - team 0: " + state.getTeamGames(0) + "    team 1: " + state.getTeamGames(1)
                  + "    (" + params.targetGames + " to win" + (state.getExtraGames() > 0
                        ? ", next deal worth " + state.getExtraGames() + " more)" : ")")
                : "A single deal: the team with more card points wins");
        parent.repaint();
    }

    private void updateTrickView(SuecaGameState state, SuecaParameters params) {
        int tricksDone = state.getTricksWon(0) + state.getTricksWon(1);
        int trick = Math.min(tricksDone + 1, params.handSize);
        FrenchCard.Suite trumps = state.getTrumpSuit();
        String trumpText = "Trumps: " + CardArt.suitText(trumps) + " (dealer's " + CardArt.shortName(state.getTrumpCard())
                + (state.isTrumpCardHeld() ? ", still held)" : ", played)");
        String header = (params.playRubber ? "Deal " + (state.getRoundCounter() + 1) + "   " : "")
                + "Trick " + trick + " of " + params.handSize + "   -   " + trumpText;
        FrenchCard.Suite lead = state.getCurrentTrick().getLeadSuit();
        String leadText;
        if (!state.isNotTerminal()) {
            header = "Game over";
            leadText = params.playRubber ? resultText(state.getTeamGames(0), state.getTeamGames(1), "games")
                    : resultText(state.getCardPoints(0), state.getCardPoints(1), "points");
        } else {
            leadText = lead == null
                    ? "Player " + state.getCurrentTrick().getLeader() + " to lead"
                    : "Suit led: " + CardArt.suitText(lead) + "   -   highest trump wins"
                      + (lead == trumps ? "" : ", or if none, the highest " + lead.name());
        }
        String footer = teamLine(state, 0) + "     " + teamLine(state, 1);
        trickView.update(state.getCurrentTrick(), trumps, header, leadText, footer, state.isNotTerminal());
    }

    private static String resultText(int team0, int team1, String unit) {
        return team0 == team1 ? "A draw: the teams are level on " + team0 + " " + unit
                : "Team " + (team0 > team1 ? 0 : 1) + " wins, " + Math.max(team0, team1) + " " + unit + " to "
                  + Math.min(team0, team1);
    }

    private String teamLine(SuecaGameState state, int team) {
        return "Team " + team + ": " + state.getCardPoints(team) + " card points, " + state.getTricksWon(team) + " tricks";
    }

    private boolean showHand(SuecaGameState state, int playerId) {
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }

    private static String rulesHtml(SuecaParameters params) {
        FrenchCard.Suite spades = FrenchCard.Suite.Spades;
        FrenchCard[] scoring = {new FrenchCard(FrenchCard.FrenchCardType.Ace, spades),
                new FrenchCard(FrenchCard.FrenchCardType.Number, spades, 7),
                new FrenchCard(FrenchCard.FrenchCardType.King, spades),
                new FrenchCard(FrenchCard.FrenchCardType.Jack, spades),
                new FrenchCard(FrenchCard.FrenchCardType.Queen, spades)};
        String[] names = {"Ace", "Seven", "King", "Jack", "Queen"};
        StringBuilder cards = new StringBuilder("<tr><th align=left>Card</th>");
        StringBuilder points = new StringBuilder("<tr><th align=left>Card points</th>");
        for (int i = 0; i < scoring.length; i++) {
            cards.append("<td align=center>").append(names[i]).append("</td>");
            points.append("<td align=center>").append(params.cardPoints(scoring[i])).append("</td>");
        }
        String winning = params.playRubber
                ? "<p><b>Winning.</b> The game is a rubber of several deals. After each deal the team with more " +
                "than 60 card points scores games:</p>" +
                "<table border=1 cellpadding=4 cellspacing=0>" +
                "<tr><th align=left>Deal won with</th><th>Games</th></tr>" +
                "<tr><td>61 to 90 card points</td><td align=center>" + SuecaUtils.gamesForDeal(61, false) +
                "</td></tr>" +
                "<tr><td>91 or more card points</td><td align=center>" + SuecaUtils.gamesForDeal(91, false) +
                "</td></tr>" +
                "<tr><td>every trick</td><td align=center>" + SuecaUtils.gamesForDeal(120, true) + "</td></tr>" +
                "</table>" +
                "<p>After a deal tied at 60 card points each, nobody scores, and the next deal is worth one more " +
                "game to the team that wins it. Each further tie adds another game. The deal passes to the next " +
                "player. The first team to " + params.targetGames + " games wins.</p>"
                : "<p><b>Winning.</b> The game is a single deal. The team with more card points wins, and 60 card " +
                "points each is a draw.</p>";
        return "<h2>Sueca</h2>" +
                "<p>Four players play in two teams. Team 0 is players 0 and 2, and team 1 is players 1 and 3. " +
                "Partners sit opposite each other.</p>" +
                "<p><b>The deal.</b> The pack has 40 cards (a standard pack without the Eights, Nines and Tens). " +
                "Each player is dealt " + params.handSize + " cards. The dealer's last card is turned face up and " +
                "sets trumps. The dealer keeps it in their hand, and everyone knows they hold it until they play " +
                "it. The player after the dealer leads the first trick.</p>" +
                "<p><b>Tricks.</b> In every suit the cards rank A 7 K J Q 6 5 4 3 2.</p><ul>" +
                "<li>Follow the suit led if you can. Otherwise play any card.</li>" +
                "<li>The highest trump wins the trick. If no trump was played, the highest card of the suit led " +
                "wins.</li>" +
                "<li>The winner leads the next trick.</li></ul>" +
                "<p><b>Card points.</b> The cards a team wins in tricks score card points, 120 in all. The other " +
                "cards score nothing.</p>" +
                "<table border=1 cellpadding=4 cellspacing=0>" + cards + "</tr>" + points + "</tr></table>" +
                winning +
                "<h3>Interface</h3>" +
                "<p>Player 0 sits at the bottom, and the others follow clockwise. Each player's area is titled " +
                "with the player's number and agent, and with \"dealer\" for the dealer. The current player's " +
                "area has a blue border. The line under a player's cards shows their number of cards, their team, " +
                "the trump card while the dealer holds it, and the suits the player is known to be void in (from " +
                "failing to follow suit).</p>" +
                "<p>The centre shows the trick so far. Each card is labelled with the player who played it, and " +
                "\"(led)\" marks the first. The card winning the trick so far has an orange outline. Above the " +
                "trick, one line shows " + (params.playRubber ? "the deal number, " : "") + "the trick number and " +
                "the trump card, and the next shows the suit led (or the player to lead). The line below the trick shows each team's card points " +
                "and tricks in this deal." +
                (params.playRubber ? " The bottom line shows each team's games in the rubber." : "") + "</p>" +
                "<p>Each action button plays one card from your hand.</p>";
    }
}
