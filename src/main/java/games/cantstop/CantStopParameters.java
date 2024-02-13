package games.cantstop;

import core.AbstractParameters;

public class CantStopParameters extends AbstractParameters {

    /*
    This could have been implemented as an array, but this more verbose set up makes it easier for
    any future conversion of this to extend TunableParameters

    Ideas for other changes are to implement the variants described at https://www.yucata.de/en/Rules/CantStop
    - Varying numbers of capped columns based on player count
    - Not able to stop if on the same square as an opponent
    - Moving onto an occupied space instead means you jump to the next unoccupied one
     */

    public final int TWO_MAX = 2;
    public final int THREE_MAX = 4;
    public final int FOUR_MAX = 6;
    public final int FIVE_MAX = 8;
    public final int SIX_MAX = 10;
    public final int SEVEN_MAX = 12;
    public final int EIGHT_MAX = 10;
    public final int NINE_MAX = 8;
    public final int TEN_MAX = 6;
    public final int ELEVEN_MAX = 4;
    public final int TWELVE_MAX = 2;

    public final int DICE_NUMBER = 4; // If you change this, then you'll need to also update code in ForwardModel._computeAvailableActions()
    public final int DICE_SIDES = 6;
    public final int COLUMNS_TO_WIN = 3;
    public final int MARKERS = 3; // number of temporary markers

    public int maxValue(int number) {
        switch (number) {
            case 2:
                return TWO_MAX;
            case 3:
                return THREE_MAX;
            case 4:
                return FOUR_MAX;
            case 5:
                return FIVE_MAX;
            case 6:
                return SIX_MAX;
            case 7:
                return SEVEN_MAX;
            case 8:
                return EIGHT_MAX;
            case 9:
                return NINE_MAX;
            case 10:
                return TEN_MAX;
            case 11:
                return ELEVEN_MAX;
            case 12:
                return TWELVE_MAX;
            default:
                throw new IllegalArgumentException(number + " is not supported");
        }
    }

    @Override
    protected AbstractParameters _copy() {
        return new CantStopParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof CantStopParameters;
    }
}
