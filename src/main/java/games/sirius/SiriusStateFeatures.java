package games.sirius;

import core.AbstractGameState;
import players.heuristics.AbstractStateFeature;

import java.util.*;

import static games.sirius.SiriusConstants.SiriusCardType.AMMONIA;
import static games.sirius.SiriusConstants.SiriusCardType.CONTRABAND;

public class SiriusStateFeatures extends AbstractStateFeature {

    String[] localNames = new String[]{"SALE_SCORE", "HAND_SIZE", "AMMONIA_TRACK", "CONTRABAND_TRACK", "AMMONIA_DECK", "CONTRABAND_DECK",
            "MEDAL_COUNT", "MEDAL_SCORE", "AM_2", "AM_3", "AM_4", "AM_5", "AM_6", "CM_2", "CM_3", "CM_4", "CM_5", "CM_6"};

    @Override
    protected double maxScore() {
        return 1;
    }

    @Override
    protected double maxRounds() {
        return 1;
    }

    @Override
    public String[] localNames() {
        return localNames;
    }

    @Override
    protected double[] localFeatureVector(AbstractGameState gs, int playerID) {
        SiriusGameState state = (SiriusGameState) gs;
        double[] retValue = new double[localNames.length];

        retValue[0] = state.playerAreas.get(playerID).soldCards.getSize();
        retValue[1] = state.playerAreas.get(playerID).deck.getSize();
        retValue[2] = state.ammoniaTrack;
        retValue[3] = state.contrabandTrack;
        retValue[4] = state.ammoniaDeck.getSize();
        retValue[5] = 0; // TODO
        Set<Medal> medals = new HashSet<>(state.playerAreas.get(playerID).medals);
        retValue[6] = medals.size();
        retValue[7] = medals.stream().mapToInt(m -> m.value).sum();
        retValue[8] = medals.contains(new Medal(AMMONIA, 2)) ? 1 : 0;
        retValue[9] = medals.contains(new Medal(AMMONIA, 3)) ? 1 : 0;
        retValue[10] = medals.contains(new Medal(AMMONIA, 4)) ? 1 : 0;
        retValue[11] = medals.contains(new Medal(AMMONIA, 5)) ? 1 : 0;
        retValue[12] = medals.contains(new Medal(AMMONIA, 6)) ? 1 : 0;
        retValue[13] = medals.contains(new Medal(CONTRABAND, 2)) ? 1 : 0;
        retValue[14] = medals.contains(new Medal(CONTRABAND, 3)) ? 1 : 0;
        retValue[15] = medals.contains(new Medal(CONTRABAND, 4)) ? 1 : 0;
        retValue[16] = medals.contains(new Medal(CONTRABAND, 5)) ? 1 : 0;
        retValue[17] = medals.contains(new Medal(CONTRABAND, 6)) ? 1 : 0;
        return retValue;
    }
}
