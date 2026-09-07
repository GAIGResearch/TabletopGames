package games.hearts;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.hearts.actions.Play;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for the tracking of suits in which a player is publicly known to be void, as a result of
 * having failed to follow suit earlier in the round.
 */
public class TestHeartsKnownVoids {

    private HeartsForwardModel forwardModel;
    private HeartsGameState gameState;
    private HeartsParameters gameParameters;

    @Before
    public void setUp() {
        gameParameters = new HeartsParameters();
        gameParameters.setRandomSeed(2934);
        forwardModel = new HeartsForwardModel();
        gameState = new HeartsGameState(gameParameters, 4);
        forwardModel.setup(gameState);
    }

    @Test
    public void knownVoidsStartEmpty() {
        assertEquals(4, gameState.knownVoids.size());
        for (int p = 0; p < 4; p++) {
            assertTrue(gameState.getKnownVoids(p).isEmpty());
        }
    }

    /**
     * Plays random actions until a player fails to follow suit, and then checks that this
     * is recorded against that player.
     */
    @Test
    public void failureToFollowSuitIsRecorded() {
        Random rnd = new Random(6);
        for (int i = 0; i < 1000 && gameState.isNotTerminal(); i++) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            AbstractAction chosen = actions.get(rnd.nextInt(actions.size()));

            int player = gameState.getCurrentPlayer();
            FrenchCard.Suite ledSuit = gameState.firstCardSuit;
            boolean failsToFollow = chosen instanceof Play && ledSuit != null
                    && ((Play) chosen).card.suite != ledSuit;

            forwardModel.next(gameState, chosen);

            if (failsToFollow) {
                assertTrue("Player " + player + " failed to follow " + ledSuit + " but this was not recorded",
                        gameState.getKnownVoids(player).contains(ledSuit));
                return;
            }
        }
        fail("No player failed to follow suit within the actions played");
    }

    /**
     * As above, but checks that we only record a void against the player who actually failed to
     * follow suit, and only in the suit that was led.
     */
    @Test
    public void noVoidRecordedForPlayersWhoFollowSuit() {
        Random rnd = new Random(6);
        for (int i = 0; i < 1000 && gameState.isNotTerminal(); i++) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            AbstractAction chosen = actions.get(rnd.nextInt(actions.size()));

            int player = gameState.getCurrentPlayer();
            FrenchCard.Suite ledSuit = gameState.firstCardSuit;
            boolean followsSuit = chosen instanceof Play && ledSuit != null
                    && ((Play) chosen).card.suite == ledSuit;

            forwardModel.next(gameState, chosen);

            if (followsSuit) {
                assertFalse(gameState.getKnownVoids(player).contains(ledSuit));
            }
        }
    }

    /**
     * Any voids recorded should be carried across by a copy, whether or not this is from the
     * perspective of a single player.
     */
    @Test
    public void knownVoidsAreCopied() {
        Random rnd = new Random(6);
        for (int i = 0; i < 1000 && gameState.isNotTerminal(); i++) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            AbstractAction chosen = actions.get(rnd.nextInt(actions.size()));

            int player = gameState.getCurrentPlayer();
            FrenchCard.Suite ledSuit = gameState.firstCardSuit;
            boolean failsToFollow = chosen instanceof Play && ledSuit != null
                    && ((Play) chosen).card.suite != ledSuit;

            forwardModel.next(gameState, chosen);

            if (failsToFollow) {
                HeartsGameState fullCopy = (HeartsGameState) gameState.copy();
                HeartsGameState playerCopy = (HeartsGameState) gameState.copy(0);
                assertEquals(gameState.knownVoids, fullCopy.knownVoids);
                assertEquals(gameState.knownVoids, playerCopy.knownVoids);
                assertTrue(fullCopy.getKnownVoids(player).contains(ledSuit));
                assertTrue(playerCopy.getKnownVoids(player).contains(ledSuit));

                // and the copies must be independent of the original
                Set<FrenchCard.Suite> originalVoids = gameState.getKnownVoids(player);
                fullCopy.getKnownVoids(player).clear();
                assertTrue(originalVoids.contains(ledSuit));
                return;
            }
        }
        fail("No player failed to follow suit within the actions played");
    }

    /**
     * Over a whole game, a player recorded as void in a suit must genuinely hold no cards of that
     * suit. This is the invariant that redeterminisation will rely on.
     */
    @Test
    public void recordedVoidsAreConsistentWithActualHands() {
        Random rnd = new Random(42);
        int voidsSeen = 0;
        while (gameState.isNotTerminal()) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            forwardModel.next(gameState, actions.get(rnd.nextInt(actions.size())));

            for (int p = 0; p < gameState.getNPlayers(); p++) {
                for (FrenchCard.Suite suit : gameState.getKnownVoids(p)) {
                    voidsSeen++;
                    assertFalse("Player " + p + " is recorded void in " + suit + " but holds one",
                            gameState.getPlayerDecks().get(p).stream().anyMatch(c -> c.suite == suit));
                }
            }
        }
        assertTrue("No voids were recorded over a whole game", voidsSeen > 0);
    }

    /**
     * Redeterminisation from one player's perspective reshuffles the other players' hands. It must
     * not deal a player a card in a suit that they are publicly known to be void in.
     * <p>
     * We sample a redeterminisation at every point in a whole game, and count both the violations and
     * the opportunities to violate - a state in which a void player could have been dealt a card of
     * their void suit, because one of the other hidden hands holds one.
     */
    @Test
    public void redeterminisationRespectsKnownVoids() {
        Random rnd = new Random(6);
        int violations = 0, opportunities = 0;
        while (gameState.isNotTerminal()) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            forwardModel.next(gameState, actions.get(rnd.nextInt(actions.size())));

            List<FrenchCard> hiddenCards = new ArrayList<>(gameState.getDrawDeck().getComponents());
            for (int p = 1; p < gameState.getNPlayers(); p++) {
                hiddenCards.addAll(gameState.getPlayerDecks().get(p).getComponents());
                hiddenCards.addAll(gameState.pendingPasses.get(p));
            }

            HeartsGameState copy = (HeartsGameState) gameState.copy(0);
            for (int p = 1; p < gameState.getNPlayers(); p++) {
                if (gameState.getPlayerDecks().get(p).getSize() == 0) continue;
                for (FrenchCard.Suite suit : gameState.getKnownVoids(p)) {
                    if (hiddenCards.stream().anyMatch(c -> c.suite == suit)) opportunities++;
                    if (copy.getPlayerDecks().get(p).stream().anyMatch(c -> c.suite == suit)) violations++;
                }
            }
        }
        assertTrue("The voids were never put to the test", opportunities > 0);
        assertEquals("Redeterminisation dealt cards in a known void suit", 0, violations);
    }

    /**
     * Honouring the voids must not otherwise disturb the redeterminisation - hand sizes, the player's own
     * hand, and the full set of cards in play all have to be preserved.
     */
    @Test
    public void redeterminisationRemainsValid() {
        Random rnd = new Random(6);
        while (gameState.isNotTerminal() && !opponentWithVoidAndCardsInHand()) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            forwardModel.next(gameState, actions.get(rnd.nextInt(actions.size())));
        }
        List<FrenchCard> expectedCards = allHeldCards(gameState);
        List<FrenchCard> ownHand = gameState.getPlayerDecks().get(0).getComponents();

        for (int i = 0; i < 50; i++) {
            HeartsGameState copy = (HeartsGameState) gameState.copy(0);
            for (int p = 0; p < gameState.getNPlayers(); p++) {
                // hand sizes are public, as is the number of cards each player has committed to pass
                assertEquals(gameState.getPlayerDecks().get(p).getSize(), copy.getPlayerDecks().get(p).getSize());
                assertEquals(gameState.pendingPasses.get(p).size(), copy.pendingPasses.get(p).size());
            }
            assertEquals(ownHand, copy.getPlayerDecks().get(0).getComponents());

            List<FrenchCard> actualCards = allHeldCards(copy);
            assertEquals(expectedCards.size(), actualCards.size());
            assertTrue(actualCards.containsAll(expectedCards));
        }
    }

    /** every card still held in a hand, in a pending pass, or in the draw deck */
    private List<FrenchCard> allHeldCards(HeartsGameState state) {
        List<FrenchCard> retValue = new ArrayList<>(state.getDrawDeck().getComponents());
        for (int p = 0; p < state.getNPlayers(); p++) {
            retValue.addAll(state.getPlayerDecks().get(p).getComponents());
            retValue.addAll(state.pendingPasses.get(p));
        }
        return retValue;
    }

    private boolean opponentWithVoidAndCardsInHand() {
        for (int p = 1; p < gameState.getNPlayers(); p++) {
            if (!gameState.getKnownVoids(p).isEmpty() && gameState.getPlayerDecks().get(p).getSize() > 0)
                return true;
        }
        return false;
    }

    /**
     * Voids only apply to the current round, as hands are re-dealt at the start of each round.
     */
    @Test
    public void voidsAreClearedAtTheStartOfANewRound() {
        Random rnd = new Random(6);
        boolean voidSeenInFirstRound = false;
        while (gameState.isNotTerminal() && gameState.getRoundCounter() == 0) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            forwardModel.next(gameState, actions.get(rnd.nextInt(actions.size())));
            if (gameState.getRoundCounter() == 0) {
                voidSeenInFirstRound |= gameState.knownVoids.stream().anyMatch(v -> !v.isEmpty());
            }
        }
        assertTrue("Expected at least one void to be recorded in the first round", voidSeenInFirstRound);
        assertEquals(1, gameState.getRoundCounter());
        for (int p = 0; p < gameState.getNPlayers(); p++) {
            assertTrue(gameState.getKnownVoids(p).isEmpty());
        }
    }

    // ========================================
    // The rememberVoids parameter
    // ========================================

    @Test
    public void rememberVoidsDefaultsToTrue() {
        HeartsParameters params = new HeartsParameters();
        assertTrue(params.rememberVoids);
        assertEquals(true, params.getParameterValue("rememberVoids"));
    }

    @Test
    public void rememberVoidsSurvivesAParameterCopy() {
        HeartsParameters params = (HeartsParameters) gameState.getGameParameters();
        params.setParameterValue("rememberVoids", false);
        assertFalse(params.rememberVoids);
        assertFalse(((HeartsParameters) params.copy()).rememberVoids);
    }

    @Test
    public void noVoidsAreRecordedWhenTheyAreNotRemembered() {
        ((HeartsParameters) gameState.getGameParameters()).setParameterValue("rememberVoids", false);
        Random rnd = new Random(42);
        while (gameState.isNotTerminal()) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            forwardModel.next(gameState, actions.get(rnd.nextInt(actions.size())));
            for (int p = 0; p < gameState.getNPlayers(); p++)
                assertTrue(gameState.getKnownVoids(p).isEmpty());
        }
    }

    /**
     * With the parameter off we fall back on the old behaviour, which takes no account of who is void
     * in what. We track the voids in the test itself to show that they really are being ignored.
     */
    @Test
    public void redeterminisationIsUnconstrainedWhenVoidsAreNotRemembered() {
        ((HeartsParameters) gameState.getGameParameters()).setParameterValue("rememberVoids", false);
        Random rnd = new Random(6);
        List<Set<FrenchCard.Suite>> observedVoids = new ArrayList<>();
        for (int p = 0; p < gameState.getNPlayers(); p++)
            observedVoids.add(EnumSet.noneOf(FrenchCard.Suite.class));

        int violations = 0, opportunities = 0, round = 0;
        while (gameState.isNotTerminal()) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            AbstractAction chosen = actions.get(rnd.nextInt(actions.size()));
            int player = gameState.getCurrentPlayer();
            FrenchCard.Suite ledSuit = gameState.firstCardSuit;
            boolean failsToFollow = chosen instanceof Play && ledSuit != null
                    && ((Play) chosen).card.suite != ledSuit;

            forwardModel.next(gameState, chosen);
            if (failsToFollow) observedVoids.get(player).add(ledSuit);
            if (gameState.getRoundCounter() != round) {
                // hands are re-dealt each round, so what we know is wiped
                round = gameState.getRoundCounter();
                observedVoids.forEach(Set::clear);
                continue;
            }

            List<FrenchCard> hiddenCards = new ArrayList<>(gameState.getDrawDeck().getComponents());
            for (int p = 1; p < gameState.getNPlayers(); p++) {
                hiddenCards.addAll(gameState.getPlayerDecks().get(p).getComponents());
                hiddenCards.addAll(gameState.pendingPasses.get(p));
            }

            HeartsGameState copy = (HeartsGameState) gameState.copy(0);
            for (int p = 1; p < gameState.getNPlayers(); p++) {
                if (gameState.getPlayerDecks().get(p).getSize() == 0) continue;
                for (FrenchCard.Suite suit : observedVoids.get(p)) {
                    if (hiddenCards.stream().anyMatch(c -> c.suite == suit)) opportunities++;
                    if (copy.getPlayerDecks().get(p).stream().anyMatch(c -> c.suite == suit)) violations++;
                }
            }
        }
        assertTrue("The voids were never put to the test", opportunities > 0);
        assertTrue("With rememberVoids off the redeterminisation should ignore the voids", violations > 0);
    }
}
