package games.toads.metrics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateKey;
import games.toads.components.ToadCard;
import games.toads.ToadGameState;
import games.toads.abilities.GeneralOne;
import games.toads.abilities.GeneralTwo;

public class ToadFeatures002 implements IStateFeatureVector, IStateKey {

    @Override
    public String[] names() {
        return new String[]{
                "TICK", "TURN", "ROUND",
                "BATTLES_US", "BATTLES_THEM",
                "BATTLES_LEAD",
                "AHEAD_THIS_ROUND",
                "CURRENTLY_TIED",
                "TIED_BATTLES",
                "INFORMATION",
                "TIEBREAK_US",
                "FIRST_ROUND_WIN",
                "FIRST_ROUND_LOSS",
                "AC_IN_HAND",
                "ASSASSIN_IN_HAND",
                "SCOUT_IN_HAND",
                "TRICKSTER_IN_HAND",
                "SABOTEUR_IN_HAND",
                "BERSERKER_IN_HAND",
                "ICONBEARER_IN_HAND",
                "GENERAL1_IN_HAND",
                "GENERAL2_IN_HAND",
                "AC_USED",
                "ASSASSIN_USED",
                "SCOUT_USED",
                "TRICKSTER_USED",
                "SABOTEUR_USED",
                "BERSERKER_USED",
                "ICONBEARER_USED",
                "GENERAL1_USED",
                "GENERAL2_USED"
        };
    }


    @Override
    public double[] doubleVector(AbstractGameState gs, int playerID) {
        double[] features = new double[31];
        ToadGameState state = (ToadGameState) gs;
        features[0] = state.getGameTick();
        features[1] = state.getTurnCounter();
        features[2] = state.getRoundCounter();

        features[3] = state.getBattlesWon(state.getRoundCounter(), playerID);
        features[4] = state.getBattlesWon(state.getRoundCounter(), 1 - playerID);
        features[5] = features[3] - features[4];
        features[6] = features[3] > features[4] ? 1 : 0;
        features[7] = features[3] == features[4] ? 1 : 0;

        features[8] = state.getBattlesTied(state.getRoundCounter());


        int visibleCount = 0;
        for (int i = 0; i < state.getPlayerHand(1 - playerID).getSize(); i++) {
            if (state.getPlayerHand(1 - playerID).getVisibilityForPlayer(i, playerID)) {
                visibleCount++;
            }
        }
        // then through deck
        for (int i = 0; i < state.getPlayerDeck(1 - playerID).getSize(); i++) {
            if (state.getPlayerDeck(1 - playerID).getVisibilityForPlayer(i, playerID)) {
                visibleCount++;
            }
        }

        features[9] = visibleCount;
        features[10] = state.getTieBreaker(playerID) == null ? 0 : state.getTieBreaker(playerID).value;
        features[11] = state.getBattlesWon(0, playerID) > state.getBattlesWon(0, 1 - playerID) ? 1 : 0;
        features[12] = state.getBattlesWon(0, playerID) < state.getBattlesWon(0, 1 - playerID) ? 1 : 0;

        for (int i = 0; i < state.getPlayerHand(playerID).getSize(); i++) {
            ToadCard card = state.getPlayerHand(playerID).get(i);
            if (card.value < 7) {
                features[13 + i] = 1;
            } else if (card.tactics instanceof GeneralOne) {
                features[20] = 1;
            } else if (card.tactics instanceof GeneralTwo) {
                features[21] = 1;
            }
        }
        // then for discards
        for (int i = 0; i < state.getDiscards(playerID).getSize(); i++) {
            ToadCard card = state.getDiscards(playerID).get(i);
            if (card.value < 7) {
                features[22 + i] = 1;
            } else if (card.tactics instanceof GeneralOne) {
                features[29] = 1;
            } else if (card.tactics instanceof GeneralTwo) {
                features[30] = 1;
            }
        }

        return features;
    }

}
