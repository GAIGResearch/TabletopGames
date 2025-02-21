package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMMapTile;

import java.util.*;

public class ReserveTile extends TMAction implements IExtendedSequence {
    public int mapTileID;
    public TMTypes.MapTileType mapType;

    boolean placed;
    boolean impossible;

    public ReserveTile() { super(); } // This is needed for JSON Deserializer

    public ReserveTile(int player, int mapTileID, boolean free) {
        super(player, free);
        this.mapTileID = mapTileID;
    }

    public ReserveTile(int player, TMTypes.MapTileType mapTile, boolean free) {
        super(player, free);
        this.mapType = mapTile;
        this.mapTileID = -1;
    }

    @Override
    public boolean _execute(TMGameState gs) {
        if (mapTileID != -1) {
            TMMapTile mt = (TMMapTile)gs.getComponentById(mapTileID);
            mt.setReserved(player);
            return true;
        }
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public ReserveTile _copy() {
        ReserveTile copy = new ReserveTile(player, mapTileID, freeActionPoint);
        copy.impossible = impossible;
        copy.placed = placed;
        copy.mapType = mapType;
        return copy;
    }

    @Override
    public ReserveTile copy() {
        return (ReserveTile) super.copy();
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (mapTileID == -1) {
            // Need to choose where to place it
            TMGameState gs = (TMGameState) state;
            for (int i = 0; i < gs.getBoard().getHeight(); i++) {
                for (int j = 0; j < gs.getBoard().getWidth(); j++) {
                    TMMapTile mt = (TMMapTile) gs.getBoard().getElement(j, i);
                    // Check if we can place tile here
                    if (mt != null && mt.getTilePlaced() == null && (mt.getTileType() == mapType)) {
                        actions.add(new ReserveTile(player, mt.getComponentID(), true));
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
        if (!(o instanceof ReserveTile)) return false;
        if (!super.equals(o)) return false;
        ReserveTile that = (ReserveTile) o;
        return mapTileID == that.mapTileID && placed == that.placed && impossible == that.impossible && mapType == that.mapType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mapTileID, mapType, placed, impossible);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Reserve tile";
    }

    @Override
    public String toString() {
        return "Reserve tile";
    }
}
