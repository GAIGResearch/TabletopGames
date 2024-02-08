package games.poker;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;
//import games.uno.UnoGameParameters;

import java.util.Arrays;
import java.util.Objects;

public class PokerGameParameters extends TunableParameters {
    public String dataPath = "data/FrenchCards/";

    // Game over condition parameters
    public int maxRounds = 10;
    public boolean endMinMoney = false;  // if true, ends when a player gets at least nWinMoney; if false, ends when all but 1 have 0 money
    public int nWinMoney = 80;

    // Other parameters
    public int nStartingMoney = 50;
    public int nFlopCards = 3;
    public int nTurnCards = 1;
    public int nRiverCards = 1;
    public int nCardsPerPlayer = 2;
    public int smallBlind = 5;
    public int bigBlind = 10;
    public double[] raiseMultipliers = new double[]{1., 2., 3., 4.};
    public int bet = 5;

    public PokerGameParameters() {
        addTunableParameter("maxRounds", 10, Arrays.asList(1, 5, 10, 15, 20));
        addTunableParameter("endMinMoney", false, Arrays.asList(false, true));
        addTunableParameter("nWinMoney", 80, Arrays.asList(50, 80, 100, 200, 500, 1000));
        addTunableParameter("nStartingMoney", 50, Arrays.asList(20, 50, 80, 100, 200, 500));
        addTunableParameter("nFlopCards", 3, Arrays.asList(1,2,3,4,5));
        addTunableParameter("nTurnCards", 1, Arrays.asList(1,2,3,4,5));
        addTunableParameter("nRiverCards", 1, Arrays.asList(1,2,3,4,5));
        addTunableParameter("nCardsPerPlayer", 2, Arrays.asList(1,2,3,4,5));
        addTunableParameter("smallBlind", 5, Arrays.asList(1, 5, 10, 15, 20));
        addTunableParameter("bigBlind", 10, Arrays.asList(2, 10, 20, 30, 40));
        addTunableParameter("bet", 5, Arrays.asList(1, 5, 10, 15, 20));
        _reset();
    }

    @Override
    public void _reset() {
        maxRounds = (int) getParameterValue("maxRounds");
        endMinMoney = (boolean) getParameterValue("endMinMoney");
        nWinMoney = (int) getParameterValue("nWinMoney");
        nStartingMoney = (int) getParameterValue("nStartingMoney");
        nFlopCards = (int) getParameterValue("nFlopCards");
        nTurnCards = (int) getParameterValue("nTurnCards");
        nRiverCards = (int) getParameterValue("nRiverCards");
        nCardsPerPlayer = (int) getParameterValue("nCardsPerPlayer");
        smallBlind = (int) getParameterValue("smallBlind");
        bigBlind = (int) getParameterValue("bigBlind");
        bet = (int) getParameterValue("bet");
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        PokerGameParameters pgp = new PokerGameParameters();
        pgp.raiseMultipliers = raiseMultipliers.clone();
        return pgp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PokerGameParameters)) return false;
        PokerGameParameters that = (PokerGameParameters) o;
        return nStartingMoney == that.nStartingMoney && nWinMoney == that.nWinMoney && nFlopCards == that.nFlopCards && nTurnCards == that.nTurnCards && nRiverCards == that.nRiverCards && nCardsPerPlayer == that.nCardsPerPlayer && smallBlind == that.smallBlind && bigBlind == that.bigBlind && bet == that.bet && endMinMoney == that.endMinMoney && Objects.equals(dataPath, that.dataPath) && Arrays.equals(raiseMultipliers, that.raiseMultipliers);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), dataPath, nStartingMoney, nWinMoney, nFlopCards, nTurnCards, nRiverCards, nCardsPerPlayer, smallBlind, bigBlind, bet, endMinMoney);
        result = 31 * result + Arrays.hashCode(raiseMultipliers);
        return result;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Poker, new PokerForwardModel(), new PokerGameState(this, GameType.Poker.getMinPlayers()));
    }
}
