package games.agram.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.FrenchCard;
import games.agram.AgramGameState;
import games.agram.AgramParameters;
import games.tricktaking.gui.CardArt;
import games.tricktaking.gui.PlayerHandView;
import games.tricktaking.gui.TrickView;
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
 * <p>GUI for Agram: one area per player around the edges, and the trick in progress in the centre.</p>
 *
 * <p>This is a view only - it holds no rules and never changes the state. It is repainted for every player,
 * human or not, so a hand is only shown face-up when the viewer is entitled to see it (see showHand).</p>
 */
public class AgramGUIManager extends AbstractGUIManager {

    static final int playerAreaWidth = 300;
    // one player area: the cards, the status line underneath, and the titled border below that
    static final int playerAreaHeight = CardArt.cardHeight + 43;

    PlayerHandView[] playerViews;
    TrickView trickView;
    Border[] playerViewBorders;
    TitledBorder[] playerTitles;
    String[] agentNames;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public AgramGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        AgramGameState state = (AgramGameState) gameState;
        int nPlayers = state.getNPlayers();

        trickView = new TrickView(nPlayers);
        int nHorizAreas = nPlayers <= 3 ? 2 : 3;
        this.width = Math.max(playerAreaWidth * nHorizAreas + 40, trickView.getPreferredSize().width + 2 * playerAreaWidth);
        // three bands: the North player, the East/West players and the trick (the taller of the two), and the South player
        this.height = playerAreaHeight * 2 + Math.max(playerAreaHeight, trickView.getPreferredSize().height) + 40;

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
        tabs.add("Rules", createRulesPanel());

        // Player areas: player 0 at the bottom, then round the table
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
            PlayerHandView playerView = new PlayerHandView(state.getPlayerHands().get(i), i, playerAreaWidth);
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
            // 5 players: two share the North side
            sides[i % locations.length].add(playerView);
            playerViews[i] = playerView;
        }
        for (int s = 0; s < locations.length; s++)
            mainGameArea.add(sides[s], locations[s]);

        JPanel centreWrapper = new JPanel(new GridBagLayout());
        centreWrapper.setOpaque(false);
        centreWrapper.add(trickView);
        mainGameArea.add(centreWrapper, BorderLayout.CENTER);

        JPanel infoPanel = createGameStateInfoPanel("Agram", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight, false);

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
     * The only action is PlayCard, one per card in hand, and a player never holds more than nCardsPerPlayer cards.
     * The largest value nCardsPerPlayer can take (AgramParameters) is 6, so at most 6 actions are ever offered.
     */
    @Override
    public int getMaxActionSpace() {
        return 6;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof AgramGameState state)) return;
        AgramParameters params = (AgramParameters) state.getGameParameters();
        int currentPlayer = state.getCurrentPlayer();
        // the leader of a deal's first trick sits after its dealer
        int dealer = (state.getFirstPlayer() + state.getNPlayers() - 1) % state.getNPlayers();

        for (int i = 0; i < playerViews.length; i++) {
            String deals = params.nDeals > 1 ? "deals won " + state.getDealsWon(i) : "";
            playerViews[i].update(state.getPlayerHands().get(i), showHand(state, i),
                    state.getKnownVoids().get(i), deals);
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]" + (i == dealer ? " - dealer" : ""));
            playerViews[i].setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i])
                    : playerViewBorders[i]);
        }
        updateTrickView(state, params);
        parent.repaint();
    }

    /**
     * The centre panel: the deal and trick numbers, the suit led, and how many tricks have been played.
     */
    private void updateTrickView(AgramGameState state, AgramParameters params) {
        int tricksDone = state.getDiscardPile().getSize() / state.getNPlayers();
        int trick = Math.min(tricksDone + 1, params.nCardsPerPlayer);
        String header = (params.nDeals > 1 ? "Deal " + (state.getRoundCounter() + 1) + " of " + params.nDeals + "   " : "")
                + "Trick " + trick + " of " + params.nCardsPerPlayer
                + (trick == params.nCardsPerPlayer ? "  - the last trick wins" : "");
        FrenchCard.Suite lead = state.getCurrentTrick().getLeadSuit();
        String leadText;
        if (!state.isNotTerminal()) {
            // the trick leader is the winner of the last trick, which has gone to the discard pile
            header = params.nDeals > 1 ? "Match over" : "Game over";
            leadText = "Player " + state.getCurrentTrick().getLeader() + " won the last trick"
                    + (params.nDeals > 1 ? " of the last deal" : " and the game");
        } else leadText = lead == null
                ? "Player " + state.getCurrentTrick().getLeader() + " to lead"
                : "Suit led: " + CardArt.suitText(lead) + "   -   highest " + lead.name() + " wins";
        String footer = "Tricks played this deal: " + tricksDone + "     Cards not dealt: "
                + state.getDrawDeck().getSize() + " (unused)";
        trickView.update(state.getCurrentTrick(), null, header, leadText, footer, state.isNotTerminal());
    }

    /**
     * A hand is face-up only for a human player, for the current player if the core parameters allow it,
     * or in full-observability mode.
     */
    private boolean showHand(AgramGameState state, int playerId) {
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }

    private JPanel createRulesPanel() {
        JPanel rules = new JPanel();
        rules.setBackground(new Color(43, 108, 25, 111));
        JLabel text = new JLabel("<html><center><h1>Agram</h1></center><hr>" +
                "<p>A trick-taking game from West Africa: <b>win the last trick</b>.</p><ul>" +
                "<li>35 cards: A, 10, 9, 8, 7, 6, 5, 4, 3 in each suit, but no Ace of Spades. Aces are high.</li>" +
                "<li>Each player is dealt 6 cards and there are 6 tricks. The rest of the cards are not used.</li>" +
                "<li>The player after the dealer leads the first trick with any card.</li>" +
                "<li>Everyone else must follow suit if they can - but need not play higher. " +
                "If you cannot follow suit you may play any card, and everyone then knows you have none of that suit.</li>" +
                "<li>The highest card of the suit led wins the trick; there are no trumps. " +
                "The winner leads the next trick.</li>" +
                "<li>Whoever wins the last trick wins the deal. Earlier tricks count for nothing.</li>" +
                "<li>In a match of several deals, the winner of each deal deals the next, and the player " +
                "who has won most deals wins.</li>" +
                "</ul><hr><p><b>INTERFACE:</b> choose a card from the action buttons at the bottom of the screen. " +
                "The centre shows the trick so far, with the winning card outlined.</p></html>");
        text.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(width * 2 / 3 + 60, height * 2 / 3 + 100));
        rules.add(scroll);
        return rules;
    }
}
