package games.diplomacy;

/**
 * Where a unit stands or moves to: a province, and for a fleet in a province with separate coasts, the coast
 * ("nc", "sc", "ec"). coast is "" for an army, and for any province without separate coasts.
 */
public record DiplomacyLocation(DiplomacyProvince province, String coast) {

    public DiplomacyLocation(DiplomacyProvince province) {
        this(province, "");
    }

    @Override
    public String toString() {
        return coast.isEmpty() ? province.name() : province.name() + "/" + coast;
    }
}
