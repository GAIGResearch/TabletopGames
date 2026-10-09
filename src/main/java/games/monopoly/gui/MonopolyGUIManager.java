package games.monopoly.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.monopoly.MonopolyBoard;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolyParameters;
import games.monopoly.SquareType;
import gui.AbstractGUIManager;
import gui.GamePanel;
import gui.IScreenHighlight;
import gui.views.RulesView;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GUI for Monopoly: the board, and beside it a table of the players with the action list below it.
 */
public class MonopolyGUIManager extends AbstractGUIManager {

    MonopolyBoardView boardView;
    MonopolyPlayersView playersView;

    public MonopolyGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;
        AbstractGameState gameState = game.getGameState();
        if (gameState == null) return;
        MonopolyGameState state = (MonopolyGameState) gameState;
        MonopolyParameters params = (MonopolyParameters) state.getGameParameters();

        String[] agentNames = new String[state.getNPlayers()];
        for (int i = 0; i < agentNames.length; i++) {
            agentNames[i] = game.getPlayers().get(i).toString();
        }
        boardView = new MonopolyBoardView();
        playersView = new MonopolyPlayersView(agentNames);

        // the action list goes in the column beside the board, under the players, to keep the window short enough
        // for a laptop screen
        int sideWidth = MonopolyPlayersView.WIDTH;
        this.width = MonopolyBoardView.SIZE + sideWidth + 20;
        this.height = MonopolyBoardView.SIZE;
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
        main.add(createGameStateInfoPanel("Monopoly", gameState, width, defaultInfoPanelHeight), BorderLayout.NORTH);
        main.add(gameArea, BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Game", main);
        tabs.add("Rules", new RulesView(rulesHtml(params), height + defaultInfoPanelHeight - 10));

        parent.setLayout(new BorderLayout());
        parent.add(tabs, BorderLayout.CENTER);
        parent.setPreferredSize(new Dimension(width, height + defaultInfoPanelHeight + 45));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();
    }

    @Override
    public int getMaxActionSpace() {
        // MANAGE offers at most two actions for each of the 22 streets (build and sell, or build and mortgage), one
        // for each of the 6 stations and utilities, and the roll or the end of the turn: 51. Raising money offers
        // fewer, an auction at most the bid increments and a pass, and a roll in Jail at most 4
        return 55;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (!(gameState instanceof MonopolyGameState state)) return;
        boardView.update(state);
        playersView.update(state);
        parent.repaint();
    }

