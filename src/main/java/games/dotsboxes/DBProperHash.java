package games.dotsboxes;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class DBProperHash implements IStateFeatureVector {
    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        DBGameState dbgs = (DBGameState) state;
        // we want the edges that exist, plus the player scores
        // nothing else matters (anyway)
        double[] retValue = new double[2];
        double[] edgesByMidPosition = dbgs.edgeToOwnerMap.keySet().stream()
                .map(e -> 100 * ((e.from.getX() + e.to.getX()) / 2.0) +
                ((e.from.getY() + e.to.getY())/ 2.0)).mapToDouble(i -> i)
                .sorted().toArray();
        retValue[0] = Arrays.hashCode(edgesByMidPosition) % 10000000;
        retValue[1] = Arrays.hashCode(dbgs.nCellsPerPlayer);
        return retValue;
    }

    @Override
    public String[] names() {
        return new String[]{"board", "scores"};
    }
}
