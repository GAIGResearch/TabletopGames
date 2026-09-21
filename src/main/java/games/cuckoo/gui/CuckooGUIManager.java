package games.cuckoo.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.cuckoo.CuckooGameState;
import games.tricktaking.gui.CardArt;
import games.tricktaking.gui.FrenchCardDeckView;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.WIN_GAME;

/**
 * <p>GUI for Cuckoo: the players' seats round an oval table, clockwise from player 0 at the bottom, with the draw
 * deck and the state of the round in the centre.</p>
 *
 * <p>This is a view only - it holds no rules and never changes the state. It is repainted for every player,
 * human or not, so a card is only shown face-up when the viewer is entitled to see it (see showCard).</p>
 */
public class CuckooGUIManager extends AbstractGUIManager {

    // the whole window must fit a 1080-pixel screen; up to ten seats do not overlap at this height
    static final int tableHeight = 660;
    static final int centreWidth = 330, centreHeight = CardArt.cardHeight + 100;

    // more than eight seats round the ellipse need a wider table, or the neighbouring side seats overlap
    int tableWidth;
    CuckooPlayerView[] playerViews;
    Border[] playerViewBorders;
    TitledBorder[] playerTitles;
    String[] agentNames;
    FrenchCardDeckView drawDeckView;
    JLabel[] centreLines;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public CuckooGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        CuckooGameState state = (CuckooGameState) gameState;
        int nPlayers = state.getNPlayers();
        tableWidth = nPlayers > 8 ? 1240 : 980;
        this.width = tableWidth;
        this.height = tableHeight;

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

        // the seats are placed by hand round an ellipse, so the table has no layout manager
        JPanel table = new JPanel(null);
        table.setOpaque(false);
        table.setPreferredSize(new Dimension(tableWidth, tableHeight));

