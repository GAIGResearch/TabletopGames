package games.crazyeights;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.crazyeights.actions.NominateSuit;
import games.crazyeights.actions.PlayCard;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.crazyeights.CZETestUtils.*;
import static org.junit.Assert.*;

/**
 * With dealerNominatesStarterSuit on, a starter Eight is followed by the dealer (the last player) nominating
 * the suit to match, before player 0 plays first. Unit tests use direct setup; integration tests drive a real game
 * with fm.next.
 */
public class CZEStarterNominationTest {

    CZEForwardModel fm = new CZEForwardModel();

    private static Set<AbstractAction> nominationsFor(int dealer) {
        Set<AbstractAction> actions = new HashSet<>();
        for (FrenchCard.Suite suit : FrenchCard.Suite.values())
            actions.add(new NominateSuit(suit));
        return actions;
    }

    private static boolean anyNomination(List<AbstractAction> actions) {
        return actions.stream().anyMatch(a -> a instanceof NominateSuit);
    }

    /** Parameters with the dealer's nomination on, and a starter-Eight suit that is not the default. */
    private static CZEParameters nominationParams() {
        CZEParameters params = new CZEParameters();
        params.setParameterValue("dealerNominatesStarterSuit", true);
        params.setParameterValue("starterEightSuit", Clubs);
        return params;
    }

    /** A 3-player state after setup, with the nomination parameter on and an Eight as the starter card. */
    private CZEGameState stateWithEightStarter() {
        CZEParameters params = nominationParams();
        seedGivingStarter(params, 3, true);
        CZEGameState state = new CZEGameState(params, 3);
        fm.setup(state);
        assertTrue("arrangement: starter is an Eight", CZEGameState.isEight(state.getTopCard()));
        return state;
    }

    // ---------------------------------------------------------------- unit

    @Test
    public void withTheParameterOnOnlyAnEightStarterMakesTheDealerNominate() {
        CZEParameters params = nominationParams();
        int eights = 0, others = 0;
        for (int seed = 0; seed < 300; seed++) {
            int nPlayers = 2 + seed % 7;   // 2 to 8 players, so the dealer is not always the same player
            int dealer = nPlayers - 1;
            params.setRandomSeed(seed);
            CZEGameState state = new CZEGameState(params, nPlayers);
            fm.setup(state);
            String label = "seed " + seed + ", " + nPlayers + " players";
            if (CZEGameState.isEight(state.getTopCard())) {
                eights++;
                assertTrue(label + ": nomination in progress", state.isActionInProgress());
                assertEquals(label + ": dealer to nominate", dealer, state.getCurrentPlayer());
                assertEquals(label, nominationsFor(dealer), new HashSet<>(fm.computeAvailableActions(state)));
            } else {
                others++;
                assertFalse(label + ": no nomination", state.isActionInProgress());
                assertEquals(label, 0, state.getCurrentPlayer());
                assertEquals(label, state.getTopCard().suite, state.getCurrentSuit());
                assertFalse(label, anyNomination(fm.computeAvailableActions(state)));
            }
        }
        assertTrue("no starter Eight in 300 deals", eights > 0);
        assertTrue("every starter was an Eight", others > 0);
    }

