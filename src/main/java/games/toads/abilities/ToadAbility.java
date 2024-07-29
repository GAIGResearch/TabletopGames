package games.toads.abilities;

public interface ToadAbility {

    default int deltaToValue(int myValue, int theirValue, boolean isAttacker) {
        return 0;
    }
}
