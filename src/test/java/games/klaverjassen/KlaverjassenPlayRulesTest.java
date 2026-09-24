package games.klaverjassen;

import core.actions.AbstractAction;
import games.tricktaking.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static core.components.FrenchCard.Suite.Hearts;
import static games.klaverjassen.KlaverjassenParameters.PartnerTrumpRule.NO_UNDERTRUMP;
import static games.klaverjassen.KlaverjassenTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.card;
import static org.junit.Assert.assertEquals;

/**
 * The legal plays the forward model offers once trumps are chosen, including the partnerTrumpRule parameter.
 * Every branch of the rules is tested on KlaverjassenUtils.legalPlays in KlaverjassenLegalPlaysTest. Hearts are
 * trumps throughout.
 */
public class KlaverjassenPlayRulesTest {

    KlaverjassenGameState state;
    KlaverjassenForwardModel fm;

    @Before
    public void setup() {
        state = newState(42);
        fm = new KlaverjassenForwardModel();
        setTrumps(state, Hearts);   // hearts trumps throughout
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
    public void theLeaderMayPlayAnyCardIncludingATrump() {
        giveHand(state, 0, "7H", "KS", "10D", "AC");
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(plays("7H", "KS", "10D", "AC"), available());
    }

    @Test
    public void aFollowerHoldingThePlainSuitLedMayPlayOnlyThatSuit() {
        giveHand(state, 1, "7S", "KS", "JH", "AC");
        arrangeTrick(state, 0, "9S");                  // player 0 led spades; player 1 to play
        assertEquals(1, state.getCurrentPlayer());
        // must follow spades: neither the trump JH nor the AC is allowed
        assertEquals(plays("7S", "KS"), available());
    }

    @Test
    public void aFollowerVoidInTheSuitLedWhosePartnerIsWinningWithAPlainCardMayPlayAnything() {
        giveHand(state, 2, "7H", "JH", "KD", "8C");
        arrangeTrick(state, 0, "AS", "7S");            // partner 0's Ace of Spades is winning; player 2 holds no spade
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(plays("7H", "JH", "KD", "8C"), available());
    }

    @Test
    public void theSuitLedDecidesForALaterPlayerAfterADiscard() {
        giveHand(state, 3, "8C", "QD", "10H");
        arrangeTrick(state, 1, "KC", "AD");            // player 1 led clubs, player 2 discarded; player 3 to play
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(plays("8C"), available());
    }

    @Test
    public void aPlayerVoidInTheSuitLedMustTrumpAnOpponentsPlainCard() {
        giveHand(state, 2, "7H", "KD", "QH", "8C");
        arrangeTrick(state, 1, "AS");                  // opponent 1 led AS; player 2 has no spade
        assertEquals(2, state.getCurrentPlayer());
        // every trump beats a plain winner, and one must be played
        assertEquals(plays("7H", "QH"), available());
    }

    @Test
    public void whenTrumpsAreLedAPlayerMustOvertrumpIfAble() {
        giveHand(state, 1, "10H", "9H", "7H", "KS");
        arrangeTrick(state, 0, "AH");
        // only 9H ranks above AH (J 9 A 10 K Q 8 7)
        assertEquals(plays("9H"), available());
    }

    @Test
    public void partnerWinningWithATrumpByDefaultAPlayerMayDiscardButNotTrump() {
        giveHand(state, 0, "JH", "KH", "QD", "8C");
        arrangeTrick(state, 1, "AS", "10H", "7S");     // partner 2 is winning with 10H; player 0 has no spade
        assertEquals(0, state.getCurrentPlayer());
        // default partnerTrumpRule DISCARD: any non-trump
        assertEquals(plays("QD", "8C"), available());
    }

    @Test
    public void partnerWinningWithATrumpUnderNoUndertrumpAPlayerMayAlsoOvertrumpPartner() {
        KlaverjassenParameters params = new KlaverjassenParameters();
        params.setParameterValue("partnerTrumpRule", NO_UNDERTRUMP);
        state = newState(42, params);
        setTrumps(state, Hearts);
        giveHand(state, 0, "JH", "KH", "QD", "8C");
        arrangeTrick(state, 1, "AS", "10H", "7S");     // as above, with NO_UNDERTRUMP
        // any non-trump, or a trump above partner's 10H: JH (KH is below)
        assertEquals(plays("JH", "QD", "8C"), available());
    }
}
