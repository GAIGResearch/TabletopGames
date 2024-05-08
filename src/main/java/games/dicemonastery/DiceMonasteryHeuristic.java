package games.dicemonastery;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.dicemonastery.components.Monk;

import java.util.Arrays;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.PILGRIMAGE;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.SPRING;
import static games.dicemonastery.DiceMonasteryConstants.Season.SUMMER;

public class DiceMonasteryHeuristic extends TunableParameters implements IStateHeuristic {

    double[] MONKS = {-0.1, -0.1, 0.0, 0.0}; // one value for each of the four years of a game
    double[] PIETY = {0.0, 0.0, 0.0, 0.0}; // one value per year
    double[] SCORE = {0.5, 0.5, 0.5, 0.5}; // one value per year
    double[] VP = {0.5, 0.5, 0.5, 0.5}; // one value per year
    double[] FOOD_SUFFICIENCY = {0.1, 0.1, 1.0}; // unlike the others, this applies on a seasonal basis
    double[] INK_TYPES = {0.0, 0.0, 0.0, 0.0};
    double[] TREASURES = {0.0, 0.0, 0.0, 0.0};
    double[] CORE_WRITING = {0.0, 0.0, 0.0, 0.0};
    double[] PILGRIMS = {0.0, 0.0, 0.0, 0.0};
    double[] SHILLINGS = {0.0, 0.0, 0.0, 0.0};
    boolean scoreOnly = true;

    public DiceMonasteryHeuristic() {
        for (int i = 1; i <= 4; i++) {
            String digit = String.valueOf(i);
            addTunableParameter("MONKS_" + digit, MONKS[i - 1]);
            addTunableParameter("PIETY_" + digit, PIETY[i - 1]);
            addTunableParameter("SCORE_" + digit, SCORE[i - 1]);
            addTunableParameter("VP_" + digit, VP[i - 1]);
            if (i < 4)
                addTunableParameter("FOOD_" + digit, FOOD_SUFFICIENCY[i - 1]);
            addTunableParameter("INK_" + digit, INK_TYPES[i - 1]);
            addTunableParameter("TREASURE_" + digit, TREASURES[i - 1]);
            addTunableParameter("WRITE_" + digit, CORE_WRITING[i - 1]);
            addTunableParameter("PILGRIM_" + digit, PILGRIMS[i - 1]);
            addTunableParameter("SHILLINGS_" + digit, SHILLINGS[i - 1]);
        }
    }

