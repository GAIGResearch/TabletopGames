package games.tricktaking;

import core.components.Deck;
import core.components.FrenchCard;
import org.junit.Test;

import java.util.Set;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.CoreConstants.VisibilityMode.VISIBLE_TO_OWNER;
import static core.components.FrenchCard.Suite.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

public class KnownVoidsTest {

    @Test
    public void notFollowingSuitRecordsTheLeadSuitAsVoid() {
        KnownVoids kv = new KnownVoids(4);
        kv.record(1, trick(0, "5H"), card("2C"));
        assertEquals(Set.of(Hearts), kv.get(1));
        for (int p : new int[]{0, 2, 3})
            assertEquals(Set.of(), kv.get(p));

        // the lead suit is the first card's: clubs was discarded by player 1, but player 2's club is still a void in hearts
        kv.record(2, trick(0, "5H", "2C"), card("4C"));
        assertEquals(Set.of(Hearts), kv.get(2));
    }

    @Test
    public void followingSuitOrLeadingRecordsNothing() {
        KnownVoids kv = new KnownVoids(4);
        kv.record(1, trick(0, "5H"), card("KH"));      // follows
        kv.record(0, trick(0), card("2C"));            // leads
        for (int p = 0; p < 4; p++)
            assertEquals(Set.of(), kv.get(p));
    }

    @Test
    public void permitsRespectsTheOwnersVoids() {
        KnownVoids kv = new KnownVoids(4);
        kv.get(1).add(Hearts);
        Deck<FrenchCard> hand1 = new Deck<>("Hand 1", 1, VISIBLE_TO_OWNER);
        Deck<FrenchCard> hand2 = new Deck<>("Hand 2", 2, VISIBLE_TO_OWNER);
        Deck<FrenchCard> noOwner = new Deck<>("Pile", VISIBLE_TO_ALL);

        assertFalse(kv.permits(hand1, card("5H")));   // player 1 is void in hearts
        assertTrue(kv.permits(hand1, card("5C")));
        assertTrue(kv.permits(hand2, card("5H")));    // player 2 is not
        assertTrue(kv.permits(noOwner, card("5H")));  // no owner: always
    }

    @Test
    public void copyIsEqualAndIndependent() {
        KnownVoids kv = new KnownVoids(4);
        kv.get(3).add(Spades);
        KnownVoids copy = kv.copy();
        assertEquals(kv, copy);
        assertEquals(kv.hashCode(), copy.hashCode());

        copy.get(3).add(Diamonds);
        copy.get(0).add(Clubs);
        assertEquals(Set.of(Spades), kv.get(3));
        assertEquals(Set.of(), kv.get(0));
        assertNotEquals(kv, copy);
    }
}
