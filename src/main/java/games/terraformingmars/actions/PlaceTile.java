package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.requirements.AdjacencyRequirement;
import utilities.Group;
import utilities.Vector2D;

import java.util.*;

import static games.terraformingmars.TMTypes.Tile.City;
import static games.terraformingmars.TMTypes.Tile.Greenery;
import static games.terraformingmars.TMTypes.neighbor_directions;

public class PlaceTile extends TMAction implements IExtendedSequence {
    public boolean respectingAdjacency = true;
    public boolean onMars = true;
    public String tileName;  // to be used with locations not on mars
    public int mapTileID;
    public TMTypes.Tile tile;
    public TMTypes.MapTileType mapType;
    public HashSet<Integer> legalPositions;  // IDs for TMMapTile components

    public TMTypes.Resource[] resourcesGainedRestriction;
    public boolean volcanicRestriction;

    public AdjacencyRequirement adjacencyRequirement;

    public boolean removeResourcesAdjacentOwner;
    public int removeResourcesAmount;
    public TMTypes.Resource removeResourcesRes;
    public boolean removeResourcesProd;

    boolean placed;
    boolean impossible;

    public PlaceTile() { super(); } // This is needed for JSON Deserializer

    public PlaceTile(int player, int mapTileID, TMTypes.Tile tile,
                     boolean respectingAdjacency, boolean onMars, String tileName, TMTypes.MapTileType mapType,
                     HashSet<Integer> legalPositions,
                     TMTypes.Resource[] resourcesGainedRestriction, boolean volcanicRestriction,
                     AdjacencyRequirement adjacencyRequirement, boolean free) {
        // Copy constructor, in extended sequence too
        super(player, free);
        this.respectingAdjacency = respectingAdjacency;
        this.onMars = onMars;
        this.tileName = tileName;
        this.mapTileID = mapTileID;
        this.tile = tile;
        this.mapType = mapType;
        this.legalPositions = legalPositions;
        this.resourcesGainedRestriction = resourcesGainedRestriction;
        this.volcanicRestriction = volcanicRestriction;
        this.adjacencyRequirement = adjacencyRequirement;
        setTile(tile, tileName);
    }

    public PlaceTile(int player, TMTypes.Tile tile, TMTypes.MapTileType mapTile, boolean free) {
        // Used in parsing
        super(player, free);
        setTile(tile, null);
        this.mapType = mapTile;
        this.mapTileID = -1;
    }

    public PlaceTile(int player, TMTypes.Tile tile, TMTypes.Resource[] resourcesGainedRestriction, boolean free) {
        // used in parsing
        super(player, free);
        setTile(tile, null);
        this.resourcesGainedRestriction = resourcesGainedRestriction;
        this.mapTileID = -1;
    }

    public PlaceTile(int player, TMTypes.Tile tile, boolean volcanicRestriction, boolean free) {
        // Used in parsing
        super(player, free);
        setTile(tile, null);
        this.volcanicRestriction = volcanicRestriction;
        this.mapTileID = -1;
    }

    public PlaceTile(int player, TMTypes.Tile tile, String tileName, boolean onMars, boolean free) {  // Place a named tile
        // Used in parsing
        super(player, free);
        setTile(tile, tileName);
        this.tileName = tileName;
        this.onMars = onMars;
        this.mapTileID = -1;
    }

    public PlaceTile(TMTypes.StandardProject standardProject, int cost, int player, TMTypes.Tile tile, TMTypes.MapTileType mapTile) {
        super(standardProject, player, false);
        setTile(tile, null);
        this.mapType = mapTile;
        this.mapTileID = -1;
        this.setActionCost(TMTypes.Resource.MegaCredit, cost, -1);
    }

    public PlaceTile(TMTypes.BasicResourceAction basicResourceAction, int cost, int player, TMTypes.Tile tile, TMTypes.MapTileType mapTile) {
        super(basicResourceAction, player, false);
        setTile(tile, null);
        this.mapType = mapTile;
        this.mapTileID = -1;
        this.setActionCost(TMTypes.Resource.Plant, cost, -1);
    }

