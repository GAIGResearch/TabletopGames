package test.games.sirius;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.dicemonastery.actions.Sell;
import games.sirius.*;
import games.sirius.actions.SellCards;
import org.junit.*;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.*;

import static core.CoreConstants.GameEvents.GAME_OVER;
import static games.sirius.SiriusConstants.MoonType.*;
import static games.sirius.SiriusConstants.SiriusCardType.*;
import static games.sirius.SiriusConstants.SiriusPhase.*;
import static org.junit.Assert.*;


public class TestBetrayalPhase {


    Game game;
    SiriusGameState state;
    SiriusForwardModel fm = new SiriusForwardModel();
    SiriusTurnOrder sto;
    SiriusParameters params;
    List<AbstractPlayer> players = new ArrayList<>();

    @Before
    public void setup() {
        players = Arrays.asList(new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer());
        game = GameType.Sirius.createGameInstance(3, 34, new SiriusParameters());
        game.reset(players);
        state = (SiriusGameState) game.getGameState();
        params = (SiriusParameters) state.getGameParameters();
        sto = (SiriusTurnOrder) state.getTurnOrder();
    }

    @Test
    public void smugglerCardsGiveOptionToSellThemInTwoDirections() {
        state.setGamePhase(Draw);
        assertEquals(TRADING, state.getMoon(0).getMoonType());
        assertEquals(OUTPOST, state.getMoon(3).getMoonType());
        state.movePlayerTo(0, 0);
        SiriusCard card1 =  new SiriusCard("Smuggler1", SMUGGLER, 1);
        SiriusCard card2 =  new SiriusCard("Smuggler2", SMUGGLER, 1);
        state.addCardToHand(0, card1);
        state.addCardToHand(0, card2);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(5, actions.size());
        assertTrue(actions.contains(new SellCards(Collections.singletonList(card1))));
        assertTrue(actions.contains(new SellCards(Arrays.asList(card1, card2))));
        assertTrue(actions.contains(new SellCards(Collections.singletonList(card1), true)));
        assertTrue(actions.contains(new SellCards(Arrays.asList(card1, card2), true)));
    }

