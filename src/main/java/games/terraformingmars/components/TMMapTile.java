package games.terraformingmars.components;

import core.components.BoardNode;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.ModifyGlobalParameter;
import utilities.Utils;

import java.util.Arrays;
import java.util.Objects;

public class TMMapTile extends BoardNode {
    int x, y;
    TMTypes.Tile tilePlaced;

    TMTypes.MapTileType type;
    TMTypes.Resource[] resources;

    boolean volcanic;
    int reserved = -1;

    public TMMapTile(int x, int y) {
        super(-1, "Tile");
        this.x = x;
        this.y = y;
    }

    protected TMMapTile(int x, int y, int componentID) {
        super(-1, "Tile", componentID);
        this.x = x;
        this.y = y;
    }

    public boolean isReserved() {
        return reserved != -1;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public boolean isVolcanic() {
        return volcanic;
    }

    public void setVolcanic(boolean volcanic) {
        this.volcanic = volcanic;
    }

    public void setType(TMTypes.MapTileType type) {
        this.type = type;
    }

    public void setResources(TMTypes.Resource[] resources) {
        this.resources = resources;
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

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setTilePlaced(TMTypes.Tile which, TMGameState gs) {
        tilePlaced = which;
        int player = gs.getCurrentPlayer();

        // Owner is current player
        if (which.canBeOwned()) {
            ownerId = player;
        }

        if (player >= 0 && player < gs.getNPlayers()) {
            gs.getPlayerTilesPlaced()[player].get(which).increment(1);

            // Current player gets resources
            for (TMTypes.Resource res : resources) {
                gs.getPlayerResources()[player].get(res).increment(1);
                gs.getPlayerResourceIncreaseGen()[player].put(res, true);
            }
        }
    }

    public boolean placeTile(TMTypes.Tile which, TMGameState gs) {
        int player = gs.getCurrentPlayer();
        if (tilePlaced == null) {
            TMTypes.GlobalParameter gp = which.getGlobalParameterToIncrease();
            setTilePlaced(which, gs);
            if (gp != null) {
                // Increase global parameter
                return new ModifyGlobalParameter(gp, 1, true).execute(gs);
            }
            return true;
        }
        return false;
    }

    public void removeTile() {
        ownerId = -1;
        tilePlaced = null;
    }

    @Override
    public TMMapTile copy() {
        TMMapTile copy = new TMMapTile(x, y, componentID);
        copyComponentTo(copy);
        copy.tilePlaced = tilePlaced;
        copy.type = type;
        copy.resources = resources.clone();
        copy.volcanic = volcanic;
        copy.reserved = reserved;
        return copy;
    }

    public static TMMapTile parseMapTile(String s) {
        return parseMapTile(s, -1, -1);
    }

    public static TMMapTile parseMapTile(String s, int x, int y) {
        if (s.equals("0")) return null;

        TMMapTile mt = new TMMapTile(x, y);

        String[] split = s.split(":");

        // First element is tile type
        TMTypes.MapTileType type = Utils.searchEnum(TMTypes.MapTileType.class, split[0]);
        if (type == null) {
            type = TMTypes.MapTileType.City;
            mt.setComponentName(split[0]); // Keep city name
        } else if (type == TMTypes.MapTileType.Volcanic) {
            type = TMTypes.MapTileType.Ground;
            mt.setVolcanic(true);
        }
        mt.setType(type);

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
        mt.setResources(resources);

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
