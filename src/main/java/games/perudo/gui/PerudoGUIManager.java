package games.perudo.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.perudo.PerudoGameState;
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
        RulesView.addTabs(tabs, GameType.Perudo, state.getGameParameters(),
                height + ACTION_PANEL_HEIGHT + defaultInfoPanelHeight - 20);

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
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                    "Player " + p + " [" + game.getPlayers().get(p).toString() + "]",
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
        // The most actions come when PerudoParameters.minBidPercent is 0 and maxBidPercent 100, at the opening bid of
        // a Palifico round, when every quantity up to the dice on the table may be bid on every face. The most dice on
        // the table are 6 players x 6 dice (the largest tunable PerudoParameters.nDicePerPlayer) = 36, on 6 faces:
        // 36 x 6 = 216 bids, + Dudo and Calza = 218.
        return 218;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof PerudoGameState state)) return;
        int currentPlayer = state.getCurrentPlayer();
        for (int p = 0; p < playerViews.length; p++) {
            playerViews[p].update(state, showHiddenInfo(state, p) || !state.isNotTerminal());
            playerAreas[p].setBorder(p == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerBorders[p])
                    : playerBorders[p]);
        }
        bidView.update(state);
        parent.repaint();
    }
}
