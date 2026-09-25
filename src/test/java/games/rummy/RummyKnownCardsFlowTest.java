package games.rummy;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.rummy.actions.DrawCard;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static games.rummy.RummyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Seeded random games that track every card taken from the discard pile, checking the visibility of the hand cards
 * at every step, and every few steps that the current player's redeterminised copy keeps the opponents' known cards
 * in place.
 */
public class RummyKnownCardsFlowTest {

    private record Checked(int takes, int copyChecksWithKnown) {
    }

    private Checked playAndCheck(Game g, long seed) {
        RummyGameState state = (RummyGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        int n = state.getNPlayers();
        Random rnd = new Random(seed);
        Set<FrenchCard> known = new HashSet<>();   // the oracle: taken from the discard pile and still held
        int steps = 0, takes = 0, copyChecksWithKnown = 0;
        while (state.isNotTerminal() && steps++ < 2000) {
            int player = state.getCurrentPlayer();
            List<AbstractAction> all = fm.computeAvailableActions(state);
            // pick a kind of action uniformly, then an action of that kind, so melds and lay-offs happen
            List<Class<?>> kinds = all.stream().<Class<?>>map(Object::getClass).distinct().toList();
            Class<?> kind = kinds.get(rnd.nextInt(kinds.size()));
            List<AbstractAction> ofKind = all.stream().filter(kind::isInstance).toList();
            AbstractAction chosen = ofKind.get(rnd.nextInt(ofKind.size()));
            if (chosen.equals(new DrawCard(true))) {
                known.add(state.getDiscardPile().peek());
                takes++;
            }
            int deal = state.getRoundCounter();
            fm.next(state, chosen);
            if (!state.isNotTerminal()) break;
            // a new deal deals fresh hands: nothing is known
            if (state.getRoundCounter() != deal) known.clear();
            // a known card that left its hand (discarded, melded, laid off) is no longer known
            Set<FrenchCard> inHands = new HashSet<>();
            for (int p = 0; p < n; p++)
                inHands.addAll(state.getPlayerHand(p).getComponents());
            known.retainAll(inHands);
            assertHandVisibility(state, known);

            if (steps % 7 == 0) {
                int observer = state.getCurrentPlayer();
                RummyGameState copy = (RummyGameState) state.copy(observer);
                boolean anyKnown = false;
                for (int p = 0; p < n; p++) {
                    PartialObservableDeck<FrenchCard> hand = state.getPlayerHand(p);
                    assertEquals(hand.getSize(), copy.getPlayerHand(p).getSize());
                    for (int i = 0; i < hand.getSize(); i++)
                        if (p == observer || known.contains(hand.get(i))) {
                            assertEquals("seed " + seed + " step " + steps + ": player " + p + "'s card at " + i
                                    + " in player " + observer + "'s copy", hand.get(i), copy.getPlayerHand(p).get(i));
                            if (p != observer) anyKnown = true;
                        }
                }
                assertAllCardsPresent(copy);
                if (anyKnown) copyChecksWithKnown++;
            }
        }
        assertFalse("step cap reached", state.isNotTerminal() && steps >= 2000);
        return new Checked(takes, copyChecksWithKnown);
    }

    @Test
    public void takenCardsAreVisibleToAllInTwoPlayerGames() {
        int copyChecks = 0;
        for (long seed = 21; seed <= 23; seed++) {
            Checked c = playAndCheck(newGame(2, seed), seed);
            assertTrue("seed " + seed + ": the discard pile was never taken", c.takes() > 0);
            copyChecks += c.copyChecksWithKnown();
        }
        assertTrue("no copy was checked while an opponent held a known card", copyChecks > 0);
    }

    @Test
    public void takenCardsAreVisibleToAllInAFourPlayerGame() {
        Checked c = playAndCheck(newGame(4, 24), 24);
        assertTrue("the discard pile was never taken", c.takes() > 0);
        assertTrue("no copy was checked while an opponent held a known card", c.copyChecksWithKnown() > 0);
    }

    @Test
    public void takenCardsAreVisibleToAllOverSeveralDeals() {
        RummyParameters params = new RummyParameters();
        // a winner scores both other hands, often over 50 points, so the target is 100 to be sure of a second deal
        params.setParameterValue("targetScore", 100);
        params.setParameterValue("maxTurnsPerDeal", 10);
        Game g = newGame(3, 25, params);
        Checked c = playAndCheck(g, 25);
        assertTrue("the discard pile was never taken", c.takes() > 0);
        assertTrue("only one deal was played", g.getGameState().getRoundCounter() > 0);
    }
}
