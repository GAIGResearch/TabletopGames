package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

public class MultiLeaderHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        // We sum our score difference compared to every other player - not just the leader (or our closest competitor if we are the leader)
        double score = gs.getGameScore(playerId);
        double retValue = 0.0;
        for (int p = 0; p < gs.getNPlayers(); p++) {
            if (p != playerId) {
                retValue += score - gs.getGameScore(p);
            }
        }

        return retValue;
    }

    // no settings (any fields are fixed, or worked out from the game): any two are the same heuristic, so that one
    // read back from JSON equals the one written
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass();
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }
}
