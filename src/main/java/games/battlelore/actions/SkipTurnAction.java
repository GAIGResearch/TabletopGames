package games.battlelore.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

import games.battlelore.BattleloreGameState;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SkipTurnAction extends AbstractAction {
    private final Unit.Faction playerFaction;
    private final int unitsID;
    private final boolean skipAttack;
    private final boolean skipMove;
    private final int playerID;

    public SkipTurnAction(int tileID, Unit.Faction faction, boolean doesSkipMove, boolean doesSkipAttack, int playerID) {
        unitsID = tileID;
        playerFaction = faction;
        this.playerID = playerID;
        skipAttack = doesSkipAttack;
        skipMove = doesSkipMove;
    }

    public SkipTurnAction(Unit.Faction faction, int playerID) {
        this(-1, faction, true, true, playerID);
    }


    @Override
    public boolean execute(AbstractGameState gameState) {

        if (this.playerFaction == Unit.Faction.NA) {
            System.out.println("Wrong player id'");
            return false;
        }

        BattleloreGameState state = (BattleloreGameState) gameState;
        MapTile units = state.getTile(unitsID);
        if (units != null) {
            List<Unit> skipped = new ArrayList<>();
            for (Unit unit : units.GetUnits()) {
                skipped.add(unit.withOrders(unit.CanMove() && !skipMove, unit.CanAttack() && !skipAttack));
            }
            state.setTile(units.withUnits(skipped));
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SkipTurnAction)) return false;
        SkipTurnAction that = (SkipTurnAction) o;

        return Objects.equals(playerFaction, that.playerFaction) &&
                Objects.equals(unitsID, that.unitsID) &&
                skipAttack == that.skipAttack &&
                skipMove == that.skipMove &&
                playerID == that.playerID;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(unitsID, playerFaction, skipAttack, skipMove, playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        MapTile units = ((BattleloreGameState) gameState).getTile(unitsID);
        if (units == null) {
            return "";
        }
        if (units.GetUnits().isEmpty()) {
            return playerFaction.name() + "skips his turn.";
        }
        else {
            return playerFaction.name() + " units in " + units.getLocationX() + ":" + units.getLocationY() + " waits.";
        }
    }
}
