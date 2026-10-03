package games.gofish.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.gofish.GoFishGameState;
import games.gofish.GoFishParameters;
import games.tricktaking.gui.CardArt;
import games.tricktaking.gui.FrenchCardDeckView;
import games.tricktaking.gui.PlayerHandView;
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
import java.util.StringJoiner;
import java.util.TreeSet;

/**
 * <p>GUI for Go Fish: one area per player, in two rows round the table, and the draw deck in the centre.</p>
 *
 * <p>This is a view only - it holds no rules and never changes the state. It is repainted for every player,
 * human or not, so a hand is only shown face-up when the viewer is entitled to see it (see showHand). A hidden hand
 * still shows face-up the cards its owner has shown to the table.</p>
 */
public class GoFishGUIManager extends AbstractGUIManager {

    static final int playerAreaWidth = 300;
    // one player area: the cards, the status line underneath, and the titled border below that
    static final int playerAreaHeight = CardArt.cardHeight + 43;

    PlayerHandView[] playerViews;
    FrenchCardDeckView drawDeckView;
    JLabel centreText;
    Border[] playerViewBorders;
    TitledBorder[] playerTitles;
    String[] agentNames;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public GoFishGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        GoFishGameState state = (GoFishGameState) gameState;
        int nPlayers = state.getNPlayers();
        // player 0 at the bottom left, then round the table: the bottom row left to right, the top row right to left
        int bottomRow = (nPlayers + 1) / 2;
        int topRow = nPlayers - bottomRow;

        // at least two player areas wide, so that the info panel is legible with 2 players
        this.width = Math.max(bottomRow, 2) * (playerAreaWidth + 20) + 40;
        this.height = 2 * playerAreaHeight + CardArt.cardHeight + 90;

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
        tabs.add("Rules", new RulesView(rulesHtml((GoFishParameters) state.getGameParameters()), height));

        playerViews = new PlayerHandView[nPlayers];
        playerViewBorders = new Border[nPlayers];
        playerTitles = new TitledBorder[nPlayers];
        agentNames = new String[nPlayers];
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        top.setOpaque(false);
        bottom.setOpaque(false);
        for (int i = 0; i < nPlayers; i++) {
            // Every card in a hand is visible either to its owner only or to everyone, so the cards another player
            // can see are exactly those shown to the table: those are drawn face-up when the hand is hidden
            PlayerHandView playerView = new PlayerHandView(state.getPlayerHands().get(i), (i + 1) % nPlayers,
                    playerAreaWidth);
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
            playerViews[i] = playerView;
        }
        for (int i = nPlayers - 1; i >= bottomRow; i--)
            top.add(playerViews[i]);
        for (int i = 0; i < bottomRow; i++)
            bottom.add(playerViews[i]);

        drawDeckView = new FrenchCardDeckView(-1, state.getDrawDeck(), false,
                new Rectangle(0, 0, CardArt.cardWidth, CardArt.cardHeight));
        drawDeckView.setOpaque(false);
        centreText = new JLabel();
        centreText.setForeground(Color.white);
        JPanel centre = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        centre.setOpaque(false);
        centre.add(drawDeckView);
        centre.add(centreText);

        JPanel mainGameArea = new JPanel(new BorderLayout());
        mainGameArea.setOpaque(false);
        if (topRow > 0)
            mainGameArea.add(top, BorderLayout.NORTH);
        mainGameArea.add(centre, BorderLayout.CENTER);
        mainGameArea.add(bottom, BorderLayout.SOUTH);

        JPanel infoPanel = createGameStateInfoPanel("Go Fish", gameState, width, defaultInfoPanelHeight);
        // a vertical list, as there can be many asks (see getMaxActionSpace)
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