    private static String rulesHtml(MonopolyParameters params) {
        MonopolyBoard board = params.getBoard();
        String c = board.currency();
        String increments = Arrays.stream(params.bidIncrements).mapToObj(i -> c + i)
                .collect(Collectors.joining(", ")).replaceFirst(", ([^,]*)$", " or $1");
        String stationRents = Arrays.stream(board.squares(SquareType.STATION).get(0).rents()).mapToObj(r -> c + r)
                .collect(Collectors.joining(", "));
        int[] utility = board.squares(SquareType.UTILITY).get(0).rents();
        String incomeTax = params.incomeTaxPercent == 0
                ? "<li><b>Income Tax and Super Tax.</b> Pay the amount on the square to the Bank.</li>"
                : "<li><b>Income Tax.</b> Choose to pay the amount on the square, or " + params.incomeTaxPercent +
                "% of your total worth (rounded down).</li>" +
                "<li><b>Super Tax.</b> Pay the amount on the square to the Bank.</li>";
        return "<h2>Monopoly</h2>" +
                "<p>The classic rules on the UK (London) board, without trading between players. The last player " +
                "not bankrupt wins.</p>" +
                "<p><a href='#turn'>Turn</a> &middot; <a href='#squares'>Squares</a> &middot; " +
                "<a href='#jail'>Jail</a> &middot; <a href='#building'>Building</a> &middot; " +
                "<a href='#mortgages'>Mortgages</a> &middot; <a href='#auctions'>Auctions</a> &middot; " +
                "<a href='#debts'>Debts and bankruptcy</a> &middot; <a href='#end'>End of the game</a> &middot; " +
                "<a href='#interface'>Interface</a></p>" +
                "<p><b>Each player</b> starts on GO with " + c + params.startingCash + ". The Bank has unlimited " +
                "money, houses and hotels.</p>" +

                "<h3><a name='turn'>Turn</a></h3><ol>" +
                "<li>Roll the two dice and move your token clockwise by their total.</li>" +
                "<li>Do what the square you stop on says.</li>" +
                "<li>Then build, sell buildings, mortgage and unmortgage as you choose.</li>" +
                "<li>After a double, roll again. Your " + MonopolyPlayersView.ordinal(params.maxDoubles) +
                " double in a row in one " +
                "turn sends you to Jail instead of moving.</li></ol>" +
                "<p>Each time you pass or stop on GO you collect " + c + board.goSalary() + ".</p>" +

                "<h3><a name='squares'>Squares</a></h3><ul>" +
                "<li><b>Unowned property.</b> Buy it at its price, or decline it and it is auctioned.</li>" +
                "<li><b>Another player's property.</b> Pay them rent, unless it is mortgaged.</li>" +
                "<li><b>Street rent</b> is on the title deed. It is doubled on a street with no buildings when its " +
                "owner holds every street of the colour group.</li>" +
                "<li><b>Station rent</b> is " + stationRents + " for 1 to 4 stations owned.</li>" +
                "<li><b>Utility rent</b> is " + utility[0] + " times the dice with one utility owned, and " +
                utility[1] + " times with both.</li>" +
                "<li><b>Chance and Community Chest.</b> Take the top card and do what it says. It then goes to the " +
                "bottom of its pile. A Get Out of Jail Free card is kept until it is used.</li>" +
                incomeTax +
                "<li><b>Go To Jail.</b> Go straight to Jail without passing GO.</li></ul>" +

                "<h3><a name='jail'>Jail</a></h3>" +
                "<p>In Jail you still collect rent. Before rolling you may pay the " + c + params.jailFine + " fine " +
                "or use a Get Out of Jail Free card, then roll and move as usual. Otherwise you roll: a double " +
                "frees you and you move by it, with no further roll. After " + params.maxJailRolls + " failed " +
                "rolls you pay the fine and move by the last roll.</p>" +

                "<h3><a name='building'>Building</a></h3>" +
                "<p>When you own every street of a colour group and none of them is mortgaged, you may build " +
                "houses on them at the cost on the title deed. Build evenly: a street may not have more than one " +
                "building more than another street of the group. A hotel replaces " + (MonopolyParameters.HOTEL - 1) +
                " houses. Buildings sell back to the Bank for " + params.buildingSalePercent + "% of their cost, " +
                "evenly too. Selling a hotel leaves " + (MonopolyParameters.HOTEL - 1) + " houses.</p>" +

                "<h3><a name='mortgages'>Mortgages</a></h3>" +
                "<p>Mortgage a property for its mortgage value when no street of its group has buildings. To " +
                "unmortgage it, pay the mortgage value plus " + params.mortgageInterestPercent + "% interest " +
                "(rounded up). A mortgaged property charges no rent.</p>" +

                "<h3><a name='auctions'>Auctions</a></h3>" +
                "<p>Players bid in turn, starting with the player after the one who declined. The opening bid is " +
                c + params.minimumBid + ". Each bid after it raises the high bid by " + increments + ". A player " +
                "who passes is out of the auction. The last player in pays their bid for the property. If nobody " +
                "bids, the property stays with the Bank.</p>" +

                "<h3><a name='debts'>Debts and bankruptcy</a></h3>" +
                "<p>A player who owes more than their cash must raise money by selling buildings and mortgaging " +
                "properties, and pays once they have enough. A player who could not raise enough is bankrupt, and " +
                "their buildings are sold to the Bank.</p><ul>" +
                "<li><b>Bankrupt to a player.</b> That player takes all their cash, properties and Get Out of Jail " +
                "Free cards. They pay " + params.mortgageInterestPercent + "% interest at once on each mortgaged " +
                "property they receive.</li>" +
                "<li><b>Bankrupt to the Bank.</b> The properties go back to the Bank unmortgaged, and are " +
                "auctioned one by one.</li></ul>" +

                "<h3><a name='end'>End of the game</a></h3>" +
                "<p>The last player not bankrupt wins. The game also ends after " + params.getMaxRounds() +
                " rounds. The players not bankrupt are then ranked by total worth: cash, the price of each " +
                "unmortgaged property, the mortgage value of each mortgaged one, and the cost of the buildings.</p>" +

                "<h3><a name='interface'>Interface</a></h3><ul>" +
                "<li>Each token is a disc with the player's number. A token with bars across it is in Jail. On " +
                "the Jail square without bars, it is Just Visiting.</li>" +
                "<li>An owned property has a frame in its owner's colour. Green squares in the colour band are " +
                "houses, and a red bar is a hotel. A grey MORTGAGED cover marks a mortgaged property.</li>" +
                "<li>The middle of the board shows the last roll, the decision being made, and the card drawn " +
                "last from each pile.</li>" +
                "<li>The table beside the board shows each player's cash, total worth, properties and square. The " +
                "player to move is highlighted.</li></ul>";
    }
}
