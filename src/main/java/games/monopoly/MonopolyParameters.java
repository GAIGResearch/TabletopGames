package games.monopoly;

import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * The defaults are the classic rules (Hasbro rulebook 00009) as played on the UK board, except that the Bank never
 * runs out of houses or hotels. The board (its squares, prices, rents, taxes, cards and GO salary) comes from
 * boardFile.
 */
public class MonopolyParameters extends TunableParameters<MonopolyParameters> {

    // the buildings level of a street with a hotel (one more than the most houses), in MonopolyGameState.buildings
    public static final int HOTEL = 5;

    // the board, loaded from boardFile; derived from that parameter, so not part of equals
    private MonopolyBoard board;

    public String boardFile = "data/monopoly/ukBoard.json";
    public int startingCash = 1500;
    public int jailFine = 50;
    // the failed rolls for doubles allowed in Jail; the fine is paid after the last
    public int maxJailRolls = 3;
    // this many doubles in a row in one turn sends the player to Jail
    public int maxDoubles = 3;
    public int mortgageInterestPercent = 10;
    // the share of its cost the Bank pays for a building sold back to it
    public int buildingSalePercent = 50;
    // auctions: the lowest opening bid, and the amounts a bid may be raised by (a comma-separated list)
    public int minimumBid = 10;
    public int[] bidIncrements = {10, 50, 100};
    // 0 for a flat Income Tax; otherwise the player may pay this percentage of their total worth instead
    public int incomeTaxPercent = 0;

    public MonopolyParameters() {
        addTunableParameter("boardFile", "data/monopoly/ukBoard.json");
        addTunableParameter("startingCash", 1500, Arrays.asList(1000, 1500, 2000, 2500));
        addTunableParameter("jailFine", 50, Arrays.asList(0, 25, 50, 100));
        addTunableParameter("maxJailRolls", 3, Arrays.asList(1, 2, 3));
        addTunableParameter("maxDoubles", 3, Arrays.asList(2, 3, 4));
        addTunableParameter("mortgageInterestPercent", 10, Arrays.asList(0, 10, 20));
        addTunableParameter("buildingSalePercent", 50, Arrays.asList(50, 100));
        addTunableParameter("minimumBid", 10, Arrays.asList(1, 10, 50));
        addTunableParameter("bidIncrements", "10,50,100");
        addTunableParameter("incomeTaxPercent", 0, Arrays.asList(0, 10));
        addTunableParameter("maxRounds", 100, Arrays.asList(50, 100, 200));
        _reset();
    }

    @Override
    public void _reset() {
        boardFile = (String) getParameterValue("boardFile");
        if (board == null || !board.fileName.equals(boardFile))
            board = new MonopolyBoard(boardFile);
        startingCash = (int) getParameterValue("startingCash");
        jailFine = (int) getParameterValue("jailFine");
        maxJailRolls = (int) getParameterValue("maxJailRolls");
        maxDoubles = (int) getParameterValue("maxDoubles");
        mortgageInterestPercent = (int) getParameterValue("mortgageInterestPercent");
        buildingSalePercent = (int) getParameterValue("buildingSalePercent");
        minimumBid = (int) getParameterValue("minimumBid");
        bidIncrements = Arrays.stream(((String) getParameterValue("bidIncrements")).split(","))
                .mapToInt(v -> Integer.parseInt(v.trim())).toArray();
        incomeTaxPercent = (int) getParameterValue("incomeTaxPercent");
        // the framework's round limit, which StandardForwardModel.endRound applies
        setMaxRounds((int) getParameterValue("maxRounds"));
    }

    public MonopolyBoard getBoard() {
        return board;
    }

    /**
     * The interest on the property's mortgage.
     */
    public int mortgageInterest(MonopolySquare square) {
        // rounded up to the pound
        return (square.mortgage() * mortgageInterestPercent + 99) / 100;
    }

    public int unmortgageCost(MonopolySquare square) {
        return square.mortgage() + mortgageInterest(square);
    }

    /**
     * What the Bank pays for one building (a house, or a hotel) on the street sold back to it.
     */
    public int buildingSaleValue(MonopolySquare square) {
        // rounded down
        return square.houseCost() * buildingSalePercent / 100;
    }

    @Override
    protected MonopolyParameters _copy() {
        return new MonopolyParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof MonopolyParameters;
    }

    @Override
    public MonopolyParameters instantiate() {
        return this;
    }
}
