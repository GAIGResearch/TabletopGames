package games.blackjack;

import core.CoreConstants.GameResult;
import core.components.FrenchCard;
import games.blackjack.BlackjackGameState.BlackjackGamePhase;
import games.blackjack.actions.Bet;
import games.blackjack.actions.Insurance;
import games.blackjack.actions.Stand;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Betting;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * Several hands (nHands). After a hand is settled a new hand starts if fewer than nHands have been played
 * and someone can still make the minimum bet (2); a player who cannot sits out.
 */
public class BlackjackNextHandTest {

    BlackjackParameters params;
    BlackjackForwardModel fm = new BlackjackForwardModel();

    private BlackjackGameState newState(int nPlayers, int nHands) {
        if (params == null)
            params = new BlackjackParameters();
        params.setRandomSeed(42);
        params.setParameterValue("nHands", nHands);
        BlackjackGameState state = new BlackjackGameState(params, nPlayers);
        fm.setup(state);
        return state;
    }

    /**
     * One player bets 4 on 10 + 8 = 18 against the dealer's 7 + 10 = 17 (which stands), and wins 1:1: 14 chips.
     * Returns the draw deck as it was just before the hand ended, in order.
     */
    private List<FrenchCard> winFirstHand(BlackjackGameState state) {
        betAndDeal(state, fm, new int[]{4}, new String[]{"10H 8C"}, "7S", "10D", "");
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getRoundCounter());
        List<FrenchCard> drawDeckBefore = new ArrayList<>(state.getDrawDeck().getComponents());
        fm.next(state, new Stand());
        return drawDeckBefore;
    }

    @Test
    public void withOneHandTheGameEndsAfterTheFirstSettlement() {
        BlackjackGameState state = newState(1, 1);
        winFirstHand(state);
        assertFalse(state.isNotTerminal());
        assertEquals(14, state.getChips(0));
        assertArrayEquals(new GameResult[]{WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void withThreeHandsASecondHandStartsWithEveryCardBackInAReshuffledDrawDeck() {
        BlackjackGameState state = newState(1, 3);
        List<FrenchCard> drawDeckBefore = winFirstHand(state);

        assertNewHandStarted(state, 1, 0);
        assertEquals("chips carried over", 14, state.getChips(0));
        assertEquals(betsUpTo(14), new HashSet<>(fm.computeAvailableActions(state)));
        assertEquals(48, drawDeckBefore.size());
        assertFalse("the draw deck was not reshuffled",
                keepsRelativeOrder(drawDeckBefore, state.getDrawDeck().getComponents()));
    }

    @Test
    public void theNewHandIsShuffledWithTheGameRandomSeed() {
        BlackjackGameState first = newState(1, 3);
        winFirstHand(first);
        BlackjackGameState second = newState(1, 3);
        winFirstHand(second);
        assertNewHandStarted(first, 1, 0);
        assertEquals(first.getDrawDeck().getComponents(), second.getDrawDeck().getComponents());
    }

    @Test
    public void chipsCarryOverAndLimitTheNextBet() {
        BlackjackGameState state = newState(2, 3);
        // player 0 bets 6 on 16 and loses (4 left); player 1 bets 2 on 18 and wins (12); dealer 7 + 10 = 17
        betAndDeal(state, fm, new int[]{6, 2}, new String[]{"10H 6C", "10C 8H"}, "7S", "10D", "");
        fm.next(state, new Stand());
        fm.next(state, new Stand());

        assertNewHandStarted(state, 1, 0);
        assertArrayEquals(new int[]{4, 12}, state.chips);
        assertEquals(Set.of(new Bet(2), new Bet(4)), new HashSet<>(fm.computeAvailableActions(state)));
        fm.next(state, new Bet(4));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(betsUpTo(12), new HashSet<>(fm.computeAvailableActions(state)));
        assertArrayEquals(new int[]{0, 12}, state.chips);
    }

    @Test
    public void aPlayerWhoCannotMakeTheMinimumBetIsSkippedInBettingDealingInsuranceAndPlay() {
        BlackjackGameState state = newState(3, 1);
        state.chips[1] = 1;
        // players 0 and 2 are dealt 17 each; the dealer's Ace + 6 is a soft 17, which stands
        stackDeal(state, new String[]{"10H 7C", "9S 8D"}, "AS", "6D", "");
        fm.next(state, new Bet(4));
        assertEquals(Betting, state.getGamePhase());
        assertEquals("player 1 cannot bet and is skipped", 2, state.getCurrentPlayer());
        assertEquals(betsUpTo(10), new HashSet<>(fm.computeAvailableActions(state)));

        fm.next(state, new Bet(2));
        // dealt: 2 cards each to players 0 and 2, up card and hole card
        assertEquals(52 - 4 - 2, state.getDrawDeck().getSize());
        assertEquals(setOf("10H 7C"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(0, state.getPlayerHand(1, 0).getSize());
        assertEquals(setOf("9S 8D"), setOf(state.getPlayerHand(2, 0)));
        assertEquals(BlackjackGamePhase.Insurance, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Insurance(false));
        assertEquals("player 1 is not offered insurance", 2, state.getCurrentPlayer());
        fm.next(state, new Insurance(false));
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Stand());
        assertEquals("player 1 does not play", 2, state.getCurrentPlayer());
        fm.next(state, new Stand());

        // 17 pushes against 17 for players 0 and 2; player 1 keeps the 1 chip and loses against the bank
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new int[]{10, 1, 10}, state.chips);
        assertEquals(0, state.getBet(1, 0));
        assertEquals(0, state.getPlayerHand(1, 0).getSize());
        assertArrayEquals(new GameResult[]{DRAW_GAME, LOSE_GAME, DRAW_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void aPlayerWithExactlyTheMinimumBetIsNotSkipped() {
        BlackjackGameState state = newState(3, 1);
        state.chips[1] = 2;
        fm.next(state, new Bet(4));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new Bet(2)), new HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void whenTheLastPlayerCannotBetTheDealFollowsTheLastBetPlaced() {
        BlackjackGameState state = newState(3, 1);
        state.chips[2] = 0;
        stackDeal(state, new String[]{"10H 7C", "9S 8D"}, "6S", "10D", "");
        fm.next(state, new Bet(2));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Bet(2));

        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(52 - 4 - 2, state.getDrawDeck().getSize());
        assertEquals(setOf("10H 7C"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("9S 8D"), setOf(state.getPlayerHand(1, 0)));
        assertEquals(0, state.getPlayerHand(2, 0).getSize());
        assertEquals(setOf("6S"), setOf(state.getDealerHand()));
    }

    @Test
    public void aPlayerWhoGoesBrokeSitsOutTheNextHands() {
        BlackjackGameState state = newState(2, 3);
        // player 0 bets all 10 on 16 and loses; player 1 bets 2 on 18 and wins; dealer 7 + 10 = 17
        betAndDeal(state, fm, new int[]{10, 2}, new String[]{"10H 6C", "10C 8H"}, "7S", "10D", "");
        fm.next(state, new Stand());
        fm.next(state, new Stand());

        assertNewHandStarted(state, 1, 1);
        assertArrayEquals(new int[]{0, 12}, state.chips);
        assertEquals(betsUpTo(12), new HashSet<>(fm.computeAvailableActions(state)));

        // hand 2: only player 1 is dealt in; 9 + 9 = 18 pushes against the dealer's 8 + 10
        stackDeal(state, new String[]{"9S 9D"}, "8C", "10S", "");
        fm.next(state, new Bet(4));
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getPlayerHand(0, 0).getSize());
        assertEquals(0, state.getBet(0, 0));
        assertEquals(setOf("9S 9D"), setOf(state.getPlayerHand(1, 0)));
        assertEquals(52 - 2 - 2, state.getDrawDeck().getSize());
        fm.next(state, new Stand());

        assertNewHandStarted(state, 2, 1);
        assertArrayEquals(new int[]{0, 12}, state.chips);
    }

    /**
     * Two players, three hands. Player 0 bets all 10 on 16; player 1 bets 6 on 16, and is offered insurance under
     * the King (4 chips left cover the 3) which player 0 cannot afford. The dealer's K + 7 = 17 beats both.
     */
    private BlackjackGameState bothLoseWithPlayer1Insuring(boolean buy) {
        BlackjackGameState state = newState(2, 3);
        betAndDeal(state, fm, new int[]{10, 6}, new String[]{"10H 6C", "9S 7C"}, "KS", "7D", "");
        assertEquals(1, state.getCurrentPlayer());
        insure(state, fm, buy);
        assertEquals(Play, state.getGamePhase());
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        return state;
    }

    @Test
    public void theGameEndsEarlyWhenNobodyCanMakeTheMinimumBet() {
        // insurance bought and lost: player 1 has 10 - 6 - 3 = 1 chip, player 0 none
        BlackjackGameState state = bothLoseWithPlayer1Insuring(true);
        assertFalse("nobody can bet 2, so no second hand", state.isNotTerminal());
        assertArrayEquals(new int[]{0, 1}, state.chips);
        assertArrayEquals(new GameResult[]{LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void theGameGoesOnWhileOnePlayerCanMakeTheMinimumBet() {
        // insurance declined: player 1 has 10 - 6 = 4 chips
        BlackjackGameState state = bothLoseWithPlayer1Insuring(false);
        assertNewHandStarted(state, 1, 1);
        assertArrayEquals(new int[]{0, 4}, state.chips);
    }

    @Test
    public void aHandEndedByADealerBlackjackIsFollowedByTheNextHand() {
        BlackjackGameState state = newState(1, 3);
        // bet 8 leaves 2, too few for insurance (4); the peek finds A + K and the bet is lost: 2 chips left
        betAndDeal(state, fm, new int[]{8}, new String[]{"10H 7C"}, "AS", "KD", "");
        assertNewHandStarted(state, 1, 0);
        assertEquals(2, state.getChips(0));
        assertEquals(Set.of(new Bet(2)), new HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void aHandEndedByANaturalPaidAtOnceIsFollowedByTheNextHand() {
        params = new BlackjackParameters();
        pagatNaturals(params);
        BlackjackGameState state = newState(1, 3);
        // natural on a bet of 4, paid 3:2 at the deal: 10 - 4 + 4 + 6 = 16
        betAndDeal(state, fm, new int[]{4}, new String[]{"AS KD"}, "7S", "9D", "");
        assertNewHandStarted(state, 1, 0);
        assertEquals(16, state.getChips(0));
    }

    /**
     * The start of hand 2 in chipsCarryOverAndLimitTheNextBet: chips {4, 12}, player 0 to bet.
     */
    private BlackjackGameState startOfSecondHand() {
        BlackjackGameState state = newState(2, 3);
        betAndDeal(state, fm, new int[]{6, 2}, new String[]{"10H 6C", "10C 8H"}, "7S", "10D", "");
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        assertNewHandStarted(state, 1, 0);
        return state;
    }

    @Test
    public void atTheStartOfALaterHandAFullCopyIsEqualAndPlaysOnIndependently() {
        BlackjackGameState state = startOfSecondHand();
        BlackjackGameState copy = (BlackjackGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(1, copy.getRoundCounter());
        assertEquals(state.getDrawDeck().getComponents(), copy.getDrawDeck().getComponents());

        int originalHash = state.hashCode();
        fm.next(copy, new Bet(4));
        assertEquals(1, copy.getCurrentPlayer());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(4, state.getChips(0));
        assertEquals(originalHash, state.hashCode());
    }

    @Test
    public void atTheStartOfALaterHandRedeterminisationShufflesTheWholeDrawDeck() {
        BlackjackGameState state = startOfSecondHand();
        int changed = 0;
        for (int i = 0; i < 20; i++) {
            BlackjackGameState copy = (BlackjackGameState) state.copy(1);
            assertEquals(52, copy.getDrawDeck().getSize());
            assertEquals(new HashSet<>(FULL_DECK), new HashSet<>(copy.getDrawDeck().getComponents()));
            assertEquals(0, copy.getDealerHand().getSize() + copy.getHoleCard().getSize());
            for (int p = 0; p < 2; p++)
                assertEquals(0, copy.getPlayerHand(p, 0).getSize());
            assertArrayEquals(state.chips, copy.chips);
            assertEquals(state.bets, copy.bets);
            assertEquals(Betting, copy.getGamePhase());
            assertEquals(0, copy.getCurrentPlayer());
            assertEquals(1, copy.getRoundCounter());
            if (!copy.getDrawDeck().getComponents().equals(state.getDrawDeck().getComponents()))
                changed++;
        }
        assertTrue("the draw deck was never redeterminised", changed > 0);
    }
}
