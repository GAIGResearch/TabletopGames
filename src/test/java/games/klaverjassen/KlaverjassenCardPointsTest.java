package games.klaverjassen;

import core.components.FrenchCard;
import org.junit.Before;
import org.junit.Test;

import static core.components.FrenchCard.Suite.*;
import static games.klaverjassen.KlaverjassenTestUtils.FULL_PACK;
import static games.tricktaking.TrickTakingTestUtils.card;
import static org.junit.Assert.assertEquals;

/**
 * KlaverjassenParameters.cardPoints.
 */
public class KlaverjassenCardPointsTest {

    KlaverjassenParameters params;

    @Before
    public void setup() {
        params = new KlaverjassenParameters();
    }

    private int points(String code, FrenchCard.Suite trumps) {
        return params.cardPoints(card(code), trumps);
    }

    @Test
    public void trumpCardPoints() {
        assertEquals(20, points("JH", Hearts));
        assertEquals(14, points("9H", Hearts));
        assertEquals(11, points("AH", Hearts));
        assertEquals(10, points("10H", Hearts));
        assertEquals(4, points("KH", Hearts));
        assertEquals(3, points("QH", Hearts));
        assertEquals(0, points("8H", Hearts));
        assertEquals(0, points("7H", Hearts));
    }

    @Test
    public void plainCardPoints() {
        assertEquals(11, points("AS", Hearts));
        assertEquals(10, points("10S", Hearts));
        assertEquals(4, points("KS", Hearts));
        assertEquals(3, points("QS", Hearts));
        assertEquals(2, points("JS", Hearts));
        assertEquals(0, points("9S", Hearts));
        assertEquals(0, points("8S", Hearts));
        assertEquals(0, points("7S", Hearts));
    }

    @Test
    public void theJackAndNineScoreDifferentlyInTrumps() {
        assertEquals(20, points("JD", Diamonds));
        assertEquals(2, points("JD", Clubs));
        assertEquals(14, points("9C", Clubs));
        assertEquals(0, points("9C", Diamonds));
        // the others score the same either way
        assertEquals(11, points("AC", Clubs));
        assertEquals(11, points("AC", Spades));
        assertEquals(3, points("QS", Spades));
        assertEquals(3, points("QS", Diamonds));
    }

    @Test
    public void thePackHolds152CardPointsWhateverTheTrumps() {
        // trumps 20 + 14 + 11 + 10 + 4 + 3 = 62; each other suit 11 + 10 + 4 + 3 + 2 = 30; 62 + 3 * 30 = 152
        assertEquals(32, FULL_PACK.size());
        for (FrenchCard.Suite trumps : FrenchCard.Suite.values()) {
            int total = 0;
            for (FrenchCard c : FULL_PACK)
                total += params.cardPoints(c, trumps);
            assertEquals("pack total with " + trumps + " trumps", 152, total);
        }
    }
}
