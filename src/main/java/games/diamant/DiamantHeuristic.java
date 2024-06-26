package games.diamant;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.Counter;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;

import java.util.List;
import java.util.stream.Collectors;

public class DiamantHeuristic extends TunableParameters implements IStateHeuristic {

    double FACTOR_SCORE = 1.0;
    double FACTOR_LEADER = 0.05;
    double FACTOR_BEHIND = -0.15;
    double FACTOR_AHEAD = 0.0;
    double FACTOR_IN_CAVE = 0.0;

    public DiamantHeuristic() {
        addTunableParameter("FACTOR_SCORE", 1.0);
        addTunableParameter("FACTOR_LEADER", 0.05);
        addTunableParameter("FACTOR_BEHIND", -0.15);
        addTunableParameter("FACTOR_AHEAD", 0.0);
        addTunableParameter("FACTOR_IN_CAVE", 0.0);
    }

    @Override
    public void _reset() {
        FACTOR_SCORE = (double) getParameterValue("FACTOR_SCORE");
        FACTOR_LEADER = (double) getParameterValue("FACTOR_LEADER");
        FACTOR_BEHIND = (double) getParameterValue("FACTOR_BEHIND");
        FACTOR_AHEAD = (double) getParameterValue("FACTOR_AHEAD");
        FACTOR_IN_CAVE = (double) getParameterValue("FACTOR_IN_CAVE");
    }

    /**
     * Get the score of a player given the game state
     * The score is estimating taking into account the number of gems on the treasure chest of the player
     * with respect the gems of the other players.
     * If the player has more -> 1.0
     * Else if the player has less -> - 1.0
     * Else -> 0.0
     *
     * @param gs        - game state to evaluate and score.
     * @param playerId: player for whom we want to estimate the score
     * @return a value -1,0,1 indicating how good is to be in this game state
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        CoreConstants.GameResult playerResult = gs.getPlayerResults()[playerId];
        if (playerResult == CoreConstants.GameResult.LOSE_GAME)
            return -1;
        if (playerResult == CoreConstants.GameResult.WIN_GAME)
            return 1;

        DiamantGameState dgs = (DiamantGameState) gs;
        List<Integer> gemsInOrder = dgs.getTreasureChests().stream()
                .mapToInt(Counter::getValue)
                .sorted().boxed().collect(Collectors.toList());

        int max_ngens = gemsInOrder.get(dgs.getNPlayers()-1);
        int min_ngens = gemsInOrder.get(0);

        int player_gems = dgs.treasureChests.get(playerId).getValue();
        double highestExpectedScore = 139.0 / gs.getNPlayers() * 2.0;
        // 1.0 if a player has every single gem in a 2-player game; 67% of gems in a 3-player; 50% in a 4-player....
        double score = FACTOR_SCORE * player_gems / highestExpectedScore;

        if (player_gems == max_ngens) score += FACTOR_LEADER; // are we in the lead (including co-lead)

        score += FACTOR_BEHIND * (max_ngens - player_gems) / highestExpectedScore;

        score += FACTOR_AHEAD * (player_gems - min_ngens) / highestExpectedScore;

        if (dgs.playerInCave.get(playerId)) score += FACTOR_IN_CAVE;  // are we in the cave...for luck-pushing strategies

        return score;
    }

    @Override
    protected DiamantHeuristic _copy() {
        DiamantHeuristic retValue = new DiamantHeuristic();
        retValue.FACTOR_AHEAD = FACTOR_AHEAD;
        retValue.FACTOR_BEHIND = FACTOR_BEHIND;
        retValue.FACTOR_LEADER = FACTOR_LEADER;
        retValue.FACTOR_SCORE = FACTOR_SCORE;
        retValue.FACTOR_IN_CAVE = FACTOR_IN_CAVE;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DiamantHeuristic) {
            DiamantHeuristic other = (DiamantHeuristic) o;
            return other.FACTOR_IN_CAVE == FACTOR_IN_CAVE && other.FACTOR_SCORE == FACTOR_SCORE &&
                    other.FACTOR_LEADER == FACTOR_LEADER && other.FACTOR_BEHIND == FACTOR_BEHIND &&
                    other.FACTOR_AHEAD == FACTOR_AHEAD;
        }
        return false;
    }

    @Override
    public DiamantHeuristic instantiate() {
        return _copy();
    }

}
