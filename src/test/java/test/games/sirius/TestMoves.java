package test.games.sirius;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.PartialObservableDeck;
import games.GameType;
import games.dicemonastery.actions.Sell;
import games.sirius.SiriusCard;
import games.sirius.SiriusForwardModel;
import games.sirius.SiriusGameState;
import games.sirius.SiriusParameters;
import games.sirius.actions.MoveToMoon;
import games.sirius.actions.SellCards;
import games.sirius.actions.TakeCard;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static games.sirius.SiriusConstants.SiriusCardType.AMMONIA;
import static games.sirius.SiriusConstants.SiriusPhase.Draw;
import static java.util.stream.Collectors.toList;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestMoves {

    Game game;
    SiriusGameState state;
    SiriusForwardModel fm = new SiriusForwardModel();
    List<AbstractPlayer> players = new ArrayList<>();

    @Before
    public void setup() {
        players = Arrays.asList(new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer());
        game = GameType.Sirius.createGameInstance(3, 34, new SiriusParameters());
        game.reset(players);
        state = (SiriusGameState) game.getGameState();
    }

    @Test
    public void plannedLocationOfPlayerMoves() {
        int currentPlayer = state.getCurrentPlayer();
        for (int i = 0; i < 3; i++)
            assertEquals(0, state.getLocationIndex(i));
        MoveToMoon moveAction = new MoveToMoon(1);
        moveAction.execute(game.getGameState());
        int[] plannedMoves = state.getMoveSelected();
        for (int i = 0; i < 3; i++) {
            assertEquals(0, state.getLocationIndex(i));
            assertEquals(i == currentPlayer ? 1 : -1, plannedMoves[i]);
        }
    }

    @Test
    public void actionLocationUpdatedOnceAllDecisionsMade() {
        MoveToMoon moveAction = new MoveToMoon(1);
        fm.next(state, moveAction);
        assertEquals(1, state.getCurrentPlayer());
        moveAction = new MoveToMoon(1);
        fm.next(state, moveAction);
        assertEquals(2, state.getCurrentPlayer());
        moveAction = new MoveToMoon(2);
        fm.next(state, moveAction);
        assertEquals(Draw, state.getGamePhase());

        assertEquals(1, state.getLocationIndex(0));
        assertEquals(1, state.getLocationIndex(1));
        assertEquals(2, state.getLocationIndex(2));
    }

    @Test
    public void allMovesAvailableExceptForCurrentLocation() {
        List<AbstractAction> moves = fm.computeAvailableActions(state);
        assertEquals(2, moves.size());
        assertEquals(new MoveToMoon(1), moves.get(0));
        assertEquals(new MoveToMoon(2), moves.get(1));

        state.movePlayerTo(0, 1);
        moves = fm.computeAvailableActions(state);
        assertEquals(2, moves.size());
        assertEquals(new MoveToMoon(0), moves.get(0));
        assertEquals(new MoveToMoon(2), moves.get(1));
    }

    @Test
    public void copyStateFromPerspectiveRemovesMoveInformation() {
        // first we set up some moves
        MoveToMoon moveAction = new MoveToMoon(1);
        fm.next(state, moveAction);
        assertEquals(1, state.getCurrentPlayer());
        moveAction = new MoveToMoon(1);
        fm.next(state, moveAction);
        int[] plannedMoves = state.getMoveSelected();

        SiriusGameState copyFull = (SiriusGameState) state.copy();
        SiriusGameState copy0 = (SiriusGameState) state.copy(0);
        SiriusGameState copy1 = (SiriusGameState) state.copy(1);
        SiriusGameState copy2 = (SiriusGameState) state.copy(2);

        for (int i = 0; i < 3; i++) {
            assertEquals(plannedMoves[i], copyFull.getMoveSelected()[i]);
            assertEquals(i == 0 ? plannedMoves[i] : -1, copy0.getMoveSelected()[i]);
            assertEquals(i == 1 ? plannedMoves[i] : -1, copy1.getMoveSelected()[i]);
            assertEquals(i == 2 ? plannedMoves[i] : -1, copy2.getMoveSelected()[i]);
        }

    }

    @Test
    public void testTakeCardActionsInDrawPhase() {
        state.setGamePhase(Draw);
        state.movePlayerTo(0, 1);
        List<Integer> distinctCardValues = state.getMoon(1).getDeck().stream().mapToInt(c -> c.value).distinct().boxed().collect(toList());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(distinctCardValues.size(), actions.size());
        assertTrue(distinctCardValues.stream().map(TakeCard::new).allMatch(actions::contains));

        state.getMoon(1).addCard(new SiriusCard("TestAmmonia", AMMONIA, 4));
        List<AbstractAction> extendedActions = fm.computeAvailableActions(state);
        assertEquals(distinctCardValues.size() + 1, extendedActions.size());
    }

    @Test
    public void testTakeCardFunctionsAsExpected() {
        TakeCard action = new TakeCard(1);
        state.getMoon(1).addCard(new SiriusCard("ammonia", AMMONIA, 1));
        assertEquals(3, state.getMoon(1).getDeckSize());
        assertEquals(0, state.getPlayerHand(0).getSize());
        state.movePlayerTo(0, 1);
        fm.next(state, action);
        assertEquals(2, state.getMoon(1).getDeckSize());
        assertEquals(1, state.getPlayerHand(0).getSize());
        assertEquals(1, state.getPlayerHand(0).draw().value);
    }

    @Test
    public void testTakeCardLooksAtWholeMoonDeck() {
        TakeCard action = new TakeCard(1);
        state.getMoon(1).addCard(new SiriusCard("ammonia", AMMONIA, 1));
        state.movePlayerTo(0, 1);
        PartialObservableDeck<SiriusCard> deck = state.getMoon(1).getDeck();
        for (int i = 0; i < 3; i++) {
            assertFalse(deck.getVisibilityForPlayer(i, 0));
            assertFalse(deck.getVisibilityForPlayer(i, 1));
            assertFalse(deck.getVisibilityForPlayer(i, 2));
        }
        fm.next(state, action);
        deck = state.getMoon(1).getDeck();
        for (int i = 0; i < 2; i++) {
            assertTrue(deck.getVisibilityForPlayer(i, 0));
            assertFalse(deck.getVisibilityForPlayer(i, 1));
            assertFalse(deck.getVisibilityForPlayer(i, 2));
        }
    }

    @Test
    public void testSellActionsAtSirius() {
        state.setGamePhase(Draw);
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new DoNothing(), fm.computeAvailableActions(state).get(0));

        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 1));
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 2));
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 1));

        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new SellCards(Arrays.asList(
                new SiriusCard("a", AMMONIA, 1),
                new SiriusCard("b", AMMONIA, 1),
                new SiriusCard("a", AMMONIA, 2)
        )), fm.computeAvailableActions(state).get(0));

    }

    @Test
    public void testSellCards() {
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 1));
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 2));
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 1));
        SellCards action = new SellCards(Arrays.asList(
                new SiriusCard("a", AMMONIA, 1),
                new SiriusCard("b", AMMONIA, 2)
        ));
        assertEquals(3, state.getPlayerHand(0).getSize());
        assertEquals(0, state.getGameScore(0), 0.01);

        fm.next(state, action);
        assertEquals(1, state.getPlayerHand(0).getSize());
        assertEquals(2, state.getGameScore(0), 0.01);
    }

    @Test
    public void testMedalGainedOnSale() {
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 1));
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 2));
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 3));
        SellCards action = new SellCards(Arrays.asList(
                new SiriusCard("a", AMMONIA, 1),
                new SiriusCard("b", AMMONIA, 2),
                new SiriusCard("a", AMMONIA, 3)
        ));
        assertEquals(Arrays.toString(new int[]{0, 0, 0, 0, 2, 0, 0, 0, 0, 3}),
                Arrays.toString(Arrays.copyOfRange(state.getMedals(AMMONIA), 1, 11) ));

        fm.next(state, action);
        assertEquals(0, state.getPlayerHand(0).getSize());
        assertEquals(5, state.getGameScore(0), 0.01);
        assertEquals(6, state.getTrackPosition(AMMONIA));
        assertEquals(Arrays.toString(new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 3}),
                Arrays.toString(Arrays.copyOfRange(state.getMedals(AMMONIA), 1, 11) ));
    }

    @Test
    public void testEndGameOnAmmoniaTrack() {
        testMedalGainedOnSale();
        state.addCardToHand(1, new SiriusCard("test", AMMONIA, 19));
        SellCards action = new SellCards(Collections.singletonList(
                new SiriusCard("a", AMMONIA, 19)
        ));
        fm.next(state, action);
        assertEquals(0, state.getPlayerHand(1).getSize());
        assertEquals(1 + 3 + 4 + 5 + 6, state.getGameScore(1), 0.01);
        assertEquals(25, state.getTrackPosition(AMMONIA));
        assertFalse(state.isNotTerminal());
        assertEquals(Utils.GameResult.WIN, state.getPlayerResults()[1]);
    }


}
