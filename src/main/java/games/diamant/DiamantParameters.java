package games.diamant;

import core.AbstractParameters;

import java.util.Arrays;

public class DiamantParameters extends AbstractParameters {
    public int nCaves              = 5;
    public int nArtifactCards      = 5;
    public int nHazardCardsPerType = 3;
    public int[] treasures         = new int[]{1, 2, 3, 4, 5, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17}; // TODO: check the real values in game rules
    public int nHazardsToDead      = 2;

    public DiamantParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        DiamantParameters copy = new DiamantParameters(System.currentTimeMillis());
        copy.nCaves              = nCaves;
        copy.nArtifactCards      = nArtifactCards;
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
               nArtifactCards      == that.nArtifactCards      &&
               nHazardCardsPerType == that.nHazardCardsPerType &&
               nHazardsToDead      == that.nHazardsToDead      &&
               Arrays.equals(treasures, that.treasures);
    }
}
