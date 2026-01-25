package games.battlelore.components;

import core.components.BoardNode;

import java.util.ArrayList;
import java.util.Objects;

public class MapTile extends BoardNode {

    protected int locationX, locationY;
    private ArrayList<Unit> units;

    public enum TileArea {
        NA, left, mid, right;
    }

    public int getLocationX() {
        return locationX;
    }

    public int getLocationY() {
        return locationY;
    }

    public MapTile(int locationX, int locationY, ArrayList<Unit> units) {
        super(-1, "MapTile");
        this.locationX = locationX;
        this.locationY = locationY;
        this.units = units;
    }


    public MapTile() {
        super(-1, "MapTile");
        this.locationX = -1;
        this.locationY = -1;
        this.units = new ArrayList<Unit>();
    }

    //Used by copy constructor only
    private MapTile(int componentID, int locationX, int locationY, ArrayList<Unit> units) {
        super(-1, "MapTile", componentID);
        this.locationX = locationX;
        this.locationY = locationY;
        this.units = units;
    }

    public Boolean SetUnits(ArrayList<Unit> newUnits) {
            units = newUnits;
            return true;
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

    public void AddUnit(Unit unit) {
            units.add(unit);
    }

    public void SetAsOrderable() {
        if (!units.isEmpty()) {
            for (Unit unit : units) {
                unit.SetCanMove(true);
                unit.SetCanAttack(true);
            }
        }
    }

    public void RemoveUnit() {
        if (!units.isEmpty()) {
            units.remove(units.size()-1);
        }
    }

    public ArrayList<Unit> GetUnits() {
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
        ArrayList<Unit> clonedUnits = new ArrayList<>();
        for (Unit i : units) {
            clonedUnits.add((Unit)i.copy());
        }

        MapTile copy = new MapTile(componentID, locationX, locationY, clonedUnits);
        return copy;
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
                && units == mapTile.units
                && type == mapTile.type
                && componentID == mapTile.componentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerId, locationX, locationY, units, type, componentID);
    }
}
