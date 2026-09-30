package games.euchre;

import core.components.FrenchCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static games.euchre.EuchreTestUtils.*;
import static org.junit.Assert.*;

public class EuchreSetupTest {

    @Test
    public void theDealGivesFiveCardsEachAndTurnsUpTheTopOfAFourCardKitty() {
        for (long seed = 1; seed <= 5; seed++) {
            EuchreGameState state = newState(seed);
            String s = "seed " + seed + ": ";
            List<FrenchCard> cards = new ArrayList<>();
            for (int p = 0; p < 4; p++) {
                assertEquals(s + "hand " + p, 5, state.getPlayerHand(p).getSize());
                cards.addAll(state.getPlayerHand(p).getComponents());
            }
            assertEquals(s + "kitty", 4, state.getKitty().getSize());          // 24 - 4 x 5
            cards.addAll(state.getKitty().getComponents());
            assertEquals(s + "24 distinct cards 9-A", new HashSet<>(ALL_CARDS), new HashSet<>(cards));
            assertEquals(s, 24, cards.size());
            assertEquals(s + "trick", 0, state.getCurrentTrick().getSize());
            assertEquals(s + "discard pile", 0, state.getDiscardPile().getSize());

            assertEquals(s + "up-card is the kitty top", state.getKitty().peek(), state.getUpCard());
            assertEquals(s + "player 3 deals first", 3, state.getDealer());
            assertEquals(s + "the dealer's left bids first", 0, state.getCurrentPlayer());
            assertNull(state.getTrumpSuit());
            assertEquals(-1, state.getMaker());
            assertEquals(0, state.getPasses());
            assertFalse(state.isAlone());
            assertNull(state.getDealerDiscard());
            assertTrue(state.isFirstBiddingRound());
            assertEquals(0, state.getTeamPoints(0));
            assertEquals(0, state.getTeamPoints(1));
        }
    }
}
