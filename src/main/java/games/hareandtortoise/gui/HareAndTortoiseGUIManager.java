package games.hareandtortoise.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.hareandtortoise.HareAndTortoiseGameState;
import games.hareandtortoise.HareAndTortoiseParameters;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * GUI for Hare and Tortoise: the board with the runners on it, and beside it a table of the players and the hare
 * cards.
 */
public class HareAndTortoiseGUIManager extends AbstractGUIManager {

    HareAndTortoiseBoardView boardView;
    HareAndTortoisePlayersView playersView;

    public HareAndTortoiseGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;
        HareAndTortoiseGameState state = (HareAndTortoiseGameState) gameState;
        HareAndTortoiseParameters params = (HareAndTortoiseParameters) state.getGameParameters();

        String[] agentNames = new String[state.getNPlayers()];
        for (int i = 0; i < agentNames.length; i++) {
            String[] split = game.getPlayers().get(i).getClass().toString().split("\\.");
            agentNames[i] = split[split.length - 1];
        }
        boardView = new HareAndTortoiseBoardView();
        playersView = new HareAndTortoisePlayersView(agentNames);

        // the action list goes in the column beside the board, under the players, to keep the window short enough
        // for a laptop screen
        Dimension boardSize = boardView.getPreferredSize();
        int sideWidth = HareAndTortoisePlayersView.WIDTH;
        this.width = boardSize.width + sideWidth + 20;
        this.height = boardSize.height;
        int actionHeight = height - playersView.getPreferredSize().height - 8;

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setOpaque(false);
        side.add(playersView);
        side.add(Box.createVerticalStrut(8));
        JComponent actionPanel = createActionPanel(new IScreenHighlight[0], sideWidth, actionHeight);
        actionPanel.setPreferredSize(new Dimension(sideWidth, actionHeight));
        actionPanel.setMaximumSize(new Dimension(sideWidth, actionHeight));
        side.add(actionPanel);

