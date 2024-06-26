package games.diamant;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.Arrays;

public class DiamantParameters extends TunableParameters {
    public int nCaves              = 5;
    public int nHazardCardsPerType = 3;
    public int nHazardsToDead      = 2;
    public int[] treasures         = new int[]{1, 2, 3, 4, 5, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};

    public DiamantParameters() {
        addTunableParameter("nCaves", 5, Arrays.asList(3,5,7,10));
        addTunableParameter("nHazardCardsPerType", 3, Arrays.asList(1,3,4,7,10));
        addTunableParameter("nHazardsToDead", 2, Arrays.asList(1,2,3,4,5));
        _reset();
    }

    @Override
    public void _reset() {
        nCaves = (int) getParameterValue("nCaves");
        nHazardCardsPerType = (int) getParameterValue("nHazardCardsPerType");
        nHazardsToDead = (int) getParameterValue("nHazardsToDead");
    }

    @Override
    protected AbstractParameters _copy() {
        DiamantParameters copy = new DiamantParameters();
        copy.nCaves              = nCaves;
        copy.nHazardCardsPerType = nHazardCardsPerType;
        copy.nHazardsToDead      = nHazardsToDead;
        copy.treasures           = new int[treasures.length];
        System.arraycopy(treasures, 0, copy.treasures, 0, treasures.length);
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o)                         return true;
        if (!(o instanceof DiamantParameters)) return false;
        if (!super.equals(o))                  return false;

        DiamantParameters that = (DiamantParameters) o;
        return nCaves              == that.nCaves              &&
               nHazardCardsPerType == that.nHazardCardsPerType &&
               nHazardsToDead      == that.nHazardsToDead      &&
               Arrays.equals(treasures, that.treasures);
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Diamant, new DiamantForwardModel(), new DiamantGameState(this, GameType.Diamant.getMinPlayers()));
    }
}
