package games.toads.abilities;

public class Assassin implements ToadAbility {
    @Override
    public int deltaToValue(int myValue, int theirValue, boolean isAttacker) {
        if (theirValue == 7)
            return 8 - myValue;
        return 0;
    }
}
