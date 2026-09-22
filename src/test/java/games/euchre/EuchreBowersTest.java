package games.euchre;

import core.Game;
import core.components.FrenchCard;
import games.euchre.actions.CallTrump;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.euchre.EuchreTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.card;
import static org.junit.Assert.*;

/**
 * The bowers in play: Hearts trumps, so the JH is the right bower and the JD the left bower, a trump and not a
 * Diamond. The bower deal (EuchreTestUtils.bowerDeal), with Hearts called in round 2 by player 1 (dealer 3, player 0
 * leads; the 9C up-card turned down in the kitty):
 * <pre>
 * P0: JD AS KS 9D 10D      (the left bower is the only trump)
 * P1: JH QH AD 9S 10S      (the right bower)
 * P2: AH KH KD QD JS       (JS is an ordinary spade, the only one)
 * P3: 10H 9H QS AC JC      (dealer; no diamonds)
 * </pre>
 * The unit tests arrange tricks with the state's card order; the integration tests at the end drive the bidding
 * through fm.next, so the tricks are the forward model's own.
 */
public class EuchreBowersTest {

    static final int COPIES = 50;

    EuchreGameState state;
    EuchreForwardModel fm;

    @Before
    public void setup() {
        state = newState(42);
        fm = new EuchreForwardModel();
        bowerDeal(state);
        startPlay(state, Hearts, 1, null);
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void leadingTheLeftBowerLeadsTrumps() {
        arrangeTrick(state, 0, "JD");
        // player 1 must follow trumps with JH or QH; the AD is not the suit led
        assertEquals(plays("JH", "QH"), available(state, fm));
    }

    @Test
    public void aPlayerWithNoTrumpsMayPlayAnyCardOnTheLeftBowerAndIsKnownVoidInTrumps() {
        giveHand(state, 1, "AD", "KD", "QD", "9S", "10S");   // no Hearts, no JD
        arrangeTrick(state, 0, "JD");
        // trumps led, player 1 holds none: any card, the Diamonds included
        assertEquals(plays("AD", "KD", "QD", "9S", "10S"), available(state, fm));
        playCards(state, fm, "AD");
        // the AD did not follow the suit led, which was Hearts (the JD's suit), not Diamonds
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(1));
    }

    @Test
    public void aPlayerWhoseOnlyTrumpIsTheLeftBowerMustPlayItWhenTrumpsAreLed() {
        arrangeTrick(state, 3, "10H");
        // player 0 holds no printed Heart, but the JD is a trump
        assertEquals(plays("JD"), available(state, fm));
        playCards(state, fm, "JD");
        // the JD followed trumps: nothing learnt about player 0
        assertEquals(Set.of(), state.getKnownVoids().get(0));
    }

    @Test
    public void theLeftBowerDoesNotFollowItsPrintedSuit() {
        arrangeTrick(state, 3, "QD");
        // Diamonds led: player 0 must follow with the 9D or 10D; the JD is not a Diamond
        assertEquals(plays("9D", "10D"), available(state, fm));
    }

    @Test
    public void aPlayerWhoseOnlyDiamondIsTheLeftBowerIsVoidInDiamondsAndTheBowerTrumpsTheAce() {
        giveHand(state, 0, "JD", "AS", "KS", "9S", "10S");   // 9D and 10D out of play
        arrangeTrick(state, 1, "AD", "KD", "AC");
        // Diamonds led: player 0 holds no Diamond (the JD is a trump), so may play any card
        assertEquals(plays("JD", "AS", "KS", "9S", "10S"), available(state, fm));
        playCards(state, fm, "JD");
        // the only trump in the trick: the JD beats the AD led, and player 0 leads next
        assertArrayEquals(new int[]{1, 0, 0, 0}, state.tricksTaken);
        assertEquals(0, state.getCurrentPlayer());
        // and player 0 did not follow Diamonds
        assertEquals(Set.of(Diamonds), state.getKnownVoids().get(0));
    }

    @Test
    public void aPlayerKnownVoidInTrumpsIsNeverGivenTheLeftBower() {
        giveHand(state, 2, "KD", "QD", "JS", "9S", "10S");   // no trumps: AH KH out of play; P1 now JH QH AD
        state.knownVoids.get(2).add(Hearts);
        // for player 3, the JD may be in the hands of 0, 1 or the kitty, but not player 2's (a trump)
        int p2Changed = 0;
        for (int i = 0; i < COPIES; i++) {
            EuchreGameState copy = (EuchreGameState) state.copy(3);
            assertAllCardsPresent(copy);
            for (FrenchCard c : copy.getPlayerHand(2).getComponents()) {
                assertNotEquals("player 2 given the left bower", card("JD"), c);
                assertNotEquals("player 2 given " + c, Hearts, c.suite);
            }
            if (!Set.copyOf(copy.getPlayerHand(2).getComponents())
                    .equals(Set.copyOf(state.getPlayerHand(2).getComponents())))
                p2Changed++;
        }
        assertTrue("player 2's hand was never redeterminised", p2Changed > 0);
    }

