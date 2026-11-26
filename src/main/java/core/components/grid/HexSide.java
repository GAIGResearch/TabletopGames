package core.components.grid;

/**
 * The side between two adjacent hexes, given by a hex and the direction it faces. It is stored facing E, SE or SW,
 * so the same side given from either hex is an equal HexSide.
 */
public record HexSide(HexCoord hex, int direction) {

    public HexSide {
        if (direction >= 3) {
            hex = hex.neighbour(direction);
            direction -= 3;
        }
    }
}
