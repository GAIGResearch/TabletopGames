package games.battlelore.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.battlelore.BattleloreGameState;
import games.battlelore.cards.CommandCard;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;
import games.dominion.DominionGameState;
import games.dotsboxes.AddGridCellEdge;

import java.util.List;
import java.util.Objects;

public class PlayCommandCardAction extends AbstractAction {

    private final Unit.Faction playerFaction;
    private final CommandCard.CommandType type;
    private final int playerID;

    public PlayCommandCardAction(CommandCard.CommandType type, Unit.Faction playerFaction, int playerID) {
        this.type = type;
        this.playerFaction = playerFaction;
        this.playerID = playerID;
    }

    public CommandCard.CommandType GetCommandType()
    {
        return type;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        BattleloreGameState state = (BattleloreGameState) gameState;

        if (this.playerFaction == Unit.Faction.NA) {
            System.out.println("Wrong player id'");
            return false;
        }
        else {
            for (int x = 0; x < state.getBoard().getWidth(); x++) {
                for (int y = 0; y < state.getBoard().getHeight(); y++) {

                    MapTile tile = (MapTile) state.getBoard().getElement(x, y);
                    if (type == CommandCard.CommandType.AttackRight && tile.GetUnits() != null &&
                            tile.GetFaction() == playerFaction && tile.IsInArea(MapTile.TileArea.right)) {
                        state.SetUnitsAsOrderable(x,y);
                    }
                    else if (type == CommandCard.CommandType.PatrolLeft && tile.GetUnits() != null &&
                            tile.GetFaction() == playerFaction && tile.IsInArea(MapTile.TileArea.left)) {
                        state.SetUnitsAsOrderable(x,y);
                    }
                    else if (type == CommandCard.CommandType.BattleMarch && tile.GetUnits() != null &&
                            tile.GetFaction() == playerFaction && tile.IsInArea(MapTile.TileArea.mid)) {
                        state.SetUnitsAsOrderable(x,y);
                    }
                }
            }
            return true;
        }
    }

    @Override
    public AbstractAction copy() {
        return this; //immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayCommandCardAction)) return false;
        PlayCommandCardAction that = (PlayCommandCardAction) o;
        return type == that.type &&
                playerID == that.playerID &&
                playerFaction == that.playerFaction;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(type, playerFaction, playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return playerFaction.name() + " uses " + type.name() + "command and readies units (marked as *).";
    }
}
