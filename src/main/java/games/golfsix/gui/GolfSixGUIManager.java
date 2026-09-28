package games.golfsix.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import games.golfsix.GolfSixGameState;
import games.golfsix.GolfSixParameters;
import games.tricktaking.gui.CardArt;
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
 * <p>GUI for Six-card Golf: each player's grid, in two rows round the table, and in the centre the draw deck, the
 * discard pile and the card the current player has drawn.</p>
 *
 * <p>Face-down grid cards are face-down for everyone, their owner included, unless the core parameters make the
 * display fully observable. A card drawn from the draw deck is shown only to a human who drew it, or as the core
 * parameters allow.</p>
 */
public class GolfSixGUIManager extends AbstractGUIManager {

    GolfSixGridView[] gridViews;
    GolfSixTableView tableView;
    JLabel centreText;
    Border[] gridBorders;
    TitledBorder[] titles;
    String[] agentNames;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public GolfSixGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        GolfSixGameState state = (GolfSixGameState) gameState;
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

        gridViews = new GolfSixGridView[nPlayers];
        gridBorders = new Border[nPlayers];
        titles = new TitledBorder[nPlayers];
        agentNames = new String[nPlayers];
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        top.setOpaque(false);
        bottom.setOpaque(false);
        for (int i = 0; i < nPlayers; i++) {
            GolfSixGridView view = new GolfSixGridView(i);
            view.setOpaque(false);
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i,
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            title.setTitleColor(Color.white);   // the table background is dark
            titles[i] = title;
            // the same width as highlightActive, so that highlighting the current player does not move anything
            gridBorders[i] = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3), title);
            view.setBorder(gridBorders[i]);
            gridViews[i] = view;
        }
        // at least two grids wide, so that the info panel is legible with 2 players
        Dimension gridSize = gridViews[0].getPreferredSize();
        this.width = Math.max(bottomRow, 2) * (gridSize.width + 10) + 30;
        this.height = 2 * gridSize.height + new GolfSixTableView().getPreferredSize().height + 20;
        tabs.add("Rules", new RulesView(rulesHtml((GolfSixParameters) state.getGameParameters()), height));

        for (int i = nPlayers - 1; i >= bottomRow; i--)
            top.add(gridViews[i]);
        for (int i = 0; i < bottomRow; i++)
            bottom.add(gridViews[i]);

        tableView = new GolfSixTableView();
        tableView.setOpaque(false);
        centreText = new JLabel();
        centreText.setForeground(Color.white);
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

        JPanel infoPanel = createGameStateInfoPanel("Six-card Golf", gameState, width, defaultInfoPanelHeight);
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

    /**
     * Seven: ReplaceCard for each of the 6 grid positions, and DiscardCard.
     */
    @Override
    public int getMaxActionSpace() {
        return 7;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof GolfSixGameState state)) return;
        GolfSixParameters params = (GolfSixParameters) state.getGameParameters();
        boolean showAll = state.getCoreGameParameters().alwaysDisplayFullObservable;
        int currentPlayer = state.getCurrentPlayer();
        for (int i = 0; i < gridViews.length; i++) {
            String status = "showing " + GolfSixGridView.faceUpScore(state.getGrid(i), i, params) + " points";
            if (params.nDeals > 1)
                status += ",  total " + state.getScore(i);
            gridViews[i].update(state.getGrid(i), showAll, status);
            titles[i].setTitle("Player " + i + " [" + agentNames[i] + "]");
            gridViews[i].setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, titles[i])
                    : gridBorders[i]);
        }
        tableView.update(state.getDrawDeck().getSize(), state.getDiscardPile().getSize(),
                state.getDiscardPile().getSize() == 0 ? null : state.getDiscardPile().peek(),
                state.getDrawnCard(), showDrawnCard(state));
        centreText.setText(centreText(state));
        parent.repaint();
    }

    /**
     * A drawn card is face-up if it came from the discard pile; for a human who drew it; for the current player if
     * the core parameters allow it; or in full-observability mode.
     */
    private boolean showDrawnCard(GolfSixGameState state) {
        int currentPlayer = state.getCurrentPlayer();
        return state.isDrawnFromDiscard()
                || humanPlayerIds.contains(currentPlayer)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || state.getCoreGameParameters().alwaysDisplayCurrentPlayer;
    }

    private String centreText(GolfSixGameState state) {
        GolfSixParameters params = (GolfSixParameters) state.getGameParameters();
        StringBuilder text = new StringBuilder("<html>");
        if (params.nDeals > 1)
            text.append("Deal ").append(Math.min(state.getRoundCounter() + 1, params.nDeals))
                    .append(" of ").append(params.nDeals).append("<br>");
        if (!state.isNotTerminal()) {
            text.append("<b>Game over</b><br>");
            for (int p = 0; p < state.getNPlayers(); p++) {
                CoreConstants.GameResult result = state.getPlayerResults()[p];
                text.append("Player ").append(p).append(": ").append(state.getScore(p)).append(" points, ")
                        .append(result == CoreConstants.GameResult.WIN_GAME ? "wins"
                                : result == CoreConstants.GameResult.DRAW_GAME ? "draws" : "loses")
                        .append("<br>");
            }
            return text.append("</html>").toString();
        }
        text.append("Dealer: player ").append(state.getDealer()).append("<br>");
        if (state.getFinisher() >= 0)
            text.append("Player ").append(state.getFinisher()).append(" has all cards face-up: last turns<br>");
        int current = state.getCurrentPlayer();
        String task = state.faceUpCount(current) < params.initialFaceUp ? "to turn up a card"
                : state.getDrawnCard() == null ? "to draw a card" : "to place the drawn card";
        text.append("<b>Player ").append(current).append(" ").append(task).append("</b>");
        return text.append("</html>").toString();
    }

    private static String rulesHtml(GolfSixParameters params) {
        int columns = GolfSixParameters.COLUMNS, gridSize = GolfSixParameters.GRID_SIZE;
        String turnUp = params.initialFaceUp == 0 ? ""
                : "<p><b>Before play</b> each player in turn turns " + params.initialFaceUp + " of their cards " +
                "face up.</p>";
        String end = params.finalTurns
                ? "When all of a player's cards are face up, each other player will have one more turn. The deal " +
                "is then scored."
                : "The deal is scored as soon as all of a player's cards are face up.";
        String deals = params.nDeals == 1 ? "The game is a single deal. The lowest score wins."
                : "The game is " + params.nDeals + " deals, and the deal passes to the next player each time. " +
                "The lowest total wins.";
        return "<h2>Six-card Golf</h2>" +
                "<p>The aim is to score as few points as possible with the cards in your grid.</p>" +
                "<p><b>The deal.</b> Each player is dealt " + gridSize + " cards face down, in a grid of " + columns +
                " columns and two rows. Nobody may look at a face-down card, including its owner. The top card of " +
                "the draw deck is turned face up to start the discard pile.</p>" +
                turnUp +
                "<p><b>Each turn</b> has two steps.</p><ol>" +
                "<li>Draw from the draw deck, or draw from the discard pile.</li>" +
                "<li>Put the drawn card face up in your grid in place of any card, face up or face down. The card " +
                "it replaces goes face up on the discard pile. A card drawn from the draw deck may instead be " +
                "discarded. A card drawn from the discard pile must be placed in the grid.</li></ol>" +
                "<p>When the draw deck runs out, the discard pile except its top card is shuffled to form a new " +
                "draw deck.</p>" +
                "<p><b>End of a deal.</b> " + end + " A deal is also scored once each player has had " +
                params.maxTurnsPerPlayer + " turns. All the cards are turned face up and scored.</p>" +
                "<table border=1 cellpadding=4 cellspacing=0>" +
                "<tr><th align=left>Card</th><th>Ace</th><th>Two</th><th>Jack, Queen</th><th>King</th></tr>" +
                "<tr><th align=left>Points</th><td align=center>" + params.aceValue + "</td><td align=center>" +
                params.twoValue + "</td><td align=center>" + params.courtValue + "</td><td align=center>" +
                params.kingValue + "</td></tr></table>" +
                "<p>The other cards score their number. Two cards of the same rank in a column score nothing (a " +
                "pair of Twos included).</p>" +
                "<p><b>Winning.</b> " + deals + " Players with the same lowest score draw.</p>" +
                "<h3>Interface</h3>" +
                "<p>Choose from the action buttons at the bottom. Turn up position, Draw from draw deck, Draw " +
                "from discard pile, Replace position and Discard drawn card are the steps above. The number on " +
                "each card is its position, from 0 to " + (columns - 1) + " along the top row and " + columns +
                " to " + (gridSize - 1) + " along the bottom row.</p>" +
                "<p>Player 0's grid is at the bottom left, and play goes round the table in the order of the " +
                "player numbers. Under each grid are the points its face-up cards show" +
                (params.nDeals > 1 ? ", and the player's total from earlier deals" : "") +
                ". The current player's grid has a blue border. The centre shows the draw deck and the discard " +
                "pile (with the number of cards in each), the drawn card, the dealer, and what the current " +
                "player is to do. A card drawn from the draw deck is face up only to the player who drew it.</p>";
    }
}
