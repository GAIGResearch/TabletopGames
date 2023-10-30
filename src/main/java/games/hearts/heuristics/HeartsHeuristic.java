package games.hearts.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.hearts.HeartsGameState;

public class HeartsHeuristic extends TunableParameters implements IStateHeuristic {

    public double HIGH_VALUE_THRESHOLD = 10.0;
    public double MAX_HIGH_VALUE_CARD_PASS_BONUS = 0.3;
    public double maxPossibleScore = 100.0;
    public double maxHighValueCards = 10.0;
    public double maxPossibleTricks = 10.0;

    public HeartsHeuristic() {
        addTunableParameter("HIGH_VALUE_THRESHOLD", 10.0);
        addTunableParameter("MAX_HIGH_VALUE_CARD_PASS_BONUS", 0.3);
        addTunableParameter("maxPossibleScore", 100.0);
        addTunableParameter("maxHighValueCards", 10.0);
        addTunableParameter("maxPossibleTricks", 10.0);
    }

    @Override
    public void _reset() {
        HIGH_VALUE_THRESHOLD = (double) getParameterValue("HIGH_VALUE_THRESHOLD");
        MAX_HIGH_VALUE_CARD_PASS_BONUS = (double) getParameterValue("MAX_HIGH_VALUE_CARD_PASS_BONUS");
        maxPossibleScore = (double) getParameterValue("maxPossibleScore");
        maxHighValueCards = (double) getParameterValue("maxHighValueCards");
        maxPossibleTricks = (double) getParameterValue("maxPossibleTricks");
    }

    @Override
    protected HeartsHeuristic _copy() {
        HeartsHeuristic retValue = new HeartsHeuristic();
        retValue.HIGH_VALUE_THRESHOLD = HIGH_VALUE_THRESHOLD;
        retValue.MAX_HIGH_VALUE_CARD_PASS_BONUS = MAX_HIGH_VALUE_CARD_PASS_BONUS;
        retValue.maxPossibleScore = maxPossibleScore;
        retValue.maxHighValueCards = maxHighValueCards;
        retValue.maxPossibleTricks = maxPossibleTricks;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof HeartsHeuristic) {
            HeartsHeuristic other = (HeartsHeuristic) o;
            return other.HIGH_VALUE_THRESHOLD == HIGH_VALUE_THRESHOLD && other.MAX_HIGH_VALUE_CARD_PASS_BONUS == MAX_HIGH_VALUE_CARD_PASS_BONUS
                    && other.maxPossibleScore == maxPossibleScore && other.maxHighValueCards == maxHighValueCards && other.maxPossibleTricks == maxPossibleTricks;
        }
        return false;
    }

    @Override
    public HeartsHeuristic instantiate() {
        return _copy();
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        HeartsGameState tgs = (HeartsGameState) gs;

        // Penalize player's score
        double scoreFactor = (maxPossibleScore - tgs.getPlayerPoints(playerId) / maxPossibleScore);

        // Reward for having less high-value cards
        double highValueCardFactor = 0.0;
        int highValueCards = (int) tgs.getPlayerDecks().get(playerId).getComponents().stream()
                .filter(card -> (card).number > HIGH_VALUE_THRESHOLD)
                .count();
        if(highValueCards < maxHighValueCards) {
            highValueCardFactor = (maxHighValueCards - highValueCards) / maxHighValueCards * MAX_HIGH_VALUE_CARD_PASS_BONUS;
        }

        // Penalize for taking tricks
        double tricksTaken = tgs.playerTricksTaken[tgs.getCurrentPlayer()];
        double tricksFactor = (maxPossibleTricks - tricksTaken) / maxPossibleTricks;

        return scoreFactor + highValueCardFactor + tricksFactor;
    }

}