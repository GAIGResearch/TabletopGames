package games.monopoly;

/**
 * A square of the board. index is its position round the board from GO (0), and in the state's arrays.
 * <ul>
 *     <li>group - the colour group of a street, null for any other square</li>
 *     <li>price - the printed price of a property, 0 otherwise</li>
 *     <li>rents - a street's rent with 0 to 4 houses, then with a hotel; a station's rent with 1 to 4 stations
 *     owned; a utility's multiple of the dice with 1 or 2 utilities owned. Empty for other squares</li>
 *     <li>houseCost - the cost of a house (and of a hotel) on a street, 0 otherwise</li>
 *     <li>mortgage - the mortgage value of a property, 0 otherwise</li>
 *     <li>tax - the amount paid on landing on a tax square, 0 otherwise</li>
 * </ul>
 */
public record MonopolySquare(int index, String name, SquareType type, MonopolyGroup group, int price, int[] rents,
                             int houseCost, int mortgage, int tax) {

    /**
     * By index and name only (equal squares have equal data): the record's default compares the rents array by
     * identity, and hashes the type enum by identity, which differs from one run to the next.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof MonopolySquare s && s.index == index && s.name.equals(name);
    }

    @Override
    public int hashCode() {
        return 31 * index + name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
