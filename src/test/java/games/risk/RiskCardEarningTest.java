package games.risk;

import games.risk.actions.*;
import games.risk.components.RiskCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * Earning a RISK card at the end of attacking after a capture, the reshuffle of the traded-in cards when the draw
 * deck runs out, and the per-turn flags.
 * Base: 3 players; player 1 holds everything with 1 army, player 2 Argentina, player 0 Indonesia (6 armies);
 * player 0 is attacking; all 44 cards in the draw deck.
 */
public class RiskCardEarningTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskGameState state;

    @Before
    public void setup() {
        state = newState(3, 7, null);
        fillBoard(state, 1);
        give(state, 2, 1, ARGENTINA);
        give(state, 0, 6, INDONESIA);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
    }

    /** Player 0 captures Siam (1 army) from Indonesia with 3 dice and moves 3 in: Indonesia 3, Siam 3. */
    private void captureSiam() {
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
    }

    @Test
    public void aCaptureIsRecordedForTheTurn() {
        assertFalse(state.hasCapturedThisTurn());
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertTrue(state.hasCapturedThisTurn());
    }

    @Test
    public void anAttackWithoutCaptureRecordsNone() {
        state.setNextRolls(1, 1, 1, 6); // 1 v 6: the attacker loses 1
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertEquals(5, state.getArmies(INDONESIA));
        assertFalse(state.hasCapturedThisTurn());
    }

    @Test
    public void endingTheAttackAfterACaptureDrawsTheTopCard() {
        captureSiam();
        RiskCard top = state.getDrawDeck().peek();
        fm.next(state, new EndAttack());
        assertEquals(List.of(top), state.getHand(0).getComponents());
        assertEquals(43, state.getDrawDeck().getSize()); // 44 - 1
        assertEquals(RiskGamePhase.FORTIFY, state.getGamePhase());
    }

    @Test
    public void endingTheAttackWithoutACaptureDrawsNothing() {
        state.setNextRolls(1, 1, 1, 6);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        fm.next(state, new EndAttack());
        assertEquals(0, state.getHand(0).getSize());
        assertEquals(44, state.getDrawDeck().getSize());
    }

    @Test
    public void twoCapturesInATurnEarnOnlyOneCard() {
        captureSiam();
        // Siam (3) takes India (1 army) with 2 dice
        state.setNextRolls(6, 6, 1);
        fm.next(state, new Attack(SIAM, INDIA, 2));
        fm.next(state, new MoveArmies(SIAM, INDIA, 2));
        assertEquals(0, state.getOwner(INDIA));
        fm.next(state, new EndAttack());
        assertEquals(1, state.getHand(0).getSize());
        assertEquals(43, state.getDrawDeck().getSize()); // 44 - 1
    }

    @Test
    public void anEmptyDrawDeckIsRefilledFromTheTradedInCards() {
        // player 1 holds 3 cards; the other 41 were traded in
        giveCards(state, 1, card(ALASKA), card(PERU), card(GREENLAND));
        discardWholeDeck(state);
        assertEquals(41, state.getDiscardPile().getSize());
        captureSiam();
        fm.next(state, new EndAttack());
        assertEquals(1, state.getHand(0).getSize());
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals(40, state.getDrawDeck().getSize()); // 41 reshuffled, 1 drawn
        assertEquals(3, state.getHand(1).getSize());
        assertEquals(44, allCards(state).size());
    }

    @Test
    public void theTradedInCardsAreShuffledToMakeTheNewDrawDeck() {
        discardWholeDeck(state);
        captureSiam();
        // the copy differs from the state only in its random number generator
        RiskGameState other = (RiskGameState) state.copy();
        assertEquals(state, other);
        fm.next(state, new EndAttack());
        fm.next(other, new EndAttack());
        List<RiskCard> order = new ArrayList<>(state.getHand(0).getComponents());
        order.addAll(state.getDrawDeck().getComponents());
        List<RiskCard> otherOrder = new ArrayList<>(other.getHand(0).getComponents());
        otherOrder.addAll(other.getDrawDeck().getComponents());
        assertEquals(44, order.size());
        assertNotEquals(order, otherOrder);
    }

    @Test
    public void withNoCardsInTheDeckOrTradedInNoCardIsDrawn() {
        // all 44 cards in player 1's hand
        while (state.getDrawDeck().getSize() > 0)
            state.getHand(1).add(state.getDrawDeck().draw());
        captureSiam();
        fm.next(state, new EndAttack());
        assertEquals(0, state.getHand(0).getSize());
        assertEquals(44, state.getHand(1).getSize());
        assertEquals(RiskGamePhase.FORTIFY, state.getGamePhase());
    }

    @Test
    public void theTurnFlagsAreClearedWhenTheNextTurnStarts() {
        state.setCapturedThisTurn(true);
        state.setTerritoryBonusTaken(true);
        startPlay(state, 0, RiskGamePhase.FORTIFY, 0);
        fm.next(state, new EndTurn());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertFalse(state.hasCapturedThisTurn());
        assertFalse(state.isTerritoryBonusTaken());
    }

    @Test
    public void theNextPlayerEarnsNoCardFromTheLastPlayersCapture() {
        captureSiam();
        fm.next(state, new EndAttack());
        fm.next(state, new EndTurn());
        assertEquals(1, state.getCurrentPlayer());
        while (state.getGamePhase() == RiskGamePhase.REINFORCE)
            fm.next(state, new PlaceArmy(state.getTerritories(1).get(0)));
        fm.next(state, new EndAttack());
        assertEquals(0, state.getHand(1).getSize());
        assertEquals(1, state.getHand(0).getSize());
        assertEquals(43, state.getDrawDeck().getSize());
    }
}
