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

    public int cardID = -1;  // Card used to place this tile

    boolean placed;
    boolean impossible;

    public PlaceTile(int player, int mapTileID, TMTypes.Tile tile, boolean free) {
        super(player, free);
        setTile(tile, null);
        this.legalPositions = new HashSet<>();
        this.mapTileID = mapTileID;
    }

    public PlaceTile(int player, TMTypes.Tile tile, HashSet<Integer> legalPositions, boolean free) {
        super(player, free);
        setTile(tile, null);
        this.legalPositions = legalPositions;
        this.mapTileID = -1;
    }

    public PlaceTile(int player, TMTypes.Tile tile, TMTypes.MapTileType mapTile, boolean free) {
        super(player, free);
        setTile(tile, null);
        this.mapType = mapTile;
        this.mapTileID = -1;
    }

    public PlaceTile(int player, TMTypes.Tile tile, TMTypes.Resource[] resourcesGainedRestriction, boolean free) {
        super(player, free);
        setTile(tile, null);
        this.resourcesGainedRestriction = resourcesGainedRestriction;
        this.mapTileID = -1;
    }

    public PlaceTile(int player, TMTypes.Tile tile, boolean volcanicRestriction, boolean free) {
        super(player, free);
        setTile(tile, null);
        this.volcanicRestriction = volcanicRestriction;
        this.mapTileID = -1;
    }

    public PlaceTile(int player, TMTypes.Tile tile, String tileName, boolean onMars, boolean free) {  // Place a named tile
        super(player, free);
        setTile(tile, tileName);
        this.tileName = tileName;
        this.onMars = onMars;
        this.mapTileID = -1;
    }

    private void setTile(TMTypes.Tile tile, String name) {
        this.tile = tile;
        if (tile == City && name != null) {
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
    public boolean execute(AbstractGameState gs) {
        if (mapTileID != -1 && tile != null) {
            TMGameState ggs = (TMGameState) gs;
            TMMapTile mt = (TMMapTile)ggs.getComponentById(mapTileID);
            boolean success = mt.placeTile(tile, ggs) && super.execute(gs);
            // Add money earned from adjacent oceans
            if (success && onMars) {
                int nOceans = isAdjacentToTile(ggs, mt, TMTypes.Tile.Ocean);
                ggs.getPlayerResources()[player].get(TMTypes.Resource.MegaCredit).increment(nOceans * ((TMGameParameters) gs.getGameParameters()).getnMCGainedOcean());
                if (resourcesGainedRestriction != null) {
                    // Production of each resource type gained increased by 1
                    TMTypes.Resource[] gained = mt.getResources();
                    HashSet<TMTypes.Resource> typesAdded = new HashSet<>();
                    for (TMTypes.Resource r : gained) {
                        if (contains(resourcesGainedRestriction, r) && !typesAdded.contains(r)) {
                            ggs.getPlayerProduction()[player].get(r).increment(1);
                            typesAdded.add(r);
                        }
                    }
                }
                if (cardID != -1) {
                    // Save location of tile placed on card
                    TMCard card = (TMCard) gs.getComponentById(cardID);
                    card.mapTileIDTilePlaced = mt.getComponentID();
                }
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
        copy.resourcesGainedRestriction = resourcesGainedRestriction.clone();
        copy.volcanicRestriction = volcanicRestriction;
        copy.tileName = tileName;
        copy.onMars = onMars;
        copy.mapType = mapType;
        copy.respectingAdjacency = respectingAdjacency;
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
                    for (int i = 0; i < gs.getBoard().getHeight(); i++) {
                        for (int j = 0; j < gs.getBoard().getWidth(); j++) {
                            TMMapTile mt = gs.getBoard().getElement(j, i);
                            // Check if we can place tile here
                            if (mt != null && mt.getTilePlaced() == null && (!mt.isReserved() || mt.getReserved() == player) &&
                                    ((mapType == null || mt.getTileType() == mapType) ||
                                    (mt.getComponentName().equalsIgnoreCase(tileName))) &&
                                    (!volcanicRestriction || mt.isVolcanic()) &&
                                    (resourcesGainedRestriction == null || contains(mt.getResources(), resourcesGainedRestriction))) {
                                // Check placement rules
                                if (respectingAdjacency && adjacencyRequirement != null) {
                                    if (adjacencyRequirement.testCondition(new Group<>(gs, mt, player))) {
                                        actions.add(new PlaceTile(player, mt.getComponentID(), tile, true));
                                    }
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
        return respectingAdjacency == placeTile.respectingAdjacency && onMars == placeTile.onMars && mapTileID == placeTile.mapTileID && volcanicRestriction == placeTile.volcanicRestriction && placed == placeTile.placed && impossible == placeTile.impossible && Objects.equals(tileName, placeTile.tileName) && tile == placeTile.tile && mapType == placeTile.mapType && Objects.equals(legalPositions, placeTile.legalPositions) && Arrays.equals(resourcesGainedRestriction, placeTile.resourcesGainedRestriction);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), respectingAdjacency, onMars, tileName, mapTileID, tile, mapType, legalPositions, volcanicRestriction, placed, impossible);
        result = 31 * result + Arrays.hashCode(resourcesGainedRestriction);
        return result;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Placing " + tile.name();
    }

    @Override
    public String toString() {
        return "Placing " + tile.name();
    }

    public static boolean isAdjacentToPlayerOwnedTiles(TMGameState gs, TMMapTile mt, int player) {
        boolean playerTileNeighbour = false;
        boolean placedAnyTiles = gs.hasPlacedTile(player);
        if (placedAnyTiles) {
            List<Vector2D> neighbours = getNeighbours(new Vector2D(mt.getX(), mt.getY()));
            for (Vector2D n : neighbours) {
                TMMapTile other = gs.getBoard().getElement(n.getX(), n.getY());
                if (other != null && other.getOwner() == player) {
                    playerTileNeighbour = true;
                    break;
                }
            }
        }
        return playerTileNeighbour;
    }

    public static int isAdjacentToAny(TMGameState gs, TMMapTile mt) {
        List<Vector2D> neighbours = getNeighbours(new Vector2D(mt.getX(), mt.getY()));
        int count = 0;
        for (Vector2D n: neighbours) {
            TMMapTile other = gs.getBoard().getElement(n.getX(), n.getY());
            if (other != null && other.getTilePlaced() != null) {
                count++;
            }
        }
        return count;
    }

    public static int isAdjacentToTile(TMGameState gs, TMMapTile mt, TMTypes.Tile t) {
        List<Vector2D> neighbours = getNeighbours(new Vector2D(mt.getX(), mt.getY()));
        int count = 0;
        for (Vector2D n: neighbours) {
            TMMapTile other = gs.getBoard().getElement(n.getX(), n.getY());
            if (other != null && other.getTilePlaced() == t) {
                count++;
            }
        }
        return count;
    }

    public static int isAdjacentToCity(TMGameState gs, TMMapTile mt) {
        List<Vector2D> neighbours = getNeighbours(new Vector2D(mt.getX(), mt.getY()));
        int count = 0;
        for (Vector2D n: neighbours) {
            TMMapTile other = gs.getBoard().getElement(n.getX(), n.getY());
            if (other != null && other.getTilePlaced() == City) {
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
