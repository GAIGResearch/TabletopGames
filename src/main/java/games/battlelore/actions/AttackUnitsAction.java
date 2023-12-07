package games.battlelore.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Component;
import games.battlelore.BattleloreGameParameters;
import games.battlelore.BattleloreGameState;
import games.battlelore.cards.CommandCard;
import games.battlelore.components.CombatDice;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class AttackUnitsAction extends AbstractAction {
    private final Unit.Faction playerFaction;
    private final int attackingUnitsTileID;
    private final int targetTileID;
    private final int playerID;

    public AttackUnitsAction(int attackingUnitsTileID, int targetTileID, Unit.Faction faction, int playerID) {
        this.attackingUnitsTileID = attackingUnitsTileID;
        this.targetTileID = targetTileID;
        this.playerFaction = faction;
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
            MapTile attacker = (MapTile) gameState.getComponentById(attackingUnitsTileID);
            MapTile defender = (MapTile) gameState.getComponentById(targetTileID);
            ArrayList<Unit> attackerUnits = ((MapTile)state.getBoard().getElement(attacker.getLocationX(), attacker.getLocationY())).GetUnits();
            ArrayList<Unit> defenderUnits = ((MapTile)state.getBoard().getElement(defender.getLocationX(), defender.getLocationY())).GetUnits();

            //COMBAT SEQUENCE: Roll a dice
            int defeatedEnemyCount = 0;
            CombatDice dice = new CombatDice();
            BattleloreGameParameters parameters = (BattleloreGameParameters) state.getGameParameters();

            for (int i = 0; i < parameters.troopCountInSquad; i++) {
                CombatDice.Result result = dice.getResult(state.getRnd());
                if (result == CombatDice.Result.Cleave) {
                    if (!parameters.isWeakAttacker(attackerUnits.size())) {
                        defeatedEnemyCount++;
                    }
                }
                else if (result == CombatDice.Result.Strike) {
                    defeatedEnemyCount++;
                }
            }

            for (int x = 0; x < defeatedEnemyCount; x++) {
                if (defenderUnits.size() > 0) {
                    defenderUnits.remove(defenderUnits.size() - 1);
                    state.AddScore(playerID, 1);
                }
            }
            if (defenderUnits.isEmpty()) {
                state.RemoveUnit(defender.getLocationX(), defender.getLocationY());
            }
            else {
                ((MapTile)state.getBoard().getElement(defender.getLocationX(), defender.getLocationY())).SetUnits(defenderUnits);
            }

            for (Unit unit : attackerUnits) {
                unit.SetCanAttack(false);
            }
            return true;
        }
    }

    public MapTile GetAttacker(AbstractGameState gameState) {
        return (MapTile) gameState.getComponentById(attackingUnitsTileID);
    }

    public MapTile GetDefender(AbstractGameState gameState) {
        return (MapTile) gameState.getComponentById(targetTileID);
    }

    public Unit.Faction GetFaction() {
        return playerFaction;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttackUnitsAction)) return false;
        AttackUnitsAction that = (AttackUnitsAction) o;
        return that.attackingUnitsTileID == attackingUnitsTileID &&
                that.targetTileID == targetTileID &&
                playerFaction == that.playerFaction &&
                playerID == that.playerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attackingUnitsTileID, targetTileID, playerFaction, playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        MapTile attacker = (MapTile) gameState.getComponentById(attackingUnitsTileID);
        MapTile defender = (MapTile) gameState.getComponentById(targetTileID);
        return playerFaction.name() + " units in " + attacker.getLocationX() + ":" +
                attacker.getLocationY() + " attacks to " + defender.getLocationX()+ ":" + defender.getLocationY();
    }
}
