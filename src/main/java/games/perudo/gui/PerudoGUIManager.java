package games.perudo.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.perudo.PerudoGameState;
import games.perudo.PerudoParameters;
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
 * The current bid at the top of the table, and one area per player below it with their dice.
 */
public class PerudoGUIManager extends AbstractGUIManager {

    static final String backgroundPath = "data/FrenchCards/table-background.jpg";
    static final int COLUMNS = 3;
    // the area's own height plus its titled border
    static final int AREA_HEIGHT = PerudoPlayerView.AREA_HEIGHT + 40;
    static final int AREA_WIDTH = PerudoPlayerView.AREA_WIDTH + 30;
    static final int ACTION_PANEL_HEIGHT = 170;

    PerudoPlayerView[] playerViews;
    JPanel[] playerAreas;
    Border[] playerBorders;
    PerudoBidView bidView;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public PerudoGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        PerudoGameState state = (PerudoGameState) gameState;
        int nPlayers = state.getNPlayers();
        int columns = Math.min(COLUMNS, nPlayers);
        int rows = (nPlayers + COLUMNS - 1) / COLUMNS;
        this.width = Math.max(columns * AREA_WIDTH + 20, 2 * defaultInfoPanelWidth + 40);
        this.height = PerudoBidView.HEIGHT + rows * AREA_HEIGHT + 20;

        parent.setBackground(ImageIO.GetInstance().getImage(backgroundPath));
        // without these the tabbed pane's content area paints over the parent's background image
        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        tabs.add("Game", main);
        tabs.add("Rules", new RulesView(rulesHtml((PerudoParameters) state.getGameParameters()),
                height + ACTION_PANEL_HEIGHT + defaultInfoPanelHeight - 20));

        bidView = new PerudoBidView();
        JPanel bidWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bidWrapper.setOpaque(false);
        bidWrapper.add(bidView);