    @Test
    public void aPlayerKnownVoidInTheLeftBowersPrintedSuitMayBeGivenIt() {
        giveHand(state, 0, "JD", "AS", "KS", "9S", "10S");   // 9D and 10D out of play; P1 now JH QH AD
        state.knownVoids.get(0).add(Diamonds);
        int withLeftBower = 0, withoutLeftBower = 0;
        for (int i = 0; i < COPIES; i++) {
            EuchreGameState copy = (EuchreGameState) state.copy(3);
            assertAllCardsPresent(copy);
            for (FrenchCard c : copy.getPlayerHand(0).getComponents())
                if (!c.equals(card("JD")))
                    assertNotEquals("player 0 given " + c, Diamonds, c.suite);
            if (copy.getPlayerHand(0).contains(card("JD")))
                withLeftBower++;
            else
                withoutLeftBower++;
        }
        // the JD is a trump, not a Diamond: player 0 may hold it, but need not
        assertTrue("player 0 never given the left bower", withLeftBower > 0);
        assertTrue("the left bower never moved from player 0", withoutLeftBower > 0);
    }

    // ---- integration: the forward model's own tricks, from the bidding ----

    private static EuchreGameState bidHeartsInRoundTwo(Game game) {
        EuchreGameState state = (EuchreGameState) game.getGameState();
        EuchreForwardModel fm = (EuchreForwardModel) game.getForwardModel();
        bowerDeal(state);
        // round 1: all pass on the 9C; round 2: player 0 passes, player 1 calls Hearts; player 0 leads
        pass(state, fm, 5);
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new CallTrump(Hearts, false));
        assertEquals(0, state.getCurrentPlayer());
        return state;
    }

    @Test
    public void theLeftBowerLedToTheFirstTrickLeadsTrumpsAndBeatsTheAceOfTrumps() {
        Game game = newGame(5);
        EuchreForwardModel fm = (EuchreForwardModel) game.getForwardModel();
        EuchreGameState state = bidHeartsInRoundTwo(game);

        playCards(state, fm, "JD");
        assertEquals(plays("JH", "QH"), available(state, fm));      // not the AD
        playCards(state, fm, "QH");
        assertEquals(plays("AH", "KH"), available(state, fm));      // not the KD, QD
        playCards(state, fm, "AH");
        assertEquals(plays("10H", "9H"), available(state, fm));
        playCards(state, fm, "9H");
        // trumps: JD (left bower) > AH > QH > 9H: player 0 wins and leads
        assertArrayEquals(new int[]{1, 0, 0, 0}, state.tricksTaken);
        assertEquals(0, state.getCurrentPlayer());
        for (int p = 0; p < 4; p++)
            assertEquals("everyone followed trumps", Set.of(), state.getKnownVoids().get(p));
        assertAllCardsPresent(state);
    }

    @Test
    public void inTheNextTrickTheRightBowerBeatsTheLeftBower() {
        Game game = newGame(11);
        EuchreForwardModel fm = (EuchreForwardModel) game.getForwardModel();
        EuchreGameState state = bidHeartsInRoundTwo(game);

        // trick 1: spades; player 2's only spade is the JS (an ordinary spade with Hearts trumps) and must be played
        playCards(state, fm, "AS", "9S");
        assertEquals(plays("JS"), available(state, fm));
        playCards(state, fm, "JS", "QS");
        // AS > QS > JS > 9S: player 0 wins and leads
        assertArrayEquals(new int[]{1, 0, 0, 0}, state.tricksTaken);
        assertEquals(0, state.getCurrentPlayer());

        // trick 2: the left bower led; trumps must follow
        playCards(state, fm, "JD");
        assertEquals(plays("JH", "QH"), available(state, fm));
        playCards(state, fm, "JH");
        assertEquals(plays("AH", "KH"), available(state, fm));
        playCards(state, fm, "KH");
        assertEquals(plays("10H", "9H"), available(state, fm));
        playCards(state, fm, "10H");
        // JH (right bower) > JD (left bower) > KH > 10H: player 1 wins and leads
        assertArrayEquals(new int[]{1, 1, 0, 0}, state.tricksTaken);
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }
}
