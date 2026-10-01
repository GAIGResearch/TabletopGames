package games.tricktaking;

import org.junit.Test;

import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.assertEquals;

public class FollowSuitTest {

    @Test
    public void anyCardMayBeLed() {
        assertEquals(cards("KS", "2H", "3D", "9H"),
                PlayRule.FOLLOW_SUIT.legalPlays(cards("KS", "2H", "3D", "9H"), trick(0)));
    }

    @Test
    public void aPlayerHoldingTheLeadSuitMustFollowIt() {
        // hearts led: only the two hearts, in hand order
        assertEquals(cards("2H", "9H"),
                PlayRule.FOLLOW_SUIT.legalPlays(cards("KS", "2H", "3D", "9H"), trick(0, "QH")));
    }

    @Test
    public void theLeadSuitIsTheFirstCardsNotTheLatest() {
        // clubs led, then a heart discarded: the hand must follow clubs, not hearts
        assertEquals(cards("5C"),
                PlayRule.FOLLOW_SUIT.legalPlays(cards("2H", "5C"), trick(0, "QC", "AH")));
    }

    @Test
    public void aPlayerVoidInTheLeadSuitMayPlayAnything() {
        // clubs led, no clubs in hand: every card, trumping never compulsory (nothing to exclude)
        assertEquals(cards("KS", "2H", "3D"),
                PlayRule.FOLLOW_SUIT.legalPlays(cards("KS", "2H", "3D"), trick(0, "QC")));
    }
}
