package games.goofspiel.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.FrenchCard;
import games.goofspiel.GoofspielGameState;
import games.goofspiel.GoofspielParameters;
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
 * <p>GUI for Goofspiel: the prize deck and the prizes on offer at the top, and one area per player below.</p>
 *
 * <p>This is a view only - it holds no rules and never changes the state. It is repainted for every player,
 * human or not, so a hand and a face-down bid are only shown face-up when the viewer is entitled to see them
 * (see showCards).</p>
 */
public class GoofspielGUIManager extends AbstractGUIManager {

    GoofspielTableView tableView;
    GoofspielPlayerView[] playerViews;
    // each player view sits in a cell carrying its titled border, so the layout makes room for the title
    JPanel[] playerCells;
    Border[] playerViewBorders;
    TitledBorder[] playerTitles;
    String[] agentNames;

    final Border highlightActive = BorderFactory.createLineBorder(new Color(47, 132, 220), 3);

    public GoofspielGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        GoofspielGameState state = (GoofspielGameState) gameState;
        int nPlayers = state.getNPlayers();

        // one column for 2 players, two for 3-4, three for 5-7
        int columns = nPlayers <= 2 ? 1 : nPlayers <= 4 ? 2 : 3;
        int rows = (nPlayers + columns - 1) / columns;
        // a player area is the view plus its titled border (about 20 pixels, mostly the title below)
        int playerAreaWidth = GoofspielPlayerView.width + 16;
        int playerAreaHeight = GoofspielPlayerView.height + 26;
        tableView = new GoofspielTableView();
        this.width = Math.max(tableView.getPreferredSize().width, columns * playerAreaWidth) + 40;
        this.height = tableView.getPreferredSize().height + rows * playerAreaHeight + 40;

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

