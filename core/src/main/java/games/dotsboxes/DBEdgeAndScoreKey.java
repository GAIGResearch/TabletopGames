package games.dotsboxes;

import core.AbstractGameState;
import core.interfaces.IStateKey;

import java.util.Arrays;
import java.util.stream.Collectors;

public class DBEdgeAndScoreKey implements IStateKey {

    @Override
    public String getKey(AbstractGameState state, int playerID) {
        // in this case playerID is irrelevant
        DBGameState dbgs = (DBGameState) state;
        String edgeString = dbgs.edgeToOwnerMap.keySet().stream()
                .map(e -> 100.0 * ((e.from.getX() + e.to.getX()) / 2.0) +
                        ((e.from.getY() + e.to.getY())/ 2.0)).mapToDouble(i -> i)
                .sorted().mapToObj(d -> String.format("%.1f", d)).collect(Collectors.joining(","));
        String scoreString = "Scores: " + Arrays.toString(dbgs.nCellsPerPlayer);
        return state.getCurrentPlayer() + " " + edgeString + scoreString;
    }
}
