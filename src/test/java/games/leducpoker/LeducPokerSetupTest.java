package games.leducpoker;

import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_OWNER;
import static games.leducpoker.LeducPokerTestUtils.*;
import static org.junit.Assert.*;

public class LeducPokerSetupTest {

    LeducPokerParameters params;
    LeducPokerGameState state;
    LeducPokerForwardModel fm;

    @Before
    public void setup() {
        params = new LeducPokerParameters();
        params.setRandomSeed(42);
        state = new LeducPokerGameState(params, 2);
        fm = new LeducPokerForwardModel();
        fm.setup(state);
    }

    @Test
    public void eachPlayerAntesAndHoldsOnePrivateCard() {
        for (int p = 0; p < 2; p++) {
            assertEquals(1, state.getHand(p).getSize());
            assertEquals(VISIBLE_TO_OWNER, state.getHand(p).getVisibilityMode());
            assertEquals(p, state.getHand(p).getOwnerId());
            assertEquals("ante", 1, state.getContribution(p));
            assertEquals(0, state.getNetChips(p));
        }
        assertEquals("6 cards - 2 dealt", 4, state.getDrawDeck().getSize());
        assertEquals(0, state.getBoard().getSize());
        assertEquals("1 + 1", 2, state.getPot());
        assertEquals(0, state.getBettingRound());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getRaisesThisRound());
        assertEquals(0, state.getActionsThisRound());
    }

    @Test
    public void deckIsTwoEachOfJackQueenKingInSpadesAndHearts() {
        assertAllCardsPresent(state);
        assertEquals(new HashSet<>(ALL_CARDS), new HashSet<>(params.deckCards()));
    }

    @Test
    public void differentSeedsGiveDifferentDeals() {
        // With only six cards two seeds can deal the same, so compare several seeds: they must not all agree
        Set<List<FrenchCard>> deals = new HashSet<>();
        for (long seed = 1; seed <= 8; seed++) {
            LeducPokerParameters p = new LeducPokerParameters();
            p.setRandomSeed(seed);
            LeducPokerGameState s = new LeducPokerGameState(p, 2);
            fm.setup(s);
            List<FrenchCard> deal = new ArrayList<>();
            deal.add(s.getHand(0).get(0));
            deal.add(s.getHand(1).get(0));
            deal.addAll(s.getDrawDeck().getComponents());
            deals.add(deal);
        }
        assertTrue("only " + deals.size() + " distinct deals from 8 seeds", deals.size() > 1);
    }
}
