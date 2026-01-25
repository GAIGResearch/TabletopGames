package games.dotsboxes;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import utilities.Utils;


public class DotsAndBoxesHeuristic extends TunableParameters implements IStateHeuristic {

    double POINT_ADVANTAGE = 0.05;
    double POINTS = 0.01;
    double THREE_BOXES = 0.0;
    double TWO_BOXES = 0.0;
    double ORDINAL = 0.0;

    DBStateFeaturesReduced featureDefinition = new DBStateFeaturesReduced();
    String[] names = featureDefinition.names();

    public DotsAndBoxesHeuristic() {
        addTunableParameter(names[0], 0.01);
        addTunableParameter(names[1], 0.05);
        addTunableParameter(names[2], 0.0);
        addTunableParameter(names[3], 0.0);
        addTunableParameter(names[4], 0.0);
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
        POINTS = (double) getParameterValue(names[0]);
        POINT_ADVANTAGE = (double) getParameterValue(names[1]);
        TWO_BOXES = (double) getParameterValue(names[2]);
        THREE_BOXES = (double) getParameterValue(names[3]);
        ORDINAL = (double) getParameterValue(names[4]);
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
        CoreConstants.GameResult playerResult = gs.getPlayerResults()[playerId];

        if (!state.isNotTerminal())
            return playerResult.value;

        double[] featureVector = featureDefinition.doubleVector(state, playerId);

        double retValue = POINTS * featureVector[0]  +
                POINT_ADVANTAGE * featureVector[1]  +
                TWO_BOXES * featureVector[2] +
                THREE_BOXES * featureVector[3] +
                ORDINAL * featureVector[4];
        return Utils.clamp(retValue, -1.0, 1.0);
    }

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     *
     * @return - new game parameters object.
     */
    @Override
    protected DotsAndBoxesHeuristic _copy() {
        // all the parameters are then copied in ITunableParameters
        return new DotsAndBoxesHeuristic();
    }

    /**
     * Checks if the given object is the same as the current.
     *
     * @param o - other object to test equals for.
     * @return true if the two objects are equal, false otherwise
     */
    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DotsAndBoxesHeuristic other) {
            return other.POINT_ADVANTAGE == POINT_ADVANTAGE &&
                    other.ORDINAL == ORDINAL &&
                    other.TWO_BOXES == TWO_BOXES && other.THREE_BOXES == THREE_BOXES &&
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

}
