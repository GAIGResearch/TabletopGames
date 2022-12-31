package test.games.sirius;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.PartialObservableDeck;
import games.GameType;
import games.sirius.*;
import games.sirius.actions.*;
import org.junit.*;
import players.simple.RandomPlayer;
import scala.concurrent.impl.FutureConvertersImpl;
import utilities.Utils;

import java.util.*;

import static games.sirius.SiriusConstants.SiriusCardType.*;
import static games.sirius.SiriusConstants.SiriusPhase.*;
import static org.junit.Assert.*;

public class TestMoves {

    Game game;
    SiriusGameState state;
    SiriusForwardModel fm = new SiriusForwardModel();
    List<AbstractPlayer> players = new ArrayList<>();
    SiriusParameters params = new SiriusParameters();

    @Before
    public void setup() {
        players = Arrays.asList(
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer()
        );
        game = GameType.Sirius.createGameInstance(3, 34, params);
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
        assertEquals(3, moves.size());
        assertEquals(new MoveToMoon(1), moves.get(0));
        assertEquals(new MoveToMoon(2), moves.get(1));
        assertEquals(new MoveToMoon(3), moves.get(2));

        state.movePlayerTo(0, 1);
        moves = fm.computeAvailableActions(state);
        assertEquals(3, moves.size());
        assertEquals(new MoveToMoon(0), moves.get(0));
        assertEquals(new MoveToMoon(2), moves.get(1));
        assertEquals(new MoveToMoon(3), moves.get(2));
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
    public void testTakeCardActionsInDrawPhaseWithMultiplePlayersPresent() {
        state.setGamePhase(Draw);
        state.addCardToHand(2, new SiriusCard("Favour", FAVOUR, 1));
        state.movePlayerTo(0, 2);
        state.movePlayerTo(1, 2);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, actions.size());
        assertEquals(new TakeCard(), actions.get(0));

        state.getMoon(2).addCard(new SiriusCard("Test", CONTRABAND, 4));
        List<AbstractAction> extendedActions = fm.computeAvailableActions(state);
        assertEquals(1, extendedActions.size());

        // now run through actions
        int count = 0;
        do {
            AbstractAction action = fm.computeAvailableActions(state).get(0);
            fm.next(state, action);
            count++;
        } while (state.getGamePhase() == Draw);
        assertEquals(Favour, state.getGamePhase());
        assertEquals(4, count); // three cards from the moon, plus a DoNothing on Sirius
        // this means that I need to skip turn order in the Draw phase if at a Moon with no cards to be drawn
        assertEquals(1, state.getPlayerHand(1).getSize());
        assertEquals(2, state.getPlayerHand(0).getSize());

    }

    @Test
    public void testTakeAllCardsIfOnlyPlayerPresent() {
        state.setGamePhase(Draw);
        state.movePlayerTo(0, 1);
        state.movePlayerTo(1, 2);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertEquals(new TakeCard(), actions.get(0));

        fm.next(state, actions.get(0)); // p0
        assertEquals(1, state.getPlayerHand(0).getSize());
        assertEquals(0, state.getPlayerHand(1).getSize());

        actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertEquals(new TakeCard(), actions.get(0));

        fm.next(state, actions.get(0)); // p1
        assertEquals(1, state.getPlayerHand(1).getSize());

        // now run through actions
        int count = 0;
        do {
            AbstractAction action = fm.computeAvailableActions(state).get(0);
            fm.next(state, action);
            count++;
        } while (state.getGamePhase() == Draw);
        assertEquals(Move, state.getGamePhase()); // skip Favaour phase as no-one has any cards
        assertEquals(3, count); // one more card from each moon, plus a DoNothing on Sirius
        assertEquals(2, state.getPlayerHand(1).getSize());
        assertEquals(2, state.getPlayerHand(0).getSize());
    }

