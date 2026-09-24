package games.klaverjassen;

import core.components.FrenchCard;
import games.klaverjassen.KlaverjassenParameters.PartnerTrumpRule;
import games.tricktaking.Trick;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static core.components.FrenchCard.Suite.Clubs;
import static core.components.FrenchCard.Suite.Hearts;
import static games.klaverjassen.KlaverjassenParameters.PartnerTrumpRule.DISCARD;
import static games.klaverjassen.KlaverjassenParameters.PartnerTrumpRule.NO_UNDERTRUMP;
import static games.klaverjassen.KlaverjassenTestUtils.trick;
import static games.tricktaking.TrickTakingTestUtils.cards;
import static org.junit.Assert.assertEquals;

/**
 * KlaverjassenUtils.legalPlays - the Amsterdam play rules - on hand-built hands and free-standing tricks. Hearts are
 * trumps unless stated. Trumps rank J 9 A 10 K Q 8 7, other suits A 10 K Q J 9 8 7. Teams: 0 and 2 v 1 and 3; the
 * player to play is the trick's leader plus the number of cards played. Where the two partnerTrumpRule values agree
 * the test checks both (assertBoth); where they differ there is one test per value.
 */
public class KlaverjassenLegalPlaysTest {

    private static List<FrenchCard> legal(PartnerTrumpRule rule, FrenchCard.Suite trumps, Trick trick,
                                          String... hand) {
        return KlaverjassenUtils.legalPlays(new ArrayList<>(cards(hand)), trick, trumps, rule);
    }

    /**
     * Both partnerTrumpRule values allow exactly the expected cards, in hand order.
     */
    private static void assertBoth(List<FrenchCard> expected, FrenchCard.Suite trumps, Trick trick, String... hand) {
        for (PartnerTrumpRule rule : PartnerTrumpRule.values())
            assertEquals(rule.name(), expected, legal(rule, trumps, trick, hand));
    }

    // ---- 1. Leading ----

    @Test
    public void theLeaderMayPlayAnyCard() {
        // player 2 leads to an empty trick
        assertBoth(cards("7H", "KS", "10D", "AC"), Hearts, trick(Hearts, 2), "7H", "KS", "10D", "AC");
    }

    // ---- 2. Trump led ----

    @Test
    public void trumpLedMustOvertrumpTheHighestTrumpIfAble() {
        // AH led by 0; player 1 holds 10H, 9H, 7H, KS. Only 9H is above the Ace (J 9 A 10 ...); 10H and 7H are below
        // it, and KS is not allowed while holding trumps
        assertBoth(cards("9H"), Hearts, trick(Hearts, 0, "AH"), "10H", "9H", "7H", "KS");
    }

    @Test
    public void trumpLedMustOvertrumpEvenWhenPartnerIsWinning() {
        // partner 0 led KH (winning), 1 played 7H; player 2 holds AH, QH, 7S. AH beats KH, QH does not: trump led, so
        // the overtrump obligation applies regardless of who is winning
        assertBoth(cards("AH"), Hearts, trick(Hearts, 0, "KH", "7H"), "AH", "QH", "7S");
    }

    @Test
    public void trumpLedOvertrumpMeansBeatingTheHighestTrumpNotTheLead() {
        // 0 led 10H, opponent 1 played 9H (highest); player 2 holds JH, AH, 8C. AH beats the lead 10H but not 9H;
        // only JH beats 9H
        assertBoth(cards("JH"), Hearts, trick(Hearts, 0, "10H", "9H"), "JH", "AH", "8C");
    }

    @Test
    public void trumpLedWithOnlyLowerTrumpsAnyTrumpMayBePlayed() {
        // 0 led 9H; player 1 holds AH, KS, 7H, QD. No trump beats 9H (only JH does), so any trump: AH or 7H
        assertBoth(cards("AH", "7H"), Hearts, trick(Hearts, 0, "9H"), "AH", "KS", "7H", "QD");
    }

    @Test
    public void trumpLedWithNoTrumpsAndAnOpponentWinningAnyCard() {
        // opponent 0 led 8H; player 1 holds no trumps: nothing beats it, every card is a non-trump
        assertBoth(cards("KS", "AD"), Hearts, trick(Hearts, 0, "8H"), "KS", "AD");
    }

    @Test
    public void trumpLedWithNoTrumpsAndPartnerWinningAnyCard() {
        // partner 0 led JH, 1 played 7H; player 2 holds no trumps: any non-trump = every card, under either rule
        assertBoth(cards("KS", "8D"), Hearts, trick(Hearts, 0, "JH", "7H"), "KS", "8D");
    }

