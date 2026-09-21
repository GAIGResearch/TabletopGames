package games.whist;

import core.actions.AbstractAction;
import games.tricktaking.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static games.tricktaking.TrickTakingTestUtils.card;
import static games.whist.WhistTestUtils.*;
import static org.junit.Assert.assertEquals;

/**
 * The legal actions: one PlayCard per card the follow-suit rule allows.
 */
public class WhistPlayRulesTest {

    WhistGameState state;
    WhistForwardModel fm;

    @Before
    public void setup() {
        state = newState(42);
        fm = new WhistForwardModel();
        setTrumps(state, "7S");   // spades trumps throughout
    }

    private Set<AbstractAction> plays(String... codes) {
        Set<AbstractAction> s = new HashSet<>();
        for (String c : codes)
            s.add(new PlayCard(card(c)));
        return s;
    }

    private Set<AbstractAction> available() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    @Test
    public void theLeaderMayPlayAnyCard() {
        giveHand(state, 0, "2H", "KS", "10D", "AC");
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(plays("2H", "KS", "10D", "AC"), available());
    }

    @Test
    public void aFollowerHoldingTheLeadSuitMayPlayOnlyThatSuit() {
        giveHand(state, 1, "2H", "KH", "3S", "4C");
        arrangeTrick(state, 0, "9H");                  // player 0 led hearts; player 1 to play
        assertEquals(1, state.getCurrentPlayer());
        // must follow hearts: the trump 3S and the 4C are not allowed
        assertEquals(plays("2H", "KH"), available());
    }

    @Test
    public void aFollowerVoidInTheLeadSuitMayPlayAnyCardIncludingTrumps() {
        giveHand(state, 2, "3S", "4C", "JD");
        arrangeTrick(state, 0, "9H", "10H");           // hearts led; player 2 to play, holding none
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(plays("3S", "4C", "JD"), available());
    }

    @Test
    public void theLeadSuitDecidesForALaterPlayerAfterADiscard() {
        giveHand(state, 3, "2C", "5H", "QS");
        arrangeTrick(state, 1, "8C", "AD");            // player 1 led clubs, player 2 discarded; player 3 to play
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(plays("2C"), available());
    }
}