    @Test
    public void withTheParameterOffAnEightStarterUsesTheFixedSuitWithoutANomination() {
        // the same deal as stateWithEightStarter, differing only by the parameter
        CZEParameters params = nominationParams();
        seedGivingStarter(params, 3, true);
        params.setParameterValue("dealerNominatesStarterSuit", false);
        CZEGameState state = new CZEGameState(params, 3);
        fm.setup(state);
        assertTrue("arrangement: starter is an Eight", CZEGameState.isEight(state.getTopCard()));

        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Clubs, state.getCurrentSuit());
        assertFalse(anyNomination(fm.computeAvailableActions(state)));
    }

    @Test
    public void nominatedSuitIsTheSuitToMatchAndPlayerZeroThenStartsPlay() {
        CZEGameState state = stateWithEightStarter();
        // Clubs is the placeholder starter-Eight suit; the dealer nominates Diamonds instead,
        // so the Nine of Clubs must no longer be playable and the Five of Diamonds must be
        giveHand(state, 0, card("5D"), card("9C"), card("KS"));
        int[] handSizes = state.getPlayerHands().stream().mapToInt(h -> h.getSize()).toArray();
        int stockSize = state.getDrawDeck().getSize();

        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new NominateSuit(Diamonds));

        assertEquals(Diamonds, state.getCurrentSuit());
        assertFalse("nomination complete", state.isActionInProgress());
        // the nomination is not a turn: player 0 on the dealer's left still plays first
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(new PlayCard(card("5D"), Diamonds)), fm.computeAvailableActions(state));
        // not a pass, and nothing moved
        assertEquals(0, state.getConsecutivePasses());
        assertTrue(state.isNotTerminal());
        assertArrayEquals(handSizes, state.getPlayerHands().stream().mapToInt(h -> h.getSize()).toArray());
        assertEquals(stockSize, state.getDrawDeck().getSize());
        assertEquals(1, state.getDiscardPile().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void copiesDuringTheNominationKeepItAndAreIndependent() {
        CZEGameState state = stateWithEightStarter();
        assertTrue("nomination in progress", state.isActionInProgress());

        CZEGameState copy = (CZEGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        // the dealer (player 2) still nominates in a redeterminised copy for the dealer or for another player
        for (int observer : new int[]{2, 0}) {
            CZEGameState observed = (CZEGameState) state.copy(observer);
            assertEquals("observer " + observer, 2, observed.getCurrentPlayer());
            assertEquals("observer " + observer, nominationsFor(2), new HashSet<>(fm.computeAvailableActions(observed)));
        }

        // nominating in the copy leaves the original still waiting for the dealer
        int originalHash = state.hashCode();
        fm.next(copy, new NominateSuit(Spades));
        assertEquals(Spades, copy.getCurrentSuit());
        assertNotEquals(state, copy);
        assertEquals(originalHash, state.hashCode());
        assertTrue(state.isActionInProgress());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(nominationsFor(2), new HashSet<>(fm.computeAvailableActions(state)));
    }

    // ---------------------------------------------------------------- integration

    @Test
    public void scriptedGameStartsWithTheDealersNominationThenNormalPlay() {
        CZEParameters params = nominationParams();
        long seed = seedGivingStarter(params, 3, true);
        Game game = newGame(3, seed, params);
        CZEGameState state = (CZEGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        assertTrue("arrangement: starter is an Eight", CZEGameState.isEight(state.getTopCard()));
        giveHand(state, 0, card("5D"), card("9C"));
        giveHand(state, 1, card("KD"), card("4C"));
        giveHand(state, 2, card("7D"), card("JC"));

        // Dealer (player 2) nominates Diamonds rather than the fixed Clubs
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(nominationsFor(2), new HashSet<>(fm.computeAvailableActions(state)));
        fm.next(state, new NominateSuit(Diamonds));

        // P0: only the Diamond matches (the Nine of Clubs would have matched the fixed suit)
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(new PlayCard(card("5D"), Diamonds)), fm.computeAvailableActions(state));
        fm.next(state, new PlayCard(card("5D"), Diamonds));

        // P1: King of Diamonds matches the Five of Diamonds by suit
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(List.of(new PlayCard(card("KD"), Diamonds)), fm.computeAvailableActions(state));
        fm.next(state, new PlayCard(card("KD"), Diamonds));

        // P2: the dealer's nomination was not their turn, so they now play normally
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(List.of(new PlayCard(card("7D"), Diamonds)), fm.computeAvailableActions(state));
        fm.next(state, new PlayCard(card("7D"), Diamonds));

        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void randomGamesWithTheDealersNominationFinishWithEveryCardAccountedFor() {
        int eightStarters = 0, nominations = 0;
        for (int nPlayers : new int[]{2, 4, 6, 8}) {
            CZEParameters params = nominationParams();
            long eightSeed = seedGivingStarter(params, nPlayers, true);
            for (long seed : new long[]{eightSeed, 1, 2, 3}) {
                String label = nPlayers + " players, seed " + seed;
                Game game = newGame(nPlayers, seed, params);
                CZEGameState state = (CZEGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                if (CZEGameState.isEight(state.getTopCard())) eightStarters++;

                int steps = 0;
                while (state.isNotTerminal() && steps < 5000) {
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    if (anyNomination(actions)) {
                        // only ever offered as the very first decision, and only to the dealer
                        assertEquals(label, 0, steps);
                        assertEquals(label, nominationsFor(nPlayers - 1), new HashSet<>(actions));
                        nominations++;
                    }
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));
                    assertAllCardsPresent(state);
                    steps++;
                }
                assertFalse(label + ": did not end within 5000 actions", state.isNotTerminal());
            }
        }
        assertTrue("fewer Eight starters than the chosen seeds", eightStarters >= 4);
        assertEquals("every Eight starter, and only those, began with a nomination", eightStarters, nominations);
    }
}
