package games.scopa.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.TarotCard;
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
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];

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
        tabs.add("Rules", new RulesView(rulesHtml((ScopaParameters) state.getGameParameters()), height));

        JPanel infoPanel = createGameStateInfoPanel("Scopa", gameState, width, defaultInfoPanelHeight);
        // a card may capture in several ways, so there can be more actions than fit on one row
        int actionPanelHeight = defaultActionPanelHeight * 2;
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, actionPanelHeight, true);

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

    private static String rulesHtml(ScopaParameters params) {
        String[] names = {"Ace", "2", "3", "4", "5", "6", "7", "Knave", "Cavalier", "King"};
        int[] numbers = {1, 2, 3, 4, 5, 6, 7, TarotCard.KNAVE, TarotCard.CAVALIER, TarotCard.KING};
        StringBuilder cards = new StringBuilder("<tr><th align=left>Card</th>");
        StringBuilder capture = new StringBuilder("<tr><th align=left>Capture value</th>");
        StringBuilder primiera = new StringBuilder("<tr><th align=left>Primiera value</th>");
        for (int i = 0; i < names.length; i++) {
            TarotCard card = new TarotCard(TarotCard.Suit.Coins, numbers[i]);
            cards.append("<td align=center>").append(names[i]).append("</td>");
            capture.append("<td align=center>").append(ScopaParameters.captureValue(card)).append("</td>");
            primiera.append("<td align=center>").append(ScopaParameters.primieraValue(card)).append("</td>");
        }
        int n = params.handSize;
        String winning = params.targetScore > 0
                ? "<p><b>Winning.</b> Each deal's points are added to the players' totals, and the deal passes to " +
                "the other player. The game ends after a deal when one player has " + params.targetScore +
                " or more points and more than the other. That player wins.</p>"
                : "<p><b>Winning.</b> The game is a single deal, and the higher score wins. Equal scores are a " +
                "draw.</p>";
        return "<h2>Scopa</h2>" +
                "<p>Two players capture cards from the table.</p>" +
                "<p><b>Cards.</b> The pack has 40 cards in four suits (Swords, Batons, Cups and Coins). Each suit " +
                "has Ace to 7, Knave, Cavalier and King.</p>" +
                "<table border=1 cellpadding=4 cellspacing=0>" + cards + "</tr>" + capture + "</tr>" + primiera +
                "</tr></table>" +
                "<p><b>The deal.</b> " + params.tableSize + " cards are dealt face up to the table and " + n +
                " to each player." +
                (params.redealOnKings ? " If three or more Kings are on the table, the cards are dealt again." : "") +
                " When both hands are empty, " + n + " more cards are dealt to each player (none to the table), " +
                "until the draw deck is empty. The player who did not deal plays first.</p>" +
                "<p><b>Each turn</b> you play one card from your hand.</p><ul>" +
                "<li>If a table card has the same rank, your card captures it (one of them, if there are " +
                "several).</li>" +
                "<li>Otherwise your card captures a set of two or more table cards whose capture values add up to " +
                "its own.</li>" +
                "<li>A card that can capture must capture. A card that cannot is added to the table.</li>" +
                "<li>A capture that clears the table is a scopa, unless it is made with the last card of the " +
                "deal.</li></ul>" +
                "<p>At the end of the deal the cards left on the table go to the last player to capture.</p>" +
                "<p><b>Scoring.</b> At the end of each deal a player scores 1 point for each of these:</p><ul>" +
                "<li>each scopa</li>" +
                "<li>more captured cards than the other player</li>" +
                "<li>more Coins than the other player</li>" +
                "<li>the 7 of Coins</li>" +
                "<li>a higher primiera than the other player</li></ul>" +
                "<p>The primiera is the total of the primiera values of your best card in each suit. It is 0 unless " +
                "you have captured a card of every suit. Equal counts score nothing.</p>" +
                winning +
                "<h3>Interface</h3>" +
                "<p>Player 1 sits at the top and player 0 at the bottom. Each player's hand is on the left, titled " +
                "with the player's number and agent, and with \"dealer\" for the dealer. The current player's " +
                "hand has a blue border. Beside it are the cards the player has captured, with a line showing " +
                "their number of cards, their Coins, whether they include the 7 of Coins, their primiera and the " +
                "player's scopas.</p>" +
                "<p>The table cards are in the middle. On a card, J is the Knave, C the Cavalier and K the King, " +
                "and the suits are Sw, Ba, Cu and Co. The line above the table shows " +
                (params.targetScore > 0 ? "the deal number and target, and " : "") +
                "the player to play. The line below it shows the number of cards in the draw deck, the last " +
                "player to capture and the scores. A score counts the deal so far" +
                (params.targetScore > 0 ? ", and the points banked from earlier deals are shown beside it" : "") +
                ".</p>" +
                "<p>Each action button plays a card, either to the table or capturing the cards it names.</p>";
    }
}
