package games.battlelore.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

import games.battlelore.BattleloreGameState;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;
import games.dotsboxes.AddGridCellEdge;

import java.util.Objects;

public class SkipTurnAction extends AbstractAction {

    private Unit.Faction playerFaction;
    private MapTile units;
    private boolean skipAttack;
    private boolean skipMove;
    private int playerID;

    public SkipTurnAction(MapTile tile, Unit.Faction faction, boolean doesSkipMove, boolean doesSkipAttack, int playerID) {
        this.units = tile;
        this.playerFaction = faction;
        this.playerID = playerID;
        skipAttack = doesSkipAttack;
        skipMove = doesSkipMove;
    }

    public SkipTurnAction() {}

    @Override
    public boolean execute(AbstractGameState gameState) {

        if (this.playerFaction == Unit.Faction.NA) {
            System.out.println("Wrong player id'");
            return false;
        }

        BattleloreGameState state = (BattleloreGameState) gameState;
        state.AddToRounds();
        state.IncrementTurn(playerID);

        if (units != null) {
            for (Unit unit : units.GetUnits()) {
                if (skipAttack) {
                    unit.SetCanAttack(false);
                }
                if (skipMove) {
                    unit.SetCanMove(false);
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return new SkipTurnAction(units.copy(), playerFaction, skipMove, skipAttack, playerID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SkipTurnAction)) return false;
        SkipTurnAction that = (SkipTurnAction) o;

        return Objects.equals(playerFaction, that.playerFaction) &&
                Objects.equals(units, that.units) &&
                skipAttack == that.skipAttack &&
                skipMove == that.skipMove &&
                playerID == that.playerID;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(units, playerFaction);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (units == null) {
            return "";
        }
        if ( units.GetUnits().isEmpty()) {
            return playerFaction.name() + "skips his turn.";
        }
        else {
            return playerFaction.name() + " units in " + units.getLocationX() + ":" + units.getLocationY() + " waits.";
        }
    }
}