    // ---- 3. Plain suit led, able to follow ----

    @Test
    public void followingAPlainSuitNeedNotBeatIt() {
        // 0 led KS; player 1 holds 7S, JH, AS, AC. Any spade: 7S (which does not beat KS) as well as AS (which
        // does); no trump, no club
        assertBoth(cards("7S", "AS"), Hearts, trick(Hearts, 0, "KS"), "7S", "JH", "AS", "AC");
    }

    @Test
    public void aPlayerWhoCanFollowNeedNotOvertrumpAnOpponentsTrump() {
        // partner 0 led KS, opponent 1 trumped with 7H; player 2 holds AS, 9H, 8S: must follow spades (either), and
        // may not trump even though 9H would beat 7H
        assertBoth(cards("AS", "8S"), Hearts, trick(Hearts, 0, "KS", "7H"), "AS", "9H", "8S");
    }

    // ---- 4. Cannot follow, an opponent winning ----

    @Test
    public void voidWithAnOpponentWinningWithAPlainCardMustTrump() {
        // opponent 1 led AS; player 2 holds 7H, KD, QH, 8C and no spade. Any trump beats a plain card: 7H or QH
        assertBoth(cards("7H", "QH"), Hearts, trick(Hearts, 1, "AS"), "7H", "KD", "QH", "8C");
    }

    @Test
    public void voidWithAnOpponentWinningMustTrumpWithOtherTrumps() {
        // clubs trumps: opponent 1 led AS; player 2 holds KD, 7C, QH. Only 7C is a trump (QH is plain now)
        assertBoth(cards("7C"), Clubs, trick(Clubs, 1, "AS"), "KD", "7C", "QH");
    }

    @Test
    public void voidWithNoTrumpsAndAnOpponentWinningAnyCard() {
        // opponent 1 led AS; player 2 holds KD, 8C: no trump to play, so any non-trump = every card
        assertBoth(cards("KD", "8C"), Hearts, trick(Hearts, 1, "AS"), "KD", "8C");
    }

    @Test
    public void whenAnOpponentTrumpsPartnersAceThePlayerMustOvertrump() {
        // partner 0 led AS, opponent 1 trumped with 10H (now winning); player 2 holds 7H, 9H, KD, QC. 9H beats 10H
        // (J 9 A 10), 7H does not: only 9H
        assertBoth(cards("9H"), Hearts, trick(Hearts, 0, "AS", "10H"), "7H", "9H", "KD", "QC");
    }

    @Test
    public void voidWithOnlyLowerTrumpsThanTheOpponentsMustNotUndertrump() {
        // partner 0 led AS, opponent 1 trumped with JH; player 2 holds 9H, KD, 7H, QC. No trump beats JH, so any
        // non-trump: KD or QC (no undertrumping)
        assertBoth(cards("KD", "QC"), Hearts, trick(Hearts, 0, "AS", "JH"), "9H", "KD", "7H", "QC");
    }

    @Test
    public void voidHoldingOnlyTrumpsLowerThanTheOpponentsMayPlayAnyCard() {
        // partner 0 led AS, opponent 1 trumped with 9H; player 2 holds AH, 7H: neither beats 9H, and there is no
        // non-trump, so any card
        assertBoth(cards("AH", "7H"), Hearts, trick(Hearts, 0, "AS", "9H"), "AH", "7H");
    }

    @Test
    public void theWinnerChangingTwiceMidTrickDecidesTheObligation() {
        // 1 led 7S, partner 2 won it with AS, opponent 3 trumped with 8H; player 0 holds 7H, QH, 10C. Opponent 3 is
        // winning with 8H: QH beats it, 7H does not
        assertBoth(cards("QH"), Hearts, trick(Hearts, 1, "7S", "AS", "8H"), "7H", "QH", "10C");
    }

    @Test
    public void whenPartnersTrumpIsOvertrumpedByAnOpponentThePlayerMustOvertrumpAgain() {
        // 0 led KS, partner 1 trumped with 8H, opponent 2 overtrumped with QH; player 3 holds 7H, 10H, 9C. 10H beats
        // QH, 7H does not
        assertBoth(cards("10H"), Hearts, trick(Hearts, 0, "KS", "8H", "QH"), "7H", "10H", "9C");
    }

    // ---- 5. Cannot follow, partner winning ----