        JPanel tableWrapper = new JPanel(new GridBagLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.add(tableView);

        JPanel players = new JPanel(new GridLayout(rows, columns, 8, 4));
        players.setOpaque(false);
        playerViews = new GoofspielPlayerView[nPlayers];
        playerCells = new JPanel[nPlayers];
        playerViewBorders = new Border[nPlayers];
        playerTitles = new TitledBorder[nPlayers];
        agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            GoofspielPlayerView view = new GoofspielPlayerView(state.getHand(i), i);
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
            TitledBorder title = BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Player " + i,
                    TitledBorder.CENTER, TitledBorder.BELOW_BOTTOM);
            title.setTitleColor(Color.white);   // the table background is dark
            playerTitles[i] = title;
            playerViewBorders[i] = title;
            playerViews[i] = view;
            JPanel cell = new JPanel(new BorderLayout());
            cell.setOpaque(false);
            cell.setBorder(title);
            cell.add(view, BorderLayout.CENTER);
            playerCells[i] = cell;
            // centre the bordered cell in its grid slot
            JPanel slot = new JPanel(new GridBagLayout());
            slot.setOpaque(false);
            slot.add(cell);
            players.add(slot);
        }

        JPanel mainGameArea = new JPanel(new BorderLayout());
        mainGameArea.setOpaque(false);
        mainGameArea.add(tableWrapper, BorderLayout.NORTH);
        mainGameArea.add(players, BorderLayout.CENTER);

        JPanel infoPanel = createGameStateInfoPanel("Goofspiel", gameState, width, defaultInfoPanelHeight);
        // up to 13 bids at once: a vertical, scrolling list
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

    @Override
    public int getMaxActionSpace() {
        // one Bid per card in hand, and GoofspielParameters.cardsPerSuit is at most 13
        return 13;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof GoofspielGameState state)) return;
        GoofspielParameters params = (GoofspielParameters) state.getGameParameters();
        int currentPlayer = state.getCurrentPlayer();

        for (int i = 0; i < playerViews.length; i++) {
            Deck<FrenchCard> bid = state.getBid(i);
            Deck<FrenchCard> played = state.getPlayedBids(i);
            Deck<FrenchCard> won = state.getWonPrizes(i);
            String status = "Score " + (int) state.getGameScore(i) + "   Won: " + cardList(won)
                    + (state.isNotTerminal() && bid.getSize() > 0 ? "   (has bid)" : "");
            playerViews[i].update(state.getHand(i), showCards(state, i),
                    bid.getSize() > 0 ? bid.get(0) : null,
                    played.getSize() > 0 ? played.get(0) : null, status);
            playerTitles[i].setTitle("Player " + i + " [" + agentNames[i] + "]");
            playerCells[i].setBorder(i == currentPlayer && state.isNotTerminal()
                    ? BorderFactory.createCompoundBorder(highlightActive, playerViewBorders[i])
                    : playerViewBorders[i]);
        }

        int offerValue = 0;
        for (FrenchCard c : state.getPrizesOnOffer())
            offerValue += params.cardValue(c);
        String[] lines;
        if (state.isNotTerminal()) {
            int round = state.getRoundCounter() + 1;
            lines = new String[]{
                    "Round " + round + " of " + params.cardsPerSuit,
                    "On offer: " + state.getPrizesOnOffer().getSize() + " prize"
                            + (state.getPrizesOnOffer().getSize() == 1 ? "" : "s") + ", worth " + offerValue,
                    "Ties: " + tieRuleText(params.tieRule),
                    "Won by nobody: " + cardList(state.getDiscardedPrizes())
            };
        } else {
            lines = new String[]{
                    "Game over",
                    resultText(state),
                    "Won by nobody: " + cardList(state.getDiscardedPrizes())
            };
        }
        tableView.update(state.getPrizeDeck(), state.getPrizesOnOffer(), lines);
        parent.repaint();
    }

    /**
     * Who won, or who drew, and with what score.
     */
    private static String resultText(GoofspielGameState state) {
        Set<Integer> winners = state.getWinners();
        Set<Integer> drawn = state.getTied();
        Set<Integer> top = winners.isEmpty() ? drawn : winners;
        if (top.isEmpty()) return "";
        int score = (int) state.getGameScore(top.iterator().next());
        StringBuilder names = new StringBuilder();
        for (int p : top)
            names.append(names.isEmpty() ? "" : " and ").append("Player ").append(p);
        return names + (winners.isEmpty() ? " draw" : " wins") + " with " + score;
    }

    private static String cardList(Deck<FrenchCard> deck) {
        if (deck.getSize() == 0) return "-";
        StringBuilder sb = new StringBuilder();
        for (FrenchCard c : deck)
            sb.append(CardArt.shortName(c)).append(' ');
        return sb.toString().trim();
    }

    private static String tieRuleText(GoofspielParameters.TieRule tieRule) {
        return switch (tieRule) {
            case CARRY_OVER -> "prizes carry over";
            case DISCARD -> "prizes are discarded";
            case HIGHEST_UNIQUE -> "highest unique bid wins";
        };
    }

    /**
     * A hand and a face-down bid are face-up only for a human player, for the current player if the core
     * parameters allow it, or in full-observability mode.
     */
    private boolean showCards(GoofspielGameState state, int playerId) {
        return humanPlayerIds.contains(playerId)
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }

    private JPanel createRulesPanel() {
        JPanel rules = new JPanel();
        rules.setBackground(new Color(43, 108, 25, 111));
        JLabel text = new JLabel("<html><center><h1>Goofspiel</h1></center><hr>" +
                "<p>A game of simultaneous bidding: <b>win the most valuable prizes</b>.</p><ul>" +
                "<li>Each player holds one whole suit, Ace to King. The Diamonds are shuffled face down as the prize deck.</li>" +
                "<li>Each round the top prize is turned face up. Every player then chooses a card from their hand " +
                "and bids it face down, all at the same time.</li>" +
                "<li>When everyone has bid, the bids are revealed. The single highest bid wins the prizes on offer. " +
                "Ace is low (1), Jack 11, Queen 12, King 13.</li>" +
                "<li>If the highest bid is tied, nobody wins: the prizes stay on offer and the next prize is added " +
                "to them, so the next round is played for all of them.</li>" +
                "<li>The bid cards are used up. After 13 rounds the hands are empty and the game ends; prizes still " +
                "on offer after a tie in the last round are won by nobody.</li>" +
                "<li>Your score is the total value of the prizes you won. The highest score wins.</li>" +
                "</ul><p>Variants (game parameters): ties may instead discard the prizes, or be won by the highest " +
                "bid nobody else made; the Ace may rank high (14); fewer cards per suit give a shorter game.</p>" +
                "<hr><p><b>INTERFACE:</b> choose your bid from the action buttons at the bottom of the screen. " +
                "The top shows the prize deck, the prizes on offer and the round. Each player's area shows their " +
                "hand, the card they have bid this round (face down), and the bid they revealed last round.</p></html>");
        text.setVerticalAlignment(SwingConstants.TOP);
        JScrollPane scroll = new JScrollPane(text);
        scroll.setPreferredSize(new Dimension(width * 2 / 3 + 60, height * 2 / 3 + 100));
        rules.add(scroll);
        return rules;
    }
}