        JPanel gameArea = new JPanel(new BorderLayout(10, 0));
        gameArea.setOpaque(false);
        gameArea.add(boardView, BorderLayout.CENTER);
        gameArea.add(side, BorderLayout.EAST);

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(createGameStateInfoPanel("Hare and Tortoise", gameState, width, defaultInfoPanelHeight),
                BorderLayout.NORTH);
        main.add(gameArea, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Game", main);
        tabs.add("Rules", new RulesView(rulesHtml(params), height + defaultInfoPanelHeight - 10));

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + defaultInfoPanelHeight + 30));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // a turn offers at most a forward move to each of the 63 squares, HOME, the move back to a tortoise square
        // and the two ways of chewing a carrot: 67 actions
        return 70;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof HareAndTortoiseGameState state)) return;
        boardView.update(state);
        playersView.update(state);
        parent.repaint();
    }

    private static String rulesHtml(HareAndTortoiseParameters params) {
        StringBuilder costs = new StringBuilder("<tr><th align=left>Squares</th>");
        StringBuilder carrots = new StringBuilder("<tr><th align=left>Carrots</th>");
        for (int d = 1; d <= 10; d++) {
            costs.append("<td align=center>").append(d).append("</td>");
            carrots.append("<td align=center>").append(params.moveCost(d)).append("</td>");
        }
        int chew = params.carrotsPerChew, pos = params.carrotsPerRacePosition;
        return "<h2>Hare and Tortoise</h2>" +
                "<p>The rules of the 1978 Ravensburger edition. Be the first to get your runner from START to " +
                "HOME. The others play on for second place, third place and so on, until one runner is left.</p>" +
                "<p><b>Each player</b> starts with " + params.startCarrots + " carrots and " +
                params.startLettuces + " lettuces. Every payment is made openly.</p>" +
                "<h3>Moving</h3>" +
                "<p>On your turn you move forwards any number of squares and pay carrots for the distance:</p>" +
                "<table border=1 cellpadding=3 cellspacing=0>" + costs + "</tr>" + carrots + "</tr></table>" +
                "<p>Moving n squares costs 1 + 2 + ... + n carrots.</p><ul>" +
                "<li>You may not end a move on an occupied square.</li>" +
                "<li>You may not move forwards onto a tortoise square.</li>" +
                "<li>You may not move onto a lettuce square once your lettuces are gone.</li></ul>" +
                "<p><b>Position in the race</b> is 1st for the runner furthest ahead. A runner HOME keeps the " +
                "place it finished in, ahead of every runner still racing.</p>" +
                "<h3>Squares</h3><ul>" +
                "<li><b>Tortoise.</b> Instead of moving forwards, you may move back to the nearest tortoise square " +
                "behind you, if it is free. You pay nothing and draw " + params.carrotsPerTortoiseStep +
                " carrots for each square moved back.</li>" +
                "<li><b>Carrot.</b> When your turn starts here, you may stay and draw " + chew + " carrots, or " +
                "pay " + chew + " carrots if you hold at least " + chew + ".</li>" +
                "<li><b>Lettuce.</b> Your next turn is spent chewing a lettuce: you discard one and draw " + pos +
                " carrots for each place in the race (" + pos + " in 1st, " + 2 * pos + " in 2nd, and so on). " +
                "On the turn after that you must move on.</li>" +
                "<li><b>Number (2, 3, 4) and flag (1, 5, 6).</b> When your turn starts here and your position in " +
                "the race matches the number, you draw " + pos + " carrots for each place.</li>" +
                "<li><b>Hare.</b> After paying to move here you draw a hare card and do what it says. The card " +
                "then goes to the bottom of the pile.</li></ul>" +
                "<h3>Hare cards</h3>" +
                "<table border=1 cellpadding=3 cellspacing=0>" +
                "<tr><th align=left>Card</th><th align=left>Effect</th></tr>" +
                "<tr><td>Fall back one position (" + params.nFallBackOnePosition + ")</td><td>Move back, free, to " +
                "the first free square behind the runner behind you.</td></tr>" +
                "<tr><td>Your last turn costs nothing (" + params.nLastTurnFree + ")</td><td>Take back the " +
                "carrots you paid for the move.</td></tr>" +
                "<tr><td>Either draw or discard " + chew + " carrots (" + params.nDrawOrDiscard + ")</td><td>Choose " +
                "which.</td></tr>" +
                "<tr><td>Leap ahead by one position (" + params.nLeapAheadOnePosition + ")</td><td>Move, free, " +
                "to the first free square beyond the runner ahead of you (not a tortoise square).</td></tr>" +
                "<tr><td>Leap ahead to the next carrot square (" + params.nNextCarrotSquare + ")</td><td>Free, " +
                "to the next free carrot square.</td></tr>" +
                "<tr><td>Fall back to the previous carrot square (" + params.nPreviousCarrotSquare + ")</td>" +
                "<td>Free, to the previous free carrot square.</td></tr>" +
                "<tr><td>Have another turn (" + params.nAnotherTurn + ")</td><td>Take another turn at once." +
                "</td></tr>" +
                "<tr><td>Miss a turn (" + params.nMissATurn + ")</td><td>Your next turn is skipped.</td></tr>" +
                "<tr><td>Chew a lettuce (" + params.nChewALettuce + ")</td><td>If you hold a lettuce, discard " +
                "one and draw " + pos + " carrots for each place.</td></tr></table>" +
                "<p>A card that has no square to send you to has no effect.</p>" +
                "<h3>Getting HOME</h3>" +
                "<p>You may move HOME only with no lettuces left, and if after paying you hold no more than " +
                params.homeCarrotsPerRacePosition + " carrots for each place you will finish in (" +
                params.homeCarrotsPerRacePosition + " for 1st, " + 2 * params.homeCarrotsPerRacePosition +
                " for 2nd, and so on).</p>" +
                "<p><b>Stuck.</b> A player who has no legal action when their turn starts goes back to START with " +
                params.startCarrots + " carrots, keeps the lettuces they still hold, and moves off at once.</p>" +
                "<p>The game also ends after " + params.getMaxRounds() + " rounds; the runners are then ranked by " +
                "position in the race.</p>" +
                "<h3>Interface</h3><ul>" +
                "<li>Each runner is a numbered disc on the board. The runner whose turn it is has a white ring.</li>" +
                "<li>The table beside the board shows each runner's square, place, carrots and lettuces, and " +
                "whether it will chew a lettuce or miss its next turn.</li>" +
                "<li>Below it are the number of hare cards never drawn and the card drawn last.</li>" +
                "<li>Each action button names the square moved to and the carrots paid or drawn.</li></ul>";
    }
}
