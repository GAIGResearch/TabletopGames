package core.components.grid;

/**
 * The side between two adjacent hexes, given by a hex and the direction it faces. Each side is shared by two hexes,
 * so it is stored facing E, SE or SW, and the same side is always an equal HexSide.
 */
public record HexSide(HexCoord hex, int direction) {

    public HexSide {
        if (direction >= 3) {
            hex = hex.neighbour(direction);
            direction -= 3;
        }
    }
}
