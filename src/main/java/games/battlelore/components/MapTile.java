package games.battlelore.components;

import core.components.Component;
//import sun.jvm.hotspot.ui.tree.BooleanTreeNodeAdapter;
//import sun.security.util.Debug;
import utilities.Utils;

import java.util.ArrayList;

public class MapTile extends Component
{
    protected int locationX, locationY;
    private ArrayList<Unit> units;
    private Terrain terrain;

    public enum TileArea
    {
        NA, left, mid, right;
    }

    public int getLocationX()
    {
        return locationX;
    }

    public int getLocationY()
    {
        return locationY;
    }

    public MapTile(int locationX, int locationY, ArrayList<Unit> units, Terrain terrain, int componentID)
    {
        super(Utils.ComponentType.BOARD_NODE, "MapTile", componentID);
        this.locationX = locationX;
        this.locationY = locationY;
        this.units = units;
        this.terrain = terrain;
    }

    public MapTile()
    {
        super(Utils.ComponentType.BOARD_NODE, "MapTile", -1);
        this.locationX = -1;
        this.locationY = -1;
        this.units = new ArrayList<Unit>();
        this.terrain = null;
    }

    public Boolean SetUnits(ArrayList<Unit> newUnits)
    {
        if (terrain == null)
        {
            units = newUnits;
            return true;
        }
        else
        {
            return false;
        }
    }

    public Unit.Faction GetFaction()
    {
        if(!units.isEmpty())
        {
            return units.get(0).faction;
        }

        return Unit.Faction.NA;
    }
    public String GetUnitNames()
    {
        if(units.isEmpty())
        {
            return " ";
        }
        else
        {
            String str = "";

            String unitName = units.get(0).shortName;

            String isOrderable = units.get(0).CanMove() ? "*" : "";

            return units.size() + " " + isOrderable + unitName;
        }
    }

    public Boolean AddUnit(Unit unit)
    {
        if (terrain == null)
        {
            units.add(unit);
            return true;
        }
        else
        {
            return false;
        }
    }

    public Boolean SetAsOrderable()
    {
        if (!units.isEmpty())
        {
            for (int i = 0; i < units.size(); i++)
            {
                units.get(i).SetCanMove(true);
                units.get(i).SetCanAttack(true);

            }
            return true;
        }
        else
        {
            return false;
        }
    }

    public Boolean RemoveUnit()
    {
        if (terrain == null && !units.isEmpty())
        {
            units.remove(units.size()-1);
            return true;
        }
        else
        {
            return false;
        }
    }

    public ArrayList<Unit> GetUnits()
    {
        if (terrain == null && !units.isEmpty())
        {
            return units;
        }
        else
        {
            return null;
        }
    }


    public String GetLocation()
    {
        return locationX + " , " + locationY;
    }

    public Boolean IsInArea(TileArea area)
    {
        if (area == TileArea.left)
        {
            return locationX <= 3;
        }
        if (area == TileArea.mid)
        {
            return ((locationX <= 7) &&
                ((locationY % 2 == 0 && locationX > 3) || (locationY % 2 == 1 && locationX >= 3)));
        }
        if (area == TileArea.right)
        {
            return ((locationY % 2 == 0 && locationX > 7) || (locationY % 2 == 1 && locationX >= 7));
        }


        System.out.println("Checking N/A Area!" );
        return false;

    }

    public Boolean SetTerrain(Terrain newTerrain)
    {
        if (units.isEmpty())
        {
            terrain = newTerrain;
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public MapTile copy()
    {
        ArrayList<Unit> clonedUnits = new ArrayList<Unit>();
        for (Unit i : units)
        {
            clonedUnits.add((Unit)i.copy());
        }

        MapTile copy = new MapTile(locationX, locationY, clonedUnits, terrain, componentID);
        return copy;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof MapTile)) return false;
        if (!super.equals(o)) return false;
        MapTile mapTile = (MapTile) o;
        return ownerId == mapTile.ownerId
                && locationX == mapTile.locationX
                && locationY == mapTile.locationY
                && units == mapTile.units
                && type == mapTile.type
                && terrain == mapTile.terrain
                && componentID == componentID;
    }

}
