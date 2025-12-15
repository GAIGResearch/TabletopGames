package games.pickomino;

import core.actions.AbstractAction;
import games.pickomino.actions.NullTurn;
import games.pickomino.actions.SelectDicesAction;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class PickominoGreedyAgentTest {

    private final PickominoGreedyAgent agent = new PickominoGreedyAgent();

    @Test
    public void choosesNullTurnWhenOnlyOption() {
        AbstractAction chosen = agent.getAction(null, List.of(new NullTurn()));
        assertEquals(new NullTurn(), chosen);
    }

    @Test
    public void prefersStealingHighestValueTileWhenStopping() {
        PickominoForwardModel fm = new PickominoForwardModel();
        PickominoParameters params = new PickominoParameters();
        PickominoGameState state = new PickominoGameState(params, 2);
        fm.setup(state);

        // Setup dice/assigned state: total = 17, remaining dice = 3, selectable faces 5 (x2) and 4 (x1)
        Arrays.fill(state.assignedDices, 0);
        state.assignedDices[2] = 4; // four 3s -> 12
        state.assignedDices[5] = 1; // one worm -> 5
        state.totalDicesValue = 17;
        state.remainingDices = 3;
        Arrays.fill(state.currentRoll, 0);
        state.currentRoll[4] = 2; // two 5s
        state.currentRoll[3] = 1; // one 4

        // Give opponent a top tile of value 27 (to be stolen); keep 21 in the deck so stopping on 21 is also legal
        PickominoTile tile27 = removeTileWithValue(state.remainingTiles, 27);
        state.playerTiles.get(1).add(tile27);

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new SelectDicesAction(5, true)));
        assertTrue(actions.contains(new SelectDicesAction(4, true)));

        AbstractAction chosen = agent.getAction(state, actions);
        assertEquals(new SelectDicesAction(5, true), chosen);
    }

    @Test
    public void prefersWormsWhenCannotStop() {
        PickominoForwardModel fm = new PickominoForwardModel();
        PickominoParameters params = new PickominoParameters();
        PickominoGameState state = new PickominoGameState(params, 2);
        fm.setup(state);

        Arrays.fill(state.assignedDices, 0);
        state.totalDicesValue = 0;
        state.remainingDices = params.numberOfDices;
        Arrays.fill(state.currentRoll, 0);
        state.currentRoll[5] = 2; // two worms available
        state.currentRoll[3] = 3; // three 4s to provide alternative choices

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertFalse(actions.stream().anyMatch(a -> a instanceof SelectDicesAction sda && sda.isStop()));
        assertTrue(actions.contains(new SelectDicesAction(6, false)));

        AbstractAction chosen = agent.getAction(state, actions);
        assertEquals(new SelectDicesAction(6, false), chosen);
    }

    private PickominoTile removeTileWithValue(core.components.Deck<PickominoTile> deck, int value) {
        for (int i = 0; i < deck.getSize(); i++) {
            if (deck.peek(i).getValue() == value) {
                return deck.pick(i);
            }
        }
        throw new AssertionError("Tile with value " + value + " not found");
    }
}