    @Test
    public void testTakeCardFunctionsAsExpected() {
        TakeCard action = new TakeCard();
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
    public void testTakeCardDoesNotLookAtWholeMoonDeck() {
        state.setGamePhase(Draw);
        state.getMoon(1).addCard(new SiriusCard("ammonia", AMMONIA, 1));
        state.movePlayerTo(1, 1);
        PartialObservableDeck<SiriusCard> deck = state.getMoon(1).getDeck();
        for (int i = 0; i < 3; i++) {
            assertFalse(deck.getVisibilityForPlayer(i, 0));
            assertFalse(deck.getVisibilityForPlayer(i, 1));
            assertFalse(deck.getVisibilityForPlayer(i, 2));
        }
        state.getTurnOrder().endPlayerTurn(state);
        // ending the previous player's turn will ensure that the next player looks at all the cards at their current location
        deck = state.getMoon(1).getDeck();
        for (int i = 0; i < 2; i++) {
            assertFalse(deck.getVisibilityForPlayer(i, 0));
            assertFalse(deck.getVisibilityForPlayer(i, 1));
            assertFalse(deck.getVisibilityForPlayer(i, 2));
        }
    }

    @Test
    public void testSellAmmoniaActionsAtSirius() {
        state.setGamePhase(Draw);
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new DoNothing(), fm.computeAvailableActions(state).get(0));

        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 1));
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 2));
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 1));
        // options should be (2, 1, 1), (2, 1), (2), (1, 1), (1), ()
        // with the (2) dominated by (1, 1) for a total of 5
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(5, actions.size() );
        assertTrue(actions.contains(new SellCards(Arrays.asList(
                new SiriusCard("test", AMMONIA, 1),
                new SiriusCard("test", AMMONIA, 1),
                new SiriusCard("test", AMMONIA, 2)))));
        assertTrue(actions.contains(new SellCards(Arrays.asList(
                new SiriusCard("test", AMMONIA, 1),
                new SiriusCard("test", AMMONIA, 2)))));
        assertTrue(actions.contains(new SellCards(Arrays.asList(
                new SiriusCard("test", AMMONIA, 1),
                new SiriusCard("test", AMMONIA, 1)))));
        assertTrue(actions.contains(new SellCards(Collections.singletonList(
                new SiriusCard("test", AMMONIA, 1)))));
        assertTrue(actions.contains(new DoNothing()));
    }

    @Test
    public void testSellContrabandActionsAtSirius() {
        state.setGamePhase(Draw);

        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 1));
        state.addCardToHand(0, new SiriusCard("test", CONTRABAND, 3));
        state.addCardToHand(0, new SiriusCard("test", CONTRABAND, 1));
        state.addCardToHand(0, new SiriusCard("test", CONTRABAND, 0));
        state.addCardToHand(0, new SiriusCard("test", CONTRABAND, 0));
        // options should be :
        // Nothing
        // 1 Ammonia
        // 1 Contraband
        // 3 Contraband
        // (1,3) Contraband

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(5, actions.size() );
        assertTrue(actions.contains(new SellCards(Arrays.asList(
                new SiriusCard("test", CONTRABAND, 1),
                new SiriusCard("test", CONTRABAND, 3)))));
        assertTrue(actions.contains(new SellCards(Collections.singletonList(
                new SiriusCard("test", CONTRABAND, 1)))));
        assertTrue(actions.contains(new SellCards(Collections.singletonList(
                new SiriusCard("test", CONTRABAND, 3)))));
        assertTrue(actions.contains(new SellCards(Collections.singletonList(
                new SiriusCard("test", AMMONIA, 1)))));
        assertTrue(actions.contains(new DoNothing()));
    }

    @Test
    public void testSellGlowingContrabandActionsAtSirius() {
        state.setGamePhase(Draw);

        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 1));
        state.addCardToHand(0, new SiriusCard("test", CONTRABAND, 3));
        state.addCardToHand(0, new SiriusCard("test", CONTRABAND, 1));
        state.addCardToHand(0, new SiriusCard("test", CONTRABAND, 0));
        state.addCardToHand(0, new SiriusCard("test", CONTRABAND, 0));
        state.addCardToHand(0, new SiriusCard("test", CONTRABAND, 0));
        state.addCardToHand(0, new SiriusCard("test", CONTRABAND, 0));
        // options should be :
        // Nothing
        // 1 Ammonia
        // 1 Contraband
        // 3 Contraband
        // (1,3) Contraband
        // 3x Glowing
        // 1 + 3xg
        // 3 + 3xg
        // (1, 3, 3xG)
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(9, actions.size() );
        assertTrue(actions.contains(new SellCards(Arrays.asList(
                new SiriusCard("test", CONTRABAND, 0),
                new SiriusCard("test", CONTRABAND, 0),
                new SiriusCard("test", CONTRABAND, 0)))));
        assertTrue(actions.contains(new SellCards(Arrays.asList(
                new SiriusCard("test", CONTRABAND, 0),
                new SiriusCard("test", CONTRABAND, 0),
                new SiriusCard("test", CONTRABAND, 1),
                new SiriusCard("test", CONTRABAND, 0)))));
        assertTrue(actions.contains(new SellCards(Arrays.asList(
                new SiriusCard("test", CONTRABAND, 0),
                new SiriusCard("test", CONTRABAND, 0),
                new SiriusCard("test", CONTRABAND, 3),
                new SiriusCard("test", CONTRABAND, 0)))));
        assertTrue(actions.contains(new SellCards(Arrays.asList(
                new SiriusCard("test", CONTRABAND, 0),
                new SiriusCard("test", CONTRABAND, 0),
                new SiriusCard("test", CONTRABAND, 1),
                new SiriusCard("test", CONTRABAND, 3),
                new SiriusCard("test", CONTRABAND, 0)))));
        assertTrue(actions.contains(new SellCards(Arrays.asList(
                new SiriusCard("test", CONTRABAND, 1),
                new SiriusCard("test", CONTRABAND, 3)))));
        assertTrue(actions.contains(new SellCards(Collections.singletonList(
                new SiriusCard("test", CONTRABAND, 1)))));
        assertTrue(actions.contains(new SellCards(Collections.singletonList(
                new SiriusCard("test", CONTRABAND, 3)))));
        assertTrue(actions.contains(new SellCards(Collections.singletonList(
                new SiriusCard("test", AMMONIA, 1)))));
        assertTrue(actions.contains(new DoNothing()));

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
        state.setGamePhase(Draw);
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 1));
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 2));
        state.addCardToHand(0, new SiriusCard("test", AMMONIA, 3));
        SellCards action = new SellCards(Arrays.asList(
                new SiriusCard("a", AMMONIA, 1),
                new SiriusCard("b", AMMONIA, 2),
                new SiriusCard("c", AMMONIA, 3)
        ));
        assertEquals(0, state.getMedalsTaken());
        assertEquals(0, state.getPlayerArea(0).getMedals().size());

        fm.next(state, action);
        assertEquals(0, state.getPlayerHand(0).getSize());
        assertEquals(6, state.getTrackPosition(AMMONIA));
        assertEquals(5, state.getGameScore(0), 0.01);
        assertEquals(1, state.getMedalsTaken());
        assertEquals(1, state.getPlayerArea(0).getMedals().size());
        assertEquals(new Medal(AMMONIA, 2), state.getPlayerArea(0).getMedals().get(0));
    }

    @Test
    public void testEndGameOnAmmoniaTrack() {
        testMedalGainedOnSale();
        assertEquals(1, state.getCurrentPlayer());
        state.addCardToHand(1, new SiriusCard("test", AMMONIA, 19));
        SellCards action = new SellCards(Collections.singletonList(
                new SiriusCard("a", AMMONIA, 19)
        ));
        fm.next(state, action);
        assertEquals(0, state.getPlayerHand(1).getSize());
        assertEquals(1 + 2 + 3 + 3 + 4, state.getGameScore(1), 0.01);
        assertEquals(25, state.getTrackPosition(AMMONIA));
        assertFalse(state.isNotTerminal());
        assertEquals(Utils.GameResult.WIN, state.getPlayerResults()[1]);
    }

    @Test
    public void testEndGameOnContrabandTrack() {
        state.setGamePhase(Draw);
        assertEquals(0, state.getCurrentPlayer());
        state.addCardToHand(0, new SiriusCard("test", CONTRABAND, params.contrabandTrack.length));
        AbstractAction action = new SellCards(Collections.singletonList(
                new SiriusCard("a", CONTRABAND, params.contrabandTrack.length)
        ));
        fm.next(state, action);
        assertEquals(0, state.getPlayerHand(0).getSize());
        assertEquals(params.contrabandTrack.length, state.getTrackPosition(CONTRABAND));
        assertEquals(1 + 2 + 2 + 3 + 3 + 4, state.getGameScore(0), 0.01);
        assertFalse(state.isNotTerminal());
        assertEquals(Utils.GameResult.WIN, state.getPlayerResults()[0]);
        assertEquals(Utils.GameResult.LOSE, state.getPlayerResults()[1]);
        assertEquals(Utils.GameResult.LOSE, state.getPlayerResults()[2]);
    }

    @Test
    public void testTakeCardOnlyPossibleIfCardsAvailable() {
        state.setGamePhase(Draw);
        state.movePlayerTo(0, 1);
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new TakeCard(), fm.computeAvailableActions(state).get(0));

        do {
            state.getMoon(1).drawCard();
        } while (state.getMoon(1).getDeck().getSize() > 0);

        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new DoNothing(), fm.computeAvailableActions(state).get(0));
    }


}
