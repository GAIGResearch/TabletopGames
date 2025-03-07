package games.tickettoride;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;

public class TicketToRideHeuristic extends TunableParameters implements IStateHeuristic {

    double FACTOR_PLAYER_SCORE = 1.0;

    public TicketToRideHeuristic() {
        addTunableParameter("PLAYER_SCORE", 1.0);
        _reset();
    }

    @Override
    public void _reset() {
        FACTOR_PLAYER_SCORE = (double) getParameterValue("PLAYER_SCORE");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        TicketToRideGameState ttg = (TicketToRideGameState) gs;

        if (!ttg.isNotTerminal()) {
            return ttg.getPlayerResults()[playerId].value;
        }


        double heuristicScore = Math.max(0.0, Math.min(ttg.getScores()[playerId] / 100.0, 1.0));
        if (heuristicScore > 1){
            System.out.println("HEURISTIC SCORE ERROR, MORE THAN 1");
        } else {
            System.out.println("heuristic score:" + heuristicScore);
        }

        return heuristicScore;
    }

    @Override
    protected TicketToRideHeuristic _copy() {
        TicketToRideHeuristic copy = new TicketToRideHeuristic();
        copy.FACTOR_PLAYER_SCORE = FACTOR_PLAYER_SCORE;
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof TicketToRideHeuristic) {
            TicketToRideHeuristic other = (TicketToRideHeuristic) o;
            return other.FACTOR_PLAYER_SCORE == FACTOR_PLAYER_SCORE;
        }
        return false;
    }

    @Override
    public TicketToRideHeuristic instantiate() {
        return this._copy();
    }
}