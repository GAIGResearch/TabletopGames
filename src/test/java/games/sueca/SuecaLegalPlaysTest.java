package games.sueca;

import core.actions.AbstractAction;
import games.tricktaking.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static games.sueca.SuecaTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * Which cards may be played. Hearts are trumps (trump card 7H, already played) and the hands stand for a deal five
 * tricks in.
 */
public class SuecaLegalPlaysTest {

    SuecaGameState state;
    SuecaForwardModel fm;

    @Before
    public void setup() {
        state = newState(5);
        fm = new SuecaForwardModel();
        arrangeHands(state,
                cards("5S", "KH", "3D", "4C", "6C"),
                cards("3S", "KS", "2H", "AH", "4D"),     // spades and trumps
                cards("7C", "JC", "AC", "5H", "3H"),     // no spades or diamonds, but trumps
                cards("QD", "2D", "6D", "5D", "6H"));
        setTrumpCard(state, "7H");                    // on team 0's pile: played in an earlier trick
        assertAllCardsPresent(state);
    }

    private static Set<AbstractAction> plays(String... codes) {
        return Arrays.stream(codes).map(c -> (AbstractAction) new PlayCard<>(card(c))).collect(Collectors.toSet());
    }

    private Set<AbstractAction> legal() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    @Test
    public void theLeaderMayLeadAnyCard() {
        arrangeTrick(state, 0);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(plays("5S", "KH", "3D", "4C", "6C"), legal());
    }

    @Test
    public void aPlayerHoldingTheSuitLedMustFollowItEvenWhenHoldingTrumps() {
        arrangeTrick(state, 0, "5S");
        assertEquals(1, state.getCurrentPlayer());
        // player 1 holds 3S and KS: the trumps 2H and AH and the diamond may not be played
        assertEquals(plays("3S", "KS"), legal());
    }

    @Test
    public void aFollowerNeedNotBeatTheCardWinningTheTrick() {
        // spades led with the Ace: player 1's 3S and KS are both still legal, though neither beats it
        giveLead(0, "5S", "AS");
        assertEquals(plays("3S", "KS"), legal());
    }

    private void giveLead(int leader, String replaced, String lead) {
        // put `lead` in the leader's hand in place of `replaced`, then lead it
        state.getPlayerHand(leader).remove(card(replaced));
        state.getTeamPile(1).addToBottom(card(replaced));
        arrangeTrick(state, leader, lead);
    }

    @Test
    public void aPlayerWithoutTheSuitLedMayPlayAnyCardTrumpsAndDiscards() {
        arrangeTrick(state, 1, "3S");
        assertEquals(2, state.getCurrentPlayer());
        // player 2 holds no spades: trumps (5H, 3H) and discards (7C, JC, AC) are all legal
        assertEquals(plays("7C", "JC", "AC", "5H", "3H"), legal());
    }

    @Test
    public void whenTrumpsAreLedAPlayerHoldingTrumpsMustFollowWithThem() {
        arrangeTrick(state, 2, "3H");
        assertEquals(3, state.getCurrentPlayer());
        // player 3 holds one heart (6H): only it
        assertEquals(plays("6H"), legal());
    }

    @Test
    public void aThirdPlayerFollowsTheSuitLedNotTheSuitOfTheWinningCard() {
        // diamonds led by player 1, trumped by player 2 with 5H: player 3 must still follow diamonds
        arrangeTrick(state, 1, "4D", "5H");
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(plays("QD", "2D", "6D", "5D"), legal());
    }
}
