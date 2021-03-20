package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMMapTile;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static games.terraformingmars.TMTypes.neighbor_directions;

public class PlaceTile extends TMAction implements IExtendedSequence {
    public boolean onMars = true;
    public String tileName;  // to be used with locations not on mars
    public int mapTileID;
    public TMTypes.Tile tile;
    public HashSet<Integer> legalPositions;  // IDs for TMMapTile components

    boolean placed;
    boolean impossible;

    public PlaceTile(int player, int mapTileID, TMTypes.Tile tile, boolean free) {
        super(player, free);
        this.tile = tile;
        this.legalPositions = new HashSet<>();
    }

    public PlaceTile(int player, TMTypes.Tile tile, HashSet<Integer> legalPositions, boolean free) {
        super(player, free);
        this.tile = tile;
        this.legalPositions = legalPositions;
        this.mapTileID = -1;
    }

    public PlaceTile(int player, String tileName, boolean onMars, boolean free) {  // Place a named tile
        super(player, free);
        this.tileName = tileName;
        this.onMars = onMars;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (mapTileID != -1 && tile != null) {
            TMGameState ggs = (TMGameState) gs;
            TMMapTile mt = (TMMapTile)ggs.getComponentById(mapTileID);
            boolean success = mt.placeTile(tile, ggs) && super.execute(gs);
            // Add money earned from adjacent oceans
            if (success && onMars) {
                int nOceans = isAdjacentToOcean(ggs, new Vector2D(mt.getX(), mt.getY()));
                ggs.getPlayerResources()[player].get(TMTypes.Resource.MegaCredit).increment(((TMGameParameters)gs.getGameParameters()).getnMCGainedOcean());
            }
            return success;
        }
        if (player == -1) player = gs.getCurrentPlayer();
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public PlaceTile copy() {
        PlaceTile copy = new PlaceTile(player, mapTileID, tile, free);
        copy.impossible = impossible;
        copy.placed = placed;
        HashSet<Integer> copyPos = null;
        if (legalPositions != null) {
            copyPos = new HashSet<>(legalPositions);
        }
        copy.legalPositions = copyPos;
        return copy;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (mapTileID == -1) {
            // Need to choose where to place it
            TMGameState gs = (TMGameState) state;
            if (legalPositions != null) {
                for (Integer pos : legalPositions) {
                    TMMapTile mt = (TMMapTile) gs.getComponentById(pos);
                    if (mt != null && mt.getTilePlaced() == null) {
                        actions.add(new PlaceTile(player, pos, tile, true));
                    }
                }
            } else {
                if (onMars) {
                    boolean placedAnyTiles = gs.hasPlacedTile(player);
                    for (int i = 0; i < gs.getBoard().getHeight(); i++) {
                        for (int j = 0; j < gs.getBoard().getWidth(); j++) {
                            TMMapTile mt = gs.getBoard().getElement(j, i);
                            // Check if we can place tile here
                            if (mt != null && mt.getTilePlaced() == null &&
                                    ((tile != null && mt.getTileType() == tile.getRegularLegalTileType()) ||
                                    (mt.getComponentName().equalsIgnoreCase(tileName)))) {
                                // Check adjacency rules
                                if (tile == TMTypes.Tile.Greenery && placedAnyTiles) {
                                    // Can only be placed adjacent to another tile owned by the player, if any
                                    boolean playerTileNeighbour = isAdjacentToPlayerOwnedTiles(gs, new Vector2D(j, i));
                                    if (playerTileNeighbour)
                                        actions.add(new PlaceTile(player, mt.getComponentID(), tile, true));
                                } else {
                                    actions.add(new PlaceTile(player, mt.getComponentID(), tile, true));
                                }
                            }
                        }
                    }
                } else {
                    for (TMMapTile mt: gs.getExtraTiles()) {
                        if (mt.getComponentName().equalsIgnoreCase(tileName)) {
                            actions.add(new PlaceTile(player, mt.getComponentID(), tile, true));
                            break;
                        }
                    }
                }
            }
            if (actions.size() == 0) {
                impossible = true;
                actions.add(new TMAction(player));
            }
        } else {
            impossible = true;
            actions.add(new TMAction(player));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        placed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return placed || impossible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaceTile)) return false;
        if (!super.equals(o)) return false;
        PlaceTile placeTile = (PlaceTile) o;
        return onMars == placeTile.onMars && mapTileID == placeTile.mapTileID && placed == placeTile.placed && impossible == placeTile.impossible && tile == placeTile.tile && Objects.equals(legalPositions, placeTile.legalPositions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), onMars, mapTileID, tile, legalPositions, placed, impossible);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Placing " + tile.name();
    }

    @Override
    public String toString() {
        return "Placing " + tile.name();
    }

    private boolean isAdjacentToPlayerOwnedTiles(TMGameState gs, Vector2D cell) {
        List<Vector2D> neighbours = getNeighbours(cell);
        boolean playerTileNeighbour = false;
        for (Vector2D n: neighbours) {
            TMMapTile other = gs.getBoard().getElement(n.getX(), n.getY());
            if (other.getOwner() == player) {
                playerTileNeighbour = true;
                break;
            }
        }
        return playerTileNeighbour;
    }

    private int isAdjacentToOcean(TMGameState gs, Vector2D cell) {
        List<Vector2D> neighbours = getNeighbours(cell);
        int count = 0;
        for (Vector2D n: neighbours) {
            TMMapTile other = gs.getBoard().getElement(n.getX(), n.getY());
            if (other.getTilePlaced() == TMTypes.Tile.Ocean) {
                count++;
            }
        }
        return count;
    }

    public static List<Vector2D> getNeighbours(Vector2D cell) {
        ArrayList<Vector2D> neighbors = new ArrayList<>();
        int parity = cell.getY() % 2;
        for (Vector2D v: neighbor_directions[parity]) {
            neighbors.add(cell.add(v));
        }
        return neighbors;
    }
}
