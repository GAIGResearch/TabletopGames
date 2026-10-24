package games.monopoly;

/**
 * A colour group of streets. index is its position in MonopolyBoard.groups(); colour is an HTML colour for
 * display.
 */
public record MonopolyGroup(int index, String name, String colour) {

    @Override
    public String toString() {
        return name;
    }
}
