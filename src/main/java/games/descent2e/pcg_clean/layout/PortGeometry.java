package games.descent2e.pcg_clean.layout;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.*;

import java.util.HashSet;
import java.util.Set;

/** Spatial operations on rotated ports, shared by layout validation and loop closure. */
public final class PortGeometry {
    private PortGeometry() {}

    public static boolean aligned(PlacedTile first, GridPoint firstOrigin, int firstPort,
                                  PlacedTile second, GridPoint secondOrigin, int secondPort,
                                  TileCatalog catalog) {
        Port a = catalog.require(first.tileId()).rotate(first.quarterTurns()).port(firstPort);
        Port b = catalog.require(second.tileId()).rotate(second.quarterTurns()).port(secondPort);
        if (a.direction() != b.direction().opposite() || a.cells().size() != b.cells().size()) return false;
        return globalCells(a, firstOrigin).equals(globalCells(b, secondOrigin));
    }

    public static Set<GridPoint> globalCells(Port port, GridPoint origin) {
        Set<GridPoint> result = new HashSet<>();
        port.cells().stream().map(origin::plus).forEach(result::add);
        return result;
    }
}
