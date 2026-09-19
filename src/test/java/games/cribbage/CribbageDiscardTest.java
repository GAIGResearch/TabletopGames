package games.cribbage;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.cribbage.actions.DiscardToCrib;
import games.cribbage.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.cribbage.CribbageGameState.CribbageGamePhase.Discard;
import static games.cribbage.CribbageGameState.CribbageGamePhase.Play;
import static games.cribbage.CribbageTestUtils.*;
import static org.junit.Assert.*;

/**
 * The deal, the discard to the crib, and turning up the starter (his heels).
 */
public class CribbageDiscardTest {

    CribbageParameters params;
    CribbageGameState state;
    CribbageForwardModel fm;

    // player 0 deals the first round, so player 1 (the non-dealer) discards first
    final List<FrenchCard> nonDealerHand = cards("10H", "4C", "8S", "7D", "QC", "AH");
    final List<FrenchCard> dealerHand = cards("9H", "5C", "3S", "KD", "6D", "2C");

    @Before
    public void setup() {
        params = new CribbageParameters();
        params.setRandomSeed(42);
        state = new CribbageGameState(params, 2);
        fm = new CribbageForwardModel();
        fm.setup(state);
        giveHand(state, 1, nonDealerHand);
        giveHand(state, 0, dealerHand);
    }

    private static Set<AbstractAction> allPairs(List<FrenchCard> hand) {
        Set<AbstractAction> pairs = new HashSet<>();
        for (int i = 0; i < hand.size(); i++)
            for (int j = i + 1; j < hand.size(); j++)
                pairs.add(new DiscardToCrib(hand.get(i), hand.get(j)));
        return pairs;
    }

    @Test
    public void setupDealsSixCardsEachAndTheNonDealerActsFirst() {
        CribbageGameState fresh = new CribbageGameState(params, 2);
        fm.setup(fresh);
        assertEquals(6, fresh.getPlayerHand(0).getSize());
        assertEquals(6, fresh.getPlayerHand(1).getSize());
        assertEquals(40, fresh.getDrawDeck().getSize());
        assertEquals(0, fresh.getCrib().getSize());
        assertNull(fresh.getStarter());
        assertEquals(Discard, fresh.getGamePhase());
        assertEquals(0, fresh.getDealer());
        assertEquals(1, fresh.getCurrentPlayer());
        assertEquals(0, fresh.getScore(0));
        assertEquals(0, fresh.getScore(1));
        assertAllCardsPresent(fresh);
    }

    @Test
    public void setupShufflesTheDeckWithTheGameSeed() {
        List<List<FrenchCard>> hands = new ArrayList<>();
        for (long seed : new long[]{42, 43}) {
            CribbageParameters p = new CribbageParameters();
            p.setRandomSeed(seed);
            CribbageGameState fresh = new CribbageGameState(p, 2);
            fm.setup(fresh);
            hands.add(fresh.getPlayerHand(1).getComponents());
        }
        assertNotEquals(hands.get(0), hands.get(1));
    }

    @Test
    public void discardActionsAreEqualWhateverTheOrderOfTheCards() {
        assertEquals(new DiscardToCrib(card("5C"), card("KD")), new DiscardToCrib(card("KD"), card("5C")));
        assertEquals(new DiscardToCrib(card("5C"), card("KD")).hashCode(), new DiscardToCrib(card("KD"), card("5C")).hashCode());
        assertNotEquals(new DiscardToCrib(card("5C"), card("KD")), new DiscardToCrib(card("5C"), card("KH")));
        assertEquals(new PlayCard(card("5C")), new PlayCard(card("5C")));
        assertEquals(new PlayCard(card("5C")).hashCode(), new PlayCard(card("5C")).hashCode());
        assertNotEquals(new PlayCard(card("5C")), new PlayCard(card("5D")));
    }

    @Test
    public void nonDealerIsOfferedOneDiscardForEachPairOfCards() {
        // 6 cards give 6 * 5 / 2 = 15 distinct pairs
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(15, actions.size());
        assertEquals(allPairs(nonDealerHand), new HashSet<>(actions));
    }

    @Test
    public void nonDealerDiscardsThenTheDealerEachSeeingOnlyTheirOwnCribCards() {
        fm.next(state, new DiscardToCrib(card("QC"), card("AH")));
        assertEquals(Set.copyOf(cards("10H", "4C", "8S", "7D")), new HashSet<>(state.getPlayerHand(1).getComponents()));
        assertEquals(Set.copyOf(cards("QC", "AH")), new HashSet<>(state.getCrib().getComponents()));
        assertEquals(Set.copyOf(cards("QC", "AH")), new HashSet<>(cribCardsVisibleTo(state, 1)));
        assertTrue(cribCardsVisibleTo(state, 0).isEmpty());
        assertEquals(Discard, state.getGamePhase());
        assertNull(state.getStarter());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(allPairs(dealerHand), new HashSet<>(fm.computeAvailableActions(state)));
        assertAllCardsPresent(state);

        fm.next(state, new DiscardToCrib(card("6D"), card("2C")));
        assertEquals(Set.copyOf(cards("9H", "5C", "3S", "KD")), new HashSet<>(state.getPlayerHand(0).getComponents()));
        assertEquals(4, state.getCrib().getSize());
        assertEquals(Set.copyOf(cards("QC", "AH")), new HashSet<>(cribCardsVisibleTo(state, 1)));
        assertEquals(Set.copyOf(cards("6D", "2C")), new HashSet<>(cribCardsVisibleTo(state, 0)));
        assertAllCardsPresent(state);
    }

    @Test
    public void starterIsTurnedUpAfterTheDealersDiscardAndTheNonDealerLeads() {
        putOnTopOfDrawDeck(state, card("9D"));
        fm.next(state, new DiscardToCrib(card("QC"), card("AH")));
        fm.next(state, new DiscardToCrib(card("6D"), card("2C")));

        assertEquals(card("9D"), state.getStarter());
        assertFalse(state.getDrawDeck().contains(card("9D")));
        assertEquals(39, state.getDrawDeck().getSize());
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        // not a Jack, so no his heels
        assertEquals(0, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertAllCardsPresent(state);
    }

    @Test
    public void jackStarterScoresHisHeelsForTheDealer() {
        putOnTopOfDrawDeck(state, card("JD"));
        fm.next(state, new DiscardToCrib(card("QC"), card("AH")));
        fm.next(state, new DiscardToCrib(card("6D"), card("2C")));

        assertEquals(card("JD"), state.getStarter());
        assertEquals(2, state.getScore(0));  // hisHeelsPoints, to the dealer (player 0)
        assertEquals(0, state.getScore(1));
    }
}
