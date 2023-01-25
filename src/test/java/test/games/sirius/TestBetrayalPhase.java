package test.games.sirius;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.sirius.*;
import games.sirius.actions.SellCards;
import org.junit.*;
import players.simple.RandomPlayer;

import java.util.*;

import static games.sirius.SiriusConstants.MoonType.*;
import static games.sirius.SiriusConstants.SiriusCardType.*;
import static games.sirius.SiriusConstants.SiriusPhase.Draw;
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
    public void movingCorruptionTrackToMarkerTriggersPolicePhase() {
        fail("Not yet implemented");
    }

    @Test
    public void sellingSmugglerCardsMovesCorruptionTrack() {
        fail("Not yet implemented");
    }

    @Test
    public void corruptionTrackEndsGame() {
        fail("Not yet implemented");

    }

    @Test
    public void policePhaseActions() {
        fail("Not yet implemented");

    }

}
