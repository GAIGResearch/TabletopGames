package games.diplomacy;

/**
 * A unit on the board: an army or a fleet, belonging to a power (its owner, the player index). coast is the coast a
 * fleet occupies in a province with separate coasts, otherwise "". The state holds units by province.
 */
public record DiplomacyUnit(Type type, int owner, String coast) {

    public enum Type {
        ARMY("A"), FLEET("F");

        public final String letter;

        Type(String letter) {
            this.letter = letter;
        }
    }

    public DiplomacyUnit(Type type, int owner) {
        this(type, owner, "");
    }

    public boolean isFleet() {
        return type == Type.FLEET;
    }

    /**
     * By the type's ordinal: the record's default would hash the enum by identity, which differs from one run to the
     * next.
     */
    @Override
    public int hashCode() {
        return (31 * type.ordinal() + owner) * 31 + coast.hashCode();
    }
}
