package games.terraformingmars.components;

import core.components.Component;
import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.rules.effects.Bonus;

import static utilities.Utils.ComponentType.BOARD_NODE;

public class TMMapTile extends Component {
    int owner = -1;
    TMTypes.Tile tilePlaced;

    TMTypes.MapTileType type;
    TMTypes.Resource[] resources;

    public TMMapTile() {
        super(BOARD_NODE, "Tile");
    }

    protected TMMapTile(int componentID) {
        super(BOARD_NODE, componentID);
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

    public int getOwner() {
        return owner;
    }

    private void setTilePlaced(TMTypes.Tile which, TMGameState gs) {
        tilePlaced = which;
        int player = gs.getCurrentPlayer();

        // Owner is current player
        if (which != TMTypes.Tile.Ocean) {
            owner = player;
        }

        gs.getTilesPlaced()[player].get(which).increment(1);

        // Current player gets resources
        for (TMTypes.Resource res: resources) {
            gs.getPlayerResources()[player].get(res).increment(1);
            gs.getPlayerResourceIncreaseGen()[player].put(res, true);
        }
    }

    public boolean placeTile(TMTypes.Tile which, TMGameState gs) {
        int player = gs.getCurrentPlayer();
        if (tilePlaced == null) {
            if (which == TMTypes.Tile.Ocean) {
                // If ocean, decrease number of tiles available and increase TR
                Counter oceanTiles = TMGameState.stringToGPCounter(gs, "oceanTiles");
                if (oceanTiles != null) {
                    boolean succeeded = oceanTiles.increment(1);
                    if (succeeded) {
                        gs.getPlayerResources()[player].get(TMTypes.Resource.TR).increment(1);
                        gs.getPlayerResourceIncreaseGen()[player].put(TMTypes.Resource.TR, true);
                        setTilePlaced(which, gs);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else if (which == TMTypes.Tile.Greenery) {
                // If greenery, increase oxygen and TR
                Counter oxygen = TMGameState.stringToGPCounter(gs, "oxygen");
                if (oxygen != null) {
                    boolean succeeded = oxygen.increment(1);
                    if (succeeded) {
                        gs.getPlayerResources()[player].get(TMTypes.Resource.TR).increment(1);
                        gs.getPlayerResourceIncreaseGen()[player].put(TMTypes.Resource.TR, true);
                    }
                }
                setTilePlaced(which, gs);
            } else {
                // Just place
                setTilePlaced(which, gs);
            }

            // Params might have increased, check bonuses
            for (Bonus b: gs.getBonuses()) {
                b.checkBonus(gs);
            }

            return true;
        }
        return false;
    }

    public void removeTile() {
        owner = -1;
        tilePlaced = null;
    }

    @Override
    public Component copy() {
        return new TMMapTile(componentID);
    }
}
