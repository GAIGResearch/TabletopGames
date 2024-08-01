package games.seasaltpaper;

import core.AbstractParameters;
import games.loveletter.LoveLetterParameters;

import java.util.Arrays;

public class SeaSaltPaperParameters extends AbstractParameters {

    int discardPileCount = 2;

    // TODO make collector bonus of suites that do not have collector card empty array
    public int[] boatCollectorBonus = new int[]{1, 2, 3};
    public int[] fishCollectorBonus = new int[]{1, 2, 3};
    public int[] shellCollectorBonus = new int[]{1, 2, 3};
    public int[] octopusCollectorBonus = new int[]{1, 2, 3};
    public int[] penguinCollectorBonus = new int[]{1, 2, 3};
    public int[] sailorCollectorBonus = new int[]{1, 2, 3};
    public int[] sharkCollectorBonus = new int[]{1, 2, 3};

    public SeaSaltPaperParameters()
    {

    }

    @Override
    protected AbstractParameters _copy() {
        return new SeaSaltPaperParameters();
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return false;
    }


}
