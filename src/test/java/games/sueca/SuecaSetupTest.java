package games.sueca;

import core.components.FrenchCard;
import org.junit.Test;

import java.util.List;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static games.sueca.SuecaTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * The deal from setup, the pack, the card order and the card points.
 */
public class SuecaSetupTest {

    @Test
    public void setupDealsTenCardsEachFromTheFortyCardPackWithTheDealersLastCardAsTrumps() {
        SuecaGameState state = newState(11);
        assertAllCardsPresent(state);                 // 40 cards: no 8, 9 or 10
        for (int p = 0; p < 4; p++)
            assertEquals("hand of " + p, 10, state.getPlayerHand(p).getSize());
        assertEquals(0, state.getTeamPile(0).getSize());
        assertEquals(0, state.getTeamPile(1).getSize());
        assertEquals(0, state.getCurrentTrick().getSize());

        // player 3 deals the first deal, and player 0 leads
        assertEquals(3, state.getDealer());
        assertEquals(0, state.getFirstLeader());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getLeader());

        // the dealer's last card (index 0 of the hand) is turned up: its suit is trumps
        assertEquals(state.getPlayerHand(3).get(0), state.getTrumpCard());
        assertEquals(state.getTrumpCard().suite, state.getTrumpSuit());
    }

    @Test
    public void differentSeedsGiveDifferentDeals() {
        SuecaGameState a = newState(1), b = newState(2);
        assertNotEquals(cardsOf(a.getPlayerHand(0)), cardsOf(b.getPlayerHand(0)));
    }

    @Test
    public void newPackHasTheFortyCardsWithoutEightsNinesAndTens() {
        List<FrenchCard> pack = SuecaUtils.newPack("Pack", HIDDEN_TO_ALL).getComponents();
        assertEquals(40, pack.size());
        assertEquals(new java.util.HashSet<>(FULL_PACK), new java.util.HashSet<>(pack));
    }

    @Test
    public void cardsRankAceSevenKingJackQueenThenSixDownToTwoInEverySuit() {
        // lowest first: 2 3 4 5 6 Q J K 7 A
        for (String suit : new String[]{"S", "H", "D", "C"}) {
            List<FrenchCard> ascending = cards("2" + suit, "3" + suit, "4" + suit, "5" + suit, "6" + suit,
                    "Q" + suit, "J" + suit, "K" + suit, "7" + suit, "A" + suit);
            for (int i = 1; i < ascending.size(); i++)
                assertTrue(ascending.get(i) + " above " + ascending.get(i - 1),
                        SuecaUtils.CARD_ORDER.rank(ascending.get(i)) > SuecaUtils.CARD_ORDER.rank(ascending.get(i - 1)));
            for (FrenchCard c : ascending)
                assertEquals(c.suite, SuecaUtils.CARD_ORDER.suitOf(c));
        }
    }

    @Test
    public void cardPointsAreAceElevenSevenTenKingFourJackThreeQueenTwo() {
        SuecaParameters params = new SuecaParameters();
        assertEquals(11, params.cardPoints(card("AH")));
        assertEquals(10, params.cardPoints(card("7S")));
        assertEquals(4, params.cardPoints(card("KD")));
        assertEquals(3, params.cardPoints(card("JC")));
        assertEquals(2, params.cardPoints(card("QH")));
        for (String c : new String[]{"6S", "5H", "4D", "3C", "2S"})
            assertEquals(c, 0, params.cardPoints(card(c)));
        // per suit A 11 + 7 10 + K 4 + J 3 + Q 2 = 30; four suits = 120
        assertEquals(120, FULL_PACK.stream().mapToInt(params::cardPoints).sum());
    }
}
