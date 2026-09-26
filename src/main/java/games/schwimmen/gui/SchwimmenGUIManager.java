package games.schwimmen.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.schwimmen.SchwimmenGameState;
import games.schwimmen.SchwimmenParameters;
import games.tricktaking.gui.CardArt;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * <p>GUI for Schwimmen: each player's hand, in two rows round the table, and in the centre the table cards (or the
 * extra hand while the dealer chooses), the draw deck and the discard pile.</p>
 *
 * <p>A hand card is face up for a human who can see it, for everyone once every player has seen it taken from the
 * table, and for all at the end of the game; otherwise as the core parameters allow.</p>
 */
public class SchwimmenGUIManager extends AbstractGUIManager {

    SchwimmenHandView[] handViews;
    SchwimmenTableView tableView;
    JLabel centreText;
    Border[] handBorders;
    TitledBorder[] titles;
    String[] agentNames;

    static final int centreTextWidth = 260;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public SchwimmenGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        SchwimmenGameState state = (SchwimmenGameState) gameState;
        int nPlayers = state.getNPlayers();
        // player 0 at the bottom left, then round the table: the bottom row left to right, the top row right to left
        int bottomRow = (nPlayers + 1) / 2;

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

        handViews = new SchwimmenHandView[nPlayers];
        handBorders = new Border[nPlayers];
        titles = new TitledBorder[nPlayers];
        agentNames = new String[nPlayers];
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        top.setOpaque(false);
        bottom.setOpaque(false);
        for (int i = 0; i < nPlayers; i++) {
            SchwimmenHandView view = new SchwimmenHandView();
            view.setOpaque(false);
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i,
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            title.setTitleColor(Color.white);   // the table background is dark
            titles[i] = title;
            // the same width as highlightActive, so that highlighting the current player does not move anything
            handBorders[i] = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3), title);
            view.setBorder(handBorders[i]);
            handViews[i] = view;
        }
        tableView = new SchwimmenTableView();
        tableView.setOpaque(false);
        Dimension handSize = handViews[0].getPreferredSize();
        this.width = Math.max(Math.max(bottomRow, 2) * (handSize.width + 10) + 30,
                tableView.getPreferredSize().width + centreTextWidth + 80);
        // at the end of the game the centre text lists every player, under two heading lines
        int centreHeight = Math.max(tableView.getPreferredSize().height, 16 * (nPlayers + 2) + 8);
        this.height = 2 * handSize.height + centreHeight + 30;
        tabs.add("Rules", createRulesPanel(state));

        for (int i = nPlayers - 1; i >= bottomRow; i--)
            top.add(handViews[i]);
        for (int i = 0; i < bottomRow; i++)
            bottom.add(handViews[i]);

        centreText = new JLabel();
        centreText.setForeground(Color.white);
        centreText.setVerticalAlignment(SwingConstants.TOP);
        // a fixed width, so that a long line cannot push the text below the table view
        centreText.setPreferredSize(new Dimension(centreTextWidth, centreHeight));
        JPanel centre = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        centre.setOpaque(false);
        centre.add(tableView);
        centre.add(centreText);

        JPanel mainGameArea = new JPanel(new BorderLayout());
        mainGameArea.setOpaque(false);
        if (nPlayers > bottomRow)
            mainGameArea.add(top, BorderLayout.NORTH);
        mainGameArea.add(centre, BorderLayout.CENTER);
        mainGameArea.add(bottom, BorderLayout.SOUTH);

        JPanel infoPanel = createGameStateInfoPanel("Schwimmen", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, true);

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

    @Override
    public int getMaxActionSpace() {
        // a turn: ExchangeOne for each of 3 hand cards and 3 table cards (9), ExchangeAll and Pass
        return 11;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof SchwimmenGameState state)) return;
        int nPlayers = state.getNPlayers();
        int currentPlayer = state.getCurrentPlayer();
        for (int p = 0; p < nPlayers; p++) {
            PartialObservableDeck<FrenchCard> hand = state.getPlayerHand(p);
            boolean[] faceUp = new boolean[hand.getSize()];
            boolean[] seenByAll = new boolean[hand.getSize()];
            for (int i = 0; i < hand.getSize(); i++) {
                seenByAll[i] = seenByAll(hand, i, nPlayers);
                faceUp[i] = seenByAll[i] || shownToViewer(state, hand, i, p);
            }
            handViews[p].update(hand, faceUp, seenByAll, status(state, p, faceUp));
            titles[p].setTitle("Player " + p + " [" + agentNames[p] + "]" + (p == state.getDealer() ? " - dealer" : ""));
            handViews[p].setBorder(p == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, titles[p])
                    : handBorders[p]);
        }
        tableView.update(state.getExtraHand().getSize(), state.getTable(), state.getDrawDeck().getSize(),
                state.getDiscardPile().getSize(),
                state.getDiscardPile().getSize() == 0 ? null : state.getDiscardPile().peek());
        centreText.setText(centreText(state));
        parent.repaint();
    }

    private static boolean seenByAll(PartialObservableDeck<FrenchCard> hand, int i, int nPlayers) {
        for (int p = 0; p < nPlayers; p++)
            if (!hand.getVisibilityForPlayer(i, p))
                return false;
        return true;
    }

    /**
     * Whether card i of player p's hand is face up on screen, apart from cards every player has seen.
     */
    private boolean shownToViewer(SchwimmenGameState state, PartialObservableDeck<FrenchCard> hand, int i, int p) {
        if (!state.isNotTerminal() || state.getCoreGameParameters().alwaysDisplayFullObservable)
            return true;
        for (int h : humanPlayerIds)
            if (hand.getVisibilityForPlayer(i, h))
                return true;
        return state.getCoreGameParameters().alwaysDisplayCurrentPlayer && p == state.getCurrentPlayer();
    }

    private String status(SchwimmenGameState state, int p, boolean[] faceUp) {
        SchwimmenParameters params = (SchwimmenParameters) state.getGameParameters();
        StringBuilder status = new StringBuilder();
        if (params.livesGame) {
            int chips = state.getChips(p);
            status.append(!state.isInGame(p) ? "out" : chips == 0 ? "swimming" : chips + (chips == 1 ? " chip" : " chips"));
        }
        boolean allFaceUp = faceUp.length > 0;
        for (boolean up : faceUp)
            allFaceUp &= up;
        if (allFaceUp) {
            if (!status.isEmpty()) status.append(",  ");
            status.append("hand ").append(formatValue(state.getHandValue(p)));
        }
        if (p == state.getCloser()) {
            if (!status.isEmpty()) status.append(",  ");
            status.append("closed");
        }
        return status.toString();
    }

    private static String formatValue(double value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.valueOf(value);
    }

    private String centreText(SchwimmenGameState state) {
        SchwimmenParameters params = (SchwimmenParameters) state.getGameParameters();
        StringBuilder text = new StringBuilder("<html>");
        if (params.livesGame)
            text.append("Deal ").append(state.getRoundCounter() + 1).append("<br>");
        if (!state.isNotTerminal()) {
            text.append("<b>Game over</b><br>");
            for (int p = 0; p < state.getNPlayers(); p++) {
                CoreConstants.GameResult result = state.getPlayerResults()[p];
                text.append("Player ").append(p).append(": ")
                        .append(params.livesGame ? (!state.isInGame(p) ? "out"
                                : state.getChips(p) + (state.getChips(p) == 1 ? " chip" : " chips"))
                                : formatValue(state.getHandValue(p)))
                        .append(", ")
                        .append(result == CoreConstants.GameResult.WIN_GAME ? "wins"
                                : result == CoreConstants.GameResult.DRAW_GAME ? "draws" : "loses")
                        .append("<br>");
            }
            return text.append("</html>").toString();
        }
        if (state.getCloser() >= 0)
            text.append("Player ").append(state.getCloser()).append(" has closed: last turns<br>");
        else
            text.append("Passes in a row: ").append(state.getConsecutivePasses()).append(" of ")
                    .append(state.getNPlayersInGame()).append("<br>");
        int current = state.getCurrentPlayer();
        String task = state.isDealerChoicePending() ? "to keep their hand or take the extra hand"
                : state.isActionInProgress() ? "to close or not" : "to exchange or pass";
        text.append("<b>Player ").append(current).append(" ").append(task).append("</b>");
        return text.append("</html>").toString();
    }

    private JPanel createRulesPanel(SchwimmenGameState state) {
        SchwimmenParameters params = (SchwimmenParameters) state.getGameParameters();
        JPanel rules = new JPanel();
        rules.setBackground(new Color(43, 108, 25, 111));
        // a fixed body width, or the label is laid out as one long line per paragraph
        JLabel text = new JLabel("<html><body style='width: " + (width / 2) + "px'><center><h1>Schwimmen</h1>" +
                "</center><hr><p>Collect three cards of one suit with the highest total you can.</p><ul>" +
                "<li>32 cards, Seven to Ace. Ace 11, King, Queen, Jack and Ten 10, the others their number. A hand " +
                "is worth its best single-suit total; three of a kind is worth " + formatValue(params.threeOfAKindValue) +
                " and three Aces (Feuer) " + formatValue(params.threeAcesValue) + ".</li>" +
                "<li>Each player is dealt three cards, and an extra hand of three is dealt face down. The dealer " +
                "keeps their hand or takes the extra hand without looking at it; the three cards not chosen go face " +
                "up on the table.</li>" +
                "<li>Play starts on the dealer's left. On your turn, exchange one card with a table card, exchange " +
                "your whole hand with the table, or pass. Then, if nobody has closed, you may close: each other " +
                "player has one more turn, and the deal ends.</li>" +
                "<li>When every player passes in a row, the table cards are discarded and three new ones dealt from " +
                "the draw deck; if it has fewer than three cards, the deal ends.</li>" +
                "<li>A player holding " + params.schnauzTotal + " in one suit (Schnauz) or three Aces (Feuer) ends " +
                "the deal at once.</li>" +
                (params.livesGame
                        ? "<li>Each player starts with " + params.startingChips + " chips. At the end of a deal the " +
                        "worst hand loses a chip (after Feuer, every other player does). A player with no chips is " +
                        "swimming, and drops out if they lose again. The deal passes to the left, and the last " +
                        "player in wins.</li>"
                        : "<li>The best hand wins.</li>") +
                "<li>Ties: a higher three of a kind wins; otherwise the higher suit (Clubs, Spades, Hearts, " +
                "Diamonds).</li>" +
                "</ul><hr><p><b>INTERFACE:</b> choose an action from the list at the bottom of the screen. A dot " +
                "marks a hand card every player saw taken from the table.</p></html>");
        text.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(width * 2 / 3 + 60, height * 2 / 3 + 100));
        rules.add(scroll);
        return rules;
    }
}
