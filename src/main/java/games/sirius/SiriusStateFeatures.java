package games.sirius;

import core.AbstractGameState;
import core.components.Deck;
import players.heuristics.AbstractStateFeature;

import java.util.*;
import java.util.stream.Collectors;

import static games.sirius.SiriusConstants.SiriusCardType.*;

public class SiriusStateFeatures extends AbstractStateFeature {

    String[] localNames = new String[]{"AM_TRACK", "CB_TRACK", "AM_DECK", "CB_DECK", "MEDAL_COUNT",
            "AM_MEDAL_SCORE", "CB_MEDAL_SCORE", "AM_MEDALS", "CB_MEDALS", "MEDAL_2", "MEDAL_3", "MEDAL_4", "MEDAL_5", "MEDAL_6",
            "TOT_HAND", "AM_HAND", "CB_HAND", "FV_HAND", "AM3_HAND", "AM2_HAND", "CB3_HAND", "GC_HAND",
            "TOT_SOLD", "AM3_SOLD", "CB3_SOLD", "GC_SOLD",
            "CARTEL_1", "CARTEL_2", "CARTEL_3"};

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

        retValue[0] = state.ammoniaTrack;
        retValue[1] = state.contrabandTrack;
        retValue[2] = state.ammoniaDeck.getSize();
        retValue[3] = state.contrabandDeck.getSize();
        retValue[4] = state.getMedalsTaken();
        List<Medal> medals = state.playerAreas.get(playerID).medals;
        retValue[5] = medals.stream().filter(m -> m.medalType == AMMONIA).mapToInt(m -> m.value).sum();
        retValue[6] = medals.stream().filter(m -> m.medalType == CONTRABAND).mapToInt(m -> m.value).sum();
        retValue[7] = medals.stream().filter(m -> m.medalType == AMMONIA).count();
        retValue[8] = medals.stream().filter(m -> m.medalType == CONTRABAND).count();
        retValue[9] = medals.stream().filter(m -> m.value == 2).count();
        retValue[10] = medals.stream().filter(m -> m.value == 3).count();
        retValue[11] = medals.stream().filter(m -> m.value == 4).count();
        retValue[12] = medals.stream().filter(m -> m.value == 5).count();
        retValue[13] = medals.stream().filter(m -> m.value == 6).count();
        Deck<SiriusCard> hand = state.getPlayerHand(playerID);
        retValue[14] = hand.getSize();
        retValue[15] = hand.stream().filter(c -> c.cardType == AMMONIA).count();
        retValue[16] = hand.stream().filter(c -> c.cardType == CONTRABAND).count();
        retValue[17] = hand.stream().filter(c -> c.cardType == FAVOUR).count();
        retValue[18] = hand.stream().filter(c -> c.cardType == AMMONIA && c.value == 3).count();
        retValue[19] = hand.stream().filter(c -> c.cardType == AMMONIA && c.value == 2).count();
        retValue[20] = hand.stream().filter(c -> c.cardType == CONTRABAND && c.value == 3).count();
        retValue[21] = hand.stream().filter(c -> c.cardType == CONTRABAND && c.value == 0).count();

        Deck<SiriusCard> soldCards = state.playerAreas.get(playerID).soldCards;
        retValue[22] = soldCards.getSize();
        retValue[23] = soldCards.stream().filter(c -> c.cardType == AMMONIA && c.value == 3).count();
        retValue[24] = soldCards.stream().filter(c -> c.cardType == CONTRABAND && c.value == 3).count();
        retValue[25] = soldCards.stream().filter(c -> c.cardType == CONTRABAND && c.value == 0).count();

        retValue[26] = state.getMoon(1).getCartelOwner() == playerID ? 1 : 0;
        retValue[27] = state.getMoon(2).getCartelOwner() == playerID ? 1 : 0;
        retValue[28] = state.getMoon(3).getCartelOwner() == playerID ? 1 : 0;

        return retValue;
    }
}
