package games.hareandtortoise;

import org.junit.Test;

import static games.hareandtortoise.HareAndTortoiseTestUtils.*;
import static org.junit.Assert.*;

public class HareAndTortoiseRacePositionTest {

    private HareAndTortoiseGameState newState(int nPlayers) {
        HareAndTortoiseGameState state = new HareAndTortoiseGameState(new HareAndTortoiseParameters(), nPlayers);
        new HareAndTortoiseForwardModel().setup(state);
        return state;
    }

    @Test
    public void runnersAtStartSharePosition() {
        HareAndTortoiseGameState state = newState(3);
        for (int p = 0; p < 3; p++)
            assertEquals(1, state.getRacePosition(p));
        place(state, 0, 5, 50, 3);
        assertEquals(1, state.getRacePosition(0));
        assertEquals(2, state.getRacePosition(1));
        assertEquals(2, state.getRacePosition(2));
    }

    @Test
    public void runnerInTheRearOfFourIsAlwaysFourth() {
        // rules example: "in a four-player game the player in the rear is always 4th, no matter how many others
        // have reached home"
        HareAndTortoiseGameState state = newState(4);
        place(state, 0, 40, 20, 0);
        place(state, 1, 30, 20, 0);
        place(state, 2, 20, 20, 0);
        place(state, 3, 10, 20, 0);
        assertEquals(1, state.getRacePosition(0));
        assertEquals(2, state.getRacePosition(1));
        assertEquals(3, state.getRacePosition(2));
        assertEquals(4, state.getRacePosition(3));

        putHome(state, 1, 1);
        putHome(state, 0, 2);
        assertEquals(1, state.getRacePosition(1));
        assertEquals(2, state.getRacePosition(0));
        assertEquals(3, state.getRacePosition(2));
        assertEquals(4, state.getRacePosition(3));
    }

    @Test
    public void ordinalIsRacePositionAndScoreCountsPlayersBehind() {
        HareAndTortoiseGameState state = newState(4);
        putHome(state, 2, 1);
        place(state, 0, 30, 20, 0);
        place(state, 1, 40, 20, 0);
        place(state, 3, 10, 20, 0);
        // p2 home 1st, p1 on 40 2nd, p0 on 30 3rd, p3 on 10 4th
        int[] expected = {3, 2, 1, 4};
        for (int p = 0; p < 4; p++) {
            assertEquals(expected[p], state.getOrdinalPosition(p));
            assertEquals(4 + 1 - expected[p], state.getGameScore(p), 1e-9);
        }
    }
}
