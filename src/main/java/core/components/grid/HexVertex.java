package core.components.grid;

/**
 * A corner where up to three hexes meet (see HexCoord for the numbering of corners). Each corner is shared by three
 * hexes, so it is stored as corner 0 or 1 of one of them, and the same point is always an equal HexVertex.
 */
public record HexVertex(HexCoord hex, int corner) {

    public HexVertex {
        // corner c of a hex is also corner c + 2 of the hex in direction c - 1, and corner c + 4 of the one in
        // direction c
        if (corner == 2 || corner == 3) {
            hex = hex.neighbour(corner);
            corner = (corner + 4) % 6;
        } else if (corner == 4 || corner == 5) {
            hex = hex.neighbour(corner - 1);
            corner = (corner + 2) % 6;
        }
    }
}
