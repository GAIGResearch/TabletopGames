package games.seasaltpaper;

import core.AbstractParameters;
import games.loveletter.LoveLetterParameters;

import java.util.Arrays;

public class SeaSaltPaperParameters extends AbstractParameters {

    int discardPileCount = 2;

    // TODO make collector bonus of suites that do not have collector card empty array
    public int[] boatCollectorBonus = new int[]{};
    public int[] fishCollectorBonus = new int[]{};
    public int[] shellCollectorBonus = new int[]{0, 2, 4, 6, 8, 10};
    public int[] octopusCollectorBonus = new int[]{0, 3, 6, 9, 12};
    public int[] penguinCollectorBonus = new int[]{1, 3, 5};
    public int[] sailorCollectorBonus = new int[]{0, 5};
    public int[] sharkCollectorBonus = new int[]{};

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
