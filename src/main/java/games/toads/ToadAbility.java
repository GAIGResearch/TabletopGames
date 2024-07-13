package games.toads;

public interface ToadAbility {

    default int deltaToValue(int myValue, int theirValue, boolean isAttacker) {
        return 0;
    }
}