        main.add(infoPanel, BorderLayout.NORTH);
        main.add(mainGameArea, BorderLayout.CENTER);
        main.add(actionPanel, BorderLayout.SOUTH);

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 40));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    /**
     * One GoFishAsk for each other player with cards and each rank the current player holds: at most 5 other players
     * (6 players) and 13 ranks, so 65.
     */
    @Override
    public int getMaxActionSpace() {
        return 65;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof GoFishGameState state)) return;
        int currentPlayer = state.getCurrentPlayer();
        for (int i = 0; i < playerViews.length; i++) {
            playerViews[i].update(state.getPlayerHands().get(i), showHand(state, i), Set.of(), statusText(state, i));
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]");
            playerViews[i].setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i])
                    : playerViewBorders[i]);
        }
        drawDeckView.updateComponent(state.getDrawDeck());
        centreText.setText(centreText(state));
        parent.repaint();
    }

    /**
     * The player's books, and the ranks they are known not to hold.
     */
    private String statusText(GoFishGameState state, int playerId) {
        Set<Integer> books = new TreeSet<>();
        state.getPlayerBooks().get(playerId).getComponents().forEach(c -> books.add(c.number));
        String text = "books: " + (books.isEmpty() ? "none" : rankNames(books));
        Set<Integer> voids = state.getKnownVoids().get(playerId);
        return voids.isEmpty() ? text : text + ",  has no " + rankNames(voids);
    }

    private String centreText(GoFishGameState state) {
        GoFishParameters params = (GoFishParameters) state.getGameParameters();
        String deck = "Draw deck: " + state.getDrawDeck().getSize() + " cards";
        String end = params.playUntilAllBooks
                ? "Play goes on until nobody has anyone to ask"
                : "The game ends when a hand or the draw deck is empty";
        String status = state.isNotTerminal()
                ? "Player " + state.getCurrentPlayer() + " to ask"
                : "Game over";
        return "<html>" + deck + "<br>" + end + "<br><b>" + status + "</b></html>";
    }

    private static String rankNames(Set<Integer> ranks) {
        StringJoiner names = new StringJoiner(" ");
        for (int rank : ranks)
            names.add(switch (rank) {
                case 11 -> "J";
                case 12 -> "Q";
                case 13 -> "K";
                case 14 -> "A";
                default -> String.valueOf(rank);
            });
        return names.toString();
    }

    /**
     * A hand is face-up only for a human player, for the current player if the core parameters allow it,
     * or in full-observability mode.
     */
    private boolean showHand(GoFishGameState state, int playerId) {
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }

    private static String rulesHtml(GoFishParameters params) {
        String given = params.continueOnSuccess
                ? "they must give you all of them, face up, and you ask again."
                : "they must give you all of them, face up, and the turn passes to the next player.";
        String drawn = params.continueOnDrawingSameRank
                ? "If it is the rank you asked for, you show it and ask again. Otherwise the turn passes to the " +
                "next player."
                : "The turn then passes to the next player.";
        String end = params.playUntilAllBooks
                ? "<p><b>The end.</b> Play goes on after a hand or the draw deck is empty. A player whose turn " +
                "starts with an empty hand draws a card, or is skipped if the draw deck is empty. You may ask " +
                "only a player who holds cards. The game ends when the player to ask has nobody to ask.</p>"
                : "<p><b>The end.</b> The game ends as soon as any player's hand or the draw deck is empty.</p>";
        return "<h2>Go Fish</h2>" +
                "<p>Collect books. A book is all four cards of a rank. The player with the most books wins, and " +
                "players tied for the most books share first place.</p>" +
                "<p><b>The deal.</b> Each player is dealt " + params.startingHandSize + " cards (" +
                params.twoPlayerHandSize + " each with 2 players). The rest form the draw deck. Player 0 asks " +
                "first.</p>" +
                "<p><b>Each turn</b> you ask another player for a rank that you hold. Asking shows everyone one of " +
                "your cards of that rank.</p><ul>" +
                "<li>If they have any cards of that rank, " + given + "</li>" +
                "<li>If they have none, they say Go fish, and you draw the top card of the draw deck. " + drawn +
                "</li></ul>" +
                "<p>As soon as you hold all four cards of a rank, they are laid down as a book.</p>" +
                end +
                "<h3>Interface</h3>" +
                "<p>The players' hands are in two rows, each titled with the player's number and agent. The current " +
                "player's hand has a blue border. In a hidden hand, the cards shown to the table are face up. The " +
                "line under a hand shows the number of cards, the ranks of the player's books, and the ranks the " +
                "player is known not to hold (\"has no\"). A player is known not to hold a rank after saying Go " +
                "fish or giving those cards away, until they next draw. J, Q, K and A stand for Jack, Queen, King " +
                "and Ace.</p>" +
                "<p>The centre shows the draw deck with its number of cards, when the game will end, and the " +
                "player to ask. Each action button is an ask, such as \"Ask P1 for Kings\".</p>";
    }
}
