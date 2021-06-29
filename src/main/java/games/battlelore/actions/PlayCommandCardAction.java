package games.battlelore.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.battlelore.BattleloreGameState;
import games.battlelore.cards.CommandCard;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;
import games.dominion.DominionGameState;

import java.util.List;

public class PlayCommandCardAction extends AbstractAction
{
    private Unit.Faction playerFaction;
    private CommandCard.CommandType type;
    private int playerID;
    //private ArrayList<MapTile> orderedTiles;

    public PlayCommandCardAction(CommandCard.CommandType type, Unit.Faction playerFaction, int playerID)
    {
        this.type = type;
        this.playerFaction = playerFaction;
        this.playerID = playerID;
        //this.orderedTiles = orderedTiles;
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
            for (int x = 0; x < state.getBoard().getWidth(); x++)
            {
                for (int y = 0; y < state.getBoard().getHeight(); y++)
                {
                    MapTile tile = state.getBoard().getElement(x, y);

                    if (type == CommandCard.CommandType.AttackRight && tile.GetUnits() != null &&
                            tile.GetFaction() == playerFaction && tile.IsInArea(MapTile.TileArea.right))
                    {
                        state.SetUnitsAsOrderable(x, y);
                    }
                    else if (type == CommandCard.CommandType.PatrolLeft && tile.GetUnits() != null &&
                            tile.GetFaction() == playerFaction && tile.IsInArea(MapTile.TileArea.left))
                    {
                        state.SetUnitsAsOrderable(x, y);
                    }
                    else if (type == CommandCard.CommandType.BattleMarch && tile.GetUnits() != null &&
                            tile.GetFaction() == playerFaction && tile.IsInArea(MapTile.TileArea.mid))
                    {
                        state.SetUnitsAsOrderable(x, y);
                    }
                }
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
    public String getString(AbstractGameState gameState)
    {
        return playerFaction.name() + " uses " + type.name() + "command and readies units (marked as *).";
    }
}
