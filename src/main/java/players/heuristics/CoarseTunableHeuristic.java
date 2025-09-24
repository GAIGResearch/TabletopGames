package players.heuristics;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;


/**
 * A Heuristic with a few (coarse), qualitatively different options to be used in tuning.
 * The options are:
 * - WIN_ONLY: only returns 1 if the game is won, 0.5 for a draw; -1 for a loss and 0 if the game is ongoing
 * - WIN_PLUS: As WIN_ONLY, but if the game is ongoing, we return the current score divided by 1000 (we have no easy way
 *   of making this specific to the game).
 * - ORDINAL: returns the negative ordinal position of the player (based on the current score/position)
 * - SCORE_ONLY: the raw score
 * - SCORE_PLUS: the raw score, plus 50% if you won; -50% if you lost
 * - LEADER: the difference of your score to the next best player (+/-50% if you won/lost)
 */
public class CoarseTunableHeuristic extends TunableParameters implements IStateHeuristic {

    HeuristicType heuristicType;
    public enum HeuristicType {
        WIN_ONLY(new WinOnlyHeuristic()),
        WIN_PLUS (new WinPlusHeuristic(1000.0)),
        ORDINAL (new OrdinalPosition()),
        SCORE_ONLY (new PureScoreHeuristic()),
        SCORE_PLUS (new ScoreHeuristic()),
        LEADER (new LeaderHeuristic()),
        HEURISTIC (new GameDefaultHeuristic());

        HeuristicType(IStateHeuristic heuristic) {
            this.heuristic = heuristic;
        }

        final IStateHeuristic heuristic;
    }

    public CoarseTunableHeuristic() {
        addTunableParameter("heuristicType", HeuristicType.WIN_ONLY);
    }

    public HeuristicType getHeuristicType() {
        return heuristicType;
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        return heuristicType.heuristic.evaluateState(gs, playerId);
    }

    @Override
    public CoarseTunableHeuristic instantiate() {
        return this._copy();
    }

    @Override
    public void _reset() {
        heuristicType = (HeuristicType) getParameterValue("heuristicType");
    }

    @Override
    protected CoarseTunableHeuristic _copy() {
        return new CoarseTunableHeuristic();  // the value is then set in TunableParameters
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof CoarseTunableHeuristic &&
                ((CoarseTunableHeuristic) o).heuristicType == heuristicType;
    }

    @Override
    public String toString() {
        return "CoarseTunableHeuristic of type " + heuristicType.toString();
    }

}
