package games.toads.abilities;

import games.toads.ToadAbility;

public class Bomb implements ToadAbility {
    @Override
    public int updatedValue(int myValue, int theirValue, boolean isAttacker) {
        if (isAttacker)
            return 0;
        if (theirValue == 4)
            return 0;
        return 9;
    }
}
