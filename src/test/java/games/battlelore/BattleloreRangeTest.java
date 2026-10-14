package games.battlelore;

import core.CoreConstants;
import core.Game;
import games.GameType;
import games.battlelore.components.MapTile;
import games.battlelore.components.Unit;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * The board's odd rows are shifted half a hex right, so (1,0) touches (0,0), (2,0), (0,1) and (1,1) on the board. The
 * scenario's units are cleared, and units added with move range 1 and melee range 1.
 */
public class BattleloreRangeTest {

    BattleloreGameState state;

    @Before
    public void setup() {
        Game game = GameType.Battlelore.createGameInstance(2, 5);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer()));
        state = (BattleloreGameState) game.getGameState();
        for (int x = 0; x < state.getWidth(); x++)
            for (int y = 0; y < state.getHeight(); y++)
                state.RemoveUnit(x, y);
    }

    MapTile addUnit(int x, int y, Unit.Faction faction) {
        state.AddUnit(x, y, new Unit(CoreConstants.ComponentType.TOKEN, "u", "Unit", 1, 1, 1, faction, "U", true));
        return state.getTile(x, y);
    }

    static Set<List<Integer>> positions(List<MapTile> tiles) {
        Set<List<Integer>> retValue = new HashSet<>();
        for (MapTile t : tiles)
            retValue.add(List.of(t.getLocationX(), t.getLocationY()));
        return retValue;
    }

    @Test
    public void meleeUnitsAttackEveryAdjacentHex() {
        MapTile attacker = addUnit(1, 0, Unit.Faction.Dakhan_Lords);
        // (0,1) is adjacent, though its offsets differ in both column and row; (2,1) is two steps away
        addUnit(0, 1, Unit.Faction.Uthuk_Yllan);
        addUnit(1, 1, Unit.Faction.Uthuk_Yllan);
        addUnit(2, 1, Unit.Faction.Uthuk_Yllan);
        addUnit(2, 0, Unit.Faction.Dakhan_Lords);
        assertEquals(Set.of(List.of(0, 1), List.of(1, 1)), positions(state.GetPossibleTargetUnits(attacker)));
    }

    @Test
    public void unitsMoveToEmptyHexesInRange() {
        // (2,0) is taken; (0,1) is adjacent, though its offsets differ in both column and row
        MapTile unit = addUnit(1, 0, Unit.Faction.Dakhan_Lords);
        addUnit(2, 0, Unit.Faction.Dakhan_Lords);
        assertEquals(Set.of(List.of(0, 0), List.of(0, 1), List.of(1, 1)),
                positions(state.GetPossibleLocationsForUnits(unit)));
    }
}
