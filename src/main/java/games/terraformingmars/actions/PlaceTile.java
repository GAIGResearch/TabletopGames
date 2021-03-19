package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMMapTile;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class PlaceTile extends TMAction implements IExtendedSequence {
    // TODO: extend for outer locations (not on mars), x and y will be -1
    public boolean onMars = true;  // TODO: initialize and work with this var
    public int x,y;
    public final TMTypes.Tile tile;
    public HashSet<Vector2D> legalPositions;

    boolean placed;
    boolean impossible;

    public PlaceTile(int player, int x, int y, TMTypes.Tile tile, boolean free) {
        super(player, free);
        this.x = x;
        this.y = y;
        this.tile = tile;
        this.legalPositions = new HashSet<>();
    }

    public PlaceTile(int player, TMTypes.Tile tile, HashSet<Vector2D> legalPositions, boolean free) {
        super(player, free);
        this.x = -1;
        this.y = -1;
        this.tile = tile;
        this.legalPositions = legalPositions;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (x != -1 && tile != null) {
            TMGameState ggs = (TMGameState) gs;
            // TODO money earned from oceans
            return ggs.getBoard().getElement(x, y).placeTile(tile, ggs) && super.execute(gs);
        }
        if (player == -1) player = gs.getCurrentPlayer();
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public PlaceTile copy() {
        PlaceTile copy = new PlaceTile(player, x, y, tile, free);
        copy.impossible = impossible;
        copy.placed = placed;
        HashSet<Vector2D> copyPos = null;
        if (legalPositions != null) {
            copyPos = new HashSet<>();
            for (Vector2D pos : legalPositions) {
                copyPos.add(pos.copy());
            }
        }
        copy.legalPositions = copyPos;
        return copy;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (x == -1) {
            // Need to choose where to place it
            // TODO adjacency rules
            TMGameState gs = (TMGameState) state;
            if (legalPositions != null) {
                for (Vector2D pos : legalPositions) {
                    TMMapTile mt = gs.getBoard().getElement(pos.getX(), pos.getY());
                    if (mt != null && mt.getTilePlaced() == null) {
                        actions.add(new PlaceTile(player, pos.getX(), pos.getY(), tile, true));
                    }
                }
            } else {
                for (int i = 0; i < gs.getBoard().getHeight(); i++) {
                    for (int j = 0; j < gs.getBoard().getWidth(); j++) {
                        TMMapTile mt = gs.getBoard().getElement(j, i);
                        if (mt != null && mt.getTilePlaced() == null && mt.getTileType() == tile.getRegularLegalTileType()) {
                            actions.add(new PlaceTile(player, j, i, tile, true));
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
        return x == placeTile.x &&
                y == placeTile.y &&
                placed == placeTile.placed &&
                impossible == placeTile.impossible &&
                player == placeTile.player &&
                tile == placeTile.tile &&
                Objects.equals(legalPositions, placeTile.legalPositions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), x, y, tile, legalPositions, placed, impossible, player);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Placing " + tile.name() + " at (" + x + "," + y + ")";
    }

    @Override
    public String toString() {
        return "Placing " + tile.name() + " at (" + x + "," + y + ")";
    }
}
