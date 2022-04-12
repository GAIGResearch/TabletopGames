package games.dotsboxes;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;
import utilities.Utils;

import java.util.Arrays;

public class DotsAndBoxesHeuristic extends TunableParameters implements IStateHeuristic, IStateFeatureVector<DBGameState> {

    double POINT_ADVANTAGE = 0.05;
    double POINTS_UNDER_LEADER = -0.10;
    double POINTS = 0.01;
    double THREE_BOXES_US = 0.0;
    double THREE_BOXES_THEM = 0.0;
    double TWO_BOXES_US = 0.0;
    double TWO_BOXES_THEM = 0.0;

    public DotsAndBoxesHeuristic() {
        addTunableParameter("POINTS", 0.01);
        addTunableParameter("POINT_ADVANTAGE", 0.05);
        addTunableParameter("POINTS_UNDER_LEADER", -0.10);
        addTunableParameter("THREE_BOXES_US", 0.0);
        addTunableParameter("THREE_BOXES_THEM", 0.0);
        addTunableParameter("TWO_BOXES_US", 0.0);
        addTunableParameter("TWO_BOXES_THEM", 0.0);
    }

    /**
     * Method that reloads all the locally stored values from currentValues
     * This is in case sub-classes decide to use the frankly more intuitive access via
     * params.paramName
     * instead of
     * params.getParameterValue("paramName")
     * (the latter is also more typo-prone if we hardcode strings everywhere)
     */
    @Override
    public void _reset() {
        POINTS = (double) getParameterValue("POINTS");
        POINT_ADVANTAGE = (double) getParameterValue("POINT_ADVANTAGE");
        POINTS_UNDER_LEADER = (double) getParameterValue("POINTS_UNDER_LEADER");
        THREE_BOXES_US = (double) getParameterValue("THREE_BOXES_US");
        TWO_BOXES_US = (double) getParameterValue("TWO_BOXES_US");
        THREE_BOXES_THEM = (double) getParameterValue("THREE_BOXES_THEM");
        TWO_BOXES_THEM = (double) getParameterValue("TWO_BOXES_THEM");
    }

    /**
     * Returns a score for the state that should be maximised by the player (the bigger, the better).
     * Ideally bounded between [-1, 1].
     *
     * @param gs       - game state to evaluate and score.
     * @param playerId
     * @return - value of given state.
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DBGameState state = (DBGameState) gs;
        Utils.GameResult playerResult = gs.getPlayerResults()[playerId];

        if (!state.isNotTerminal())
            return playerResult.value;

        int[] deltaToPlayer = new int[state.getNPlayers()];
        double retValue = 0.0;
        for (int p = 0; p < state.getNPlayers(); p++) {
            deltaToPlayer[p] = state.nCellsPerPlayer[playerId] - state.nCellsPerPlayer[p];
            retValue += deltaToPlayer[p] * POINT_ADVANTAGE / state.getNPlayers();
        }
        if (POINTS_UNDER_LEADER != 0.0) {
            int maxScore = Arrays.stream(deltaToPlayer).max().orElse(0);
            retValue += POINTS_UNDER_LEADER * (maxScore - state.nCellsPerPlayer[playerId]);
        }
        retValue += POINTS * state.nCellsPerPlayer[playerId];

        boolean collectData = (state.getCurrentPlayer() == playerId && (TWO_BOXES_US != 0.0 || THREE_BOXES_US != 0.0))
                ||
                (state.getCurrentPlayer() != playerId && (TWO_BOXES_THEM != 0.0 || THREE_BOXES_THEM != 0.0));

        if (collectData) {
            int[] cellCountByEdges = new int[5];
            for (DBCell cell : state.cells) {
                int edges = state.countCompleteEdges(cell);
                cellCountByEdges[edges]++;
            }
            retValue += (state.getCurrentPlayer() == playerId ? THREE_BOXES_US : THREE_BOXES_THEM) * cellCountByEdges[3];
            retValue += (state.getCurrentPlayer() == playerId ? TWO_BOXES_US : TWO_BOXES_THEM) * cellCountByEdges[2];
        }

        return retValue;
    }

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     *
     * @return - new game parameters object.
     */
    @Override
    protected DotsAndBoxesHeuristic _copy() {
        DotsAndBoxesHeuristic retValue = new DotsAndBoxesHeuristic();
        retValue.POINTS_UNDER_LEADER = POINTS_UNDER_LEADER;
        retValue.POINTS = POINTS;
        retValue.POINT_ADVANTAGE = POINT_ADVANTAGE;
        retValue.THREE_BOXES_US = THREE_BOXES_US;
        retValue.TWO_BOXES_US = TWO_BOXES_US;
        retValue.THREE_BOXES_THEM = THREE_BOXES_THEM;
        retValue.TWO_BOXES_THEM = TWO_BOXES_THEM;
        return retValue;
    }

    /**
     * Checks if the given object is the same as the current.
     *
     * @param o - other object to test equals for.
     * @return true if the two objects are equal, false otherwise
     */
    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DotsAndBoxesHeuristic) {
            DotsAndBoxesHeuristic other = (DotsAndBoxesHeuristic) o;
            return other.POINT_ADVANTAGE == POINT_ADVANTAGE &&
                    other.POINTS_UNDER_LEADER == POINTS_UNDER_LEADER &&
                    other.TWO_BOXES_US == TWO_BOXES_US && other.THREE_BOXES_US == THREE_BOXES_US &&
                    other.TWO_BOXES_THEM == TWO_BOXES_THEM && other.THREE_BOXES_THEM == THREE_BOXES_THEM &&
                    other.POINTS == POINTS;
        }
        return false;
    }

    /**
     * @return Returns Tuned Parameters corresponding to the current settings
     * (will use all defaults if setParameterValue has not been called at all)
     */
    @Override
    public Object instantiate() {
        return this._copy();
    }

    @Override
    public double[] featureVector(DBGameState state, int playerID) {
        return new double[0];
    }
}
