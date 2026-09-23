package games.golfsix;

import core.actions.AbstractAction;
import games.golfsix.actions.DrawCard;
import games.golfsix.actions.TurnUp;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static games.golfsix.GolfSixTestUtils.*;
import static org.junit.Assert.*;

/**
 * The initial reveal: each player in turn turns up GolfSixParameters.initialFaceUp cards, then play starts
 * with player 0.
 */
public class GolfSixRevealTest {

    GolfSixParameters params;
    GolfSixGameState state;
    GolfSixForwardModel fm;

    static final Set<AbstractAction> DRAWS = Set.of(new DrawCard(false), new DrawCard(true));

    @Before
    public void setup() {
        params = new GolfSixParameters();
        params.setRandomSeed(23);
        fm = new GolfSixForwardModel();
    }

    private static Set<AbstractAction> turnUps(int... positions) {
        return IntStream.of(positions).mapToObj(TurnUp::new).collect(Collectors.toSet());
    }

    private Set<AbstractAction> actions() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    @Test
    public void firstPlayerMayTurnUpAnyOfTheirSixCards() {
        state = newState(params, 3);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(turnUps(0, 1, 2, 3, 4, 5), actions());
    }

    @Test
    public void aTurnedUpCardIsVisibleToAllAndIsNotOfferedAgain() {
        state = newState(params, 3);
        var before = new java.util.ArrayList<>(state.getGrid(0).getComponents());
        fm.next(state, new TurnUp(3));
        assertEquals("DDDUDD", faceUpPattern(state, 0));
        assertEquals("the grid's cards do not move", before, state.getGrid(0).getComponents());
        assertEquals("still player 0's turn", 0, state.getCurrentPlayer());
        assertEquals(turnUps(0, 1, 2, 4, 5), actions());
        assertEquals("other grids untouched", "DDDDDD", faceUpPattern(state, 1));
    }

    @Test
    public void theTurnPassesAfterTheSecondCardIsTurnedUp() {
        state = newState(params, 3);
        fm.next(state, new TurnUp(3));
        fm.next(state, new TurnUp(0));
        assertEquals("UDDUDD", faceUpPattern(state, 0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(turnUps(0, 1, 2, 3, 4, 5), actions());
    }

    @Test
    public void playStartsWithPlayerZeroOnceEveryoneHasRevealed() {
        state = newState(params, 3);
        fm.next(state, new TurnUp(3));
        fm.next(state, new TurnUp(0));
        fm.next(state, new TurnUp(5));
        fm.next(state, new TurnUp(1));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new TurnUp(2));
        fm.next(state, new TurnUp(4));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DRAWS, actions());
        assertEquals("UDDUDD", faceUpPattern(state, 0));
        assertEquals("DUDDDU", faceUpPattern(state, 1));
        assertEquals("DDUDUD", faceUpPattern(state, 2));
        // one turn per player so far
        assertEquals(3, state.getTurnCounter());
        assertAllCardsPresent(state);
    }

    @Test
    public void withNoInitialFaceUpCardsPlayStartsAtOnce() {
        params.setParameterValue("initialFaceUp", 0);
        state = newState(params, 3);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DRAWS, actions());
        assertEquals("DDDDDD", faceUpPattern(state, 0));
    }

    @Test
    public void withOneInitialFaceUpCardTheTurnPassesAfterOneTurnUp() {
        params.setParameterValue("initialFaceUp", 1);
        state = newState(params, 3);
        assertEquals(turnUps(0, 1, 2, 3, 4, 5), actions());
        fm.next(state, new TurnUp(2));
        assertEquals("DDUDDD", faceUpPattern(state, 0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(turnUps(0, 1, 2, 3, 4, 5), actions());
    }
}
