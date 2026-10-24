package games.crazyeights.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.crazyeights.CZEGameState;
import gui.AbstractGUIManager;
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
import java.util.Set;

/**
 * <p>GUI for Crazy Eights, in the style of the Uno GUI: one area per player around the edges, with the stock,
 * the discard pile and the suit to match in the centre.</p>
 *
 * <p>This is a view only - it holds no rules and never changes the state. It is repainted for every player,
 * human or not, so a hand is only shown face-up when the viewer is entitled to see it (see showHand).</p>
 */
public class CZEGUIManager extends AbstractGUIManager {

    static final String dataPath = "data/FrenchCards/";

    static final int playerAreaWidth = 300;
    static final int cardWidth = 90;
    static final int cardHeight = 115;
    // one player area: the cards, the card count underneath, and the titled border below that
    static final int playerAreaHeight = cardHeight + 43;
    // taller than the default, because a hand can offer many playable cards at once
    static final int actionPanelHeight = defaultActionPanelHeight + 50;

    CZEPlayerView[] playerViews;
    CZEDeckView drawPileView;
    CZEDeckView discardPileView;
    CZESuitView suitView;

    int activePlayer = -1;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);
    Border[] playerViewBorders;

    public CZEGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        CZEGameState state = (CZEGameState) gameState;
        activePlayer = state.getCurrentPlayer();
        int nPlayers = state.getNPlayers();

        int nHorizAreas = 1 + (nPlayers <= 3 ? 2 : nPlayers == 4 ? 3 : nPlayers <= 8 ? 4 : 5);
        this.width = playerAreaWidth * nHorizAreas;
        // three bands: the North player, the East/West players and the centre, and the South player
        this.height = playerAreaHeight * 3 + 30;

        parent.setBackground(ImageIO.GetInstance().getImage(dataPath + "table-background.jpg"));

        // without these the tabbed pane's content area paints over the parent's background image
        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        tabs.add("Game", main);
        RulesView.addTabs(tabs, GameType.CrazyEights, state.getGameParameters(), height);

        // Player areas, spread round the four sides
        playerViews = new CZEPlayerView[nPlayers];
        playerViewBorders = new Border[nPlayers];
        JPanel mainGameArea = new JPanel(new BorderLayout());
        mainGameArea.setOpaque(false);
        // player 0 sits at the bottom (the human seat), the rest fill the other sides
        String[] locations = {BorderLayout.SOUTH, BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.WEST};
        JPanel[] sides = {new JPanel(), new JPanel(), new JPanel(), new JPanel()};
        for (JPanel side : sides) {
            side.setLayout(new GridBagLayout());
            side.setOpaque(false);   // an unused side would otherwise paint a grey block over the table
        }
        int next = 0;
        for (int i = 0; i < nPlayers; i++) {
            CZEPlayerView playerView = new CZEPlayerView(state.getPlayerHands().get(i), i, dataPath);
            playerView.setOpaque(false);

            String agentName = game.getPlayers().get(i).toString();
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                    "Player " + i + " [" + agentName + "]" + (i == nPlayers - 1 ? " - dealer" : ""),
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            playerViewBorders[i] = title;
            playerView.setBorder(title);

            sides[next].add(playerView);
            next = (next + 1) % locations.length;
            playerViews[i] = playerView;
        }
        for (int i = 0; i < locations.length; i++)
            mainGameArea.add(sides[i], locations[i]);

        // Centre: the stock, the discard pile and the suit to match
        drawPileView = new CZEDeckView(-1, state.getDrawDeck(),
                state.getCoreGameParameters().alwaysDisplayFullObservable, dataPath,
                new Rectangle(0, 0, cardWidth, cardHeight));
        discardPileView = new CZEDeckView(-1, state.getDiscardPile(), true, dataPath,
                new Rectangle(0, 0, cardWidth, cardHeight));
        suitView = new CZESuitView();

        JPanel centreArea = new JPanel();
        centreArea.setOpaque(false);
        centreArea.setLayout(new BoxLayout(centreArea, BoxLayout.X_AXIS));
        centreArea.add(labelled("Stock", drawPileView));
        centreArea.add(Box.createHorizontalStrut(8));
        centreArea.add(labelled("Discards", discardPileView));
        centreArea.add(Box.createHorizontalStrut(14));
        centreArea.add(suitView);

        JPanel centreWrapper = new JPanel(new GridBagLayout());
        centreWrapper.setOpaque(false);
        centreWrapper.add(centreArea);
        mainGameArea.add(centreWrapper, BorderLayout.CENTER);

        JPanel infoPanel = createGameStateInfoPanel("Crazy Eights", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanelOpaque(new IScreenHighlight[0], width, actionPanelHeight, false);

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

    /** A deck view with a caption above it, at a fixed size so the surrounding BoxLayout cannot squash it. */
    private JPanel labelled(String text, JComponent view) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel label = new JLabel(text);
        label.setForeground(Color.white);
        panel.add(label, BorderLayout.NORTH);
        panel.add(view, BorderLayout.CENTER);
        Dimension size = new Dimension(cardWidth, cardHeight + 20);
        panel.setPreferredSize(size);
        panel.setMinimumSize(size);
        panel.setMaximumSize(size);
        return panel;
    }

    /**
     * The most actions ever offered at one decision point. A player must play if they can, so a hand can offer
     * at most: four Eights x four suit nominations (16), the twelve non-Eight cards of the suit to match (12), and
     * the at most three other cards matching the top card's rank (3) - 31 in all. When nothing is playable there is
     * a single Draw or Pass, and the dealer's starter nomination is four actions.
     */
    @Override
    public int getMaxActionSpace() {
        return 31;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof CZEGameState state)) return;

        int currentPlayer = state.getCurrentPlayer();
        if (currentPlayer != activePlayer) {
            if (activePlayer >= 0 && activePlayer < playerViews.length)
                playerViews[activePlayer].getHandView().setCardHighlight(-1);
            activePlayer = currentPlayer;
        }

        for (int i = 0; i < playerViews.length; i++) {
            playerViews[i].update(state, showHand(state, i));
            playerViews[i].setBorder(i == currentPlayer
                    ? BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i])
                    : playerViewBorders[i]);
        }

        drawPileView.updateComponent(state.getDrawDeck());
        drawPileView.setFront(state.getCoreGameParameters().alwaysDisplayFullObservable);
        discardPileView.updateComponent(state.getDiscardPile());
        suitView.update(state);

        parent.repaint();
    }

    /**
     * A hand is face-up only for a human player, for the current player if the core parameters allow it,
     * or in full-observability mode.
     */
    private boolean showHand(CZEGameState state, int playerId) {
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }
}
