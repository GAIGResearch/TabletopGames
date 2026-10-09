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
import gui.views.RulesView;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
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
        tabs.add("Rules", new RulesView(rulesHtml((GoofspielParameters) state.getGameParameters()), height));

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
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

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

    private static String rulesHtml(GoofspielParameters params) {
        List<FrenchCard> suit = params.suitCards(GoofspielParameters.PRIZE_SUIT);
        StringBuilder names = new StringBuilder("<tr><th align=left>Card</th>");
        StringBuilder values = new StringBuilder("<tr><th align=left>Value</th>");
        for (FrenchCard c : suit) {
            if (c.type == FrenchCard.FrenchCardType.Number) continue;
            names.append("<td align=center>").append(rankName(c)).append("</td>");
            values.append("<td align=center>").append(params.cardValue(c)).append("</td>");
        }
        boolean special = names.indexOf("<td") >= 0;
        String win = switch (params.tieRule) {
            case CARRY_OVER -> "The highest bid wins all the prizes on offer. If the highest bid is tied, nobody " +
                    "wins them. They stay on offer, and the next prize will be added to them.";
            case DISCARD -> "The highest bid wins all the prizes on offer. If the highest bid is tied, the prizes " +
                    "are won by nobody.";
            case HIGHEST_UNIQUE -> "Bids made by more than one player are set aside. The highest remaining bid " +
                    "wins all the prizes on offer. If no bid remains, the prizes are won by nobody.";
        };
        return "<h2>Goofspiel</h2>" +
                "<p>Win the most valuable prizes by bidding cards from your hand.</p>" +
                "<p><b>Cards.</b> The Diamonds are shuffled face down as the prize deck. Each player's hand is one " +
                "of the other suits (Clubs, Spades, Hearts, repeated from a second pack for more than three " +
                "players). Each suit has " + params.cardsPerSuit + " cards, from " + rankName(suit.get(0)) +
                " to " + rankName(suit.get(suit.size() - 1)) + ". A number card is worth its number." +
                (special ? " The other cards are worth:</p>" +
                        "<table border=1 cellpadding=4 cellspacing=0>" + names + "</tr>" + values + "</tr></table>"
                        : "</p>") +
                "<p><b>Each round</b> the top card of the prize deck is turned face up and added to the prizes on " +
                "offer.</p><ol>" +
                "<li>Every player bids one card from their hand, face down.</li>" +
                "<li>When all have bid, the bids are revealed. The bid cards are out of the game.</li>" +
                "<li>" + win + "</li></ol>" +
                "<p><b>End.</b> The game ends after " + params.cardsPerSuit + " rounds, when the hands are " +
                "empty." + (params.tieRule == GoofspielParameters.TieRule.CARRY_OVER
                ? " Prizes still on offer after a tie in the last round are won by nobody." : "") +
                " Your score is the total value of the prizes you have won. The highest score wins, and players " +
                "with the same highest score draw.</p>" +
                "<h3>Interface</h3>" +
                "<p>Choose your bid from the action buttons at the bottom. The top panel shows the Prize deck " +
                "(with the number of cards left in it) and the Prizes on offer. Beside them are the round, the " +
                "total value of the prizes on offer, the tie rule, and the prizes won by nobody.</p>" +
                "<p>Each player's area shows their Hand, their Bid for this round (face down to the other " +
                "players) and their Last bid, revealed at the end of the previous round. Below the cards are " +
                "their score and the prizes they have won (Won). \"(has bid)\" appears once they have bid this " +
                "round. The player to bid next has a blue border. Cards in text are written as rank and suit, " +
                "so Q&#9830; is the Queen of Diamonds.</p>";
    }

    private static String rankName(FrenchCard card) {
        return card.type == FrenchCard.FrenchCardType.Number ? String.valueOf(card.number) : card.type.name();
    }
}
