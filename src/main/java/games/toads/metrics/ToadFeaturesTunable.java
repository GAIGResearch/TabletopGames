package games.toads.metrics;

import core.AbstractGameState;
import evaluation.features.TunableStateFeatures;
import games.toads.ToadConstants;
import games.toads.ToadGameState;
import games.toads.components.ToadCard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ToadFeaturesTunable extends TunableStateFeatures {

    static final String[] allPossibleFeatures = new String[]{
            "TICK", "PHASE", "TURN", "ROUND",
            "FIELD_CARD", "FLANK_CARD",
            "THEIR_FIELD", "HAND_CONTENTS",
            "ROUND_ONE_RESULT",
            "THEIR_HAND",
            "TIES", "OUR_BATTLES", "THEIR_BATTLES",
            "TIEBREAKER",
            "OUR_DISCARD", "THEIR_DISCARD"

    };

    public ToadFeaturesTunable() {
        super(allPossibleFeatures);
    }

    private double multiplier(int i) {
        return Math.pow(8, i+1);
    }

    @Override
    public double[] fullFeatureVector(AbstractGameState gs, int playerID) {
        double[] features = new double[16];
        ToadGameState state = (ToadGameState) gs;
        features[0] = state.getGameTick();
        features[1] = state.getGamePhase() == ToadConstants.ToadGamePhase.PLAY ? 0
                : state.getGamePhase() == ToadConstants.ToadGamePhase.DISCARD ? 1
                : 2;
        int round = state.getRoundCounter();
        features[2] = state.getTurnCounter();
        features[3] = round;
        if (active[4]) // Field card (hashcode)
            features[4] = state.getFieldCard(playerID) == null ? 0 : state.getFieldCard(playerID).hashCode();
        if (active[5]) // Flank card (hashcode)
            features[5] = state.getHiddenFlankCard(playerID) == null ? 0 : state.getHiddenFlankCard(playerID).hashCode();
        if (active[6]) // Their field card (hashcode)
            features[6] = state.getFieldCard(1 - playerID) == null ? 0 : state.getFieldCard(1 - playerID).hashCode();

        if (active[7]) { // Hand contents
            List<Integer> handValuesInOrder = state.getPlayerHand(playerID).stream().map(c -> c.value).sorted().toList();
            for (int i = 0; i < handValuesInOrder.size(); i++) {
                features[7] += handValuesInOrder.get(i) * multiplier(i) - 1;
            }
        }
        if (state.getRoundCounter() > 0) {
            int scoreDiff = state.getBattlesWon(0, playerID) - state.getBattlesWon(0, 1 - playerID);
            features[8] = scoreDiff > 0 ? 1 : scoreDiff < 0 ? 2 : 3;
        }
        List<ToadCard> oppHand = new ArrayList<>();
        if (active[9]) {
            for (int i = 0; i < state.getPlayerHand(1 - playerID).getSize(); i++) {
                if (state.getPlayerHand(1 - playerID).getVisibilityForPlayer(i, playerID)) {
                    oppHand.add(state.getPlayerHand(1 - playerID).get(i));
                }
            }
            if (!oppHand.isEmpty()) {
                oppHand.sort(Comparator.comparingInt(c -> c.value));
                features[9] = oppHand.get(0).hashCode();
            }
        }
        features[10] = state.getBattlesTied(round);
        features[11] = state.getBattlesWon(round, playerID);
        features[12] = state.getBattlesWon(round, 1 - playerID);
        features[13] = state.getTieBreaker(playerID) == null ? -1 : state.getTieBreaker(playerID).value;
        List<ToadCard> discards;
        if (active[14]) {
            discards = state.getDiscards(playerID).stream().toList();
            for (int i = 0; i < discards.size(); i++) {
                features[14] += discards.get(i).hashCode() * multiplier(i) - 1;
            }
        }
        if (active[15]) {
            discards = state.getDiscards(1 - playerID).stream().toList();
            for (int i = 0; i < discards.size(); i++) {
                features[15] += discards.get(i).hashCode() * multiplier(i);
            }
        }
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
