package games.risk;

import core.actions.AbstractAction;
import games.risk.actions.*;
import games.risk.components.RiskCard;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.risk.WorldMap.*;
import static games.risk.RiskTestUtils.*;
import static org.junit.Assert.*;

public class RiskCopyTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskGameState state;

    @Before
    public void setup() {
        // mid-game: player 0 attacks from Indonesia; cards moved from the draw deck to hands (3, 2, 0)
        state = newState(3, 7, null);
        fillBoard(state, 1);
        give(state, 2, 1, ARGENTINA);
        give(state, 0, 6, INDONESIA);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        for (int i = 0; i < 3; i++) state.getHand(0).add(state.getDrawDeck().draw());
        for (int i = 0; i < 2; i++) state.getHand(1).add(state.getDrawDeck().draw());
    }

    @Test
    public void copyEqualsTheOriginalWithTheSameHash() {
        RiskGameState copy = (RiskGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void changingTheCopyLeavesTheOriginalAlone() {
        RiskGameState copy = (RiskGameState) state.copy();
        copy.addArmies(INDONESIA, 2);
        assertEquals(6, state.getArmies(INDONESIA));
        assertNotEquals(state, copy);

        copy = (RiskGameState) state.copy();
        copy.setOwner(SIAM, 2);
        assertEquals(1, state.getOwner(SIAM));
        assertNotEquals(state, copy);

        copy = (RiskGameState) state.copy();
        copy.setGamePhase(RiskGamePhase.FORTIFY);
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertNotEquals(state, copy);

        copy = (RiskGameState) state.copy();
        copy.setArmiesToPlace(1, 4);
        assertEquals(0, state.getArmiesToPlace(1));
        assertNotEquals(state, copy);
    }

    @Test
    public void playerCopyKeepsHandSizesAndTheObserversOwnHand() {
        for (int observer = 0; observer < 3; observer++) {
            RiskGameState copy = (RiskGameState) state.copy(observer);
            for (int p = 0; p < 3; p++)
                assertEquals(state.getHand(p).getSize(), copy.getHand(p).getSize());
            assertEquals(state.getDrawDeck().getSize(), copy.getDrawDeck().getSize());
            assertEquals(state.getHand(observer).getComponents(), copy.getHand(observer).getComponents());
            for (RiskTerritory t : WorldMap.ALL) {
                assertEquals(state.getOwner(t), copy.getOwner(t));
                assertEquals(state.getArmies(t), copy.getArmies(t));
            }
        }
    }

    @Test
    public void playerCopyRedeterminisesTheOtherPlayersHands() {
        // player 1's 2 cards are hidden from player 0: over 20 copies they are dealt again from the 41 unseen cards
        // (2 in player 1's hand + 39 in the draw deck), so some copy gives player 1 a different hand
        boolean changed = false;
        for (int i = 0; i < 20 && !changed; i++) {
            RiskGameState copy = (RiskGameState) state.copy(0);
            changed = !copy.getHand(1).getComponents().equals(state.getHand(1).getComponents());
        }
        assertTrue("player 1's hand never redeterminised", changed);
    }

    /** 3 cards traded in (the top 3 of the draw deck), player 0 has captured and taken the bonus this turn. */
    private void arrangeCardTrading() {
        for (int i = 0; i < 3; i++)
            state.getDiscardPile().add(state.getDrawDeck().draw());
        state.setNSetsTraded(1);
        state.setCapturedThisTurn(true);
        state.setTerritoryBonusTaken(true);
    }

    @Test
    public void copyKeepsTheTradedInCardsAndTheTurnFlags() {
        arrangeCardTrading();
        RiskGameState copy = (RiskGameState) state.copy();
        assertEquals(state.getDiscardPile().getComponents(), copy.getDiscardPile().getComponents());
        assertEquals(1, copy.getNSetsTraded());
        assertTrue(copy.hasCapturedThisTurn());
        assertTrue(copy.isTerritoryBonusTaken());
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void theCardTradingStateIsPartOfEquality() {
        arrangeCardTrading();
        RiskGameState copy = (RiskGameState) state.copy();
        copy.setNSetsTraded(2);
        assertEquals(1, state.getNSetsTraded());
        assertNotEquals(state, copy);

        copy = (RiskGameState) state.copy();
        copy.setCapturedThisTurn(false);
        assertTrue(state.hasCapturedThisTurn());
        assertNotEquals(state, copy);

        copy = (RiskGameState) state.copy();
        copy.setTerritoryBonusTaken(false);
        assertTrue(state.isTerritoryBonusTaken());
        assertNotEquals(state, copy);

        // the same cards traded in, in a different order
        List<RiskCard> traded = new ArrayList<>(state.getDiscardPile().getComponents());
        copy = (RiskGameState) state.copy();
        List<RiskCard> reversed = new ArrayList<>(copy.getDiscardPile().getComponents());
        Collections.reverse(reversed);
        copy.getDiscardPile().setComponents(reversed);
        assertEquals(traded, state.getDiscardPile().getComponents());
        assertNotEquals(state, copy);

        // the copy's discard pile is its own
        copy = (RiskGameState) state.copy();
        copy.getDiscardPile().draw();
        assertEquals(3, state.getDiscardPile().getSize());
    }

    @Test
    public void playerCopyLeavesTheTradedInCardsFaceUp() {
        arrangeCardTrading();
        List<RiskCard> traded = new ArrayList<>(state.getDiscardPile().getComponents());
        for (int i = 0; i < 20; i++) {
            RiskGameState copy = (RiskGameState) state.copy(0);
            assertEquals(traded, copy.getDiscardPile().getComponents());
            // the redeterminised hands and deck are dealt from the unseen cards only
            for (RiskCard c : traded) {
                assertFalse(copy.getHand(1).contains(c));
                assertFalse(copy.getDrawDeck().contains(c));
            }
            assertEquals(44, allCards(copy).size());
        }
    }

    @Test
    public void copyKeepsAPendingEliminationTrade() {
        // the hands go back to the draw deck; player 2 holds only Siam (1 army) and 3 cards, and player 0 has 3
        // cards, so will have 6 after the capture
        give(state, 1, 1, ARGENTINA);
        give(state, 2, 1, SIAM);
        while (state.getHand(0).getSize() > 0) state.getDrawDeck().add(state.getHand(0).draw());
        while (state.getHand(1).getSize() > 0) state.getDrawDeck().add(state.getHand(1).draw());
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        giveCards(state, 2, card(GREENLAND), card(ALBERTA), card(ONTARIO));
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));

        // the move-in pending, the trade under it
        RiskGameState copy = (RiskGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(new EliminationTrade(0), copy.getQueuedAction(0));
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        fm.next(copy, new MoveArmies(INDONESIA, SIAM, 3));
        assertEquals(state, copy);

        // the trade pending
        copy = (RiskGameState) state.copy();
        assertEquals(state, copy);
        assertTrue(copy.isActionInProgress());
        assertEquals(new EliminationTrade(0), copy.currentActionInProgress());
        Set<AbstractAction> expected = Set.of(new TradeCards(List.of(card(ALASKA), card(VENEZUELA), card(PERU)), null),
                new TradeCards(List.of(card(GREENLAND), card(ALBERTA), card(ONTARIO)), null));
        assertEquals(expected, new HashSet<>(fm.computeAvailableActions(copy)));
    }

    @Test
    public void copyDuringACaptureMoveKeepsThePendingChoice() {
        state.setNextRolls(6, 6, 6, 1); // Siam (1 army) captured
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertTrue(state.currentActionInProgress() instanceof MoveArmiesChoice);
        RiskGameState copy = (RiskGameState) state.copy();
        assertEquals(state, copy);
        assertTrue(copy.currentActionInProgress() instanceof MoveArmiesChoice);
        assertEquals(new HashSet<>(fm.computeAvailableActions(state)), new HashSet<>(fm.computeAvailableActions(copy)));
        assertEquals(3, fm.computeAvailableActions(copy).size()); // 3 .. 5
    }
}
