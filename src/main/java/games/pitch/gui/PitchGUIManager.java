package games.pitch.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.pitch.PitchGameState;
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
 * Player 0 sits at the bottom, then clockwise: player 1 on the left, 2 at the top, 3 on the right. The trick and the
 * deal's public information are in the middle.
 */
public class PitchGUIManager extends AbstractGUIManager {

    static final int CARD_WIDTH = 60, CARD_HEIGHT = 80;
    // six cards side by side, just overlapping
    static final int HAND_WIDTH = 6 * CARD_WIDTH + 20, SEAT_WIDTH = HAND_WIDTH + 20, SEAT_HEIGHT = CARD_HEIGHT + 35;
    static final int TABLE_WIDTH = 340, TABLE_HEIGHT = 300;

    private PitchDeckView[] hands;
    private TitledBorder[] seatTitles;
    private JPanel[] seats;
    private PitchTableView table;
    private final Border activeBorder = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public PitchGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null || game.getGameState() == null) return;
        PitchGameState state = (PitchGameState) game.getGameState();
        int nPlayers = state.getNPlayers();

        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);
        parent.setBackground(ImageIO.GetInstance().getImage(PitchDeckView.DATA_PATH + "table-background.jpg"));

        hands = new PitchDeckView[nPlayers];
        seatTitles = new TitledBorder[nPlayers];
        seats = new JPanel[nPlayers];
        JPanel tableArea = new JPanel(new BorderLayout());
        tableArea.setOpaque(false);
        String[] places = {BorderLayout.SOUTH, BorderLayout.WEST, BorderLayout.NORTH, BorderLayout.EAST};
        for (int p = 0; p < nPlayers; p++) {
            hands[p] = new PitchDeckView(p, state.getPlayerHand(p), false, HAND_WIDTH, CARD_HEIGHT);
            seatTitles[p] = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                    "Player " + p, TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            seatTitles[p].setTitleColor(Color.WHITE);
            seats[p] = new JPanel(new GridBagLayout());
            seats[p].setOpaque(false);
            seats[p].add(hands[p]);
            seats[p].setBorder(seatTitles[p]);
            Dimension seatSize = new Dimension(SEAT_WIDTH, SEAT_HEIGHT);
            seats[p].setPreferredSize(seatSize);
            JPanel holder = new JPanel(new GridBagLayout());
            holder.setOpaque(false);
            holder.add(seats[p]);
            tableArea.add(holder, places[p]);
        }
        table = new PitchTableView(TABLE_WIDTH, TABLE_HEIGHT);
        JPanel middle = new JPanel(new GridBagLayout());
        middle.setOpaque(false);
        middle.add(table);
        tableArea.add(middle, BorderLayout.CENTER);

        width = 2 * SEAT_WIDTH + TABLE_WIDTH + 20;
        int tableHeight = 2 * SEAT_HEIGHT + Math.max(TABLE_HEIGHT, SEAT_HEIGHT) + 20;

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(createGameStateInfoPanel("Pitch", state, width, defaultInfoPanelHeight), BorderLayout.NORTH);
        main.add(tableArea, BorderLayout.CENTER);
        main.add(createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight), BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        tabs.add("Main", main);
        RulesView.addTabs(tabs, GameType.Pitch, state.getGameParameters(), tableHeight);

        height = tableHeight + defaultInfoPanelHeight + defaultActionPanelHeight + 60;
        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height));
        parent.revalidate();
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // bidding offers at most Pass and the bids 2 to 5 (5 actions); play at most the 6 cards in hand
        return 6;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (hands == null) return;
        PitchGameState state = (PitchGameState) gameState;
        int current = state.getCurrentPlayer();
        boolean fullyObservable = state.getCoreGameParameters().alwaysDisplayFullObservable;
        boolean showCurrent = state.getCoreGameParameters().alwaysDisplayCurrentPlayer;
        for (int p = 0; p < state.getNPlayers(); p++) {
            hands[p].updateComponent(state.getPlayerHand(p));
            hands[p].setFront(fullyObservable || humanPlayerIds.contains(p) || (showCurrent && p == current));
            seatTitles[p].setTitle(seatTitle(state, p));
            boolean active = state.isNotTerminal() && p == current;
            seats[p].setBorder(active ? BorderFactory.createCompoundBorder(activeBorder, seatTitles[p]) : seatTitles[p]);
        }
        table.update(state);
        parent.repaint();
    }

    private String seatTitle(PitchGameState state, int p) {
        String agent = game.getPlayers() == null ? "" : " [" + game.getPlayers().get(p).toString() + "]";
        String role = p == state.getDealer() ? ", dealer" : "";
        int bid = state.getPlayerBid(p);
        String bidText = bid < 0 ? "" : bid == 0 ? " - passed" : " - bid " + bid;
        return "Player " + p + agent + " (team " + state.getTeam(p) + role + ")" + bidText;
    }
}
