package games.toads.metrics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateKey;
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
    public double[] featureVector(AbstractGameState gs, int playerID) {
        double[] features = new double[31];
        ToadGameState state = (ToadGameState) gs;
        features[0] = state.getGameTick();
        features[1] = state.getTurnCounter();
        features[2] = state.getRoundCounter();

        features[3] = state.getBattlesWon(playerID, state.getRoundCounter());
        features[4] = state.getBattlesWon(1 - playerID, state.getRoundCounter());
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
        features[11] = state.getBattlesWon(playerID, 0) > state.getBattlesWon(1 - playerID, 0) ? 1 : 0;
        features[12] = state.getBattlesWon(playerID, 0) < state.getBattlesWon(1 - playerID, 0) ? 1 : 0;

        features[13] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 0) ? 1 : 0;
        features[14] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 1) ? 1 : 0;
        features[15] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 2) ? 1 : 0;
        features[16] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 3) ? 1 : 0;
        features[17] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 4) ? 1 : 0;
        features[18] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 5) ? 1 : 0;
        features[19] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 6) ? 1 : 0;
        features[20] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.getTactics() instanceof GeneralOne) ? 1 : 0;
        features[21] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.getTactics() instanceof GeneralTwo) ? 1 : 0;

        features[22] = state.getDiscards(playerID).stream().anyMatch(c -> c.value == 0) ? 1 : 0;
        features[23] = state.getDiscards(playerID).stream().anyMatch(c -> c.value == 1) ? 1 : 0;
        features[24] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 2) ? 1 : 0;
        features[25] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 3) ? 1 : 0;
        features[26] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 4) ? 1 : 0;
        features[27] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 5) ? 1 : 0;
        features[28] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.value == 6) ? 1 : 0;
        features[29] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.getTactics() instanceof GeneralOne) ? 1 : 0;
        features[30] = state.getPlayerHand(playerID).stream().anyMatch(c -> c.getTactics() instanceof GeneralTwo) ? 1 : 0;

        return features;
    }

    @Override
    public Integer getKey(AbstractGameState state) {
        int retValue = state.getCurrentPlayer();
        double[] features = featureVector(state, state.getCurrentPlayer());
        for (int i = 0; i < features.length; i++) {
            retValue += (int) (features[i] * Math.pow(31, i+1));
        }
        return retValue;
    }
}
