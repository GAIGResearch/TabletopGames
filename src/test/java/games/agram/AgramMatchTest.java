package games.agram;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static core.components.FrenchCard.Suite.Hearts;
import static games.agram.AgramTestUtils.*;
import static org.junit.Assert.*;

/**
 * A match of nDeals deals. The winner of each deal's last trick wins the deal and deals the next, so the
 * player after them leads it; the score is the number of deals won.
 */
public class AgramMatchTest {

    AgramForwardModel fm = new AgramForwardModel();

    private AgramGameState newState(int nDeals, long seed) {
        AgramParameters params = new AgramParameters();
        params.setParameterValue("nDeals", nDeals);
        params.setRandomSeed(seed);
        AgramGameState state = new AgramGameState(params, 3);
        fm.setup(state);
        return state;
    }

    /**
     * A one-trick first deal: player 0 leads the Four of Hearts, player 1 wins with the Ace, and player 2, with no
     * Hearts, plays the Five of Clubs (and so is known to be void in Hearts).
     */
    private void playFirstDealWonByPlayerOne(AgramGameState state) {
        assertEquals("player 0 leads the first deal", 0, state.getCurrentPlayer());
        playOneTrickDeal(state, fm, "4H", "AH", "5C");
    }

    @Test
    public void aCompletedDealScoresItsWinnerAndThePlayerAfterThemLeadsAFreshDeal() {
        AgramGameState state = newState(3, 42);
        for (int p = 0; p < 3; p++)
            assertEquals("no deals won at the start, player " + p, 0, state.getDealsWon(p));
        assertEquals(0, state.getRoundCounter());

        playFirstDealWonByPlayerOne(state);

        assertTrue("the first of three deals does not end the match", state.isNotTerminal());
        assertEquals(0, state.getDealsWon(0));
        assertEquals(1, state.getDealsWon(1));
        assertEquals(0, state.getDealsWon(2));
        // the score is the deals won, during the match as well as at its end
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertEquals(1.0, state.getGameScore(1), 0.0);
        assertEquals(0.0, state.getGameScore(2), 0.0);
        assertEquals("one deal completed", 1, state.getRoundCounter());

        // player 1 won, so deals the next deal: player 2 leads it
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(2, state.getCurrentTrick().getLeader());

        // all 35 cards were gathered and re-dealt: a full hand each, even though the first deal was one trick long
        for (int p = 0; p < 3; p++) {
            assertEquals("hand size, player " + p, 6, state.getPlayerHands().get(p).getSize());
            assertEquals("known voids are cleared for a new deal, player " + p, Set.of(), state.getKnownVoids().get(p));
        }
        assertEquals(17, state.getDrawDeck().getSize());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(0, state.getDiscardPile().getSize());
        assertAllCardsPresent(state);

        // the new leader may lead any card of their new hand
        Set<AbstractAction> expected = new HashSet<>();
        for (FrenchCard c : state.getPlayerHands().get(2).getComponents())
            expected.add(new PlayCard(c));
        assertEquals(expected, new HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void withOneDealTheFirstDealEndsTheMatchAndItsWinnerHasWonOneDeal() {
        AgramGameState state = newState(1, 42);
        playFirstDealWonByPlayerOne(state);
        assertFalse(state.isNotTerminal());
        // there is no re-deal after the last deal: its tricks and voids remain
        assertEquals(3, state.getDiscardPile().getSize());
        assertEquals(Set.of(Hearts), state.getKnownVoids().get(2));
        assertEquals(0, state.getDealsWon(0));
        assertEquals(1, state.getDealsWon(1));
        assertEquals(0, state.getDealsWon(2));
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertEquals(1.0, state.getGameScore(1), 0.0);
        assertEquals(0.0, state.getGameScore(2), 0.0);
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(WIN_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
    }

    @Test
    public void theCardsPlayedInADealAreShuffledBackIntoTheNextDeal() {
        // Over several seeds, each of the three cards played in the first deal ends up sometimes in a new hand and
        // sometimes among the undealt cards (18 of the 35 are dealt). Without a shuffle they would always land on
        // the same side, wherever the gathered cards are put.
        int inHands = 0, undealt = 0;
        for (long seed = 1; seed <= 20; seed++) {
            AgramGameState state = newState(3, seed);
            playFirstDealWonByPlayerOne(state);
            for (FrenchCard c : cards("4H", "AH", "5C")) {
                if (state.getDrawDeck().contains(c))
                    undealt++;
                else if (state.getPlayerHands().stream().anyMatch(h -> h.contains(c)))
                    inHands++;
                else
                    fail("seed " + seed + ": " + c + " was not re-dealt");
            }
        }
        assertTrue("a card played in the first deal was never dealt to a hand again", inHands > 0);
        assertTrue("a card played in the first deal was always dealt to a hand again", undealt > 0);
    }

    @Test
    public void dealsWonIsCopiedIndependentlyAndIncludedInEqualsAndHash() {
        AgramGameState state = newState(3, 42);
        playFirstDealWonByPlayerOne(state);
        assertTrue("the first of three deals does not end the match", state.isNotTerminal());
        // mid-match and mid-trick: player 2 leads a card of the second deal
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getCurrentTrick().getSize());

        AgramGameState copy = (AgramGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        for (int p = 0; p < 3; p++)
            assertEquals("copy, player " + p, state.getDealsWon(p), copy.getDealsWon(p));

        // deals won are public, so a redeterminised copy keeps them, and the deal count
        AgramGameState redeterminised = (AgramGameState) state.copy(0);
        for (int p = 0; p < 3; p++) {
            assertEquals("redeterminised, player " + p, state.getDealsWon(p), redeterminised.getDealsWon(p));
            assertEquals("redeterminised, player " + p, state.getGameScore(p), redeterminised.getGameScore(p), 0.0);
        }
        assertEquals(1, redeterminised.getRoundCounter());

        // a change to the copy's deals won leaves the original alone, and makes the two unequal
        int originalHash = state.hashCode();
        copy.dealsWon[1] = 0;
        assertEquals(1, state.getDealsWon(1));
        assertEquals(originalHash, state.hashCode());
        assertNotEquals(state, copy);
        assertNotEquals(state.hashCode(), copy.hashCode());
    }
}
