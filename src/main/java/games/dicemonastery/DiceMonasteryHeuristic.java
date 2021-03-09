package games.dicemonastery;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;
import utilities.Utils;

import java.util.Arrays;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;

public class DiceMonasteryHeuristic extends TunableParameters implements IStateHeuristic {

    double[] MONKS = {0.1, 0.05, 0.0}; // one value for each of the three years of a game
    double[] PIETY = {0.3, 0.2, 0.1}; // one value per year
    double[] SCORE = {0.0, 0.2, 0.5}; // one value per year
    double[] VP = {0.1, 0.1, 0.0}; // one value per year
    double[] FOOD_SUFFICIENCY = {0.1, 0.2, 0.5}; // unlike the others, this applies on a seasonal basis

    public DiceMonasteryHeuristic() {
        for (int i = 1; i <= 3; i++) {
            String digit = String.valueOf(i);
            addTunableParameter("MONKS_" + digit, MONKS[i - 1]);
            addTunableParameter("PIETY_" + digit, PIETY[i - 1]);
            addTunableParameter("SCORE_" + digit, SCORE[i - 1]);
            addTunableParameter("VP_" + digit, VP[i - 1]);
            addTunableParameter("FOOD_" + digit, FOOD_SUFFICIENCY[i - 1]);
        }
    }

    @Override
    public void _reset() {
        for (int i = 1; i <= 3; i++) {
            String digit = String.valueOf(i);
            MONKS[i - 1] = (double) getParameterValue("MONKS_" + digit);
            PIETY[i - 1] = (double) getParameterValue("PIETY_" + digit);
            SCORE[i - 1] = (double) getParameterValue("SCORE_" + digit);
            VP[i - 1] = (double) getParameterValue("VP_" + digit);
            FOOD_SUFFICIENCY[i - 1] = (double) getParameterValue("FOOD_" + digit);
        }
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) state.getTurnOrder();
        Utils.GameResult playerResult = state.getPlayerResults()[playerId];

        if (playerResult == Utils.GameResult.LOSE)
            return -1;
        if (playerResult == Utils.GameResult.WIN)
            return 1;

        int year = turnOrder.getYear() - 1;
        int season = 2; // AUTUMN or WINTER
        if (turnOrder.getSeason() == SPRING)
            season = 0;
        if (turnOrder.getSeason() == SUMMER)
            season = 1;

        double totalCoeff = Math.abs(MONKS[year]) + Math.abs(PIETY[year]) + +Math.abs(SCORE[year])
                + Math.abs(VP[year]) + Math.abs(FOOD_SUFFICIENCY[season]);
        if (totalCoeff == 0.0) return 0.0;

        double monks = Math.min(1.0, state.monksIn(null, playerId).size() / 10.0);
        double piety = Math.min(1.0, state.monksIn(null, playerId).stream().mapToInt(Monk::getPiety).sum() / 50.0);
        double score = Math.min(1.0, state.getGameScore(playerId) / 80.0);
        double vp = Math.min(1.0, state.getVictoryPoints(playerId) / 40.0);
        double food = state.getResource(playerId, BREAD, STOREROOM) + state.getResource(playerId, BERRIES, STOREROOM) +
                state.getResource(playerId, HONEY, STOREROOM);
        food = monks == 0.0 ? 1.0 : Math.min(1.0, food / monks);

        return (monks * MONKS[year] + piety * PIETY[year] + score * SCORE[year] + vp * VP[year]
                + food * FOOD_SUFFICIENCY[season]) / totalCoeff;
    }

    @Override
    protected DiceMonasteryHeuristic _copy() {
        DiceMonasteryHeuristic retValue = new DiceMonasteryHeuristic();
        retValue.MONKS = Arrays.copyOf(MONKS, MONKS.length);
        retValue.PIETY = Arrays.copyOf(PIETY, PIETY.length);
        retValue.SCORE = Arrays.copyOf(SCORE, SCORE.length);
        retValue.VP = Arrays.copyOf(VP, VP.length);
        retValue.FOOD_SUFFICIENCY = Arrays.copyOf(FOOD_SUFFICIENCY, FOOD_SUFFICIENCY.length);
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DiceMonasteryHeuristic) {
            DiceMonasteryHeuristic other = (DiceMonasteryHeuristic) o;
            return Arrays.equals(other.MONKS, MONKS) && Arrays.equals(other.PIETY, PIETY) &&
                    Arrays.equals(other.SCORE, SCORE) && Arrays.equals(other.VP, VP) &&
                    Arrays.equals(other.FOOD_SUFFICIENCY, FOOD_SUFFICIENCY);
        }
        return false;
    }

    @Override
    public DiceMonasteryHeuristic instantiate() {
        return _copy();
    }


}
