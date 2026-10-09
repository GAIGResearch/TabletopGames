package games.tricktaking;

import core.components.Deck;
import core.components.FrenchCard;
import core.components.TarotCard;
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
        KnownVoids<FrenchCard.Suite> kv = new KnownVoids<>(4, FrenchCard.Suite.class);
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
        KnownVoids<FrenchCard.Suite> kv = new KnownVoids<>(4, FrenchCard.Suite.class);
        kv.record(1, trick(0, "5H"), card("KH"));      // follows
        kv.record(0, trick(0), card("2C"));            // leads
        for (int p = 0; p < 4; p++)
            assertEquals(Set.of(), kv.get(p));
    }

    @Test
    public void permitsRespectsTheOwnersVoids() {
        KnownVoids<FrenchCard.Suite> kv = new KnownVoids<>(4, FrenchCard.Suite.class);
        kv.get(1).add(Hearts);
        Deck<FrenchCard> hand1 = new Deck<>("Hand 1", 1, VISIBLE_TO_OWNER);
        Deck<FrenchCard> hand2 = new Deck<>("Hand 2", 2, VISIBLE_TO_OWNER);
        Deck<FrenchCard> noOwner = new Deck<>("Pile", VISIBLE_TO_ALL);

        var permits = kv.permits(CardOrder.STANDARD);
        assertFalse(permits.test(hand1, card("5H")));   // player 1 is void in hearts
        assertTrue(permits.test(hand1, card("5C")));
        assertTrue(permits.test(hand2, card("5H")));    // player 2 is not
        assertTrue(permits.test(noOwner, card("5H")));  // no owner: always
    }

    // ---- recordMustTrump ----

    private static KnownVoids<TarotCard.Suit> tarotVoids() {
        return new KnownVoids<>(3, TarotCard.Suit.class);
    }

    @Test
    public void mustTrumpFollowingSuitRecordsNothing() {
        KnownVoids<TarotCard.Suit> kv = tarotVoids();
        kv.recordMustTrump(1, tarotTrick(0, cup(5)), cup(9), TarotCard.Suit.Trumps);
        assertEquals(Set.of(), kv.get(1));
    }

    @Test
    public void mustTrumpATrumpOnAPlainLeadShowsOnlyTheSuitLed() {
        // a trump on a Cup lead: no Cups, but the trump is what the rule demands - nothing said about trumps
        KnownVoids<TarotCard.Suit> kv = tarotVoids();
        kv.recordMustTrump(1, tarotTrick(0, cup(5)), trump(3), TarotCard.Suit.Trumps);
        assertEquals(Set.of(TarotCard.Suit.Cups), kv.get(1));
    }

    @Test
    public void mustTrumpAPlainDiscardShowsTheSuitLedAndTrumps() {
        // a Coin on a Cup lead: no Cups, and (having to trump if able) no trumps either
        KnownVoids<TarotCard.Suit> kv = tarotVoids();
        kv.recordMustTrump(1, tarotTrick(0, cup(5)), coin(4), TarotCard.Suit.Trumps);
        assertEquals(Set.of(TarotCard.Suit.Cups, TarotCard.Suit.Trumps), kv.get(1));
        assertEquals(Set.of(), kv.get(0));
        assertEquals(Set.of(), kv.get(2));
    }

    @Test
    public void mustTrumpTheFoolRevealsNothing() {
        KnownVoids<TarotCard.Suit> kv = tarotVoids();
        kv.recordMustTrump(1, tarotTrick(0, cup(5)), fool(), TarotCard.Suit.Trumps);
        assertEquals(Set.of(), kv.get(1));
    }

    @Test
    public void mustTrumpAPlainCardOnATrumpLeadShowsOnlyTrumps() {
        // the suit led is trumps: failing to follow shows no trumps; the Cup's own suit says nothing
        KnownVoids<TarotCard.Suit> kv = tarotVoids();
        kv.recordMustTrump(1, tarotTrick(0, trump(10)), cup(3), TarotCard.Suit.Trumps);
        assertEquals(Set.of(TarotCard.Suit.Trumps), kv.get(1));
    }

    @Test
    public void mustTrumpLeadingOrPlayingAfterALoneFoolLeadRevealsNothing() {
        KnownVoids<TarotCard.Suit> kv = tarotVoids();
        kv.recordMustTrump(0, tarotTrick(0), coin(4), TarotCard.Suit.Trumps);          // leads
        kv.recordMustTrump(1, tarotTrick(0, fool()), coin(4), TarotCard.Suit.Trumps);  // no suit led yet
        for (int p = 0; p < 3; p++)
            assertEquals(Set.of(), kv.get(p));
    }

    @Test
    public void mustTrumpAddsToVoidsAlreadyKnown() {
        // player 1 already known void in Swords; a Coin on a Cup lead adds Cups and Trumps
        KnownVoids<TarotCard.Suit> kv = tarotVoids();
        kv.get(1).add(TarotCard.Suit.Swords);
        kv.recordMustTrump(1, tarotTrick(0, cup(5)), coin(4), TarotCard.Suit.Trumps);
        assertEquals(Set.of(TarotCard.Suit.Swords, TarotCard.Suit.Cups, TarotCard.Suit.Trumps), kv.get(1));
    }

    @Test
    public void copyIsEqualAndIndependent() {
        KnownVoids<FrenchCard.Suite> kv = new KnownVoids<>(4, FrenchCard.Suite.class);
        kv.get(3).add(Spades);
        KnownVoids<FrenchCard.Suite> copy = kv.copy();
        assertEquals(kv, copy);
        assertEquals(kv.hashCode(), copy.hashCode());

        copy.get(3).add(Diamonds);
        copy.get(0).add(Clubs);
        assertEquals(Set.of(Spades), kv.get(3));
        assertEquals(Set.of(), kv.get(0));
        assertNotEquals(kv, copy);
    }
}
