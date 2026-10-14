package games.terraformingmars.components;

import core.components.BoardNode;
import games.terraformingmars.TMTypes;
import utilities.Utils;

import java.util.Arrays;
import java.util.Objects;

/**
 * A space for a tile, on Mars or off it (at x = y = -1). Immutable, as the values of a SparseGrid must be: when a
 * tile is placed or the space reserved, it is replaced by a new one with the same componentID
 * (see TMGameState.setMapTile).
 */
public class TMMapTile extends BoardNode {
    final int x, y;
    final TMTypes.Tile tilePlaced;

    final TMTypes.MapTileType type;
    final TMTypes.Resource[] resources;

    final boolean volcanic;
    final int reserved;

    // a new space, with no tile and not reserved
    private TMMapTile(int x, int y, TMTypes.MapTileType type, TMTypes.Resource[] resources, boolean volcanic) {
        super(-1, "Tile");
        this.x = x;
        this.y = y;
        this.tilePlaced = null;
        this.type = type;
        this.resources = resources;
        this.volcanic = volcanic;
        this.reserved = -1;
    }

    // the space with a tile and reservation
    private TMMapTile(TMMapTile orig, TMTypes.Tile tilePlaced, int reserved) {
        super(-1, "Tile", orig.componentID);
        orig.copyComponentTo(this);
        this.x = orig.x;
        this.y = orig.y;
        this.tilePlaced = tilePlaced;
        this.type = orig.type;
        this.resources = orig.resources;
        this.volcanic = orig.volcanic;
        this.reserved = reserved;
    }

    public boolean isReserved() {
        return reserved != -1;
    }

    public int getReserved() {
        return reserved;
    }

    /**
     * This space reserved for the player.
     */
    public TMMapTile withReserved(int player) {
        return new TMMapTile(this, tilePlaced, player);
    }

    public boolean isVolcanic() {
        return volcanic;
    }

    public TMTypes.MapTileType getTileType() {
        return type;
    }

    public TMTypes.Resource[] getResources() {
        return resources;
    }

    public TMTypes.Tile getTilePlaced() {
        return tilePlaced;
    }

    /**
     * This space with the tile placed by the player, who owns it if it can be owned.
     */
    public TMMapTile withTilePlaced(TMTypes.Tile which, int player) {
        TMMapTile retValue = new TMMapTile(this, which, reserved);
        if (which.canBeOwned()) {
            retValue.ownerId = player;
        }
        return retValue;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public TMMapTile copy() {
        return this;
    }

    public static TMMapTile parseMapTile(String s) {
        return parseMapTile(s, -1, -1);
    }

    public static TMMapTile parseMapTile(String s, int x, int y) {
        if (s.equals("0")) return null;

        String[] split = s.split(":");

        // First element is tile type
        TMTypes.MapTileType type = Utils.searchEnum(TMTypes.MapTileType.class, split[0]);
        String cityName = null;
        boolean volcanic = false;
        if (type == null) {
            type = TMTypes.MapTileType.City;
            cityName = split[0];
        } else if (type == TMTypes.MapTileType.Volcanic) {
            type = TMTypes.MapTileType.Ground;
            volcanic = true;
        }

        // The rest are resources existing here
        int nResources = split.length-1;
        TMTypes.Resource[] resources = new TMTypes.Resource[nResources];
        for (int i = 1; i < split.length; i++) {
            TMTypes.Resource res = Utils.searchEnum(TMTypes.Resource.class, split[i]);
            if (res != null) {
                resources[i - 1] = res;
            } else {
                // TODO: Ocean (place tile), MegaCredit/-6 (reduce MC by 6)
            }
        }

        TMMapTile mt = new TMMapTile(x, y, type, resources, volcanic);
        if (cityName != null) {
            mt.setComponentName(cityName); // Keep city name
        }
        return mt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMMapTile)) return false;
        if (!super.equals(o)) return false;
        TMMapTile tmMapTile = (TMMapTile) o;
        return ownerId == tmMapTile.ownerId && x == tmMapTile.x && y == tmMapTile.y && volcanic == tmMapTile.volcanic && reserved == tmMapTile.reserved && tilePlaced == tmMapTile.tilePlaced && type == tmMapTile.type && Arrays.equals(resources, tmMapTile.resources);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), ownerId, x, y, tilePlaced, type, volcanic, reserved);
        result = 31 * result + Arrays.hashCode(resources);
        return result;
    }
}
