package games.scopa.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.TarotCard;
import games.GameType;
import games.scopa.ScopaGameState;
import games.scopa.ScopaParameters;
import games.scopa.ScopaUtils;
import games.tricktaking.gui.CardArt;
import games.tricktaking.gui.CardHandView;
import games.tricktaking.gui.TarotCardFace;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Player 1 across the top, player 0 at the bottom, each with their hand and their captured cards, and the table
 * cards in the middle.
 */
public class ScopaGUIManager extends AbstractGUIManager {

    // by suit (Swords, Batons, Cups, Coins), then Ace up to King
    static final Comparator<TarotCard> HAND_DISPLAY_ORDER = Comparator
            .comparingInt((TarotCard c) -> ScopaParameters.SUITS.indexOf(c.suit))
            .thenComparingInt(c -> c.number);
    static final TarotCardFace FACE = new TarotCardFace(HAND_DISPLAY_ORDER);

    // a hand of three cards side by side; a captured pile of up to 40 overlapping cards
    static final int handWidth = 3 * (TarotCardFace.cardWidth + 6) + TarotCardFace.cardWidth / 2;
    static final int pileWidth = 560;

    List<CardHandView<TarotCard, TarotCard.Suit>> handViews = new ArrayList<>();
    List<CardHandView<TarotCard, TarotCard.Suit>> pileViews = new ArrayList<>();
    Border[] handBorders;
    TitledBorder[] handTitles;
    String[] agentNames;
    ScopaTableView tableView;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public ScopaGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        ScopaGameState state = (ScopaGameState) gameState;
        int nPlayers = state.getNPlayers();
        tableView = new ScopaTableView(FACE);

        parent.setBackground(ImageIO.GetInstance().getImage(CardArt.dataPath + "table-background.jpg"));
        // without these the tabbed pane's content area paints over the parent's background image
        UIManager.put("TabbedPane.contentOpaque", false);
        UIManager.put("TabbedPane.opaque", false);
        UIManager.put("TabbedPane.tabsOpaque", false);

        handBorders = new Border[nPlayers];
        handTitles = new TitledBorder[nPlayers];
        agentNames = new String[nPlayers];
        JPanel[] playerRows = new JPanel[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            agentNames[i] = game.getPlayers().get(i).toString();

            CardHandView<TarotCard, TarotCard.Suit> hand = new CardHandView<>(FACE, state.getPlayerHand(i), i, handWidth);
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i,
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            title.setTitleColor(Color.white);   // the table background is dark
            handTitles[i] = title;
            handBorders[i] = title;
            hand.setBorder(title);
            handViews.add(hand);

            // the captured pile is face up: every card in it was seen when it was captured
            CardHandView<TarotCard, TarotCard.Suit> pile = new CardHandView<>(FACE, state.getCapturedCards(i), i, pileWidth);
            TitledBorder pileTitle = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Captured by player " + i,
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            pileTitle.setTitleColor(Color.white);
            pile.setBorder(pileTitle);
            pileViews.add(pile);

            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 4));
            row.setOpaque(false);
            row.add(hand);
            row.add(pile);
            playerRows[i] = row;
        }

        int rowHeight = handViews.get(0).getPreferredSize().height + 20;
        this.width = Math.max(handWidth + pileWidth + 80, tableView.getPreferredSize().width + 40);
        this.height = 2 * rowHeight + tableView.getPreferredSize().height + 30;

        JPanel mainGameArea = new JPanel(new BorderLayout());
        mainGameArea.setOpaque(false);
        mainGameArea.add(playerRows[1], BorderLayout.NORTH);
        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        centre.add(tableView);
        mainGameArea.add(centre, BorderLayout.CENTER);
        mainGameArea.add(playerRows[0], BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        tabs.add("Game", main);
        RulesView.addTabs(tabs, GameType.Scopa, state.getGameParameters(), height);

        JPanel infoPanel = createGameStateInfoPanel("Scopa", gameState, width, defaultInfoPanelHeight);
        // a card may capture in several ways, so there can be more actions than fit on one row
        int actionPanelHeight = defaultActionPanelHeight * 2;
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, actionPanelHeight);

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

    @Override
    public int getMaxActionSpace() {
        // three cards in hand, each with one action per capture. A large table of low cards has many sets adding up
        // to a card's value, so there is no small hard bound; 3000 random games never offered more than 15 actions
        return 60;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof ScopaGameState state)) return;
        int currentPlayer = state.getCurrentPlayer();
        int dealer = state.getDealer();
        for (int i = 0; i < handViews.size(); i++) {
            handViews.get(i).update(state.getPlayerHand(i), showHand(state, i), Set.of(), "");
            handTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]" + (i == dealer ? " - dealer" : ""));
            handViews.get(i).setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, handBorders[i])
                    : handBorders[i]);
            List<TarotCard> pile = state.getCapturedCards(i).getComponents();
            int scopas = state.getScopas(i);
            String extra = ScopaUtils.coins(pile) + " Coins"
                    + (pile.contains(ScopaParameters.SETTEBELLO) ? ", the 7 of Coins" : "")
                    + ", primiera " + ScopaUtils.primiera(pile)
                    + ", " + scopas + (scopas == 1 ? " scopa" : " scopas");
            pileViews.get(i).update(state.getCapturedCards(i), true, Set.of(), extra);
        }
        tableView.update(state.getTable(), headerText(state), footerText(state));
        parent.repaint();
    }

    private String headerText(ScopaGameState state) {
        ScopaParameters params = (ScopaParameters) state.getGameParameters();
        String deal = params.targetScore > 0 ? "Deal " + (state.getRoundCounter() + 1) + " (to " + params.targetScore + ")   " : "";
        if (!state.isNotTerminal())
            return "Game over   -   " + resultText(state);
        return deal + "Player " + state.getCurrentPlayer() + " to play";
    }

    private String footerText(ScopaGameState state) {
        StringBuilder text = new StringBuilder("Draw deck: " + state.getDrawDeck().getSize() + " cards");
        if (state.getLastCapturer() >= 0)
            text.append("     Last capture: player ").append(state.getLastCapturer());
        text.append("     Scores");
        ScopaParameters params = (ScopaParameters) state.getGameParameters();
        for (int i = 0; i < state.getNPlayers(); i++) {
            text.append(" - player ").append(i).append(": ").append((int) state.getGameScore(i));
            if (params.targetScore > 0 && state.isNotTerminal())
                text.append(" (").append(state.getBankedScore(i)).append(" banked)");
        }
        return text.toString();
    }

    private String resultText(ScopaGameState state) {
        int s0 = (int) state.getGameScore(0), s1 = (int) state.getGameScore(1);
        if (s0 == s1)
            return "a draw at " + s0 + " points each";
        return "won by player " + (s0 > s1 ? 0 : 1) + ", " + Math.max(s0, s1) + " to " + Math.min(s0, s1);
    }

    private boolean showHand(ScopaGameState state, int playerId) {
        // face-up for a human player, for the current player if the core parameters allow it, or in
        // full-observability mode
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }
}
