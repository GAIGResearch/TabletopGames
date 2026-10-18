package games.coltexpress;

import core.Game;
import games.GameType;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;

import static org.junit.Assert.*;

public class ColtExpressGameStateTest {

    Game game;
    ColtExpressGameState state;
    ColtExpressForwardModel fm;

    @Before
    public void setup() {
        game = GameType.ColtExpress.createGameInstance(3, 42);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        state = (ColtExpressGameState) game.getGameState();
        fm = (ColtExpressForwardModel) game.getForwardModel();
    }

    @Test
    public void fullCopyIsEqualToOriginal() {
        ColtExpressGameState copy = (ColtExpressGameState) state.copy();
        assertNotSame(state, copy);
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void firstTurnOfEachRoundUsesThatRoundsCard() {
        assertEquals(state.getRounds().get(0).getTurnTypes()[0], state.getCurrentTurnType());
        assertTrue(state.getRounds().getVisibilityForPlayer(0, 0));
        assertFalse(state.getRounds().getVisibilityForPlayer(1, 0));

        while (state.getRoundCounter() == 0)
            fm.next(state, fm.computeAvailableActions(state).get(0));

        assertEquals(state.getRounds().get(1).getTurnTypes()[0], state.getCurrentTurnType());
        assertTrue(state.getRounds().getVisibilityForPlayer(1, 0));
        assertFalse(state.getRounds().getVisibilityForPlayer(2, 0));
    }

    @Test
    public void turnStateIsCopiedAndPartOfEquality() {
        // play into the second turn of the round, so that the turn state is not the initial one
        for (int i = 0; i < state.getNPlayers(); i++)
            fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getFullPlayerTurnCounter());

        ColtExpressGameState copy = (ColtExpressGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.getCurrentTurnType(), copy.getCurrentTurnType());
        assertEquals(state.getFullPlayerTurnCounter(), copy.getFullPlayerTurnCounter());

        copy.fullPlayerTurnCounter++;
        assertNotEquals(state, copy);
    }
}
