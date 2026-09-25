package games.toads;

import core.Game;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.GameType;
import games.toads.actions.PlayFieldCard;
import games.toads.actions.RecycleCard;
import games.toads.actions.ReturnCardToDeck;
import games.toads.components.ToadCard;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.MCTSEnums;
import players.mcts.MCTSParams;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.Collectors;

import static games.toads.ToadConstants.ToadGamePhase.*;
import static games.toads.ToadTestUtils.*;
import static org.junit.Assert.*;

/**
 * The Rulebook 3 opening return, the Rulebook 3 defaults including the War 2 first Attacker, the redeterminisation
 * of a deck by its owner, and a full default game (random and MCTS).
 * With the Rulebook 3 deck each player owns 9 distinct cards.
 */
public class OpeningReturnTest {

    ToadParameters params;
    ToadForwardModel fm = new ToadForwardModel();
    ToadGameState state;

    @Before
    public void setUp() {
        params = new ToadParameters(); // the defaults: the Rulebook 3 game
        params.setRandomSeed(933);
    }

    // ---------------------------------------------------------------- defaults

    @Test
    public void defaultsAreTheRulebook3Set() {
        ToadParameters p = new ToadParameters();
        // registered defaults
        assertEquals(true, p.getParameterValue("openingReturn"));
        assertEquals(false, p.getParameterValue("discardOption"));
        assertEquals(ToadParameters.SecondRoundStart.TWO, p.getParameterValue("secondRoundStart"));
        // the field initialisers match them (the forward model reads the fields)
        assertTrue(p.openingReturn);
        assertFalse(p.discardOption);
        assertEquals(ToadParameters.SecondRoundStart.TWO, p.secondRoundStart);
    }

    // ---------------------------------------------------------------- War 1

