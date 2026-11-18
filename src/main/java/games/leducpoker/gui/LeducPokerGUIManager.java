package games.leducpoker.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import games.leducpoker.LeducPokerGameState;
import games.leducpoker.LeducPokerParameters;
import games.tricktaking.gui.CardArt;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * GUI for Leduc Poker: player 1 at the top, the draw deck, board card and pot in the middle, player 0 at the
 * bottom.
 */
public class LeducPokerGUIManager extends AbstractGUIManager {

    LeducPokerTableView tableView;
    LeducPokerPlayerView[] playerViews;
    String[] agentNames;

    public LeducPokerGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;

        int nPlayers = gameState.getNPlayers();
        tableView = new LeducPokerTableView();
        playerViews = new LeducPokerPlayerView[nPlayers];
        agentNames = new String[nPlayers];
        for (int i = 0; i < nPlayers; i++) {
            playerViews[i] = new LeducPokerPlayerView();
            agentNames[i] = game.getPlayers().get(i).toString();
        }

        int gap = 12;
        this.width = Math.max(LeducPokerTableView.width, LeducPokerPlayerView.width) + 40;
        this.height = LeducPokerTableView.height + 2 * LeducPokerPlayerView.height + 2 * gap + 40;

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
        RulesView.addTabs(tabs, GameType.LeducPoker, gameState.getGameParameters(), height);

        // player 1 above the table, player 0 below it
        JPanel table = new JPanel(new GridBagLayout());
        table.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(gap / 2, 0, gap / 2, 0);
        c.gridy = 0;
        table.add(playerViews[1], c);
        c.gridy = 1;
        table.add(tableView, c);
        c.gridy = 2;
        table.add(playerViews[0], c);

        JPanel infoPanel = createGameStateInfoPanel("Leduc Poker", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], width, defaultActionPanelHeight);

        main.add(infoPanel, BorderLayout.NORTH);
        main.add(table, BorderLayout.CENTER);
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
        // at most Fold, Call and Raise
        return 3;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof LeducPokerGameState state)) return;
        LeducPokerParameters params = (LeducPokerParameters) state.getGameParameters();
        int currentPlayer = state.getCurrentPlayer();

        for (int i = 0; i < playerViews.length; i++) {
            Deck<FrenchCard> hand = state.getHand(i);
            String role = state.isNotTerminal() && i == state.getFirstPlayer() ? "   (acts first)" : "";
            playerViews[i].update(hand.getSize() > 0 ? hand.get(0) : null, showCard(state, i),
                    state.isNotTerminal() && i == currentPlayer,
                    "Player " + i + " [" + agentNames[i] + "]" + role,
                    "In the pot this hand: " + state.getContribution(i),
                    "Net chips: " + signed(state.getNetChips(i)));
        }

        Deck<FrenchCard> board = state.getBoard();
        String[] lines;
        if (state.isNotTerminal()) {
            int toCall = state.amountToCall(currentPlayer);
            lines = new String[]{
                    "Hand " + (state.getRoundCounter() + 1) + " of " + params.nHands,
                    "Betting round " + (state.getBettingRound() + 1) + " of 2"
                            + " (raises of " + params.raiseAmount(state.getBettingRound()) + ")",
                    "Pot: " + state.getPot(),
                    "Raises this round: " + state.getRaisesThisRound() + " of " + params.maxRaisesPerRound,
                    "Player " + currentPlayer + (toCall == 0 ? " may check" : " to call " + toCall)
            };
        } else {
            lines = new String[]{
                    "Game over",
                    resultText(state),
                    "Last pot: " + state.getPot()
            };
        }
        tableView.update(state.getDrawDeck().getSize(), board.getSize() > 0 ? board.get(0) : null, lines);
        parent.repaint();
    }

    private static String signed(int chips) {
        return chips > 0 ? "+" + chips : String.valueOf(chips);
    }

    private static String resultText(LeducPokerGameState state) {
        Set<Integer> winners = state.getWinners();
        if (winners.isEmpty())
            return "Draw, net chips " + signed(state.getNetChips(0)) + " each";
        int winner = winners.iterator().next();
        return "Player " + winner + " wins " + signed(state.getNetChips(winner));
    }

    /**
     * True if the player's private card is shown face-up.
     */
    private boolean showCard(LeducPokerGameState state, int playerId) {
        // both cards are revealed once the game is over
        return humanPlayerIds.contains(playerId)
                || !state.isNotTerminal()
                || state.getCoreGameParameters().alwaysDisplayFullObservable
                || (playerId == state.getCurrentPlayer() && state.getCoreGameParameters().alwaysDisplayCurrentPlayer);
    }
}
