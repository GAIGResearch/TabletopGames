package games.thegame;

import core.AbstractParameters;
import core.CoreConstants;
import gametemplate.GTParameters;

import java.util.Arrays;
import java.util.Objects;

public class TheGameParameters extends AbstractParameters {


    public final CoreConstants.VisibilityMode playerHandVisibility = CoreConstants.VisibilityMode.VISIBLE_TO_OWNER;
    public final int numAscendingRows = 2;
    public final int numDescendingRows = 2;

    public final int minCardNumber = 1;
    public final int maxCardNumber = 100;
    public final int[] handSize = new int[]{8, 7, 6, 6, 6}; // -1 to all for expert play.

    public final int nCardsToPlay = 2; // 3: expert play
    public final int nCardsToPlayNoDrawDeck = 1;
    public final int backwardsTrickValue = 10;


    public TheGameParameters() {}


    @Override
    protected AbstractParameters _copy() {
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), playerHandVisibility, numAscendingRows, numDescendingRows, minCardNumber, maxCardNumber, Arrays.hashCode(handSize), nCardsToPlay, nCardsToPlayNoDrawDeck, backwardsTrickValue);
    }
}
