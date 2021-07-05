package games.blackjack;

import core.AbstractParameters;
import java.util.Arrays;
import java.util.Objects;

public class BlackjackParameters extends AbstractParameters {
    public String dataPath = "data/FrenchCards/";

    public int nCardsPerPlayer = 2;
    public int jackCard = 10;
    public int queenCard = 10;
    public int kingCard = 10;
    public int aceCardBelowThreshold = 1;
    public int aceCardAboveThreshold = 11;
    public int pointThreshold = 10;
    public int winScore = 21;
    public int dealerStand = 17;
    public int nDealerCardsHidden = 1;

    public BlackjackParameters(long seed) {
        super(seed);
    }

    public String getDataPath(){
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        BlackjackParameters bjgp = new BlackjackParameters(System.currentTimeMillis());
        bjgp.dataPath = dataPath;
        bjgp.nCardsPerPlayer = nCardsPerPlayer;
        bjgp.jackCard = jackCard;
        bjgp.queenCard = queenCard;
        bjgp.kingCard = kingCard;
        bjgp.aceCardBelowThreshold = aceCardBelowThreshold;
        bjgp.aceCardAboveThreshold = aceCardAboveThreshold;
        bjgp.pointThreshold = pointThreshold;
        bjgp.winScore = winScore;
        bjgp.dealerStand = dealerStand;
        bjgp.nDealerCardsHidden = nDealerCardsHidden;
        return bjgp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlackjackParameters)) return false;
        if (!super.equals(o)) return false;
        BlackjackParameters that = (BlackjackParameters) o;
        return nCardsPerPlayer == that.nCardsPerPlayer && jackCard == that.jackCard && queenCard == that.queenCard && kingCard == that.kingCard && aceCardBelowThreshold == that.aceCardBelowThreshold && aceCardAboveThreshold == that.aceCardAboveThreshold && pointThreshold == that.pointThreshold && winScore == that.winScore && dealerStand == that.dealerStand && nDealerCardsHidden == that.nDealerCardsHidden && Objects.equals(dataPath, that.dataPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataPath, nCardsPerPlayer, jackCard, queenCard, kingCard, aceCardBelowThreshold, aceCardAboveThreshold, pointThreshold, winScore, dealerStand, nDealerCardsHidden);
    }
}
