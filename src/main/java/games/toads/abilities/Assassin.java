package games.toads.abilities;

import games.toads.ToadAbility;

public class Assassin implements ToadAbility {
    @Override
    public int deltaToValue(int myValue, int theirValue, boolean isAttacker) {
        if (theirValue == 7)
            return 8 - myValue;
        return 0;
    }
}
