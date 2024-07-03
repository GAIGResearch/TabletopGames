package games.seasaltpaper;

import core.AbstractParameters;
import games.loveletter.LoveLetterParameters;

public class SeaSaltPaperParameters extends AbstractParameters {

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