        JPanel playersPanel = new JPanel(new GridLayout(rows, columns, 10, 6));
        playersPanel.setOpaque(false);
        playerViews = new PerudoPlayerView[nPlayers];
        playerAreas = new JPanel[nPlayers];
        playerBorders = new Border[nPlayers];
        for (int p = 0; p < nPlayers; p++) {
            playerViews[p] = new PerudoPlayerView(p);
            String[] split = game.getPlayers().get(p).getClass().toString().split("\\.");
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                    "Player " + p + " [" + split[split.length - 1] + "]",
                    TitledBorder.CENTER, TitledBorder.TOP);
            title.setTitleColor(Color.white);
            playerBorders[p] = title;
            JPanel area = new JPanel(new GridBagLayout());
            area.setOpaque(false);
            area.setBorder(title);
            area.add(playerViews[p]);
            playerAreas[p] = area;
            playersPanel.add(area);
        }
        JPanel playersWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        playersWrapper.setOpaque(false);
        playersWrapper.add(playersPanel);

        JPanel table = new JPanel(new BorderLayout());
        table.setOpaque(false);
        table.add(bidWrapper, BorderLayout.NORTH);
        table.add(playersWrapper, BorderLayout.CENTER);

        JPanel infoPanel = createGameStateInfoPanel("Perudo", gameState, width, defaultInfoPanelHeight);
        // the opening bid can offer every quantity on every face; the standard panel wraps the buttons into rows that scroll
        JComponent actionPanel = createActionPanelOpaque(new IScreenHighlight[0], width, ACTION_PANEL_HEIGHT, false);

        main.add(infoPanel, BorderLayout.NORTH);
        main.add(table, BorderLayout.CENTER);
        main.add(actionPanel, BorderLayout.SOUTH);

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + ACTION_PANEL_HEIGHT + defaultInfoPanelHeight + 40));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // The most actions come at the opening bid of a Palifico round, when every quantity up to the dice on the
        // table may be bid on every face. The most dice on the table are 6 players x 6 dice (the largest tunable
        // PerudoParameters.nDicePerPlayer) = 36, on 6 faces: 36 x 6 = 216 bids, + Dudo and Calza = 218.
        return 218;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof PerudoGameState state)) return;
        int currentPlayer = state.getCurrentPlayer();
        for (int p = 0; p < playerViews.length; p++) {
            playerViews[p].update(state, showDice(state, p));
            playerAreas[p].setBorder(p == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerBorders[p])
                    : playerBorders[p]);
        }
        bidView.update(state);
        parent.repaint();
    }

    /**
     * A player's dice are face up for a human player, for the current player if the core parameters allow it, in
     * full-observability mode, or once the game is over.
     */
    private boolean showDice(PerudoGameState state, int playerId) {
        return humanPlayerIds.contains(playerId) || !state.isNotTerminal()
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }

    private static String rulesHtml(PerudoParameters params) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Perudo</h2>")
                .append("<p>Be the last player with dice.</p>")
                .append("<p><b>Each round.</b> Every player starts the game with ").append(params.nDicePerPlayer)
                .append(" dice. At the start of each round every player still in rolls their dice in secret. ")
                .append("Player 0 opens the first round.</p>")
                .append("<p><b>Bids.</b> A bid such as \"5 x 3\" claims that at least 5 of all the dice on the table ")
                .append("show a 3. A Paco (the 1, shown as a star) is wild: it counts towards a bid on any other ")
                .append("face. Bids go round the table in seat order, and each player must raise the bid or call ")
                .append("Dudo. No bid may be for more dice than are on the table.</p>")
                .append("<p>The opening bid of a round may be on any face but Pacos. A raise is:</p><ul>")
                .append("<li>more dice of any face, or the same number of dice of a higher face;</li>")
                .append("<li>Pacos, at least half the number of dice bid (rounded up): 5 x 4 may be raised to 3 Pacos;</li>")
                .append("<li>from Pacos, more Pacos, or more than twice as many dice of another face: 3 Pacos may be ")
                .append("raised to 7 of any face.</li></ul>")
                .append("<p><b>Dudo.</b> A player who doubts the bid calls Dudo, and all the dice are shown. If fewer ")
                .append("dice match the bid than it claims, the bidder loses a die. Otherwise the caller loses a die.</p>");
        if (params.calza)
            sb.append("<p><b>Calza.</b> Instead of Dudo a player may call Calza, claiming the bid is exactly ")
                    .append("right. If exactly that many dice match, the caller gains a die (up to ")
                    .append(params.nDicePerPlayer).append("). Otherwise the caller loses a die. The bidder neither ")
                    .append("gains nor loses. The caller starts the next round. There is no Calza in a Palifico round.</p>");
        sb.append("<p><b>Next round.</b> The player who lost a die starts the next round. A player who loses their ")
                .append("last die is out, and the next player still in starts.</p>");
        if (params.palifico)
            sb.append("<p><b>Palifico.</b> The first time a player is left with one die, the next round is a ")
                    .append("Palifico round, which they start. In it Pacos are not wild, the opening bid may be on ")
                    .append("Pacos, and every later bid must be on the same face for more dice. Each player has one ")
                    .append("Palifico round at most.</p>");
        sb.append("<p><b>End.</b> The last player with dice wins. The others are ranked by how late they went out.</p>")
                .append("<h3>Interface</h3>")
                .append("<p>The top of the table shows the current bid, who made it, and the number of dice on the ")
                .append("table");
        if (params.palifico)
            sb.append(", with a note when the round is a Palifico round");
        sb.append(". Each player's area shows their dice, face down (?) unless you may see them, and how many dice ")
                .append("they hold. A blue outline shows whose turn it is. The action buttons are the legal bids, ")
                .append("written \"Bid 5 x 3\" for five 3s and \"Bid 3 Pacos\", and Dudo");
        if (params.calza)
            sb.append(" and Calza");
        sb.append(". When a challenge is called, the dice shown and the result appear in the history panel.</p>");
        return sb.toString();
    }
}
