package games.lawnandorder;

import org.junit.Test;

import java.util.stream.IntStream;

import static games.lawnandorder.LawnAndOrderGameState.Decision.NONE;
import static games.lawnandorder.LawnAndOrderGameState.Phase.PLAY_OBJECT;
import static games.lawnandorder.LawnAndOrderGameState.PlayerStatus.ACTIVE;
import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static org.junit.Assert.*;

/**
 * Round Setup at the start of the game: the deal, the HOA Agenda less one Insider Tip per player, and a clean slate.
 */
public class LawnAndOrderSetupTest {

    @Test
    public void setupDealsFiveCardsAndOneInsiderTipPerPlayer() {
        for (int n : new int[]{2, 3, 6}) {
            LawnAndOrderGameState state = newState(n, 3);
            for (int p = 0; p < n; p++) {
                assertEquals(5, state.getHand(p).getSize());
                assertEquals(0, state.getChosenCard(p).getSize());
                assertEquals(0, state.getLawn(p).getSize());
                assertEquals(0, state.getCitations(p));
                assertEquals(ACTIVE, state.getStatus(p));
                assertEquals(NONE, state.getDecision(p));
                assertArrayEquals(new int[]{0, 0, 0}, tracks(state, p));
            }
            // 64 Lawn cards less 5 per player; 16 Rule cards less one Insider Tip per player
            assertEquals(64 - 5 * n, state.getDrawDeck().getSize());
            assertEquals(16 - n, state.getAgenda().getSize());
            assertEquals(n, state.getInsiderTips().getSize());
            assertEquals(0, state.getRevealedRules().getSize());
            assertEquals(0, state.getDiscardDeck().getSize());
            assertEquals(PLAY_OBJECT, state.getGamePhase());
            assertEquals(0, state.getRoundCounter());
            assertEquals(IntStream.range(0, n).boxed().toList(), state.getPlayersStillToChoose());
            assertAllCardsPresent(state);
        }
    }
}
