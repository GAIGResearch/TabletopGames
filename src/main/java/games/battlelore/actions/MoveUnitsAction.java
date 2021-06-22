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
import games.dominion.cards.CardType;

import java.util.ArrayList;
import java.util.Map;

public class MoveUnitsAction extends AbstractAction
{
    private Unit.Faction playerFaction;
    private MapTile tile;
    private int locationX;
    private int locationY;

    public MoveUnitsAction(MapTile tile, Unit.Faction faction, int locX, int locY)
    {
        this.tile = tile;
        this.playerFaction = faction;
        this.locationX = locX;
        this.locationY = locY;
    }


    @Override
    public boolean execute(AbstractGameState gameState)
    {
        BattleloreGameState state = (BattleloreGameState) gameState;

        if (this.playerFaction == Unit.Faction.NA)
        {
            System.out.println("Wrong player id'");
            return false;
        }
        else
        {
            ArrayList<Unit> units = state.getBoard().getElement(tile.getLocationX(), tile.getLocationY()).GetUnits();
            state.RemoveUnit(tile.getLocationX(), tile.getLocationY());
            for(Unit unit : units)
            {
                state.getBoard().getElement(locationX, locationY).AddUnit(unit);
            }

            //state.setGamePhase(BattleloreGameState.BattleloreGamePhase.MoveStep);
            return true;
        }

    }

    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return playerFaction.name() + " moves units from " + tile.getLocationX() + ":" + tile.getLocationY() + " to " + locationX + ":" + locationY;
    }
}
