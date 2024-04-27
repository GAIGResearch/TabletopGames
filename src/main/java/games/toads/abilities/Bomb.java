package games.toads.abilities;

import games.toads.ToadAbility;

public class Bomb implements ToadAbility {
    @Override
    public int deltaToValue(int myValue, int theirValue, boolean isAttacker) {
        if (isAttacker)
            return -myValue;
        if (theirValue == 4)
            return -myValue;
        return 10;
    }
}