    @Test
    public void setupDealsFiveCardsEachAndPlayerZeroReturnsFirst() {
        state = newState(params, fm);
        assertEquals(OPENING_RETURN, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        for (int p = 0; p < 2; p++) {
            // handSize + 1 = 5 in hand; 9 - 5 = 4 left in the deck
            assertEquals(5, state.getPlayerHand(p).getSize());
            assertEquals(4, state.getPlayerDeck(p).getSize());
        }
    }

    @Test
    public void returnOptionsAreOnePerCardInHandWithNoKeepAll() {
        state = newState(params, fm);
        List<AbstractAction> expected = state.getPlayerHand(0).stream()
                .map(ReturnCardToDeck::new).collect(Collectors.toList());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // 5 distinct cards -> 5 options; no RecycleCard(null) or other "keep all" option
        assertEquals(5, actions.size());
        assertEquals(new HashSet<>(expected), new HashSet<>(actions));
        assertFalse(actions.contains(new RecycleCard(null)));
    }

    @Test
    public void returnedCardGoesToTheBottomOfTheDeckWithNoReplacementDraw() {
        state = newState(params, fm);
        ToadCard returned = state.getPlayerHand(0).get(2);
        fm.next(state, new ReturnCardToDeck(returned));
        // hand 5 - 1 = 4 (no draw); deck 4 + 1 = 5, the returned card last (the bottom)
        assertEquals(4, state.getPlayerHand(0).getSize());
        assertFalse(state.getPlayerHand(0).contains(returned));
        assertEquals(5, state.getPlayerDeck(0).getSize());
        assertSame(returned, state.getPlayerDeck(0).get(4));
        // player 1 is untouched and chooses next
        assertEquals(5, state.getPlayerHand(1).getSize());
        assertEquals(4, state.getPlayerDeck(1).getSize());
        assertEquals(OPENING_RETURN, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void returnedCardIsVisibleToItsOwnerOnly() {
        state = newState(params, fm);
        fm.next(state, new ReturnCardToDeck(state.getPlayerHand(0).get(0)));
        PartialObservableDeck<ToadCard> deck = state.getPlayerDeck(0);
        assertTrue(deck.getVisibilityForPlayer(4, 0));
        assertFalse(deck.getVisibilityForPlayer(4, 1));
        // the undrawn cards above it stay hidden from both
        for (int i = 0; i < 4; i++) {
            assertFalse(deck.getVisibilityForPlayer(i, 0));
            assertFalse(deck.getVisibilityForPlayer(i, 1));
        }
    }

    @Test
    public void afterBothReturnsPlayStartsWithPlayerZeroAttacking() {
        state = newState(params, fm);
        fm.next(state, new ReturnCardToDeck(state.getPlayerHand(0).get(0)));
        ToadCard returned1 = state.getPlayerHand(1).get(3);
        assertEquals(new HashSet<>(state.getPlayerHand(1).stream().map(ReturnCardToDeck::new).toList()),
                new HashSet<>(fm.computeAvailableActions(state)));
        fm.next(state, new ReturnCardToDeck(returned1));
        assertSame(returned1, state.getPlayerDeck(1).get(4));
        // discardOption is off by default, so straight to PLAY
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        for (int p = 0; p < 2; p++) {
            assertEquals(4, state.getPlayerHand(p).getSize());
            assertEquals(5, state.getPlayerDeck(p).getSize());
        }
        assertTrue(fm.computeAvailableActions(state).stream().allMatch(a -> a instanceof PlayFieldCard));
    }

    @Test
    public void withDiscardOptionTheRecyclePhaseFollowsTheReturns() {
        params.setParameterValue("discardOption", true);
        state = newState(params, fm);
        assertEquals(OPENING_RETURN, state.getGamePhase());
        fm.next(state, new ReturnCardToDeck(state.getPlayerHand(0).get(0)));
        fm.next(state, new ReturnCardToDeck(state.getPlayerHand(1).get(0)));
        assertEquals(DISCARD, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(fm.computeAvailableActions(state).contains(new RecycleCard(null)));
    }

    @Test
    public void withoutOpeningReturnFourCardsAreDealtAndPlayStarts() {
        params.setParameterValue("openingReturn", false);
        state = newState(params, fm);
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        for (int p = 0; p < 2; p++) {
            // handSize 4 in hand; 9 - 4 = 5 in the deck
            assertEquals(4, state.getPlayerHand(p).getSize());
            assertEquals(5, state.getPlayerDeck(p).getSize());
        }
    }

    // ---------------------------------------------------------------- War 2

    /**
     * Arranges the last Battle of War 1 (conserving each player's 9 cards) with player 0 ahead 3-0 and resolves it
     * through _afterAction: Field = card 0, Flank = card 1 (also in hand, as when played), Casualty-to-be = card 2
     * (the only other card in hand), Discards = cards 3..8; decks empty. Tactics off so there is no post-battle
     * decision. Player 1 can gain at most 2 in this Battle, so player 0 wins War 1 (3 v <= 2) - the WINNER start
     * would give player 0. Afterwards each War 2 deck is the opponent's 6 + 2 discards = 8 cards.
     * Returns the two Casualties.
     */
    private ToadCard[] finishWarOneWithPlayerZeroAhead() {
        params.setParameterValue("useTactics", false);
        state = newState(params, fm);
        state.setGamePhase(PLAY);
        ToadCard[] casualties = new ToadCard[2];
        for (int p = 0; p < 2; p++) {
            List<ToadCard> all = new ArrayList<>(state.getPlayerHand(p).getComponents());
            all.addAll(state.getPlayerDeck(p).getComponents());
            assertEquals(9, all.size());
            state.getPlayerHand(p).clear();
            state.getPlayerDeck(p).clear();
            state.getDiscards(p).clear();
            state.fieldCards[p] = all.get(0);
            state.hiddenFlankCards[p] = all.get(1);
            state.getPlayerHand(p).add(all.get(1));
            state.getPlayerHand(p).add(all.get(2));
            casualties[p] = all.get(2);
            for (int i = 3; i < 9; i++)
                state.getDiscards(p).add(all.get(i));
        }
        state.battlesWon[0][0] = 3;
        state.battlesWon[0][1] = 0;
        state.nextBattle = 3;
        fm._afterAction(state, null);
        assertEquals("War 1 did not end", 1, state.getRoundCounter());
        assertTrue(state.battlesWon[0][0] > state.battlesWon[0][1]);
        assertSame(casualties[0], state.getTieBreaker(0));
        assertSame(casualties[1], state.getTieBreaker(1));
        return casualties;
    }

    @Test
    public void warTwoStartsWithFiveCardsAndPlayerOneReturningFirst() {
        finishWarOneWithPlayerZeroAhead();
        // War 2 first Attacker = the Defender of War 1's first Battle = player 1 (TWO), even though player 0 won War 1
        assertEquals(OPENING_RETURN, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        for (int p = 0; p < 2; p++) {
            // 8 cards: 5 in hand, 8 - 5 = 3 in the deck
            assertEquals(5, state.getPlayerHand(p).getSize());
            assertEquals(3, state.getPlayerDeck(p).getSize());
        }
        List<AbstractAction> expected = state.getPlayerHand(1).stream()
                .map(ReturnCardToDeck::new).collect(Collectors.toList());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(5, actions.size());
        assertEquals(new HashSet<>(expected), new HashSet<>(actions));
    }

    @Test
    public void warTwoReturnsGoToTheBottomAndPlayerOneAttacksFirst() {
        finishWarOneWithPlayerZeroAhead();
        ToadCard returned1 = state.getPlayerHand(1).get(1);
        fm.next(state, new ReturnCardToDeck(returned1));
        // player 1: hand 5 - 1 = 4, deck 3 + 1 = 4 with the returned card at the bottom, seen by player 1 only
        assertEquals(4, state.getPlayerHand(1).getSize());
        assertEquals(4, state.getPlayerDeck(1).getSize());
        assertSame(returned1, state.getPlayerDeck(1).get(3));
        assertTrue(state.getPlayerDeck(1).getVisibilityForPlayer(3, 1));
        assertFalse(state.getPlayerDeck(1).getVisibilityForPlayer(3, 0));
        assertEquals(OPENING_RETURN, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());

        ToadCard returned0 = state.getPlayerHand(0).get(4);
        fm.next(state, new ReturnCardToDeck(returned0));
        assertSame(returned0, state.getPlayerDeck(0).get(3));
        assertEquals(4, state.getPlayerHand(0).getSize());
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void warTwoWithoutOpeningReturnDealsFourAndPlayerOneAttacks() {
        params.setParameterValue("openingReturn", false);
        finishWarOneWithPlayerZeroAhead();
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        for (int p = 0; p < 2; p++) {
            // 8 cards: 4 in hand, 4 in the deck
            assertEquals(4, state.getPlayerHand(p).getSize());
            assertEquals(4, state.getPlayerDeck(p).getSize());
        }
    }

    // ---------------------------------------------------------------- copy / redeterminisation

    private static final int COPIES = 20;

    @Test
    public void ownersCopyKeepsTheReturnedCardAtTheBottomAndVisible() {
        state = newState(params, fm);
        ToadCard returned = state.getPlayerHand(0).get(2);
        fm.next(state, new ReturnCardToDeck(returned));
        for (int i = 0; i < COPIES; i++) {
            ToadGameState copy = (ToadGameState) state.copy(0);
            PartialObservableDeck<ToadCard> deck = copy.getPlayerDeck(0);
            assertEquals(5, deck.getSize());
            assertEquals("copy " + i, returned, deck.get(4));
            assertTrue("copy " + i, deck.getVisibilityForPlayer(4, 0));
            assertFalse("copy " + i, deck.getVisibilityForPlayer(4, 1));
        }
    }

    @Test
    public void opponentsCopyDoesNotKnowTheReturnedCard() {
        state = newState(params, fm);
        ToadCard returned = state.getPlayerHand(0).get(2);
        fm.next(state, new ReturnCardToDeck(returned));
        // player 0's 9 cards (hand 4 + deck 5) are all unknown to player 1, so the bottom card is redeterminised
        // among them: over 20 copies it cannot stay the returned card every time ((1/9)^20)
        boolean moved = false;
        for (int i = 0; i < COPIES; i++) {
            ToadGameState copy = (ToadGameState) state.copy(1);
            PartialObservableDeck<ToadCard> deck = copy.getPlayerDeck(0);
            assertEquals(5, deck.getSize());
            for (int j = 0; j < 5; j++)
                assertFalse("copy " + i + " card " + j, deck.getVisibilityForPlayer(j, 1));
            if (!deck.get(4).equals(returned)) moved = true;
        }
        assertTrue(moved);
    }

    @Test
    public void ownersWarTwoCopyKeepsTheReturnedCardAndStillHidesTheOpponentsCasualty() {
        ToadCard[] casualties = finishWarOneWithPlayerZeroAhead();
        ToadCard returned1 = state.getPlayerHand(1).get(0);
        fm.next(state, new ReturnCardToDeck(returned1));
        // player 1's copy: player 0's Casualty is shuffled with player 1's unknown undrawn cards (deck 0..2),
        // but the known returned card stays at index 3
        boolean casualtyHidden = false;
        for (int i = 0; i < COPIES; i++) {
            ToadGameState copy = (ToadGameState) state.copy(1);
            PartialObservableDeck<ToadCard> deck = copy.getPlayerDeck(1);
            assertEquals(4, deck.getSize());
            assertEquals("copy " + i, returned1, deck.get(3));
            assertTrue("copy " + i, deck.getVisibilityForPlayer(3, 1));
            if (!casualties[0].equals(copy.getTieBreaker(0))) casualtyHidden = true;
        }
        // 1 Casualty among 4 unknown cards: (1/4)^20 to stay put every time
        assertTrue(casualtyHidden);
    }

    // ---------------------------------------------------------------- full games

    @Test
    public void defaultGamePlaysToTheEndWithTwoReturnsEach() {
        state = newState(params, fm);
        Random rnd = new Random(933);
        List<Integer> returners = new ArrayList<>();
        int steps = 0;
        while (state.isNotTerminal() && steps < 500) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            if (state.getGamePhase() == OPENING_RETURN) {
                int player = state.getCurrentPlayer();
                int war = state.getRoundCounter();
                assertTrue(action instanceof ReturnCardToDeck);
                assertEquals(5, state.getPlayerHand(player).getSize());
                fm.next(state, action);
                returners.add(player);
                // after the return: hand 4; deck 9 - 4 = 5 in War 1, 8 - 4 = 4 in War 2
                assertEquals(4, state.getPlayerHand(player).getSize());
                assertEquals(war == 0 ? 5 : 4, state.getPlayerDeck(player).getSize());
                assertSame(((ReturnCardToDeck) action).card, state.getPlayerDeck(player).peek(state.getPlayerDeck(player).getSize() - 1));
            } else {
                fm.next(state, action);
            }
            steps++;
        }
        assertFalse("game did not end within 500 steps", state.isNotTerminal());
        // War 1: player 0 then 1; War 2: the War 2 first Attacker (player 1) then player 0
        assertEquals(List.of(0, 1, 1, 0), returners);
    }

    @Test
    public void mctsPlayerPlaysAFullDefaultGame() {
        state = new ToadGameState(params, 2);
        Game game = new Game(GameType.WarOfTheToads, fm, state);
        MCTSParams mctsParams = new MCTSParams();
        mctsParams.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        mctsParams.budget = 200;
        mctsParams.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OneTree;
        ToadMCTSPlayer mcts = new ToadMCTSPlayer(mctsParams);
        game.reset(List.of(mcts, new RandomPlayer(new Random(933))), 933);
        game.run();
        ToadGameState finalState = (ToadGameState) game.getGameState();
        assertFalse(finalState.isNotTerminal());
        // the MCTS player (player 0) made both of its opening returns
        long mctsReturns = finalState.getHistory().stream()
                .filter(p -> p.a == 0 && p.b instanceof ReturnCardToDeck).count();
        assertEquals(2, mctsReturns);
    }
}
