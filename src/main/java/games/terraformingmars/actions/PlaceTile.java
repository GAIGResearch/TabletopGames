package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
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
    int x,y;
    final TMTypes.Tile tile;
    HashSet<Vector2D> legalPositions;

    boolean placed;
    boolean impossible;
    int player;

    public PlaceTile(int x, int y, TMTypes.Tile tile, boolean free) {
        super(free);
        this.x = x;
        this.y = y;
        this.tile = tile;
        this.legalPositions = new HashSet<>();
    }

    public PlaceTile(TMTypes.Tile tile, HashSet<Vector2D> legalPositions, boolean free) {
        super(free);
        this.x = -1;
        this.y = -1;
        this.tile = tile;
        this.legalPositions = legalPositions;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (x != -1 && tile != null) {
            TMGameState ggs = (TMGameState) gs;
            return ggs.getBoard().getElement(x, y).placeTile(tile, ggs) && super.execute(gs);
        }
        player = gs.getCurrentPlayer();
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public PlaceTile copy() {
        PlaceTile copy = new PlaceTile(x, y, tile, free);
        copy.impossible = impossible;
        copy.placed = placed;
        HashSet<Vector2D> copyPos = new HashSet<>();
        for (Vector2D pos: legalPositions) {
            copyPos.add(pos.copy());
        }
        copy.legalPositions = copyPos;
        copy.player = player;
        return copy;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (x == -1) {
            // Need to choose where to place it
            TMGameState gs = (TMGameState) state;
            for (int i = 0; i < gs.getBoard().getHeight(); i++) {
                for (int j = 0; j < gs.getBoard().getWidth(); j++) {
                    TMMapTile mt = gs.getBoard().getElement(j, i);
                    if (mt.getTilePlaced() == null && legalPositions.contains(new Vector2D(j, i))) {
                        actions.add(new PlaceTile(j, i, tile, free));
                    }
                }
            }
            if (actions.size() == 0) {
                impossible = true;
                actions.add(new DoNothing());
            }
        } else {
            impossible = true;
            actions.add(new DoNothing());
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
}
