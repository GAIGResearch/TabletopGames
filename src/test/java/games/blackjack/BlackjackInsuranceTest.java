package games.blackjack;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.blackjack.BlackjackGameState.BlackjackGamePhase;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Insurance;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.FrenchCardType.Ace;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackGameState.isTenValue;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * Insurance. Straight after the deal, under an Ace or ten-value up card and before the dealer's peek, each
 * player in the hand who has chips of at least half their bet chooses in seat order to buy insurance (half the bet)
 * or not. Players who cannot afford it are skipped; if nobody can, there is no Insurance phase. After the last
 * decision the dealer peeks: a dealer Blackjack pays each insurance 2:1 (3 x the insurance back) and the hand is
 * settled at once; otherwise every insurance is lost and play goes on. Until the peek the hole
 * card is not known not to make a Blackjack.
 */
public class BlackjackInsuranceTest {

    static final Set<AbstractAction> BUY_OR_DECLINE = Set.of(new Insurance(true), new Insurance(false));
    static final Set<AbstractAction> HIT_OR_STAND = Set.of(new Hit(), new Stand());
    static final BlackjackGamePhase INSURANCE = BlackjackGamePhase.Insurance;

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        newState(3);
    }

    private void newState(int nPlayers) {
        state = new BlackjackGameState(params, nPlayers);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    private Set<AbstractAction> legalActions() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    @Test
    public void insuranceIsOfferedUnderAnAceBeforeThePeek() {
        betAndDeal(state, fm, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "AS", "9D", "");
        assertEquals(INSURANCE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(BUY_OR_DECLINE, legalActions());
        assertTrue(state.isNotTerminal());
        assertEquals("the hole card is still face down", setOf("9D"), setOf(state.getHoleCard()));
        assertEquals(setOf("AS"), setOf(state.getDealerHand()));
        assertArrayEquals(new int[]{8, 6, 4}, state.chips);
        assertArrayEquals(new int[]{0, 0, 0}, state.insurance);
        assertAllCardsPresent(state);
    }

    @Test
    public void insuranceIsOfferedUnderEveryTenValueUpCard() {
        for (String up : new String[]{"10S", "JS", "QS", "KS"}) {
            newState(1);
            betAndDeal(state, fm, new int[]{4}, new String[]{"9H 8C"}, up, "9D", "");
            assertEquals(up, INSURANCE, state.getGamePhase());
            assertEquals(up, 0, state.getCurrentPlayer());
            assertEquals(up, BUY_OR_DECLINE, legalActions());
            assertEquals(up, 1, state.getHoleCard().getSize());
        }
    }

    @Test
    public void thereIsNoInsuranceUnderATwoToNineUpCard() {
        // even with an Ace in the hole: play starts at once
        for (int rank = 2; rank <= 9; rank++) {
            String up = rank + "S";
            newState(1);
            betAndDeal(state, fm, new int[]{4}, new String[]{"10H 7C"}, up, "AD", "");
            assertEquals(up, Play, state.getGamePhase());
            assertEquals(up, 0, state.getCurrentPlayer());
            assertEquals(up, HIT_OR_STAND, legalActions());
            assertEquals(up, 6, state.getChips(0));
            assertEquals(up, 0, state.getInsurance(0));
        }
    }

    @Test
    public void insuranceIsOfferedToEachPlayerInSeatOrderAndCostsHalfTheBet() {
        // dealer A + 9: no Blackjack
        betAndDeal(state, fm, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "AS", "9D", "");
        fm.next(state, new Insurance(true));
        assertEquals(INSURANCE, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(BUY_OR_DECLINE, legalActions());
        assertEquals("bet 2: insurance 1, 8 - 1", 7, state.getChips(0));
        assertEquals(1, state.getInsurance(0));
        assertEquals("the bet stays on the hand", 2, state.getBet(0, 0));

        fm.next(state, new Insurance(false));
        assertEquals(INSURANCE, state.getGamePhase());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(BUY_OR_DECLINE, legalActions());
        assertEquals("declining costs nothing", 6, state.getChips(1));
        assertEquals(0, state.getInsurance(1));
        assertEquals(1, state.getInsurance(0));

        // the last decision: bet 6 costs 3 (4 - 3); then the peek finds no Blackjack and play starts with player 0
        fm.next(state, new Insurance(true));
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(HIT_OR_STAND, legalActions());
        assertArrayEquals(new int[]{7, 6, 1}, state.chips);
        assertEquals(6, state.getBet(2, 0));
    }

    @Test
    public void aPlayerWhoCannotAffordInsuranceIsSkipped() {
        // player 1 bets all 10 chips (0 left, needs 5): the offer goes from player 0 to player 2
        betAndDeal(state, fm, new int[]{4, 10, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "AS", "9D", "");
        assertEquals(INSURANCE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Insurance(false));
        assertEquals(INSURANCE, state.getGamePhase());
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Insurance(false));
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());

        // player 0 bets 8 (2 left, needs 4): the first offer goes to player 1
        newState(3);
        betAndDeal(state, fm, new int[]{8, 2, 2}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "KS", "9D", "");
        assertEquals(INSURANCE, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void insuranceNeedsChipsOfAtLeastHalfTheBet() {
        // bet 8 from 12 chips: 4 left, exactly half the bet - offered
        params.setParameterValue("startingChips", 12);
        newState(1);
        betAndDeal(state, fm, new int[]{8}, new String[]{"10H 7C"}, "AS", "9D", "");
        assertEquals(INSURANCE, state.getGamePhase());
        assertEquals(BUY_OR_DECLINE, legalActions());

        // bet 8 from 10 chips: 2 left - not offered, and as nobody else is in the hand there is no Insurance phase
        params.setParameterValue("startingChips", 10);
        newState(1);
        betAndDeal(state, fm, new int[]{8}, new String[]{"10H 7C"}, "AS", "9D", "");
        assertEquals(Play, state.getGamePhase());
        assertEquals(HIT_OR_STAND, legalActions());
    }

    @Test
    public void whenNobodyCanAffordInsuranceThePeekHappensAtOnce() {
        // bets 10 and 8 leave 0 and 2 chips: no Insurance phase, the peek finds no Blackjack, play starts
        newState(2);
        betAndDeal(state, fm, new int[]{10, 8}, new String[]{"10H 2C", "9S 8D"}, "AS", "9D", "");
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(HIT_OR_STAND, legalActions());
        assertEquals(1, state.getHoleCard().getSize());

        // the same with a dealer Blackjack: the hand ends at the last bet. The natural pushes (10), 20 loses (0)
        newState(2);
        betAndDeal(state, fm, new int[]{10, 10}, new String[]{"AH KC", "10S QD"}, "AS", "KD", "");
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("AS KD"), setOf(state.getDealerHand()));
        assertArrayEquals(new int[]{10, 0}, state.chips);
        assertArrayEquals(new Object[]{DRAW_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void insuranceIsPaidThreeTimesOverOnADealerBlackjackAfterTheLastDecision() {
        // dealer A + K. Player 0 (19, bet 4) insures for 2: 6 - 2 = 4. Player 1 (14, bet 6) insures for 3: 4 - 3 = 1.
        // Player 2 (17, bet 2) declines: 8
        betAndDeal(state, fm, new int[]{4, 6, 2}, new String[]{"10H 9C", "7C 7D", "9S 8D"}, "AS", "KD", "5H");
        insure(state, fm, true, true);

        // no peek yet: player 2 still has to decide
        assertTrue(state.isNotTerminal());
        assertEquals(INSURANCE, state.getGamePhase());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(1, state.getHoleCard().getSize());
        assertEquals(1, state.getDealerHand().getSize());
        assertArrayEquals(new int[]{4, 1, 8}, state.chips);
        assertArrayEquals(new int[]{2, 3, 0}, state.insurance);
        assertEquals(4, state.getBet(0, 0));

        // after the last decision: Blackjack. Each bet is lost; insurance pays 3 x 2 = 6 (10) and 3 x 3 = 9 (10)
        fm.next(state, new Insurance(false));
        assertFalse(state.isNotTerminal());
        assertEquals(0, state.getHoleCard().getSize());
        assertEquals(setOf("AS KD"), setOf(state.getDealerHand()));
        assertEquals("the dealer draws nothing", card("5H"), state.getDrawDeck().peek());
        assertArrayEquals(new int[]{10, 10, 8}, state.chips);
        assertArrayEquals(new int[]{0, 0, 0}, state.insurance);
        for (int p = 0; p < 3; p++)
            assertEquals(0, state.getBet(p, 0));
        assertArrayEquals(new Object[]{DRAW_GAME, DRAW_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void insuranceIsPaidOnADealerBlackjackUnderATen() {
        // Q + A. Bet 4 on 19, insured for 2: 10 - 4 - 2 + 6 = 10
        newState(1);
        betAndDeal(state, fm, new int[]{4}, new String[]{"10H 9C"}, "QS", "AD", "");
        insure(state, fm, true);
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("QS AD"), setOf(state.getDealerHand()));
        assertEquals(10, state.getChips(0));
        assertEquals(0, state.getInsurance(0));
        assertArrayEquals(new Object[]{DRAW_GAME}, state.getPlayerResults());
    }

    @Test
    public void withANaturalTheBetPushesAndTheInsuranceIsPaidInEitherMode() {
        for (boolean naturalOnly : new boolean[]{false, true}) {
            params = new BlackjackParameters();
            params.setRandomSeed(42);
            if (naturalOnly)
                pagatNaturals(params);
            newState(2);
            // dealer A + Q. Player 0's natural (bet 4) insured for 2: 4 + 4 (push) + 6 = 14.
            // Player 1's natural (bet 2) not insured: pushes, 10
            betAndDeal(state, fm, new int[]{4, 2}, new String[]{"AH KC", "AC JD"}, "AS", "QH", "");
            insure(state, fm, true, false);
            String mode = "naturalOnly " + naturalOnly;
            assertFalse(mode, state.isNotTerminal());
            assertArrayEquals(mode, new int[]{14, 10}, state.chips);
            assertArrayEquals(mode, new int[]{0, 0}, state.insurance);
            assertArrayEquals(mode, new Object[]{WIN_GAME, DRAW_GAME}, state.getPlayerResults());
        }
    }

    @Test
    public void insuranceIsLostWhenTheDealerHasNoBlackjack() {
        // K + 7. Bet 4 on 19, insured for 2: 4 left and the insurance gone at the peek; play goes on
        newState(1);
        betAndDeal(state, fm, new int[]{4}, new String[]{"10H 9C"}, "KS", "7D", "");
        insure(state, fm, true);
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(4, state.getChips(0));
        assertEquals(0, state.getInsurance(0));
        assertEquals(4, state.getBet(0, 0));
        assertEquals(1, state.getHoleCard().getSize());

        // 19 beats 17: 4 + 8 = 12
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertEquals(12, state.getChips(0));
        assertArrayEquals(new Object[]{WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void withNaturalOnlyANaturalIsOfferedInsuranceAndPaidOnlyAfterTheLastDecision() {
        // dealer A + 5, no Blackjack. Player 0 has a natural (bet 2) and is offered insurance like anyone else
        pagatNaturals(params);
        newState(2);
        betAndDeal(state, fm, new int[]{2, 4}, new String[]{"AH KC", "10S 7D"}, "AS", "5D", "");
        assertEquals(INSURANCE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(BUY_OR_DECLINE, legalActions());
        assertEquals("not paid before the peek", 8, state.getChips(0));
        assertEquals(2, state.getBet(0, 0));

        fm.next(state, new Insurance(true));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals("not paid before the peek", 7, state.getChips(0));
        assertEquals(2, state.getBet(0, 0));

        // the peek: no Blackjack, the insurance is lost and the natural paid 2 + 3: 7 + 5 = 12. Player 1 plays
        fm.next(state, new Insurance(false));
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(12, state.getChips(0));
        assertEquals(0, state.getBet(0, 0));
        assertEquals(0, state.getInsurance(0));
    }

    /**
     * The hole cards seen in copies from each player's point of view; the face-up cards, chips, insurance and the set
     * of hidden cards are checked to be unchanged in every copy.
     */
    private Set<FrenchCard> holeCardsInCopies(int nCopies) {
        Set<FrenchCard> hidden = new HashSet<>(state.getDrawDeck().getComponents());
        hidden.addAll(state.getHoleCard().getComponents());
        Set<FrenchCard> seen = new HashSet<>();
        for (int i = 0; i < nCopies; i++) {
            BlackjackGameState copy = (BlackjackGameState) state.copy(i % state.getNPlayers());
            assertEquals(state.getDealerHand().getComponents(), copy.getDealerHand().getComponents());
            assertArrayEquals(state.chips, copy.chips);
            assertArrayEquals(state.insurance, copy.insurance);
            assertEquals(state.getGamePhase(), copy.getGamePhase());
            Set<FrenchCard> copyHidden = new HashSet<>(copy.getDrawDeck().getComponents());
            copyHidden.addAll(copy.getHoleCard().getComponents());
            assertEquals(hidden, copyHidden);
            seen.add(copy.getHoleCard().peek());
        }
        return seen;
    }

    @Test
    public void underAnAceTheHoleCardIsUnconstrainedUntilThePeek() {
        betAndDeal(state, fm, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "AS", "9D", "");
        insure(state, fm, true);
        assertEquals("arrangement: still in the Insurance phase", INSURANCE, state.getGamePhase());
        assertTrue("a ten-value card may be in the hole before the peek",
                holeCardsInCopies(300).stream().anyMatch(BlackjackGameState::isTenValue));

        insure(state, fm, false, false);
        assertEquals("arrangement: play after a failed peek", Play, state.getGamePhase());
        for (FrenchCard c : holeCardsInCopies(300))
            assertFalse("ten-value " + c + " in the hole under an Ace after the peek", isTenValue(c));
    }

    @Test
    public void underATenTheHoleCardIsUnconstrainedUntilThePeek() {
        betAndDeal(state, fm, new int[]{2, 4, 6}, new String[]{"10H 2C", "9S 8D", "7C 7D"}, "KS", "9D", "");
        assertEquals("arrangement: in the Insurance phase", INSURANCE, state.getGamePhase());
        assertTrue("an Ace may be in the hole before the peek",
                holeCardsInCopies(300).stream().anyMatch(c -> c.type == Ace));

        insure(state, fm, false, true, false);
        assertEquals("arrangement: play after a failed peek", Play, state.getGamePhase());
        for (FrenchCard c : holeCardsInCopies(300))
            assertNotEquals("Ace " + c + " in the hole under a ten after the peek", Ace, c.type);
    }
}