        playerViews = new CuckooPlayerView[nPlayers];
        playerViewBorders = new Border[nPlayers];
        playerTitles = new TitledBorder[nPlayers];
        agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            CuckooPlayerView playerView = new CuckooPlayerView(state.getPlayerCards().get(i), i);
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
            Dimension size = playerView.getPreferredSize();
            Point seat = seat(i, nPlayers, size);
            playerView.setBounds(seat.x, seat.y, size.width, size.height);
            table.add(playerView);
            playerViews[i] = playerView;
        }

        JPanel centre = createCentrePanel(state);
        centre.setBounds((tableWidth - centreWidth) / 2, (tableHeight - centreHeight) / 2, centreWidth, centreHeight);
        table.add(centre);

        JPanel infoPanel = createGameStateInfoPanel("Cuckoo", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

        main.add(infoPanel, BorderLayout.NORTH);
        main.add(table, BorderLayout.CENTER);
        main.add(actionPanel, BorderLayout.SOUTH);

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        // the extra 60 is the tab header and the panels' insets, without which the bottom seat is cut off
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + 60));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    /**
     * The top-left corner of the player's seat: on an ellipse round the table, player 0 at the bottom and the next
     * player on their left, clockwise.
     */
    private Point seat(int player, int nPlayers, Dimension size) {
        double angle = 2 * Math.PI * player / nPlayers;
        double rx = (tableWidth - size.width) / 2.0, ry = (tableHeight - size.height) / 2.0;
        int x = (int) Math.round(tableWidth / 2.0 - rx * Math.sin(angle) - size.width / 2.0);
        int y = (int) Math.round(tableHeight / 2.0 + ry * Math.cos(angle) - size.height / 2.0);
        return new Point(x, y);
    }

    /**
     * The centre of the table: the draw deck, face down, beside the round, the dealer and whose turn it is.
     */
    private JPanel createCentrePanel(CuckooGameState state) {
        JPanel centre = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                super.paintComponent(g);
            }
        };
        centre.setOpaque(false);
        centre.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        drawDeckView = new FrenchCardDeckView(-1, state.getDrawDeck(), false,
                new Rectangle(0, 0, CardArt.cardWidth, CardArt.cardHeight));
        drawDeckView.setOpaque(false);
        drawDeckView.setPreferredSize(new Dimension(CardArt.cardWidth, CardArt.cardHeight));
        JPanel deckColumn = new JPanel(new BorderLayout());
        deckColumn.setOpaque(false);
        deckColumn.add(drawDeckView, BorderLayout.NORTH);
        JLabel deckLabel = new JLabel("Draw deck", SwingConstants.CENTER);
        deckLabel.setForeground(Color.white);
        deckColumn.add(deckLabel, BorderLayout.CENTER);
        centre.add(deckColumn, BorderLayout.WEST);

        JPanel text = new JPanel(new GridLayout(4, 1));
        text.setOpaque(false);
        centreLines = new JLabel[4];
        for (int i = 0; i < centreLines.length; i++) {
            centreLines[i] = new JLabel();
            centreLines[i].setForeground(Color.white);   // the table background is dark
            text.add(centreLines[i]);
        }
        centreLines[0].setFont(centreLines[0].getFont().deriveFont(Font.BOLD, 14f));
        centre.add(text, BorderLayout.CENTER);
        return centre;
    }

    /**
     * Two actions at most: keep the card or swap it.
     */
    @Override
    public int getMaxActionSpace() {
        return 2;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof CuckooGameState state)) return;
        int currentPlayer = state.getCurrentPlayer();
        int dealer = state.getDealer();

        for (int i = 0; i < playerViews.length; i++) {
            playerViews[i].update(state.getPlayerCards().get(i), showCard(state, i),
                    (i == dealer && state.isNotTerminal() ? "Dealer   " : "") + statusText(state, i));
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]");
            playerViews[i].setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i])
                    : playerViewBorders[i]);
        }
        drawDeckView.updateComponent(state.getDrawDeck());
        updateCentreText(state);
        parent.repaint();
    }

    private String statusText(CuckooGameState state, int player) {
        if (state.isInGame(player))
            return "Lives: " + "♥".repeat(state.getLives(player));
        return "Out in round " + (state.getRoundEliminated(player) + 1);
    }

    private void updateCentreText(CuckooGameState state) {
        if (!state.isNotTerminal()) {
            List<String> winners = new ArrayList<>();
            for (int p = 0; p < state.getNPlayers(); p++) {
                if (state.getPlayerResults()[p] == WIN_GAME)
                    winners.add("Player " + p);
            }
            centreLines[0].setText("Game over");
            centreLines[1].setText(winners.size() == 1 ? winners.get(0) + " wins"
                    : winners.isEmpty() ? "Nobody wins" : "Joint winners:");
            centreLines[2].setText(winners.size() > 1 ? String.join(", ", winners) : "");
            centreLines[3].setText("");
            return;
        }
        int current = state.getCurrentPlayer();
        centreLines[0].setText("Round " + (state.getRoundCounter() + 1));
        centreLines[1].setText("Dealer: Player " + state.getDealer());
        centreLines[2].setText("Player " + current + " to keep or swap");
        centreLines[3].setText(current == state.getDealer()
                ? "(a swap cuts the draw deck)"
                : "(a swap is with Player " + state.nextPlayerInGame(current) + ")");
    }

    /**
     * A card is face-up for a human player, for any card a human player knows (one they gave away, or a King shown
     * to refuse a swap), for the current player if the core parameters allow it, in full-observability mode, and
     * once the game is over, when every card has been shown.
     */
    private boolean showCard(CuckooGameState state, int playerId) {
        if (humanPlayerIds.contains(playerId) || !state.isNotTerminal()
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer))
            return true;
        for (int human : humanPlayerIds) {
            if (state.knowsCard(human, playerId))
                return true;
        }
        return false;
    }

    private JPanel createRulesPanel() {
        JPanel rules = new JPanel();
        rules.setBackground(new Color(43, 108, 25, 111));
        JLabel text = new JLabel("<html><center><h1>Cuckoo</h1></center><hr>" +
                "<p>An old card game of passing on a bad card. Each player starts with <b>three lives</b> (by default).</p><ul>" +
                "<li>Each round every player still in the game is dealt one card, which only they see. " +
                "Kings are high and Aces low; suits do not matter.</li>" +
                "<li>Starting on the dealer's left and going clockwise, each player either <b>keeps</b> their card " +
                "or <b>swaps</b> it with the next player on their left. That player must accept, unless they " +
                "hold a King: they show it, and the swap is refused.</li>" +
                "<li>The dealer decides last. A swap by the dealer takes the top card of the draw deck instead - " +
                "unless it is a King, in which case the dealer keeps their own card.</li>" +
                "<li>Then all cards are shown, and whoever has the lowest card loses a life. Every player tied " +
                "for lowest loses one.</li>" +
                "<li>A player with no lives left is out. The deal passes to the next player on the left who is " +
                "still in.</li>" +
                "<li>The last player left wins. If everyone left loses their last life in the same round, they " +
                "are joint winners.</li>" +
                "</ul><hr><p><b>INTERFACE:</b> choose Keep card or Swap card from the buttons at the bottom of the " +
                "screen. Play goes clockwise from player 0 at the bottom. A player's seat shows their lives; a " +
                "card you know - your own, one you gave away, or a King shown to refuse a swap - is face up. " +
                "The centre shows the draw deck, the dealer and whose turn it is.</p></html>");
        text.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(width * 2 / 3 + 60, height * 2 / 3));
        rules.add(scroll);
        return rules;
    }
}
