package games.risk;

import core.actions.AbstractAction;
import games.risk.actions.*;
import games.risk.components.RiskCard;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * Scripted walk-through of RISK cards over two turns of a real game: a trade with the territory bonus, a capture
 * earning a card, and the next player forced to trade at the start of their turn with 5 cards.
 */
public class RiskCardFlowTest {

    RiskForwardModel fm = new RiskForwardModel();

    @Test
    public void tradeCaptureEarnCardThenTheNextPlayerMustTrade() {
        // 3 players: player 1 holds everything with 1 army, player 2 Argentina, player 0 Indonesia (6) and Alaska (1)
        RiskGameState state = newState(3, 7, null);
        fillBoard(state, 1);
        give(state, 2, 1, ARGENTINA);
        give(state, 0, 6, INDONESIA);
        give(state, 0, 1, ALASKA);
        startPlay(state, 0, RiskGamePhase.REINFORCE, 3);
        // player 0: 3 Infantry (Alaska, Venezuela, Peru)
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        // player 1: Infantry Argentina, Kamchatka, Siam; Artillery Indonesia, Japan
        giveCards(state, 1, card(ARGENTINA), card(KAMCHATKA), card(SIAM), card(INDONESIA), card(JAPAN));

        // player 0 trades the Infantry set, taking the bonus on Alaska
        List<RiskCard> infantry0 = List.of(card(ALASKA), card(VENEZUELA), card(PERU));
        assertEquals(Set.of(new PlaceArmy(ALASKA), new PlaceArmy(INDONESIA), new TradeCards(infantry0, ALASKA)),
                actionSet(fm, state));
        fm.next(state, new TradeCards(infantry0, ALASKA));
        assertEquals(3, state.getArmies(ALASKA));      // 1 + 2
        assertEquals(7, state.getArmiesToPlace(0));    // 3 + 4 (first set in the game)
        for (int i = 0; i < 7; i++)
            fm.next(state, new PlaceArmy(INDONESIA));
        assertEquals(13, state.getArmies(INDONESIA));  // 6 + 7
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());

        // captures Siam, ends attacking: draws the top card
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        RiskCard top = state.getDrawDeck().peek();
        fm.next(state, new EndAttack());
        assertEquals(List.of(top), state.getHand(0).getComponents());
        fm.next(state, new EndTurn());

        // player 1's turn: 38 territories (all but Indonesia, Alaska, Siam, Argentina) -> 12, + Europe 5 + Africa 3
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(RiskGamePhase.REINFORCE, state.getGamePhase());
        assertEquals(20, state.getArmiesToPlace(1));
        assertFalse(state.isTerritoryBonusTaken());
        // 5 cards: must trade. The only set is the 3 Infantry (no Cavalry for one of each); of their territories
        // player 1 holds only Kamchatka (Argentina is player 2's, Siam player 0's)
        List<RiskCard> infantry1 = List.of(card(ARGENTINA), card(KAMCHATKA), card(SIAM)); // 12, 29, 37
        assertEquals(Set.of(new TradeCards(infantry1, KAMCHATKA)), actionSet(fm, state));
        fm.next(state, new TradeCards(infantry1, KAMCHATKA));
        assertEquals(3, state.getArmies(KAMCHATKA));   // 1 + 2
        assertEquals(26, state.getArmiesToPlace(1));   // 20 + 6 (second set in the game)
        assertEquals(2, state.getNSetsTraded());
        assertEquals(6, state.getDiscardPile().getSize());
        assertEquals(2, state.getHand(1).getSize());

        // 2 cards left: placing on the 38 territories
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(38, actions.size());
        for (AbstractAction a : actions)
            assertTrue(a instanceof PlaceArmy);
        assertEquals(44, allCards(state).size());
    }
}
