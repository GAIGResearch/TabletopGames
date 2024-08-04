package games.toads.metrics;

import core.AbstractGameState;
import evaluation.features.TunableStateFeatures;
import games.toads.ToadConstants;
import games.toads.ToadGameState;

public class ToadFeaturesTunable extends TunableStateFeatures {

    static final String[] allPossibleFeatures = new String[]{
                "PHASE", "TURN", "ROUND", "FLANK",
                        "FIELD_CARD", "FLANK_CARD",
                        "THEIR_FIELD"
    };

    public ToadFeaturesTunable() {
        super(allPossibleFeatures);
    }

    @Override
    public double[] fullFeatureVector(AbstractGameState gs, int playerID) {
        double[] features = new double[7];
        ToadGameState state = (ToadGameState) gs;
        features[0] = state.getGamePhase() == ToadConstants.ToadGamePhase.PLAY ? 0
                : state.getGamePhase() == ToadConstants.ToadGamePhase.DISCARD ? 1
                : 2;
        features[1] = state.getTurnCounter();
        features[2] = state.getRoundCounter();
        features[3] = state.getFieldCard(playerID) == null ? 0 : 1;
        return features;
    }

    @Override
    protected ToadFeaturesTunable _copy() {
        return new ToadFeaturesTunable();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof ToadFeaturesTunable;
    }

    @Override
    public ToadFeaturesTunable instantiate() {
        return this;
    }

}
