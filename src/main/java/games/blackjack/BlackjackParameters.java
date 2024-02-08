package games.blackjack;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.Arrays;
import java.util.Objects;

public class BlackjackParameters extends TunableParameters {
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

    public BlackjackParameters() {
        addTunableParameter("nCardsPerPlayer", 2, Arrays.asList(1,2,3,4,5));
        addTunableParameter("jackCard", 10, Arrays.asList(5, 10, 15, 20));
        addTunableParameter("queenCard", 10, Arrays.asList(5, 10, 15, 20));
        addTunableParameter("kingCard", 10, Arrays.asList(5, 10, 15, 20));
        addTunableParameter("aceCardBelowThreshold", 1, Arrays.asList(1, 2, 3, 4));
        addTunableParameter("aceCardAboveThreshold", 11, Arrays.asList(10, 13, 15, 17, 20));
        addTunableParameter("pointThreshold", 10, Arrays.asList(7, 10, 15));
        addTunableParameter("winScore", 21, Arrays.asList(15, 21, 30, 50));
        addTunableParameter("dealerStand", 17, Arrays.asList(5, 7, 10, 13, 15, 17, 20));
        addTunableParameter("nDealerCardsHidden", 1, Arrays.asList(0,1,2,3,4,5));
        _reset();
    }

    @Override
    public void _reset() {
        nCardsPerPlayer = (int) getParameterValue("nCardsPerPlayer");
        jackCard = (int) getParameterValue("jackCard");
        queenCard = (int) getParameterValue("queenCard");
        kingCard = (int) getParameterValue("kingCard");
        aceCardBelowThreshold = (int) getParameterValue("aceCardBelowThreshold");
        aceCardAboveThreshold = (int) getParameterValue("aceCardAboveThreshold");
        pointThreshold = (int) getParameterValue("pointThreshold");
        winScore = (int) getParameterValue("winScore");
        dealerStand = (int) getParameterValue("dealerStand");
        nDealerCardsHidden = (int) getParameterValue("nDealerCardsHidden");
    }

    public String getDataPath(){
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        return new BlackjackParameters();
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlackjackParameters)) return false;
        BlackjackParameters that = (BlackjackParameters) o;
        return nCardsPerPlayer == that.nCardsPerPlayer && jackCard == that.jackCard && queenCard == that.queenCard && kingCard == that.kingCard && aceCardBelowThreshold == that.aceCardBelowThreshold && aceCardAboveThreshold == that.aceCardAboveThreshold && pointThreshold == that.pointThreshold && winScore == that.winScore && dealerStand == that.dealerStand && nDealerCardsHidden == that.nDealerCardsHidden && Objects.equals(dataPath, that.dataPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataPath, nCardsPerPlayer, jackCard, queenCard, kingCard, aceCardBelowThreshold, aceCardAboveThreshold, pointThreshold, winScore, dealerStand, nDealerCardsHidden);
    }

    @Override
    public Game instantiate() {
        return new Game(GameType.Blackjack, new BlackjackForwardModel(), new BlackjackGameState(this, GameType.Blackjack.getMinPlayers()));
    }
}
