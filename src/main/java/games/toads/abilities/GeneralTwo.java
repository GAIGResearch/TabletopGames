package games.toads.abilities;

import games.toads.ToadAbility;

public class GeneralTwo implements ToadAbility {
    @Override
    public int deltaToValue(int myValue, int theirValue, boolean isAttacker) {
        return 0;
    }
}
