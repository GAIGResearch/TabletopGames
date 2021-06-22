package games.battlelore.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.battlelore.BattleloreGameState;
import games.battlelore.cards.CommandCard;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

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
                    boolean r = tile.IsInArea(MapTile.TileArea.right);
                    boolean m = tile.IsInArea(MapTile.TileArea.mid);
                    boolean l = tile.IsInArea(MapTile.TileArea.left);
                    if (type == CommandCard.CommandType.AttackRight && tile.GetUnits() != null &&
                            tile.GetFaction() == playerFaction && tile.IsInArea(MapTile.TileArea.right))
                    {
                        state.ToggleUnitsOrderable(true, x, y);
                        state.SetOrderableUnitCount(playerID, state.GetOrderableUnitCount(playerID) + 1);

                    }
                    else if (type == CommandCard.CommandType.PatrolLeft && tile.GetUnits() != null &&
                            tile.GetFaction() == playerFaction && tile.IsInArea(MapTile.TileArea.left))
                    {
                        state.ToggleUnitsOrderable(true, x, y);
                        state.SetOrderableUnitCount(playerID, state.GetOrderableUnitCount(playerID) + 1);
                    }
                    else if (type == CommandCard.CommandType.BattleMarch && tile.GetUnits() != null &&
                            tile.GetFaction() == playerFaction && tile.IsInArea(MapTile.TileArea.mid))
                    {
                        state.ToggleUnitsOrderable(true, x, y);
                        state.SetOrderableUnitCount(playerID, state.GetOrderableUnitCount(playerID) + 1);
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
