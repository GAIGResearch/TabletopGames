package games.battlelore.components;

import core.components.BoardNode;
import core.components.grid.HexCoord;

import java.util.List;
import java.util.Objects;

/**
 * A hex of the board and the units on it. Immutable, as the values of a SparseGrid must be: when the units change,
 * the tile is replaced on the board by a new one with the same componentID.
 */
public class MapTile extends BoardNode {

    protected final int locationX, locationY;
    private final List<Unit> units;

    public enum TileArea {
        NA, left, mid, right;
    }

    public int getLocationX() {
        return locationX;
    }

    public int getLocationY() {
        return locationY;
    }

    /**
     * The hex of the tile on the board, whose odd rows are shifted right.
     */
    public HexCoord getHex() {
        return HexCoord.fromOffset(locationX, locationY, HexCoord.Offset.ODD_R);
    }

    public MapTile(int locationX, int locationY, List<Unit> units) {
        super(-1, "MapTile");
        this.locationX = locationX;
        this.locationY = locationY;
        this.units = List.copyOf(units);
    }

    private MapTile(int componentID, int locationX, int locationY, List<Unit> units) {
        super(-1, "MapTile", componentID);
        this.locationX = locationX;
        this.locationY = locationY;
        this.units = List.copyOf(units);
    }

    /**
     * This tile with the units on it.
     */
    public MapTile withUnits(List<Unit> newUnits) {
        return new MapTile(componentID, locationX, locationY, newUnits);
    }

    public Unit.Faction GetFaction() {
        if (!units.isEmpty()) {
            return units.get(0).faction;
        }
        return Unit.Faction.NA;
    }

    public String GetUnitNames() {
        if (units.isEmpty()) {
            return " ";
        }
        else {
            String str = "";
            String unitName = units.get(0).shortName;
            String isOrderable = units.get(0).CanMove() ? "*" : "";

            return units.size() + " " + isOrderable + unitName;
        }
    }

    /**
     * The units on the tile, or null if there are none.
     */
    public List<Unit> GetUnits() {
        if (!units.isEmpty()) {
            return units;
        }
        else {
            return null;
        }
    }

    public String GetLocation() {
        return locationX + " , " + locationY;
    }

    public Boolean IsInArea(TileArea area) {
        if (area == TileArea.left) {
            return locationX <= 3;
        }
        if (area == TileArea.mid) {
            return ((locationX <= 7) &&
                ((locationY % 2 == 0 && locationX > 3) || (locationY % 2 == 1 && locationX >= 3)));
        }
        if (area == TileArea.right) {
            return ((locationY % 2 == 0 && locationX > 7) || (locationY % 2 == 1 && locationX >= 7));
        }

        System.out.println("Checking N/A Area!" );
        return false;
    }

    @Override
    public MapTile copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapTile)) return false;
        if (!super.equals(o)) return false;
        MapTile mapTile = (MapTile) o;
        return ownerId == mapTile.ownerId
                && locationX == mapTile.locationX
                && locationY == mapTile.locationY
                && units.equals(mapTile.units)
                && type == mapTile.type
                && componentID == mapTile.componentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerId, locationX, locationY, units, type, componentID);
    }
}
