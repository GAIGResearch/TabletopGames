package games.battlelore.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.battlelore.BattleloreGameState;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

import java.util.ArrayList;
import java.util.Objects;

public class MoveUnitsAction extends AbstractAction {
    private final Unit.Faction playerFaction;
    private final int tileID;
    private final int locationX;
    private final int locationY;
    private final int playerID;

    public MoveUnitsAction(int tileID, Unit.Faction faction, int locX, int locY, int playerID) {
        this.tileID = tileID;
        this.playerFaction = faction;
        this.locationX = locX;
        this.locationY = locY;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        BattleloreGameState state = (BattleloreGameState) gameState;

        if (this.playerFaction == Unit.Faction.NA) {
            System.out.println("Wrong player id'");
            return false;
        } else {
            MapTile tile = (MapTile) gameState.getComponentById(tileID);
            ArrayList<Unit> units = ((MapTile)state.getBoard().getElement(tile.getLocationX(), tile.getLocationY())).GetUnits();

            for (Unit unit : units) {
                state.AddUnit(locationX, locationY, unit);
                unit.SetCanMove(false);
            }

            state.RemoveUnit(tile.getLocationX(), tile.getLocationY());
            return true;
        }
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MoveUnitsAction)) return false;
        MoveUnitsAction that = (MoveUnitsAction) o;

        return tileID == that.tileID &&
                playerFaction == that.playerFaction &&
                locationX == that.locationX &&
                locationY == that.locationY &&
                playerID == that.playerID;
    }

    public int hashCode() {
        return Objects.hash(tileID, playerFaction, locationX, locationY);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        MapTile tile = (MapTile) gameState.getComponentById(tileID);
        return playerFaction.name() + " moves units from " + tile.getLocationX() + ":" +
                tile.getLocationY() + " to " + locationX + ":" + locationY;
    }
}
