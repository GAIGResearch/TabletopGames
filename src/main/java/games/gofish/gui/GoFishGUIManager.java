package games.gofish.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.gofish.GoFishGameState;
import games.gofish.GoFishParameters;
import games.gofish.actions.GoFishAsk;
import games.tricktaking.gui.CardArt;
import games.tricktaking.gui.FrenchCardDeckView;
import games.tricktaking.gui.PlayerHandView;
import gui.AbstractGUIManager;
import gui.ClickRegion;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * <p>GUI for Go Fish: one area per player, in two rows round the table, and the draw deck in the centre.</p>
 *
 * <p>A human player asks by clicking: a card in their hand for its rank, then the player to ask (see
 * {@link gui.ClickableActions}). The action buttons stay as well.</p>
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
    static final Color askable = new Color(255, 210, 0), chosen = new Color(60, 220, 60);
    final Border highlightAskable = BorderFactory.createLineBorder(chosen, 3);

    // the state last shown, and the rank a human player has clicked in their hand (-1 for none)
    GoFishGameState shown;
    int chosenRank = -1;

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
        RulesView.addTabs(tabs, GameType.GoFish, state.getGameParameters(), height);

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
            agentNames[i] = game.getPlayers().get(i).toString();
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i,
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            title.setTitleColor(Color.white);   // the table background is dark
            playerTitles[i] = title;
            playerViewBorders[i] = title;
            playerView.setBorder(title);
            playerViews[i] = playerView;
            int seat = i;
            playerView.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    clicked(seat, e);
                }
            });
            playerView.setToolTips(e -> toolTip(seat, e));
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
    public void update(AbstractPlayer player, AbstractGameState gameState, boolean showActions) {
        super.update(player, gameState, showActions);
        // after the actions are offered (or withdrawn) for this state
        showClickable();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof GoFishGameState state)) return;
        shown = state;
        for (int i = 0; i < playerViews.length; i++) {
            playerViews[i].update(state.getPlayerHands().get(i), showHand(state, i), Set.of(), statusText(state, i));
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]");
        }
        drawDeckView.updateComponent(state.getDrawDeck());
        centreText.setText(centreText(state));
        parent.repaint();
    }

    @Override
    public List<ClickRegion> getClickRegions() {
        int asker = clickable.player();
        if (shown == null || asker < 0 || asker >= playerViews.length) return List.of();
        // a region for each card in the asker's hand of a rank they may ask for, offering an ask of each player who
        // may be asked for it
        PartialObservableDeck<FrenchCard> hand = shown.getPlayerHands().get(asker);
        List<ClickRegion> regions = new ArrayList<>();
        for (int c = 0; c < hand.getSize(); c++) {
            int rank = hand.get(c).number;
            List<GoFishAsk> asks = clickable.matching(GoFishAsk.class, a -> a.rank == rank);
            Shape shape = playerViews[asker].cardShape(c);
            if (!asks.isEmpty() && shape != null)
                regions.add(new ClickRegion(playerViews[asker], shape, asks));
        }
        return regions;
    }

    /**
     * A click on a player's area. In the clicking player's own hand it chooses the rank of the card clicked (and
     * asks at once if only one player can be asked for it); on another player's area it asks them for the chosen
     * rank. Any other button than the left clears the choice.
     */
    private void clicked(int seat, MouseEvent e) {
        if (!clickable.isOffered() || shown == null) return;
        if (e.getButton() != MouseEvent.BUTTON1) {
            chosenRank = -1;
        } else if (seat == clickable.player()) {
            int index = playerViews[seat].cardIndexAt(e.getPoint());
            if (index < 0) return;
            int rank = shown.getPlayerHands().get(seat).get(index).number;
            List<GoFishAsk> asks = clickable.matching(GoFishAsk.class, a -> a.rank == rank);
            if (asks.size() == 1)
                clickable.submit(asks.get(0));
            else if (!asks.isEmpty())
                chosenRank = rank == chosenRank ? -1 : rank;
        } else if (chosenRank != -1) {
            List<GoFishAsk> asks = clickable.matching(GoFishAsk.class,
                    a -> a.rank == chosenRank && a.targetPlayer == seat);
            if (asks.size() == 1)
                clickable.submit(asks.get(0));
        }
        showClickable();
    }

    /**
     * The outlines and borders that show what may be clicked: the asking player's cards of a rank they can ask for,
     * in green for the chosen rank, and the players who can be asked for it. Without a human player to act, just the
     * current player's border.
     */
    private void showClickable() {
        if (shown == null) return;
        if (!clickable.isOffered())
            chosenRank = -1;
        int asker = clickable.player();
        Set<Integer> ranks = new HashSet<>(), targets = new HashSet<>();
        for (GoFishAsk a : clickable.matching(GoFishAsk.class, a -> true)) {
            ranks.add(a.rank);
            if (a.rank == chosenRank) targets.add(a.targetPlayer);
        }
        for (int i = 0; i < playerViews.length; i++) {
            Map<Integer, Color> outlines = new HashMap<>();
            if (i == asker) {
                PartialObservableDeck<FrenchCard> hand = shown.getPlayerHands().get(i);
                for (int c = 0; c < hand.getSize(); c++)
                    if (ranks.contains(hand.get(c).number))
                        outlines.put(c, hand.get(c).number == chosenRank ? chosen : askable);
            }
            playerViews[i].setOutlines(outlines);
            Border border = playerViewBorders[i];
            if (targets.contains(i))
                border = BorderFactory.createCompoundBorder(highlightAskable, border);
            else if (i == shown.getCurrentPlayer() && shown.isNotTerminal())
                border = BorderFactory.createCompoundBorder(highlightActive, border);
            playerViews[i].setBorder(border);
        }
    }

    /**
     * What a click at this point of the player's area would do, and what would follow.
     */
    private String toolTip(int seat, MouseEvent e) {
        if (shown == null) return null;
        GoFishParameters params = (GoFishParameters) shown.getGameParameters();
        if (!clickable.isOffered())
            return "Player " + seat + ": " + shown.getPlayerHands().get(seat).getSize() + " cards";
        int asker = clickable.player();
        if (seat == asker) {
            int index = playerViews[seat].cardIndexAt(e.getPoint());
            if (index < 0) return "Your hand: click a card to ask for its rank";
            int rank = shown.getPlayerHands().get(seat).get(index).number;
            List<GoFishAsk> asks = clickable.matching(GoFishAsk.class, a -> a.rank == rank);
            if (asks.isEmpty()) return "You cannot ask for " + rankName(rank) + "s now";
            int held = count(seat, rank);
            String holding = "You hold " + held + " " + rankName(rank) + (held == 1 ? "" : "s");
            if (asks.size() == 1)
                return html("Click to ask Player " + asks.get(0).targetPlayer + " for " + rankName(rank) + "s",
                        holding, knowledge(asker, asks.get(0).targetPlayer, rank), consequence(params, rank));
            return html(rank == chosenRank ? "Click again to choose another rank"
                    : "Click to ask for " + rankName(rank) + "s, then click the player to ask", holding);
        }
        if (chosenRank == -1)
            return "Player " + seat + ": first click a card in your hand for the rank to ask for";
        List<GoFishAsk> asks = clickable.matching(GoFishAsk.class,
                a -> a.rank == chosenRank && a.targetPlayer == seat);
        if (asks.isEmpty()) return "You cannot ask Player " + seat;
        return html("Click to ask Player " + seat + " for " + rankName(chosenRank) + "s",
                knowledge(asker, seat, chosenRank), consequence(params, chosenRank));
    }

    private int count(int player, int rank) {
        int n = 0;
        for (FrenchCard c : shown.getPlayerHands().get(player).getComponents())
            if (c.number == rank) n++;
        return n;
    }

    /**
     * What the asker knows about whether the target holds the rank: cards of it shown to the table, or the rank
     * known to be missing from their hand.
     */
    private String knowledge(int asker, int target, int rank) {
        PartialObservableDeck<FrenchCard> hand = shown.getPlayerHands().get(target);
        int seen = 0;
        for (int i = 0; i < hand.getSize(); i++)
            if (hand.get(i).number == rank && hand.isComponentVisible(i, asker)) seen++;
        if (seen > 0)
            return "Player " + target + " has shown " + seen + " " + rankName(rank) + (seen == 1 ? "" : "s");
        if (shown.getKnownVoids().get(target).contains(rank))
            return "Player " + target + " is known to have no " + rankName(rank) + "s";
        return "Player " + target + " holds " + hand.getSize() + " cards";
    }

    private String consequence(GoFishParameters params, int rank) {
        String success = "If they have any, they give you all of them"
                + (params.continueOnSuccess ? " and you ask again." : ".");
        int deck = shown.getDrawDeck().getSize();
        String fish = deck == 0
                ? "If not, the draw deck is empty and your turn ends."
                : "If not, you draw from the deck (" + deck + " cards)"
                + (params.continueOnDrawingSameRank ? ", and ask again if you draw a " + rankName(rank) + "." : ".");
        return success + " " + fish;
    }

    private static String html(String... lines) {
        return "<html>" + String.join("<br>", lines) + "</html>";
    }

    private static String rankName(int rank) {
        return switch (rank) {
            case 11 -> "Jack";
            case 12 -> "Queen";
            case 13 -> "King";
            case 14 -> "Ace";
            default -> String.valueOf(rank);
        };
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
}
