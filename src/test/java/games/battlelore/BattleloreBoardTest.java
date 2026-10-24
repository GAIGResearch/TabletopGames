package games.battlelore;

import core.CoreConstants;
import core.Game;
import games.GameType;
import games.battlelore.actions.AttackUnitsAction;
import games.battlelore.actions.MoveUnitsAction;
import games.battlelore.actions.SkipTurnAction;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static games.battlelore.components.Unit.Faction.Dakhan_Lords;
import static games.battlelore.components.Unit.Faction.Uthuk_Yllan;
import static org.junit.Assert.*;

public class BattleloreBoardTest {

    BattleloreGameState state;

    @Before
    public void setup() {
        Game game = GameType.Battlelore.createGameInstance(2, 5);
        state = (BattleloreGameState) game.getGameState();
        for (int x = 0; x < state.getWidth(); x++)
            for (int y = 0; y < state.getHeight(); y++)
                state.RemoveUnit(x, y);
    }

    MapTile addUnit(int x, int y, Unit.Faction faction, int strength) {
        state.AddUnit(x, y, new Unit(CoreConstants.ComponentType.TOKEN, "u", "Unit", 1, strength, 1, faction, "U", true));
        return state.getTile(x, y);
    }

    Unit unitAt(int x, int y) {
        return state.getTile(x, y).GetUnits().get(0);
    }

    @Test
    public void ordersCanBeGivenAndUsed() {
        addUnit(2, 2, Dakhan_Lords, 1);
        assertFalse(unitAt(2, 2).CanMove());
        state.SetUnitsAsOrderable(2, 2);
        assertTrue(unitAt(2, 2).CanMove());
        assertTrue(unitAt(2, 2).CanAttack());
        new SkipTurnAction(state.getTile(2, 2).getComponentID(), Dakhan_Lords, true, false, 0).execute(state);
        assertFalse(unitAt(2, 2).CanMove());
        assertTrue(unitAt(2, 2).CanAttack());
    }

    @Test
    public void movingTakesTheUnitsAndUsesTheirMove() {
        MapTile from = addUnit(2, 2, Dakhan_Lords, 1);
        state.SetUnitsAsOrderable(2, 2);
        new MoveUnitsAction(from.getComponentID(), Dakhan_Lords, 3, 2, 0).execute(state);
        assertNull(state.getTile(2, 2).GetUnits());
        assertEquals(1, state.getTile(3, 2).GetUnits().size());
        assertFalse(unitAt(3, 2).CanMove());
        assertTrue(unitAt(3, 2).CanAttack());
    }

    @Test
    public void attackingUsesTheAttackAndScoresForEachUnitDefeated() {
        MapTile attacker = addUnit(2, 2, Dakhan_Lords, 1);
        MapTile defender = addUnit(3, 2, Uthuk_Yllan, 1);
        state.SetUnitsAsOrderable(2, 2);
        new AttackUnitsAction(attacker.getComponentID(), defender.getComponentID(), Dakhan_Lords, 0).execute(state);
        assertFalse(unitAt(2, 2).CanAttack());
        assertTrue(unitAt(2, 2).CanMove());
        int defeated = state.getTile(3, 2).GetUnits() == null ? 1 : 0;
        assertEquals(defeated, state.getGameScore(0), 0);
    }

    @Test
    public void anActionSeesTheTileAsItNowIs() {
        // getComponentById would give the tile as it was first seen
        MapTile attacker = addUnit(2, 2, Dakhan_Lords, 1);
        MapTile defender = addUnit(3, 2, Uthuk_Yllan, 1);
        state.getComponentById(attacker.getComponentID());
        AttackUnitsAction attack = new AttackUnitsAction(attacker.getComponentID(), defender.getComponentID(), Dakhan_Lords, 0);
        state.AddUnit(2, 2, unitAt(2, 2));
        assertEquals(2, attack.GetAttacker(state).GetUnits().size());
    }

    @Test
    public void copiesAreEqualAndIndependent() {
        MapTile from = addUnit(2, 2, Dakhan_Lords, 1);
        BattleloreGameState copy = (BattleloreGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        // a tile rebuilt with the same units is the same tile
        copy.setTile(copy.getTile(2, 2).withUnits(new ArrayList<>(copy.getTile(2, 2).GetUnits())));
        assertEquals(state, copy);
        copy.SetUnitsAsOrderable(2, 2);
        new MoveUnitsAction(from.getComponentID(), Dakhan_Lords, 3, 2, 0).execute(copy);
        assertNotNull(state.getTile(2, 2).GetUnits());
        assertFalse(unitAt(2, 2).CanMove());
        assertNull(state.getTile(3, 2).GetUnits());
        assertNotEquals(state, copy);
    }
}
