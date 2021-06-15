package games.battlelore.components;

import core.components.Component;
//import sun.jvm.hotspot.ui.tree.BooleanTreeNodeAdapter;
//import sun.security.util.Debug;
import utilities.Utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class MapTile extends Component
{
    protected int locationX, locationY;
    private ArrayList<Unit> units;
    private Terrain terrain;

    public enum TileArea
    {
        NA, left, mid, right;
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

    public String GetLocation()
    {
        return locationX + " , " + locationY;
    }

    public Boolean IsInArea(TileArea area)
    {
        if (area == TileArea.left)
        {
            return locationY <= 3;
        }
        else if (area == TileArea.mid)
        {
            return ((locationY <= 7) &&
                ((locationX % 2 == 0 && locationY > 3) || (locationX % 2 == 1 && locationY >= 3)));
        }
        else if (area == TileArea.right)
        {
            return ((locationX % 2 == 0 && locationY > 7) || (locationX % 2 == 1 && locationY >= 7));
        }
        else
        {
            System.out.println("Checking N/A Area!" );
            return false;
        }
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
        MapTile copy = new MapTile(locationX, locationY, units, terrain, componentID);
        copyComponentTo(copy);
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
