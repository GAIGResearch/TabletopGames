package games.toads.metrics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateKey;
import games.toads.ToadConstants;
import games.toads.ToadGameState;

public class ToadFeatures998 implements IStateFeatureVector, IStateKey {

    @Override
    public String[] names() {
        return new String[]{
                "PHASE", "TURN", "ROUND", "FLANK",
                "FIELD_CARD", "FLANK_CARD",
                "THEIR_FIELD"
        };
    }

    @Override
    public double[] featureVector(AbstractGameState gs, int playerID) {
        double[] features = new double[18];
        ToadGameState state = (ToadGameState) gs;
        features[0] = state.getGamePhase() == ToadConstants.ToadGamePhase.PLAY ? 0
                : state.getGamePhase() == ToadConstants.ToadGamePhase.DISCARD ? 1
                : 2;
        features[1] = state.getTurnCounter();
        features[2] = state.getRoundCounter();
        features[3] = state.getFieldCard(playerID) == null ? 0 : 1;
        return features;
    }

}