    private void setTile(TMTypes.Tile tile, String name) {
        this.tile = tile;
        if (tile == City && name == null) {
            // Cities can't be adjacent to other cities, unless named
            adjacencyRequirement = new AdjacencyRequirement(new HashMap<TMTypes.Tile, Integer>() {{ put(City, 1);}});
            adjacencyRequirement.reversed = true;
        } else if (tile == Greenery) {
            // Greeneries must be adjacent to other owned tiles
            adjacencyRequirement = new AdjacencyRequirement();
            adjacencyRequirement.owned = true;
        }
    }

    @Override
    public boolean _execute(TMGameState gs) {
        if (mapTileID != -1 && tile != null) {
            TMMapTile mt = (TMMapTile)gs.getComponentById(mapTileID);
            boolean success = mt.placeTile(tile, gs);
            if (success && onMars) {
                if (getCardID() != -1) {
                    // Save location of tile placed on card
                    TMCard card = (TMCard) gs.getComponentById(getCardID());
                    card.mapTileIDTilePlaced = mt.getComponentID();
                }
                if (player < 0 || player >= gs.getNPlayers()) return super._execute(gs);

                // Add money earned from adjacent oceans
                int nOceans = nAdjacentTiles(gs, mt, TMTypes.Tile.Ocean);
                gs.getPlayerResources()[player].get(TMTypes.Resource.MegaCredit).increment(nOceans * ((TMGameParameters) gs.getGameParameters()).getnMCGainedOcean());

                if (resourcesGainedRestriction != null) {
                    // Production of each resource type gained increased by 1
                    TMTypes.Resource[] gained = mt.getResources();
                    HashSet<TMTypes.Resource> typesAdded = new HashSet<>();
                    for (TMTypes.Resource r : gained) {
                        if (contains(resourcesGainedRestriction, r) && !typesAdded.contains(r)) {
                            gs.getPlayerProduction()[player].get(r).increment(1);
                            typesAdded.add(r);
                        }
                    }
                }
                if (removeResourcesAdjacentOwner) {
                    HashSet<Integer> adjacentOwners = new HashSet<>();
                    List<Vector2D> neighbours = getNeighbours(new Vector2D(mt.getX(), mt.getY()));
                    for (Vector2D n : neighbours) {
                        TMMapTile other = (TMMapTile) gs.getBoard().getElement(n.getX(), n.getY());
                        if (other != null && other.getTilePlaced() != null) {
                            adjacentOwners.add(other.getOwnerId());
                        }
                    }
                    if (adjacentOwners.size() > 0) {
                        ModifyPlayerResource mpr = new ModifyPlayerResource(player, -removeResourcesAmount, removeResourcesRes, removeResourcesProd);
                        mpr.targetPlayer = -2;
                        mpr.targetPlayerOptions = adjacentOwners;
                        mpr.execute(gs);
                    }
                }
            }
            return success && super._execute(gs);
        }
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public PlaceTile _copy() {
        PlaceTile copy = new PlaceTile(player, mapTileID, tile, respectingAdjacency, onMars, tileName, mapType,
                legalPositions, resourcesGainedRestriction, volcanicRestriction, adjacencyRequirement, freeActionPoint);
        copy.impossible = impossible;
        copy.placed = placed;
        HashSet<Integer> copyPos = null;
        if (legalPositions != null) {
            copyPos = new HashSet<>(legalPositions);
        }
        copy.legalPositions = copyPos;
        if (adjacencyRequirement != null) {
            copy.adjacencyRequirement = adjacencyRequirement.copy();
        }
        if (resourcesGainedRestriction != null) {
            copy.resourcesGainedRestriction = resourcesGainedRestriction.clone();
        }
        copy.removeResourcesAdjacentOwner = removeResourcesAdjacentOwner;
        copy.removeResourcesAmount = removeResourcesAmount;
        copy.removeResourcesRes = removeResourcesRes;
        copy.removeResourcesProd = removeResourcesProd;
        return copy;
    }

    @Override
    public PlaceTile copy() {
        return (PlaceTile) super.copy();
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
                        actions.add(new PlaceTile(player, pos, tile, respectingAdjacency, onMars, tileName, mapType,
                                legalPositions, resourcesGainedRestriction, volcanicRestriction, adjacencyRequirement, true));
                    }
                }
            } else {
                if (onMars) {
                    for (int i = 0; i < gs.getBoard().getHeight(); i++) {
                        for (int j = 0; j < gs.getBoard().getWidth(); j++) {
                            TMMapTile mt = (TMMapTile) gs.getBoard().getElement(j, i);

                            // Check if we can place tile here
                            if (mt == null || mt.getTilePlaced() != null) continue;
                            if (mt.isReserved() && mt.getReserved() != player) continue;
                            if (tileName != null && !mt.getComponentName().equalsIgnoreCase(tileName)) continue;
                            if (mapType != null && mt.getTileType() != mapType) continue;
                            if (volcanicRestriction && !mt.isVolcanic()) continue;
                            if (resourcesGainedRestriction != null && !contains(mt.getResources(), resourcesGainedRestriction)) continue;

                            // Check placement rules
                            if (respectingAdjacency && adjacencyRequirement != null) {
                                if (adjacencyRequirement.testCondition(new Group<>(gs, mt, player))) {
                                    actions.add(new PlaceTile(player, mt.getComponentID(), tile, respectingAdjacency, onMars, tileName, mapType,
                                            legalPositions, resourcesGainedRestriction, volcanicRestriction, adjacencyRequirement, true));
                                } else {
                                    int a = 0;
                                }
                            } else {
                                actions.add(new PlaceTile(player, mt.getComponentID(), tile, respectingAdjacency, onMars, tileName, mapType,
                                        legalPositions, resourcesGainedRestriction, volcanicRestriction, adjacencyRequirement, true));
                            }

                        }
                    }
                } else {
                    for (TMMapTile mt: gs.getExtraTiles()) {
                        if (mt.getComponentName().equalsIgnoreCase(tileName)) {
                            actions.add(new PlaceTile(player, mt.getComponentID(), tile, respectingAdjacency, onMars, tileName, mapType,
                                    legalPositions, resourcesGainedRestriction, volcanicRestriction, adjacencyRequirement, true));
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
    public void _afterAction(AbstractGameState state, AbstractAction action) {
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
        return respectingAdjacency == placeTile.respectingAdjacency && onMars == placeTile.onMars && mapTileID == placeTile.mapTileID && volcanicRestriction == placeTile.volcanicRestriction && removeResourcesAdjacentOwner == placeTile.removeResourcesAdjacentOwner && removeResourcesAmount == placeTile.removeResourcesAmount && removeResourcesProd == placeTile.removeResourcesProd && placed == placeTile.placed && impossible == placeTile.impossible && Objects.equals(tileName, placeTile.tileName) && tile == placeTile.tile && mapType == placeTile.mapType && Objects.equals(legalPositions, placeTile.legalPositions) && Arrays.equals(resourcesGainedRestriction, placeTile.resourcesGainedRestriction) && Objects.equals(adjacencyRequirement, placeTile.adjacencyRequirement) && removeResourcesRes == placeTile.removeResourcesRes;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), respectingAdjacency, onMars, tileName, mapTileID, tile, mapType, legalPositions, volcanicRestriction, adjacencyRequirement, removeResourcesAdjacentOwner, removeResourcesAmount, removeResourcesRes, removeResourcesProd, placed, impossible);
        result = 31 * result + Arrays.hashCode(resourcesGainedRestriction);
        return result;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        TMMapTile mt = (TMMapTile) gameState.getComponentById(mapTileID);
        if (mt != null) {
            return "Place " + (tileName != null? tileName : (tile.name() + " on " + mt.getTileType()));  // TODO restrictions and stuff
        } else {
            return "Place " + (tileName != null? tileName : tile.name());
        }
    }

    @Override
    public String toString() {
        return "Place " + tile.name();
    }

    public static boolean isAdjacentToPlayerOwnedTiles(TMGameState gs, TMMapTile mt, int player) {
        boolean placedAnyTiles = gs.hasPlacedTile(player);
        if (placedAnyTiles) {
            boolean playerTileNeighbour = false;
            List<Vector2D> neighbours = getNeighbours(new Vector2D(mt.getX(), mt.getY()));
            for (Vector2D n : neighbours) {
                TMMapTile other = (TMMapTile) gs.getBoard().getElement(n.getX(), n.getY());
                if (other != null && other.getOwnerId() == player) {
                    playerTileNeighbour = true;
                    break;
                }
            }
            return playerTileNeighbour;
        }
        return true;
    }

    public static boolean isAdjacentToAny(TMGameState gs, TMMapTile mt) {
        boolean placedAnyTiles = gs.anyTilesPlaced();
        if (placedAnyTiles) {
            List<Vector2D> neighbours = getNeighbours(new Vector2D(mt.getX(), mt.getY()));
            int count = 0;
            for (Vector2D n : neighbours) {
                TMMapTile other = (TMMapTile) gs.getBoard().getElement(n.getX(), n.getY());
                if (other != null && other.getTilePlaced() != null) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public static int nAdjacentTiles(TMGameState gs, TMMapTile mt) {
        boolean placedAnyTiles = gs.anyTilesPlaced();
        int count = 0;
        if (placedAnyTiles) {
            List<Vector2D> neighbours = getNeighbours(new Vector2D(mt.getX(), mt.getY()));
            for (Vector2D n : neighbours) {
                TMMapTile other = (TMMapTile) gs.getBoard().getElement(n.getX(), n.getY());
                if (other != null && other.getTilePlaced() != null) {
                    count++;
                }
            }
        }
        return count;
    }

    public static boolean isAdjacentToTile(TMGameState gs, TMMapTile mt, TMTypes.Tile t) {
        boolean placedAnyTiles = gs.anyTilesPlaced();
        if (placedAnyTiles) {
            List<Vector2D> neighbours = getNeighbours(new Vector2D(mt.getX(), mt.getY()));
            int count = 0;
            for (Vector2D n : neighbours) {
                TMMapTile other = (TMMapTile) gs.getBoard().getElement(n.getX(), n.getY());
                if (other != null && other.getTilePlaced() == t) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public static int nAdjacentTiles(TMGameState gs, TMMapTile mt, TMTypes.Tile t) {
        boolean placedAnyTiles = gs.anyTilesPlaced();
        int count = 0;
        if (placedAnyTiles) {
            List<Vector2D> neighbours = getNeighbours(new Vector2D(mt.getX(), mt.getY()));
            for (Vector2D n : neighbours) {
                TMMapTile other = (TMMapTile) gs.getBoard().getElement(n.getX(), n.getY());
                if (other != null && other.getTilePlaced() == t) {
                    count++;
                }
            }
        }
        return count;
    }

    public static List<Vector2D> getNeighbours(Vector2D cell) {
        ArrayList<Vector2D> neighbors = new ArrayList<>();
        int parity = Math.abs(cell.getY() % 2);
        for (Vector2D v: neighbor_directions[parity]) {
            neighbors.add(cell.add(v));
        }
        return neighbors;
    }

    public static boolean contains(TMTypes.Resource[] array, TMTypes.Resource[] objects) {
        for (TMTypes.Resource r1: array) {
            for (TMTypes.Resource r2: objects) {
                if (r1 == r2) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean contains(TMTypes.Resource[] array, TMTypes.Resource r2) {
        for (TMTypes.Resource r1: array) {
            if (r1 == r2) {
                return true;
            }
        }
        return false;
    }

}
