package games.battlelore.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.Area;
import de.erichseifert.vectorgraphics2d.intermediate.commands.Command;
import games.battlelore.BattleloreForwardModel;
import games.battlelore.BattleloreGame;
import games.battlelore.BattleloreGameState;
import games.battlelore.cards.CommandCard;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;
import games.battlelore.gui.BattleloreGUI;
import games.dominion.cards.CardType;


import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class MoveUnitsAction extends AbstractAction
{
    private final Unit.Faction playerFaction;
    private final int tileID;
    private final int locationX;
    private final int locationY;
    private final int playerID;

    public MoveUnitsAction( int tileID, Unit.Faction faction, int locX, int locY, int playerID) {
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
        }
        else {
            MapTile tile = (MapTile) gameState.getComponentById(tileID);
            ArrayList<Unit> units = state.getBoard().getElement(tile.getLocationX(), tile.getLocationY()).GetUnits();

            if (units == null) {
                state.getBoard().getElement(locationX, locationY);
            }

            for (Unit unit : units) {
                state.getBoard().getElement(locationX, locationY).AddUnit(unit);
                unit.SetCanMove(false);
            }

            state.RemoveUnit(tile.getLocationX(), tile.getLocationY());
            state.IncrementTurn(playerID);
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

        return Objects.equals(tileID, that.tileID) &&
                playerFaction == that.playerFaction &&
                locationX == that.locationX &&
                locationY == that.locationY &&
                playerID == that.playerID;
    }

    public int hashCode()
    {
        return Objects.hash(tileID, playerFaction, locationX, locationY);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        MapTile tile = (MapTile) gameState.getComponentById(tileID);
        return playerFaction.name() + " moves units from " + tile.getLocationX() + ":" +
                tile.getLocationY() + " to " + locationX + ":" + locationY;
    }
}
