package games.diplomacy;

import java.util.List;

/**
 * A province of the map. Made when the map is loaded (DiplomacyMap); index is its position in
 * DiplomacyMap.provinces() and in the state's arrays. home is the index of the power for which it is a home supply
 * centre, or -1. coasts names the separately identified coasts of a province such as Spain ("nc", "sc"), and is
 * empty for every other province.
 */
public record DiplomacyProvince(int index, String name, String fullName, Type type, boolean supplyCentre, int home,
                                List<String> coasts) {

    public enum Type {
        /** only armies may enter */
        LAND,
        /** a land province on the sea: armies and fleets may enter */
        COAST,
        /** only fleets may enter */
        SEA
    }

    public boolean hasCoasts() {
        return !coasts.isEmpty();
    }

    /**
     * By index and name only (equal provinces are equal in every other field): the record's default would hash the
     * type enum by identity, which differs from one run to the next.
     */
    @Override
    public int hashCode() {
        return 31 * index + name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
