package games.poker;

import core.actions.AbstractAction;
import games.poker.actions.*;
import org.junit.*;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class TestBasicFlow {

    public PokerGameState state;
    public PokerForwardModel fm;
    public PokerGameParameters params;
    Random rnd = new Random(39027);

    @Before
    public void setup() {
        params = new PokerGameParameters();
        fm = new PokerForwardModel();
        state = new PokerGameState(params, 4);
        fm.setup(state);
    }


    @Test
    public void playersTakeTurnsInPreFlop() {
        assertEquals(3, state.getBigId());
        assertEquals(2, state.getSmallId());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Call(0));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Call(1));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Call(2));
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, new Call(3));
        assertEquals(state.getSmallId(), state.getCurrentPlayer());
    }

    @Test
    public void playersTakeTurnsPostFlop() {
        playersTakeTurnsInPreFlop();
        assertEquals(PokerGameState.PokerGamePhase.Flop, state.getGamePhase());
        assertEquals(state.getSmallId(), state.getCurrentPlayer());
        fm.next(state, new Check(state.getSmallId()));
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, new Check(3));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Check(0));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Check(1));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(PokerGameState.PokerGamePhase.Turn, state.getGamePhase());
    }

    @Test
    public void afterFoldingPlayerTakesNoMoreTurns() {
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Call(0));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Fold(1));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Call(2));
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, new Fold(3));
        assertEquals(state.getSmallId(), state.getCurrentPlayer());
        assertEquals(PokerGameState.PokerGamePhase.Flop, state.getGamePhase());
        fm.next(state, new Check(state.getSmallId()));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Check(0));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(PokerGameState.PokerGamePhase.Turn, state.getGamePhase());
    }

    @Test
    public void afterAllInPlayerTakesNoMoreTurns() {
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new AllIn(0));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Call(1));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new AllIn(2)); // this requires player 1 to call again
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, new Call(3));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(PokerGameState.PokerGamePhase.Preflop, state.getGamePhase());
        fm.next(state, new Call(1));
        assertEquals(PokerGameState.PokerGamePhase.Flop, state.getGamePhase());
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, new Call(3));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Call(1));
        assertEquals(PokerGameState.PokerGamePhase.Turn, state.getGamePhase());
    }

    @Test
    public void gameEnds() {
        do {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(rnd.nextInt(actions.size())));
        } while (state.isNotTerminal());
    }
}
