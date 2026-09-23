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
        tabs.add("Rules", createRulesPanel(state));

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

    private JPanel createRulesPanel(GolfSixGameState state) {
        GolfSixParameters params = (GolfSixParameters) state.getGameParameters();
        JPanel rules = new JPanel();
        rules.setBackground(new Color(43, 108, 25, 111));
        // a fixed body width, or the label is laid out as one long line per paragraph. Swing's CSS pixels are larger
        // than screen pixels, so it is well inside the scroll pane's width
        JLabel text = new JLabel("<html><body style='width: " + (width / 2) + "px'><center><h1>Six-card Golf</h1>" +
                "</center><hr><p>Score as few points as you can with the six cards in your grid.</p><ul>" +
                "<li>Each player is dealt six cards face-down in a grid of three columns and two rows. Nobody may " +
                "look at a face-down card, not even its owner. The top card of the draw deck starts the discard " +
                "pile.</li>" +
                "<li>Before play starts, each player turns " + params.initialFaceUp + " of their cards face-up.</li>" +
                "<li>On your turn, draw the top card of the draw deck or of the discard pile. Then put it face-up " +
                "in your grid in place of any card, face-up or face-down; the card it replaces goes face-up on the " +
                "discard pile. A card drawn from the draw deck may instead be discarded; a card taken from the " +
                "discard pile must be used.</li>" +
                "<li>When the draw deck runs out, the discard pile, except its top card, is shuffled to form a new " +
                "one.</li>" +
                "<li>Play ends as soon as a player's six cards are all face-up. Then every card is turned face-up " +
                "and scored: Ace 1, Two -2, Three to Ten their number, Jack and Queen 10, King 0. Two cards of the " +
                "same rank in a column score nothing.</li>" +
                (params.finalTurns ? "<li>Once a player's cards are all face-up, each other player has one more " +
                        "turn before the cards are scored.</li>" : "") +
                (params.nDeals > 1 ? "<li>The game is " + params.nDeals + " deals, and the scores are added up. " +
                        "The deal passes to the left each time.</li>" : "") +
                "<li>The lowest score wins; players tied for lowest share a draw.</li>" +
                "</ul><hr><p><b>INTERFACE:</b> choose an action from the list at the bottom of the screen. Grid " +
                "positions are numbered 0 to 2 along the top row and 3 to 5 along the bottom, as shown on each " +
                "card. Under each grid are the points its face-up cards show.</p></html>");
        text.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(width * 2 / 3 + 60, height * 2 / 3 + 100));
        rules.add(scroll);
        return rules;
    }
}
