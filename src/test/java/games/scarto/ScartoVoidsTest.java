package games.scarto;

import core.components.TarotCard;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static core.components.TarotCard.Suit.*;
import static games.scarto.ScartoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Known voids recorded as cards are played, through fm.next. Each scenario starts from a new deal (player 0 leads
 * the first trick), with hands arranged so that every play is legal.
 */
public class ScartoVoidsTest {

    ScartoForwardModel fm;
    ScartoGameState state;

    @Before
    public void setup() {
        fm = new ScartoForwardModel();
        state = newState(7, fm);
    }

    private Set<TarotCard.Suit> voids(int p) {
        return state.getKnownVoids().get(p);
    }

    private void assertNoVoids() {
        for (int p = 0; p < 3; p++)
            assertEquals("voids of player " + p, Set.of(), voids(p));
    }

    @Test
    public void trumpingShowsAVoidInTheSuitLed() {
        // player 1 holds no Cups and trumps the Cup lead: void in Cups; the trump says nothing about trumps
        giveHand(state, 0, cup(5), sword(1));
        giveHand(state, 1, trump(3), sword(2));
        play(state, fm, 0, cup(5));
        play(state, fm, 1, trump(3));
        assertEquals(Set.of(Cups), voids(1));
        assertEquals(Set.of(), voids(0));
    }

    @Test
    public void discardingAPlainCardShowsAVoidInTheSuitLedAndInTrumps() {
        // player 1 holds neither Cups nor trumps and plays a Coin on the Cup lead: void in Cups and (being obliged to
        // trump if able) in Trumps
        giveHand(state, 0, cup(5), sword(1));
        giveHand(state, 1, coin(4), sword(2));
        play(state, fm, 0, cup(5));
        play(state, fm, 1, coin(4));
        assertEquals(Set.of(Cups, Trumps), voids(1));
        assertEquals(Set.of(), voids(0));
        assertEquals(Set.of(), voids(2));
    }

    @Test
    public void theFoolOnAPlainLeadRevealsNothing() {
        // player 1 holds a Cup but may play the Fool at any time: nothing is learnt
        giveHand(state, 0, cup(5), sword(1));
        giveHand(state, 1, fool(), cup(9));
        play(state, fm, 0, cup(5));
        play(state, fm, 1, fool());
        assertNoVoids();
    }

    @Test
    public void afterAFoolLeadTheNextCardRevealsNothingButTheThirdIsJudgedByIt() {
        // player 0 leads the Fool (nothing), player 1 plays a Cup, which sets the suit to follow but reveals nothing
        // (no suit had been led); player 2 then plays a Sword: not a Cup and not a trump -> void in Cups and Trumps
        giveHand(state, 0, fool(), cup(5));
        giveHand(state, 1, cup(3), trump(4));
        giveHand(state, 2, sword(4), coin(6));
        play(state, fm, 0, fool());
        play(state, fm, 1, cup(3));
        assertNoVoids();
        play(state, fm, 2, sword(4));
        assertEquals(Set.of(), voids(0));
        assertEquals(Set.of(), voids(1));
        assertEquals(Set.of(Cups, Trumps), voids(2));
    }

    @Test
    public void leadingAndFollowingSuitRevealNothing() {
        giveHand(state, 0, cup(5), sword(1));
        giveHand(state, 1, cup(9), coin(2));
        giveHand(state, 2, cup(1), coin(3));
        play(state, fm, 0, cup(5));
        assertNoVoids();
        play(state, fm, 1, cup(9));
        play(state, fm, 2, cup(1));
        assertEquals("the trick is complete", 0, state.getCurrentTrick().getSize());
        assertNoVoids();
    }

    @Test
    public void aPlainCardOnATrumpLeadShowsAVoidInTrumpsOnly() {
        // trumps led; player 1 has none and plays a Cup: void in Trumps (the suit led) - and nothing about Cups
        giveHand(state, 0, trump(10), sword(1));
        giveHand(state, 1, cup(3), coin(2));
        play(state, fm, 0, trump(10));
        play(state, fm, 1, cup(3));
        assertEquals(Set.of(Trumps), voids(1));
    }

    @Test
    public void nothingIsRecordedWhenVoidsAreNotRemembered() {
        // the plays of discardingAPlainCardShowsAVoidInTheSuitLedAndInTrumps, which would make player 1 void in Cups
        // and Trumps, but with rememberVoids = false
        ((ScartoParameters) state.getGameParameters()).setParameterValue("rememberVoids", false);
        giveHand(state, 0, cup(5), sword(1));
        giveHand(state, 1, coin(4), sword(2));
        giveHand(state, 2, trump(6), baton(3));
        play(state, fm, 0, cup(5));
        play(state, fm, 1, coin(4));
        play(state, fm, 2, trump(6));
        assertNoVoids();
    }

    @Test
    public void voidsSurviveACompletedTrickAndAreIndependentInACopy() {
        // p0 leads Cup 5, p1 discards Coin 4 (void in Cups and Trumps), p2 follows with Cup 9: the trick completes and
        // the next begins, but what was learnt stays known
        giveHand(state, 0, cup(5), sword(1));
        giveHand(state, 1, coin(4), baton(2));
        giveHand(state, 2, cup(9), cup(10));
        play(state, fm, 0, cup(5));
        play(state, fm, 1, coin(4));
        play(state, fm, 2, cup(9));
        assertEquals("the trick is complete", 0, state.getCurrentTrick().getSize());
        assertEquals(Set.of(Cups, Trumps), voids(1));

        ScartoGameState copy = (ScartoGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(Set.of(Cups, Trumps), copy.getKnownVoids().get(1));

        copy.getKnownVoids().get(1).clear();
        copy.getKnownVoids().get(2).add(Swords);
        assertEquals(Set.of(Cups, Trumps), voids(1));
        assertEquals(Set.of(), voids(2));
        assertNotEquals("the voids are part of the state", state, copy);
    }
}
