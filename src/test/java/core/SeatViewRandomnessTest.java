package core;

import games.GameType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SeatViewRandomnessTest {

    @Test
    public void coltExpressSeatCopiesPreserveLiveRandomness() {
        assertSeatCopiesPreserveLiveRandomness(GameType.ColtExpress);
    }

    @Test
    public void monopolyDealSeatCopiesPreserveLiveRandomness() {
        assertSeatCopiesPreserveLiveRandomness(GameType.MonopolyDeal);
    }

    private void assertSeatCopiesPreserveLiveRandomness(GameType gameType) {
        long seed = 42;
        AbstractGameState control = gameType.createGameInstance(2, seed).getGameState();
        AbstractGameState observed = gameType.createGameInstance(2, seed).getGameState();

        for (int repeat = 0; repeat < 5; repeat++) {
            observed.copy(0);
            observed.copy(1);
        }

        assertEquals("Seat copies advanced the live game's RNG",
                control.getRnd().nextLong(), observed.getRnd().nextLong());
    }
}
