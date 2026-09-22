package games.euchre;

import core.actions.AbstractAction;
import games.euchre.actions.CallTrump;
import games.euchre.actions.Discard;
import games.euchre.actions.Pass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.euchre.EuchreTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * Going alone: the calls offered, the maker's partner sitting out of the play, 3-card tricks, the lead on
 * the lone maker's left, and the dealer sitting out (EuchreParameters.sittingOutDealerPicksUp). Driven by fm.next
 * from the standard deal: up-card 9H (kitty 9H JH JD 9C), dealer 3, player 0 first to bid.
 * <pre>
 * P0: 9S 10S JS AH 9D
 * P1: QS KS AS 10H 10D
 * P2: QD AD QH QC 10C
 * P3: KD KH AC KC JC   (dealer)
 * </pre>
 */
public class EuchreGoingAloneTest {

    EuchreForwardModel fm = new EuchreForwardModel();

    private EuchreGameState standardState() {
        return standardState(new EuchreParameters());
    }

    private EuchreGameState standardState(EuchreParameters params) {
        EuchreGameState state = newState(42, params);
        standardDeal(state);
        assertEquals(3, state.getDealer());
        assertEquals(0, state.getCurrentPlayer());
        return state;
    }

    @Test
    public void inRoundOneEachPlayerMayPassOrCallTheUpCardsSuitAloneOrNot() {
        EuchreGameState state = standardState();
        for (int p = 0; p < 4; p++) {
            assertEquals(p, state.getCurrentPlayer());
            assertEquals("actions of player " + p,
                    Set.of(new Pass(), new CallTrump(Hearts, false), new CallTrump(Hearts, true)),
                    available(state, fm));
            pass(state, fm, 1);
        }
    }

    @Test
    public void inRoundTwoEachOtherSuitMayBeCalledAloneOrNotAndTheStuckDealerHasTheSixCalls() {
        EuchreGameState state = standardState();
        pass(state, fm, 4);
        Set<AbstractAction> sixCalls = Set.of(
                new CallTrump(Diamonds, false), new CallTrump(Diamonds, true),
                new CallTrump(Clubs, false), new CallTrump(Clubs, true),
                new CallTrump(Spades, false), new CallTrump(Spades, true));
        for (int p = 0; p < 3; p++) {
            assertEquals(p, state.getCurrentPlayer());
            // Pass + 3 suits x (alone or not) = 7
            Set<AbstractAction> expected = new HashSet<>(sixCalls);
            expected.add(new Pass());
            assertEquals("actions of player " + p, expected, available(state, fm));
            pass(state, fm, 1);
        }
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(sixCalls, available(state, fm));
    }

