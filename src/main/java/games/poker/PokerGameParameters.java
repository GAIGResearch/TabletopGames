package games.poker;

import core.AbstractParameters;
//import games.uno.UnoGameParameters;

import java.util.Arrays;
import java.util.Objects;

public class PokerGameParameters extends AbstractParameters {
    public String dataPath = "data/FrenchCards/";

    public int nStartingMoney = 50;
    public int nWinMoney = 80;
    public int nFlopCards = 3;
    public int nTurnCards = 1;
    public int nRiverCards = 1;
    public int nCardsPerPlayer = 2;
    public int smallBlind = 5;
    public int bigBlind = 10;
    public int[] raiseMultipliers = new int[]{2, 3, 4};
    public int bet = 5;
    public boolean endMinMoney = false;  // if true, ends when a player gets at least nWinMoney; if false, ends when all but 1 have 0 money

    public PokerGameParameters(long seed) {
        super(seed);
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        PokerGameParameters pgp = new PokerGameParameters(System.currentTimeMillis());
        pgp.dataPath = dataPath;
        pgp.nCardsPerPlayer = nCardsPerPlayer;

        pgp.nStartingMoney = nStartingMoney;
        pgp.nWinMoney = nWinMoney;
        pgp.nFlopCards = nFlopCards;
        pgp.nTurnCards = nTurnCards;
        pgp.nRiverCards = nRiverCards;
        pgp.smallBlind = smallBlind;
        pgp.bigBlind = bigBlind;
        pgp.raiseMultipliers = raiseMultipliers.clone();
        pgp.bet = bet;
        pgp.endMinMoney = endMinMoney;

        return pgp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PokerGameParameters)) return false;
        if (!super.equals(o)) return false;
        PokerGameParameters that = (PokerGameParameters) o;
        return nStartingMoney == that.nStartingMoney && nWinMoney == that.nWinMoney && nFlopCards == that.nFlopCards && nTurnCards == that.nTurnCards && nRiverCards == that.nRiverCards && nCardsPerPlayer == that.nCardsPerPlayer && smallBlind == that.smallBlind && bigBlind == that.bigBlind && bet == that.bet && endMinMoney == that.endMinMoney && Objects.equals(dataPath, that.dataPath) && Arrays.equals(raiseMultipliers, that.raiseMultipliers);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), dataPath, nStartingMoney, nWinMoney, nFlopCards, nTurnCards, nRiverCards, nCardsPerPlayer, smallBlind, bigBlind, bet, endMinMoney);
        result = 31 * result + Arrays.hashCode(raiseMultipliers);
        return result;
    }
}
