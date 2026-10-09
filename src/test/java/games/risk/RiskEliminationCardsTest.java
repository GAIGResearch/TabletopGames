package games.risk;

import core.actions.AbstractAction;
import games.risk.actions.*;
import games.risk.components.RiskCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * The cards of a player eliminated by a capture, and the EliminationTrade when the attacker then holds
 * eliminationTradeLimit (6) cards or more.
 * Base: 4 players; player 1 holds everything with 1 army, player 0 Indonesia (6 armies), player 2 only Siam
 * (1 army), player 3 Argentina; player 0 is attacking.
 * Indonesia borders Siam, New Guinea, Western Australia; Siam borders India, China, Indonesia.
 * Symbols: Infantry - Alaska, Venezuela, Peru, Siam; Cavalry - Greenland, Alberta, Ontario;
 * Artillery - Northwest Territory, Western and Eastern United States.
 */
public class RiskEliminationCardsTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskGameState state;

    static final List<RiskCard> INFANTRY_SET = List.of(card(ALASKA), card(VENEZUELA), card(PERU));
    static final List<RiskCard> CAVALRY_SET = List.of(card(GREENLAND), card(ALBERTA), card(ONTARIO));
    static final List<RiskCard> ARTILLERY_SET = List.of(card(NORTHWEST_TERRITORY), card(WESTERN_UNITED_STATES),
            card(EASTERN_UNITED_STATES));

    @Before
    public void setup() {
        state = newState(4, 7, null);
        arrange(state);
    }

    private static void arrange(RiskGameState s) {
        fillBoard(s, 1);
        give(s, 0, 6, INDONESIA);
        give(s, 2, 1, SIAM);
        give(s, 3, 1, ARGENTINA);
        startPlay(s, 0, RiskGamePhase.ATTACK, 0);
    }

    /** Player 0 takes Siam with 3 dice, eliminating player 2; the move-in is still to choose. */
    private void eliminatePlayer2() {
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertTrue(state.isEliminated(2));
    }

    /**
     * The ATTACK actions with Indonesia 3 armies (1-2 dice into New Guinea, Western Australia) and Siam with the
     * given armies (up to 3 dice into India, China), a Blitz for each of those pairs (allowBlitz), plus EndAttack.
     */
    private static Set<AbstractAction> attackActions(int siamArmies) {
        Set<AbstractAction> expected = new HashSet<>();
        for (int n = 1; n <= 2; n++) {
            expected.add(new Attack(INDONESIA, NEW_GUINEA, n));
            expected.add(new Attack(INDONESIA, WESTERN_AUSTRALIA, n));
        }
        for (int n = 1; n <= Math.min(3, siamArmies - 1); n++) {
            expected.add(new Attack(SIAM, INDIA, n));
            expected.add(new Attack(SIAM, CHINA, n));
        }
        expected.add(new Blitz(INDONESIA, NEW_GUINEA));
        expected.add(new Blitz(INDONESIA, WESTERN_AUSTRALIA));
        if (siamArmies >= 2) {
            expected.add(new Blitz(SIAM, INDIA));
            expected.add(new Blitz(SIAM, CHINA));
        }
        expected.add(new EndAttack());
        return expected;
    }

    @Test
    public void theEliminatedPlayersCardsGoToTheAttacker() {
        giveCards(state, 0, card(ALASKA));
        giveCards(state, 2, card(GREENLAND), card(NORTHWEST_TERRITORY));
        eliminatePlayer2();
        assertEquals(new HashSet<>(List.of(card(ALASKA), card(GREENLAND), card(NORTHWEST_TERRITORY))),
                new HashSet<>(state.getHand(0).getComponents()));
        assertEquals(3, state.getHand(0).getSize()); // 1 + 2
        assertEquals(0, state.getHand(2).getSize());
        assertEquals(44, allCards(state).size());
        // the move-in comes first, as usual
        assertTrue(state.currentActionInProgress() instanceof MoveArmiesChoice);
    }

    @Test
    public void withFewerThanSixCardsThereIsNoTradeUntilTheNextTurn() {
        // 3 + 2 = 5 cards, including the Infantry set: 5 < 6, so no trade now (though 5 >= handLimit)
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        giveCards(state, 2, card(GREENLAND), card(ALBERTA));
        eliminatePlayer2();
        assertEquals(5, state.getHand(0).getSize());
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3)); // Indonesia 3, Siam 3
        assertFalse(state.isActionInProgress());
        assertEquals(attackActions(3), attackOptions(fm, state));
        assertEquals(0, state.getArmiesToPlace(0));
    }

    @Test
    public void theEliminationTradeLimitParameterSetsWhenTheTradeIsForced() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("eliminationTradeLimit", 5);
        state = newState(4, 7, params);
        arrange(state);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        giveCards(state, 2, card(GREENLAND), card(ALBERTA));
        eliminatePlayer2();
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        // 5 >= 5: the trade is forced; the only set is the Infantry one (2 Cavalry, no Artillery)
        assertEquals(Set.of(new TradeCards(INFANTRY_SET, null)), actionSet(fm, state));
    }

    @Test
    public void withSixCardsTheAttackerMovesInThenTradesThenPlacesThenAttacksAgain() {
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        giveCards(state, 2, card(GREENLAND), card(ALBERTA), card(ONTARIO));
        eliminatePlayer2();
        assertEquals(6, state.getHand(0).getSize()); // 3 + 3
        // the move-in choice first (3 .. 5 of Indonesia's 6), with the trade waiting underneath it
        assertEquals(Set.of(new MoveArmies(INDONESIA, SIAM, 3), new MoveArmies(INDONESIA, SIAM, 4),
                new MoveArmies(INDONESIA, SIAM, 5)), actionSet(fm, state));
        assertEquals(new EliminationTrade(0), state.getQueuedAction(0));
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3)); // Indonesia 3, Siam 3

        // 6 >= 5 cards: only trades - the two sets of one symbol, no territory held (no Artillery for one of each)
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Set.of(new TradeCards(INFANTRY_SET, null), new TradeCards(CAVALRY_SET, null)),
                actionSet(fm, state));
        fm.next(state, new TradeCards(INFANTRY_SET, null));
        assertEquals(3, state.getHand(0).getSize());
        assertEquals(1, state.getNSetsTraded());
        assertEquals(4, state.getArmiesToPlace(0)); // 0 + 4 (first set)
        assertEquals(3, state.getDiscardPile().getSize());

        // 3 < 5: no more trades (even with the Cavalry set in hand); place the 4 armies
        assertEquals(Set.of(new PlaceArmy(INDONESIA), new PlaceArmy(SIAM)), actionSet(fm, state));
        for (int i = 0; i < 4; i++) {
            assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
            fm.next(state, new PlaceArmy(SIAM));
        }
        assertEquals(7, state.getArmies(SIAM)); // 3 + 4
        assertEquals(0, state.getArmiesToPlace(0));

        // attacking resumes
        assertFalse(state.isActionInProgress());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertEquals(attackActions(7), attackOptions(fm, state));
        assertEquals(44, allCards(state).size());

        // and the capture still earns a card at the end of attacking: 3 + 1
        fm.next(state, new EndAttack());
        assertEquals(4, state.getHand(0).getSize());
    }

    @Test
    public void withNineCardsTheAttackerTradesTwiceToGetBelowFive() {
        // two sets already traded in the game: these are the third (8) and fourth (10)
        state.setNSetsTraded(2);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU), card(NORTHWEST_TERRITORY));
        giveCards(state, 2, card(GREENLAND), card(ALBERTA), card(ONTARIO), card(WESTERN_UNITED_STATES),
                card(EASTERN_UNITED_STATES));
        eliminatePlayer2();
        assertEquals(9, state.getHand(0).getSize()); // 4 + 5
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new TradeCards(INFANTRY_SET, null)));
        for (AbstractAction a : actions)
            assertTrue(a + " offered with 9 cards", a instanceof TradeCards);
        fm.next(state, new TradeCards(INFANTRY_SET, null));
        assertEquals(6, state.getHand(0).getSize());
        assertEquals(8, state.getArmiesToPlace(0)); // third set

        // 6 >= 5: trade again
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new TradeCards(CAVALRY_SET, null)));
        for (AbstractAction a : actions)
            assertTrue(a + " offered with 6 cards", a instanceof TradeCards);
        fm.next(state, new TradeCards(CAVALRY_SET, null));
        assertEquals(3, state.getHand(0).getSize()); // the Artillery set, kept
        assertEquals(18, state.getArmiesToPlace(0)); // 8 + 10 (fourth set)
        assertEquals(4, state.getNSetsTraded());

        // 18 -> 13 -> 8 -> 3 in batches of 5, then the last 3 one at a time
        assertEquals(Set.of(new PlaceArmy(INDONESIA, 5), new PlaceArmy(SIAM, 5)), actionSet(fm, state));
        for (int i = 0; i < 3; i++)
            fm.next(state, new PlaceArmy(INDONESIA, 5));
        assertEquals(Set.of(new PlaceArmy(INDONESIA), new PlaceArmy(SIAM)), actionSet(fm, state));
        for (int i = 0; i < 3; i++)
            fm.next(state, new PlaceArmy(INDONESIA));
        assertEquals(21, state.getArmies(INDONESIA)); // 3 + 18
        assertFalse(state.isActionInProgress());
        assertEquals(RiskGamePhase.ATTACK, state.getGamePhase());
        assertTrue(fm.computeAvailableActions(state).contains(new EndAttack()));
    }

    @Test
    public void theEliminationTradeCanTakeTheTerritoryBonus() {
        // the Siam card: Siam is player 0's after the capture
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(SIAM));
        giveCards(state, 2, card(GREENLAND), card(ALBERTA), card(ONTARIO));
        eliminatePlayer2();
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        List<RiskCard> infantry = List.of(card(ALASKA), card(VENEZUELA), card(SIAM)); // 0, 9, 37
        assertEquals(Set.of(new TradeCards(infantry, SIAM), new TradeCards(CAVALRY_SET, null)), actionSet(fm, state));
        fm.next(state, new TradeCards(infantry, SIAM));
        assertEquals(5, state.getArmies(SIAM)); // 3 + 2
        assertEquals(4, state.getArmiesToPlace(0));
        assertTrue(state.isTerritoryBonusTaken());
    }

    @Test
    public void theEliminationTradeGetsNoBonusIfItWasTakenThisTurn() {
        state.setTerritoryBonusTaken(true);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(SIAM));
        giveCards(state, 2, card(GREENLAND), card(ALBERTA), card(ONTARIO));
        eliminatePlayer2();
        fm.next(state, new MoveArmies(INDONESIA, SIAM, 3));
        List<RiskCard> infantry = List.of(card(ALASKA), card(VENEZUELA), card(SIAM));
        assertEquals(Set.of(new TradeCards(infantry, null), new TradeCards(CAVALRY_SET, null)), actionSet(fm, state));
    }

    @Test
    public void aCaptureThatWinsTheGameTakesTheCardsButNoTrade() {
        // 3 players: player 2 already out; player 1 holds only Siam, player 0 everything else
        state = newState(3, 7, null);
        fillBoard(state, 0);
        give(state, 0, 4, INDONESIA);
        give(state, 1, 1, SIAM);
        eliminate(state, 2, 3);
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        giveCards(state, 0, card(ALASKA), card(VENEZUELA), card(PERU));
        giveCards(state, 1, card(GREENLAND), card(ALBERTA), card(ONTARIO));
        state.setNextRolls(6, 6, 6, 1);
        fm.next(state, new Attack(INDONESIA, SIAM, 3));
        assertFalse(state.isNotTerminal());
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(6, state.getHand(0).getSize()); // 3 + 3
        assertEquals(0, state.getHand(1).getSize());
        assertEquals(0, state.getNSetsTraded());
        assertFalse(state.isActionInProgress());
    }
}
