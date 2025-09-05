package games.toads.metrics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateKey;
import games.toads.components.ToadCard;
import games.toads.ToadGameState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ToadFeatures001 implements IStateFeatureVector {

    @Override
    public String[] names() {
        return new String[]{
                "TICK",
                "HAND_1", "HAND_2", "HAND_3", "HAND_4",
                "SCORE_US", "SCORE_THEM",
                "FIELD_CARD", "FLANK_CARD",
                "THEIR_FIELD",
                "KNOWN_HAND_1", "KNOWN_HAND_2",
                "TIEBREAK_US",
                "DISCARD_US", "DISCARD_THEM",
                "IS_SECOND_ROUND",
                "FIRST_ROUND_WIN",
                "FIRST_ROUND_LOSS"
        };
    }

    @Override
    public double[] doubleVector(AbstractGameState gs, int playerID) {
        double[] features = new double[18];
        ToadGameState state = (ToadGameState) gs;
        features[0] = state.getGameTick();
        List<Integer> handValuesInOrder = state.getPlayerHand(playerID).stream().map(c -> c.value).sorted().toList();
        for (int i = 0; i < handValuesInOrder.size(); i++) {
            features[i + 1] = handValuesInOrder.get(i);
        }
        features[5] = state.getGameScore(playerID);
        features[6] = state.getGameScore(1 - playerID);
        features[7] = state.getFieldCard(playerID) == null ? 0 : state.getFieldCard(playerID).value;
        features[8] = state.getHiddenFlankCard(playerID) == null ? 0 : state.getHiddenFlankCard(playerID).value;
        features[9] = state.getFieldCard(1 - playerID) == null ? 0 : state.getFieldCard(1 - playerID).value;
        List<ToadCard> oppHand = new ArrayList<>();
        for (int i = 0; i < state.getPlayerHand(1 - playerID).getSize(); i++) {
            if (state.getPlayerHand(1 - playerID).getVisibilityForPlayer(i, playerID)) {
                oppHand.add(state.getPlayerHand(1 - playerID).get(i));
            }
        }
        if (!oppHand.isEmpty()) {
            oppHand.sort(Comparator.comparingInt(c -> c.value));
            features[10] = oppHand.get(0).value;
            if (oppHand.size() > 1) {
                features[11] = oppHand.get(1).value;
            }
        }
        features[12] = state.getTieBreaker(playerID) == null ? 0 : state.getTieBreaker(playerID).value;
        for (int i = 0; i < state.getDiscards(playerID).getSize(); i++) {
            features[13] += state.getDiscards(playerID).get(i).value  * multiplier(i) - 1;
        }
        for (int i = 0; i < state.getDiscards(1 - playerID).getSize(); i++) {
            features[14] += state.getDiscards(1 - playerID).get(i).value  * multiplier(i) - 1;
        }
        features[15] = state.getRoundCounter() == 1 ? 1 : 0;
        features[16] = state.getBattlesWon(0, playerID) > state.getBattlesWon(0, 1 - playerID) ? 1 : 0;
        features[17] = state.getBattlesWon(0, playerID) < state.getBattlesWon(0, 1 - playerID) ? 1 : 0;

        return features;
    }

    private double multiplier(int i) {
        return Math.pow(8, i+1);
    }

}