    @Override
    public void _reset() {
        for (int i = 1; i <= 4; i++) {
            String digit = String.valueOf(i);
            MONKS[i - 1] = (double) getParameterValue("MONKS_" + digit);
            PIETY[i - 1] = (double) getParameterValue("PIETY_" + digit);
            SCORE[i - 1] = (double) getParameterValue("SCORE_" + digit);
            VP[i - 1] = (double) getParameterValue("VP_" + digit);
            if (i < 4)
                FOOD_SUFFICIENCY[i - 1] = (double) getParameterValue("FOOD_" + digit);
            INK_TYPES[i - 1] = (double) getParameterValue("INK_" + digit);
            TREASURES[i - 1] = (double) getParameterValue("TREASURE_" + digit);
            CORE_WRITING[i - 1] = (double) getParameterValue("WRITE_" + digit);
            PILGRIMS[i - 1] = (double) getParameterValue("PILGRIM_" + digit);
            SHILLINGS[i - 1] = (double) getParameterValue("SHILLINGS_" + digit);
        }
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        CoreConstants.GameResult playerResult = state.getPlayerResults()[playerId];

        if (!gs.isNotTerminal()) {
            if (scoreOnly) {
                return gs.getGameScore(playerId) / 50.0;
            } else {
                return gs.getPlayerResults()[playerId].value;
            }
        }
        int year = state.getYear() - 1;
        int season = 2; // AUTUMN or WINTER
        if (state.getSeason() == SPRING)
            season = 0;
        if (state.getSeason() == SUMMER)
            season = 1;

        double totalCoeff = Math.abs(MONKS[year]) + Math.abs(PIETY[year]) + Math.abs(SCORE[year])
                + Math.abs(VP[year]) + Math.abs(FOOD_SUFFICIENCY[season]) + Math.abs(INK_TYPES[year]) +
                Math.abs(CORE_WRITING[year]) + Math.abs(TREASURES[year]) + Math.abs(PILGRIMS[year]) +
                Math.abs(SHILLINGS[year]);
        if (totalCoeff == 0.0) return 0.0;

        double monks = 0.0;
        if (MONKS[year] != 0.0) monks = Math.min(1.0, state.monksIn(null, playerId).size() / 10.0);
        double piety = 0.0;
        if (PIETY[year] != 0.0)
            piety = Math.min(1.0, state.monksIn(null, playerId).stream().mapToInt(Monk::getPiety).sum() / 50.0);
        double score = 0.0;
        if (SCORE[year] != 0.0) score = Math.min(1.0, state.getGameScore(playerId) / 80.0);
        double vp = 0.0;
        if (VP[year] != 0.0) vp = Math.min(1.0, state.getVictoryPoints(playerId) / 40.0);
        double ink = 0.0;
        if (INK_TYPES[year] != 0.0) {
            int differentInks = state.getStores(playerId, r -> r.isInk).size();
            ink = differentInks / 7.0;
        }
        double writing = 0.0;
        if (CORE_WRITING[year] != 0.0) {
            int vellum = state.getResource(playerId, VELLUM, STOREROOM);
            int candles = state.getResource(playerId, CANDLE, STOREROOM);
            writing = Math.min(Math.max(vellum, candles) / 5.0, 1.0);
        }
        double treasure = 0.0;
        if (TREASURES[year] != 0.0) {
            treasure = Math.min(state.getTreasures(playerId).stream().mapToInt(t -> t.vp).sum() / 36.0, 1.0);
        }
        double shillings = 0.0;
        if (SHILLINGS[year] != 0.0) {
            shillings = Math.min(state.getResource(playerId, DiceMonasteryConstants.Resource.SHILLINGS, STOREROOM) / 20.0, 1.0);
        }
        double pilgrims = 0.0;
        if (PILGRIMS[year] != 0.0) {
            pilgrims = Math.min(state.monksIn(PILGRIMAGE, playerId).size() / 4.0, 1.0);
        }
        double food = 0.0;
        if (FOOD_SUFFICIENCY[season] != 0.0) {
            food = state.getResource(playerId, BREAD, STOREROOM) +
                    state.getResource(playerId, HONEY, STOREROOM);
            int numberMonks = state.monksIn(null, playerId).size();
            food = numberMonks == 0 ? 1.0 : Math.min(1.0, food / numberMonks);
        }

        return (monks * MONKS[year] + piety * PIETY[year] + score * SCORE[year] + vp * VP[year]
                + food * FOOD_SUFFICIENCY[season] + ink * INK_TYPES[year] + writing * CORE_WRITING[year]
                + treasure * TREASURES[year] + pilgrims * PILGRIMS[year] + shillings * SHILLINGS[year])
                / totalCoeff;
    }

    @Override
    protected DiceMonasteryHeuristic _copy() {
        DiceMonasteryHeuristic retValue = new DiceMonasteryHeuristic();
        retValue.MONKS = Arrays.copyOf(MONKS, MONKS.length);
        retValue.PIETY = Arrays.copyOf(PIETY, PIETY.length);
        retValue.SCORE = Arrays.copyOf(SCORE, SCORE.length);
        retValue.VP = Arrays.copyOf(VP, VP.length);
        retValue.INK_TYPES = Arrays.copyOf(INK_TYPES, INK_TYPES.length);
        retValue.CORE_WRITING = Arrays.copyOf(CORE_WRITING, CORE_WRITING.length);
        retValue.TREASURES = Arrays.copyOf(TREASURES, TREASURES.length);
        retValue.PILGRIMS = Arrays.copyOf(PILGRIMS, PILGRIMS.length);
        retValue.FOOD_SUFFICIENCY = Arrays.copyOf(FOOD_SUFFICIENCY, FOOD_SUFFICIENCY.length);
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DiceMonasteryHeuristic) {
            DiceMonasteryHeuristic other = (DiceMonasteryHeuristic) o;
            return Arrays.equals(other.MONKS, MONKS) && Arrays.equals(other.PIETY, PIETY) &&
                    Arrays.equals(other.SCORE, SCORE) && Arrays.equals(other.VP, VP) &&
                    Arrays.equals(other.INK_TYPES, INK_TYPES) && Arrays.equals(other.CORE_WRITING, CORE_WRITING) &&
                    Arrays.equals(other.TREASURES, TREASURES) && Arrays.equals(other.PILGRIMS, PILGRIMS) &&
                    Arrays.equals(other.FOOD_SUFFICIENCY, FOOD_SUFFICIENCY);
        }
        return false;
    }

    @Override
    public DiceMonasteryHeuristic instantiate() {
        return _copy();
    }


}
