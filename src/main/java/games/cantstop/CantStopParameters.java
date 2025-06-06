package games.cantstop;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.Arrays;

public class CantStopParameters extends TunableParameters {

    /*
    This could have been implemented as an array, but this more verbose set up makes it easier for
    any future conversion of this to extend TunableParameters

    Ideas for other changes are to implement the variants described at https://www.yucata.de/en/Rules/CantStop
    - Varying numbers of capped columns based on player count
    - Not able to stop if on the same square as an opponent
    - Moving onto an occupied space instead means you jump to the next unoccupied one
     */

    public int TWO_MAX = 2;
    public int THREE_MAX = 4;
    public int FOUR_MAX = 6;
    public int FIVE_MAX = 8;
    public int SIX_MAX = 10;
    public int SEVEN_MAX = 12;
    public int EIGHT_MAX = 10;
    public int NINE_MAX = 8;
    public int TEN_MAX = 6;
    public int ELEVEN_MAX = 4;
    public int TWELVE_MAX = 2;

    public int DICE_NUMBER = 4; // If you change this, then you'll need to also update code in ForwardModel._computeAvailableActions()
    public int DICE_SIDES = 6; // Same here (but with scoring)
    public int COLUMNS_TO_WIN = 3;
    public int MARKERS = 3; // number of temporary markers

    public CantStopParameters() {
        // Column sizes
        addTunableParameter("TWO_MAX", 2, Arrays.asList(1,2,3,4,5));
        addTunableParameter("THREE_MAX", 4, Arrays.asList(2,3,4,5,6));
        addTunableParameter("FOUR_MAX", 6, Arrays.asList(4,5,6,7,8));
        addTunableParameter("FIVE_MAX", 8, Arrays.asList(6,7,8,9,10));
        addTunableParameter("SIX_MAX", 10, Arrays.asList(8,9,10,11,12));
        addTunableParameter("SEVEN_MAX", 12, Arrays.asList(10,11,12,13,14));
        addTunableParameter("EIGHT_MAX", 10, Arrays.asList(8,9,10,11,12));
        addTunableParameter("NINE_MAX", 8, Arrays.asList(6,7,8,9,10));
        addTunableParameter("TEN_MAX", 6, Arrays.asList(4,5,6,7,8));
        addTunableParameter("ELEVEN_MAX", 4, Arrays.asList(2,3,4,5,6));
        addTunableParameter("TWELVE_MAX", 2, Arrays.asList(1,2,3,4,5));

        addTunableParameter("COLUMNS_TO_WIN", 3, Arrays.asList(2, 3, 4, 5, 6));
        addTunableParameter("MARKERS", 3, Arrays.asList(2, 3, 4, 5, 6));

    }

    @Override
    public void _reset() {
        // Column sizes
        TWO_MAX = (int) getParameterValue("TWO_MAX");
        THREE_MAX = (int) getParameterValue("THREE_MAX");
        FOUR_MAX = (int) getParameterValue("FOUR_MAX");
        FIVE_MAX = (int) getParameterValue("FIVE_MAX");
        SIX_MAX = (int) getParameterValue("SIX_MAX");
        SEVEN_MAX = (int) getParameterValue("SEVEN_MAX");
        EIGHT_MAX = (int) getParameterValue("EIGHT_MAX");
        NINE_MAX = (int) getParameterValue("NINE_MAX");
        TEN_MAX = (int) getParameterValue("TEN_MAX");
        ELEVEN_MAX = (int) getParameterValue("ELEVEN_MAX");
        TWELVE_MAX = (int) getParameterValue("TWELVE_MAX");

        COLUMNS_TO_WIN = (int) getParameterValue("COLUMNS_TO_WIN");
        MARKERS = (int) getParameterValue("MARKERS");
    }

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

    @Override
    public Object instantiate() {
        return new Game(GameType.CantStop, new CantStopForwardModel(), new CantStopGameState(this, GameType.CantStop.getMinPlayers()));
    }
}
