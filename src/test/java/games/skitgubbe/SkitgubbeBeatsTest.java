package games.skitgubbe;

import core.components.FrenchCard;
import org.junit.Test;

import static games.skitgubbe.SkitgubbeTestUtils.card;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** SkitgubbeUtils.beats: which card may be played on the top card of a phase-two trick. Trumps are Hearts. */
public class SkitgubbeBeatsTest {

    private static final FrenchCard.Suite TRUMP = FrenchCard.Suite.Hearts;

    private static boolean beats(String card, String top) {
        return SkitgubbeUtils.beats(card(card), card(top), TRUMP);
    }

    @Test
    public void aHigherCardOfTheSameSuitBeats() {
        assertTrue(beats("9S", "7S"));
        assertTrue(beats("AS", "KS"));      // Ace 14 > King 13
        assertTrue(beats("JC", "10C"));
    }

    @Test
    public void aLowerOrEqualCardOfTheSameSuitDoesNotBeat() {
        assertFalse(beats("5S", "7S"));
        assertFalse(beats("KS", "AS"));     // King 13 < Ace 14
        assertFalse(beats("7S", "7S"));     // equal rank does not beat
    }

    @Test
    public void anyTrumpBeatsANonTrumpEvenTheTwo() {
        assertTrue(beats("2H", "AS"));
        assertTrue(beats("2H", "7D"));
    }

    @Test
    public void aNonTrumpNeverBeatsATrump() {
        assertFalse(beats("AS", "2H"));
        assertFalse(beats("AD", "5H"));
    }

    @Test
    public void onATrumpOnlyAHigherTrumpBeats() {
        assertTrue(beats("8H", "5H"));
        assertFalse(beats("3H", "5H"));
        assertFalse(beats("5H", "5H"));
    }

    @Test
    public void aCardOfAThirdSuitNeverBeatsEvenIfHigher() {
        assertFalse(beats("AD", "7S"));
        assertFalse(beats("KC", "2D"));
    }
}