    @Test
    public void theLoneMakersPartnerSitsOutAndThePlayerOnTheMakersLeftLeads() {
        EuchreGameState state = standardState();
        pass(state, fm, 2);
        fm.next(state, new CallTrump(Hearts, true));      // player 2 calls alone in round 1
        assertTrue(state.isAlone());
        assertEquals(2, state.getMaker());
        assertEquals(0, state.getSittingOut());           // player 2's partner
        // the dealer (3, not sitting out) takes the 9H and discards as usual
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(discards("KD", "KH", "AC", "KC", "JC", "9H"), available(state, fm));
        fm.next(state, new Discard(card("KD")));

        // player 3, on the lone maker's left, leads - not player 0 on the dealer's left (who sits out)
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(3, state.getCurrentTrick().getLeader());
        assertEquals(0, state.getCurrentTrick().getSittingOut());
        assertEquals(plays("KH", "AC", "KC", "JC", "9H"), available(state, fm));

        // 3 leads the AC; player 0 is skipped, so 1 is next
        playCards(state, fm, "AC");
        assertEquals(1, state.getCurrentPlayer());
        playCards(state, fm, "10H");                      // void in clubs: trumps
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(plays("QC", "10C"), available(state, fm));
        playCards(state, fm, "QC");
        // 3 cards complete the trick: the 10H (trump) wins for player 1, who leads the next
        assertArrayEquals(new int[]{0, 1, 0, 0}, state.tricksTaken);
        assertEquals(3, state.getDiscardPile().getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(1, state.getCurrentTrick().getLeader());
        assertEquals(0, state.getCurrentTrick().getSittingOut());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(5, state.getPlayerHand(0).getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void whenTheDealersPartnerCallsAloneTheDealerStillTakesTheUpCardAndDiscardsByDefault() {
        EuchreGameState state = standardState();
        pass(state, fm, 1);
        fm.next(state, new CallTrump(Hearts, true));      // player 1 alone: dealer 3 sits out
        assertEquals(3, state.getSittingOut());
        // EuchreParameters.sittingOutDealerPicksUp (default true): 5 + the 9H = 6 cards, kitty 3, the dealer discards
        assertEquals(6, state.getPlayerHand(3).getSize());
        assertEquals(3, state.getKitty().getSize());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(discards("KD", "KH", "AC", "KC", "JC", "9H"), available(state, fm));
        fm.next(state, new Discard(card("KC")));
        assertEquals(card("KC"), state.getDealerDiscard());
        assertEquals(4, state.getKitty().getSize());
        assertEquals(5, state.getPlayerHand(3).getSize());

        // player 2, on player 1's left, leads; then 0 (3 skipped), then 1
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(2, state.getCurrentTrick().getLeader());
        assertEquals(3, state.getCurrentTrick().getSittingOut());
        playCards(state, fm, "10C");
        assertEquals(0, state.getCurrentPlayer());
        playCards(state, fm, "9D");
        assertEquals(1, state.getCurrentPlayer());
        playCards(state, fm, "10D");
        // Hearts trumps, none played: the 10C is the only club -> player 2 wins and leads
        assertArrayEquals(new int[]{0, 0, 1, 0}, state.tricksTaken);
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(3, state.getDiscardPile().getSize());
        assertEquals(5, state.getPlayerHand(3).getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void withoutSittingOutDealerPicksUpTheDealerSittingOutNeitherTakesTheUpCardNorDiscards() {
        EuchreGameState state = standardState(params(1, false));
        pass(state, fm, 1);
        fm.next(state, new CallTrump(Hearts, true));      // player 1 alone: dealer 3 sits out
        assertEquals(Hearts, state.getTrumpSuit());
        assertEquals(3, state.getSittingOut());
        // no pickup: the dealer's hand is as dealt, the 9H stays on top of the 4-card kitty, no discard
        assertEquals(Set.copyOf(cards("KD", "KH", "AC", "KC", "JC")), Set.copyOf(cardsOf(state.getPlayerHand(3))));
        assertEquals(4, state.getKitty().getSize());
        assertEquals(card("9H"), state.getKitty().peek());
        assertNull(state.getDealerDiscard());
        // play starts at once with player 2, on player 1's left
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(2, state.getCurrentTrick().getLeader());
        assertEquals(3, state.getCurrentTrick().getSittingOut());
        assertEquals(plays("QD", "AD", "QH", "QC", "10C"), available(state, fm));
        assertAllCardsPresent(state);
    }

    @Test
    public void withoutSittingOutDealerPicksUpADealerWhoPlaysStillTakesTheUpCard() {
        // player 2 alone: player 0 sits out, dealer 3 plays and picks up whatever the parameter
        EuchreGameState state = standardState(params(1, false));
        pass(state, fm, 2);
        fm.next(state, new CallTrump(Hearts, true));
        assertEquals(6, state.getPlayerHand(3).getSize());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(discards("KD", "KH", "AC", "KC", "JC", "9H"), available(state, fm));

        // and a call not alone picks up too
        EuchreGameState other = standardState(params(1, false));
        pass(other, fm, 1);
        fm.next(other, new CallTrump(Hearts, false));
        assertEquals(6, other.getPlayerHand(3).getSize());
        assertEquals(3, other.getKitty().getSize());
    }

    @Test
    public void theDealerMayCallAloneInRoundOneAndThenDiscardsAsUsual() {
        EuchreGameState state = standardState();
        pass(state, fm, 3);
        fm.next(state, new CallTrump(Hearts, true));      // dealer 3 alone: player 1 sits out
        assertEquals(3, state.getMaker());
        assertEquals(1, state.getSittingOut());
        assertEquals(6, state.getPlayerHand(3).getSize());
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(discards("KD", "KH", "AC", "KC", "JC", "9H"), available(state, fm));
        fm.next(state, new Discard(card("KD")));

        // player 0 is on the dealer's (the lone maker's) left and leads; then 2 (1 skipped), then 3
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, state.getCurrentTrick().getSittingOut());
        playCards(state, fm, "9S");
        assertEquals(2, state.getCurrentPlayer());
        playCards(state, fm, "10C");                      // void in spades
        assertEquals(3, state.getCurrentPlayer());
        playCards(state, fm, "9H");                       // void in spades: trumps
        // the 9H, the only trump, wins for player 3, who leads; the next trick is 3, 0, 2
        assertArrayEquals(new int[]{0, 0, 0, 1}, state.tricksTaken);
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(5, state.getPlayerHand(1).getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void aRoundTwoAloneCallHasNoPickupAndTheMakersLeftLeads() {
        EuchreGameState state = standardState();
        pass(state, fm, 4);
        fm.next(state, new CallTrump(Spades, true));      // player 0 alone in round 2: player 2 sits out
        assertEquals(0, state.getMaker());
        assertEquals(2, state.getSittingOut());
        for (int p = 0; p < 4; p++)
            assertEquals("hand " + p, 5, state.getPlayerHand(p).getSize());
        assertEquals(4, state.getKitty().getSize());
        assertEquals(card("9H"), state.getKitty().peek());
        assertNull(state.getDealerDiscard());

        // player 1, on player 0's left, leads - not player 0 on the dealer's left
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getCurrentTrick().getLeader());
        playCards(state, fm, "10D");
        assertEquals(3, state.getCurrentPlayer());        // 2 skipped
        assertEquals(plays("KD"), available(state, fm));  // the JC is the left bower, a Spade
        playCards(state, fm, "KD");
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(plays("9D"), available(state, fm));
        playCards(state, fm, "9D");
        // Spades trumps, none played: KD > 10D > 9D -> player 3 wins and leads
        assertArrayEquals(new int[]{0, 0, 0, 1}, state.tricksTaken);
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(3, state.getDiscardPile().getSize());
        assertEquals(5, state.getPlayerHand(2).getSize());
    }
}