    @Test
    public void voidWithPartnerWinningWithAPlainCardMayPlayAnything() {
        // 3 led KC, partner 0 took the lead with AC, 1 discarded 8D; player 2 holds 7H, 9H, QS and no club. Partner is
        // winning with a plain card: any card, trumps included
        assertBoth(cards("7H", "9H", "QS"), Hearts, trick(Hearts, 3, "KC", "AC", "8D"), "7H", "9H", "QS");
    }

    /**
     * 1 led AS, partner 2 trumped with 10H, 3 followed with 7S; player 0 (void in spades) is to play, partner winning
     * with the 10 of trumps.
     */
    private static Trick partnerWinsWithTenOfTrumps() {
        return trick(Hearts, 1, "AS", "10H", "7S");
    }

    @Test
    public void partnerWinningWithATrumpDiscardAllowsOnlyNonTrumps() {
        // player 0 holds JH (above 10H), KH (below), QD, 8C. DISCARD: any non-trump - QD or 8C
        assertEquals(cards("QD", "8C"), legal(DISCARD, Hearts, partnerWinsWithTenOfTrumps(), "JH", "KH", "QD", "8C"));
    }

    @Test
    public void partnerWinningWithATrumpNoUndertrumpAlsoAllowsAHigherTrump() {
        // the same hand, NO_UNDERTRUMP: any non-trump or a trump above 10H - JH, QD, 8C (not KH, below 10H)
        assertEquals(cards("JH", "QD", "8C"),
                legal(NO_UNDERTRUMP, Hearts, partnerWinsWithTenOfTrumps(), "JH", "KH", "QD", "8C"));
    }

    @Test
    public void partnerWinningWithATrumpDiscardWithOnlyTrumpsAllowsAnyCard() {
        // player 0 holds AH (above 10H) and 7H (below), no non-trump. DISCARD: any card
        assertEquals(cards("AH", "7H"), legal(DISCARD, Hearts, partnerWinsWithTenOfTrumps(), "AH", "7H"));
    }

    @Test
    public void partnerWinningWithATrumpNoUndertrumpWithOnlyTrumpsAllowsOnlyHigherOnes() {
        // the same hand, NO_UNDERTRUMP: no non-trump, so a trump above 10H - AH only
        assertEquals(cards("AH"), legal(NO_UNDERTRUMP, Hearts, partnerWinsWithTenOfTrumps(), "AH", "7H"));
    }

    @Test
    public void partnerWinningWithATrumpAndOnlyLowerTrumpsHeldBesidesNonTrumpsBothRulesAllowTheNonTrumps() {
        // player 0 holds KH, 7H (both below 10H) and QD. DISCARD: non-trumps; NO_UNDERTRUMP: non-trumps or higher
        // trumps (none): QD either way
        assertBoth(cards("QD"), Hearts, partnerWinsWithTenOfTrumps(), "KH", "7H", "QD");
    }

    @Test
    public void partnerWinningWithATrumpAndOnlyLowerTrumpsHeldBothRulesAllowAnyCard() {
        // player 0 holds KH, 7H only, both below 10H: DISCARD (only trumps held) and NO_UNDERTRUMP (neither a
        // non-trump nor a higher trump) both allow any card
        assertBoth(cards("KH", "7H"), Hearts, partnerWinsWithTenOfTrumps(), "KH", "7H");
    }

    @Test
    public void partnerWinningWithATrumpAndNoTrumpsHeldBothRulesAllowAnyCard() {
        // player 0 holds QD, 8C: every card is a non-trump
        assertBoth(cards("QD", "8C"), Hearts, partnerWinsWithTenOfTrumps(), "QD", "8C");
    }

    /**
     * 3 led AD (opponent winning), partner 0 trumped with 8H (partner now winning), 1 discarded 7C; player 2 to play,
     * with no diamond.
     */
    private static Trick partnerTrumpsAnOpponentsAce() {
        return trick(Hearts, 3, "AD", "8H", "7C");
    }

    @Test
    public void partnerTakingTheTrickWithATrumpMidTrickDiscardForbidsTrumps() {
        // player 2 holds QH (above 8H), 7H (below), 10S. DISCARD: non-trumps only - 10S
        assertEquals(cards("10S"), legal(DISCARD, Hearts, partnerTrumpsAnOpponentsAce(), "QH", "7H", "10S"));
    }

    @Test
    public void partnerTakingTheTrickWithATrumpMidTrickNoUndertrumpAllowsTheHigherTrump() {
        // the same hand, NO_UNDERTRUMP: QH (above 8H) or 10S; not 7H (below)
        assertEquals(cards("QH", "10S"), legal(NO_UNDERTRUMP, Hearts, partnerTrumpsAnOpponentsAce(), "QH", "7H", "10S"));
    }
}
