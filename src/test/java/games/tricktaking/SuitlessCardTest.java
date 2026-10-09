package games.tricktaking;

import core.components.TarotCard;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static core.components.TarotCard.Suit.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * Suitless cards (a CardOrder returning null from suitOf), modelled with a tarot pack whose Fool belongs to no suit
 * ({@link TrickTakingTestUtils#TAROT_ORDER}). All tricks have 3 players, so the card at index i of a trick led by
 * player L was played by (L + i) % 3.
 */
public class SuitlessCardTest {

    private final PlayRule<TarotCard, TarotCard.Suit> follow = PlayRule.followSuit();

    // ---- lead suit ----

    @Test
    public void leadSuitIsNullForAnEmptyTrickAndForATrickHoldingOnlyTheFool() {
        assertNull(tarotTrick(0).getLeadSuit());
        // the Fool has no suit, and no suited card has been played yet
        assertNull(tarotTrick(0, fool()).getLeadSuit());
    }

    @Test
    public void leadSuitIsSetByTheFirstSuitedCardAfterTheFool() {
        // Fool led (no suit), then the 3 of Cups: the first card with a suit is a Cup
        assertEquals(Cups, tarotTrick(0, fool(), cup(3)).getLeadSuit());
        // a later Coin does not change it: the lead suit is the FIRST suited card's
        assertEquals(Cups, tarotTrick(0, fool(), cup(3), coin(5)).getLeadSuit());
    }

    @Test
    public void foolPlayedAfterACupLeadLeavesTheLeadSuitCups() {
        assertEquals(Cups, tarotTrick(0, cup(5), fool()).getLeadSuit());
    }

    // ---- winner ----

    @Test
    public void foolLedNeverWinsTheBestOfTheLeadSuitDoes() {
        // leader 1: Fool (p1), 3 of Cups (p2), King of Cups (p0). The Fool cannot win; the lead suit is Cups (the
        // first suited card), no trump played, so the highest Cup wins: King (14) > 3 -> player 0
        Trick<TarotCard, TarotCard.Suit> t = tarotTrick(1, fool(), cup(3), cup(TarotCard.KING));
        assertEquals(0, t.winner(Trumps));
        assertEquals(0, t.winner(null));
    }

    @Test
    public void foolPlayedLastNeverWins() {
        // leader 0: 5 of Cups (p0), King of Cups (p1), Fool (p2). Best Cup is the King -> player 1, whether Trumps
        // are trumps or there are no trumps at all (the Fool's null suit must not be taken for a null trump suit)
        Trick<TarotCard, TarotCard.Suit> t = tarotTrick(0, cup(5), cup(TarotCard.KING), fool());
        assertEquals(1, t.winner(Trumps));
        assertEquals(1, t.winner(null));
    }

    @Test
    public void foolPlayedBetweenACupLeadAndATrumpDoesNotWin() {
        // leader 2: 5 of Cups (p2), Fool (p0), trump 3 (p1)
        Trick<TarotCard, TarotCard.Suit> t = tarotTrick(2, cup(5), fool(), trump(3));
        // Trumps are trumps: the only trump played wins -> player 1
        assertEquals(1, t.winner(Trumps));
        // no trumps: trump 3 is just off-suit; the only Cup (the lead suit) is the 5 -> its player, 2
        assertEquals(2, t.winner(null));
    }

    @Test
    public void foolAloneInTheTrickLeavesTheLeaderWinning() {
        // degenerate case: no suited card yet -> the leader (player of index 0), here player 2
        assertEquals(2, tarotTrick(2, fool()).winner(Trumps));
        assertEquals(2, tarotTrick(2, fool()).winner(null));
    }

    // ---- followSuit ----

    @Test
    public void afterTheFoolIsLedAloneAnyCardMayBePlayed() {
        // no suited card in the trick: no suit to follow -> the whole hand, in hand order
        List<TarotCard> hand = List.of(cup(3), coin(5), trump(4), sword(10));
        assertEquals(hand, follow.legalPlays(hand, tarotTrick(0, fool())));
    }

    @Test
    public void aSuitlessCardMayBeLed() {
        // empty trick: no suit to follow -> the whole hand, the Fool included
        List<TarotCard> hand = List.of(fool(), cup(3), trump(4));
        assertEquals(hand, follow.legalPlays(hand, tarotTrick(0)));
    }

    @Test
    public void afterTheFoolThenACupAHandHoldingCupsMustPlayACup() {
        // Fool then 3 of Cups: the suit to follow is Cups; the hand holds the 2 and King of Cups
        List<TarotCard> hand = List.of(coin(5), cup(2), trump(4), cup(TarotCard.KING));
        assertEquals(List.of(cup(2), cup(TarotCard.KING)), follow.legalPlays(hand, tarotTrick(0, fool(), cup(3))));
        // followSuit does not add the Fool (Scarto's play rule does): a held Fool is not a Cup, so not legal
        List<TarotCard> withFool = List.of(fool(), cup(2), coin(5));
        assertEquals(List.of(cup(2)), follow.legalPlays(withFool, tarotTrick(0, fool(), cup(3))));
    }

    @Test
    public void afterTheFoolThenACupAHandWithoutCupsMayPlayAnything() {
        List<TarotCard> hand = List.of(coin(5), trump(4), sword(10));
        assertEquals(hand, follow.legalPlays(hand, tarotTrick(0, fool(), cup(3))));
    }

    // ---- KnownVoids ----

    @Test
    public void foolPlayedToACupLeadRecordsNoVoid() {
        // the Fool may be played whatever the player holds, so it reveals nothing
        KnownVoids<TarotCard.Suit> kv = new KnownVoids<>(3, TarotCard.Suit.class);
        kv.record(1, tarotTrick(0, cup(5)), fool());
        for (int p = 0; p < 3; p++)
            assertEquals(Set.of(), kv.get(p));
    }

    @Test
    public void cardPlayedAfterAFoolLeadRecordsNoVoid() {
        // the Coin is the first suited card: it sets the lead suit rather than failing to follow one
        KnownVoids<TarotCard.Suit> kv = new KnownVoids<>(3, TarotCard.Suit.class);
        kv.record(1, tarotTrick(0, fool()), coin(5));
        for (int p = 0; p < 3; p++)
            assertEquals(Set.of(), kv.get(p));
    }

    @Test
    public void coinPlayedAfterTheFoolThenACupRecordsAVoidInCups() {
        // leader 0: Fool (p0), 3 of Cups (p1) -> lead suit Cups; player 2 plays a Coin, so holds no Cups
        KnownVoids<TarotCard.Suit> kv = new KnownVoids<>(3, TarotCard.Suit.class);
        kv.record(2, tarotTrick(0, fool(), cup(3)), coin(5));
        assertEquals(Set.of(Cups), kv.get(2));
        assertEquals(Set.of(), kv.get(0));
        assertEquals(Set.of(), kv.get(1));
        // following with a Cup instead reveals nothing
        KnownVoids<TarotCard.Suit> kv2 = new KnownVoids<>(3, TarotCard.Suit.class);
        kv2.record(2, tarotTrick(0, fool(), cup(3)), cup(9));
        assertEquals(Set.of(), kv2.get(2));
    }

    @Test
    public void trumpsOverloadTreatsTheFoolAsRevealingNothing() {
        KnownVoids<TarotCard.Suit> kv = new KnownVoids<>(3, TarotCard.Suit.class);
        // the Fool on a Cup lead: not a trump, but suitless -> nothing (with Trumps as trumps, and with no trumps)
        kv.record(1, tarotTrick(0, cup(5)), fool(), Trumps);
        kv.record(1, tarotTrick(0, cup(5)), fool(), null);
        // a trump after a Fool lead: the first suited card -> nothing
        kv.record(1, tarotTrick(0, fool()), trump(4), Trumps);
        assertEquals(Set.of(), kv.get(1));

        // a Coin after Fool then Cup, with Trumps as trumps: not a trump, not a Cup -> void in Cups
        kv.record(2, tarotTrick(0, fool(), cup(3)), coin(5), Trumps);
        assertEquals(Set.of(Cups), kv.get(2));
        assertEquals(Set.of(), kv.get(0));
    }

    // ---- walk-through ----

    @Test
    public void trickLedWithTheFoolPlayedThroughWithFollowSuitAndKnownVoids() {
        // leader 1 leads the Fool; player 2 then player 0 play, each choosing from followSuit and recording voids
        Trick<TarotCard, TarotCard.Suit> t = new Trick<>("Trick", 3, 1, TAROT_ORDER);
        KnownVoids<TarotCard.Suit> kv = new KnownVoids<>(3, TarotCard.Suit.class);

        kv.record(1, t, fool());
        t.play(fool());
        assertNull(t.getLeadSuit());
        assertEquals(1, t.winner(Trumps));          // only the Fool played: the leader

        // player 2: nothing to follow -> whole hand; plays the 3 of Cups, which sets the lead suit
        List<TarotCard> hand2 = List.of(coin(5), cup(3));
        assertEquals(hand2, follow.legalPlays(hand2, t));
        kv.record(2, t, cup(3));
        t.play(cup(3));
        assertEquals(Cups, t.getLeadSuit());
        assertEquals(2, t.winner(Trumps));          // the 3 of Cups is the only suited card

        // player 0: holds a Cup -> must play it; with no Cup, a Coin reveals a void in Cups
        assertEquals(List.of(cup(9)), follow.legalPlays(List.of(coin(2), cup(9)), t));
        List<TarotCard> hand0 = List.of(coin(2), sword(4));
        assertEquals(hand0, follow.legalPlays(hand0, t));
        kv.record(0, t, coin(2));
        t.play(coin(2));
        assertTrue(t.isComplete());
        // Coin is off-suit, no trump: the 3 of Cups (player 2) wins; only player 0 is known void (in Cups)
        assertEquals(2, t.winner(Trumps));
        assertEquals(Set.of(Cups), kv.get(0));
        assertEquals(Set.of(), kv.get(1));
        assertEquals(Set.of(), kv.get(2));
    }
}
