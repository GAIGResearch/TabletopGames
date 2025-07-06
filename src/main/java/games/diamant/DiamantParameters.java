package games.diamant;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.Arrays;

public class DiamantParameters extends TunableParameters {
    public int nCaves = 5;
    public int nHazardCardsPerType = 3;
    public int nHazardsToDead = 2;
    public int[] treasures = new int[]{1, 2, 3, 4, 5, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17};
    public boolean relicVariant = false;
    public int[] relics = new int[]{5, 7, 8, 10, 12};

    public DiamantParameters() {
        addTunableParameter("nCaves", 5, Arrays.asList(3, 5, 7, 10));
        addTunableParameter("nHazardCardsPerType", 3, Arrays.asList(1, 3, 4, 7, 10));
        addTunableParameter("nHazardsToDead", 2, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("relicVariant", true, Arrays.asList(true, false));
        _reset();
    }

    @Override
    public void _reset() {
        nCaves = (int) getParameterValue("nCaves");
        nHazardCardsPerType = (int) getParameterValue("nHazardCardsPerType");
        nHazardsToDead = (int) getParameterValue("nHazardsToDead");
        relicVariant = (boolean) getParameterValue("relicVariant");
    }

    @Override
    protected AbstractParameters _copy() {
        DiamantParameters copy = new DiamantParameters();
        // tunable parameters are copied automatically by super class
        copy.treasures = new int[treasures.length];
        copy.relics = new int[relics.length];
        System.arraycopy(treasures, 0, copy.treasures, 0, treasures.length);
        System.arraycopy(relics, 0, copy.relics, 0, relics.length);
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DiamantParameters that) {
            return Arrays.equals(treasures, that.treasures) && Arrays.equals(relics, that.relics);
        }
        return false;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Diamant, new DiamantForwardModel(), new DiamantGameState(this, GameType.Diamant.getMinPlayers()));
    }
}