    @Test
    public void afterSellingOneCanStillBetray() {
        state.setGamePhase(Draw);
        state.movePlayerTo(0, 0);
        SiriusCard card1 =  new SiriusCard("Smuggler1", SMUGGLER, 1);
        SiriusCard card2 =  new SiriusCard("Ammonia1", AMMONIA, 1);
        state.addCardToHand(0, card1);
        state.addCardToHand(0, card2);
        fm.next(state, new SellCards(Collections.singletonList(card2)));
        while (!(state.getCurrentPlayer() == 0)) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(Draw, state.getGamePhase());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size());
        assertTrue(actions.contains(new SellCards(Collections.singletonList(card1))));
        assertTrue(actions.contains(new SellCards(Collections.singletonList(card1), true)));
    }

    @Test
    public void afterBetrayingOneCanStillSell() {
        state.setGamePhase(Draw);
        state.movePlayerTo(0, 0);
        SiriusCard card1 =  new SiriusCard("Smuggler1", SMUGGLER, 1);
        SiriusCard card2 =  new SiriusCard("Ammonia1", AMMONIA, 1);
        state.addCardToHand(0, card1);
        state.addCardToHand(0, card2);
        fm.next(state, new SellCards(Collections.singletonList(card1)));
        while (!(state.getCurrentPlayer() == 0)) {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        }
        assertEquals(Draw, state.getGamePhase());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertTrue(actions.contains(new SellCards(Collections.singletonList(card2))));
    }


    @Test
    public void sellingSmugglerCardsMovesCorruptionTrackUp() {
        state.setGamePhase(Draw);
        state.movePlayerTo(1, 1);
        state.movePlayerTo(2, 4);
        for (int i = 0; i < 2; i++)
            state.addCardToHand(0, state.getDeck(SMUGGLER).draw());
        assertEquals(params.startingCorruption, state.getTrackPosition(SMUGGLER));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        AbstractAction sellOne = actions.stream().
                filter(a -> a instanceof SellCards &&
                        ((SellCards) a).salesType == SMUGGLER &&
                        ((SellCards) a).getTotalValue() == 1).findFirst().orElseThrow(() -> new AssertionError("No matching action"));
        fm.next(state, sellOne);
        assertEquals(params.startingCorruption + 1, state.getTrackPosition(SMUGGLER));
        assertEquals(Draw, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void sellingSmugglerCardsMovesCorruptionTrackDown() {
        state.setGamePhase(Draw);
        state.movePlayerTo(1, 1);
        state.movePlayerTo(2, 4);
        for (int i = 0; i < 2; i++)
            state.addCardToHand(0, state.getDeck(SMUGGLER).draw());
        assertEquals(params.startingCorruption, state.getTrackPosition(SMUGGLER));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        AbstractAction sellOne = actions.stream().
                filter(a -> a instanceof SellCards &&
                        ((SellCards) a).salesType == SMUGGLER &&
                        ((SellCards) a).getTotalValue() == -1).findFirst().orElseThrow(() -> new AssertionError("No matching action"));
        fm.next(state, sellOne);
        assertEquals(params.startingCorruption - 1, state.getTrackPosition(SMUGGLER));
        assertEquals(Draw, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void corruptionTrackHasCeilingAtMaximum() {
        state.setGamePhase(Draw);
        state.movePlayerTo(1, 1);
        state.movePlayerTo(2, 4);
        for (int i = 0; i < 4; i++)
            state.addCardToHand(0, state.getDeck(SMUGGLER).draw());
        assertEquals(params.startingCorruption, state.getTrackPosition(SMUGGLER));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        AbstractAction sellFour = actions.stream().
                filter(a -> a instanceof SellCards &&
                        ((SellCards) a).salesType == SMUGGLER &&
                        ((SellCards) a).getTotalValue() == 4).findFirst().orElseThrow(() -> new AssertionError("No matching action"));
        fm.next(state, sellFour);
        assertEquals(params.corruptionTrack.length - 1, state.getTrackPosition(SMUGGLER));
        assertEquals(Draw, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }


    @Test
    public void movingCorruptionTrackToMarkerTriggersPolicePhase() {
        state.setGamePhase(Draw);
        state.movePlayerTo(1, 1);
        state.movePlayerTo(2, 4);
        for (int i = 0; i < 4; i++)
            state.addCardToHand(0, state.getDeck(SMUGGLER).draw());
        assertEquals(14, state.getTrackPosition(SMUGGLER));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        AbstractAction sellTwo = actions.stream().
                filter(a -> a instanceof SellCards &&
                        ((SellCards) a).salesType == SMUGGLER &&
                        ((SellCards) a).getTotalValue() == -2).findFirst().orElseThrow(() -> new AssertionError("No matching action"));
        fm.next(state, sellTwo);
        assertEquals(12, state.getTrackPosition(SMUGGLER));
        assertEquals(Police, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void corruptionTrackEndsGame() {
        state.setGamePhase(Draw);
        state.movePlayerTo(1, 1);
        state.movePlayerTo(2, 4);
        SiriusCard superSmuggler = new SiriusCard("SuperSmuggler", SMUGGLER, params.startingCorruption);
        state.addCardToHand(0, superSmuggler);
        SellCards endGameAction = new SellCards(Collections.singletonList(superSmuggler), true);
        assertEquals(params.startingCorruption, state.getTrackPosition(SMUGGLER));
        fm.next(state, endGameAction);
        assertEquals(0, state.getTrackPosition(SMUGGLER));
        assertFalse(state.isNotTerminal());
        assertEquals(Utils.GameResult.WIN, state.getPlayerResults()[0]);
    }

    @Test
    public void policePhaseActions() {
        fail("Not yet implemented");
    }

}
