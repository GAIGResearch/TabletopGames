package test.games.sirius;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.sirius.*;
import games.sirius.actions.*;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;

import static games.sirius.SiriusConstants.SiriusCardType.*;
import static games.sirius.SiriusConstants.SiriusPhase.*;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;


public class TestFavours {


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
    public void testFavourCardDrawnAtMetropolis() {
        state.setGamePhase(Draw);
        state.movePlayerTo(0, 3);
        SiriusCard topCard = state.getMoon(3).getDeck().peek();
        assertEquals(FAVOUR, topCard.cardType);
        assertEquals(1, topCard.value);
        TakeCard action = new TakeCard();
        fm.next(state, action);
        assertEquals(1, state.getPlayerHand(0).getSize());
        assertEquals(topCard, state.getPlayerHand(0).get(0));
    }

    @Test
    public void testComputeAvailableFavourActions() {
        state.addCardToHand(1, new SiriusCard("Favour", FAVOUR, 1));
        moveToPhase(Favour);
        assertEquals(1, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(6, actions.size());
        assertEquals(new PassOnFavour(), actions.get(0));
        assertEquals(new FavourForRank(2), actions.get(1));
        assertEquals(new FavourForRank(3), actions.get(2));
        assertEquals(new FavourForCartel(1), actions.get(3));
        assertEquals(new FavourForCartel(2), actions.get(4));
        assertEquals(new FavourForCartel(3), actions.get(5));
        fm.next(state, new PassOnFavour());
    }

    private void moveToPhase(SiriusConstants.SiriusPhase phase) {
        // players will always move to MINING and take AMMONIA cards
        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.getGamePhase() != phase);
    }

    @Test
    public void testCartelInhibitsFurtherCartel() {
        state.addCardToHand(0, new SiriusCard("Favour", FAVOUR, 1));
        state.getMoon(1).setCartelOwner(0);
        state.getMoon(2).setCartelOwner(1);
        moveToPhase(Favour);
        assertEquals(0, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(5, actions.size());
        assertEquals(new PassOnFavour(), actions.get(0));
        assertEquals(new FavourForRank(1), actions.get(1));
        assertEquals(new FavourForRank(2), actions.get(2));
        assertEquals(new FavourForCartel(2), actions.get(3));
        assertEquals(new FavourForCartel(3), actions.get(4));
    }

    @Test
    public void pointsForCartels() {
        assertEquals(0.0, state.getGameScore(2), 0.01);
        state.getMoon(2).setCartelOwner(2);
        assertEquals(1.0 * params.pointsPerCartel, state.getGameScore(2), 0.01);
        state.getMoon(1).setCartelOwner(2);
        assertEquals(2.0 * params.pointsPerCartel, state.getGameScore(2), 0.01);
        state.getMoon(1).setCartelOwner(0);
        assertEquals(1.0 * params.pointsPerCartel, state.getGameScore(2), 0.01);
        assertEquals(1.0 * params.pointsPerCartel, state.getGameScore(0), 0.01);
    }


    @Test
    public void testChangeRankWithFavour() {
        // we should change rank (but not the nextPlayer)
        // and the card should vanish
        state.addCardToHand(2, new SiriusCard("Favour", FAVOUR, 1));
        state.addCardToHand(0, new SiriusCard("Favour", FAVOUR, 1));
        moveToPhase(Favour);
        // we have now shifted rank
        assertEquals(1, sto.getRank(1));
        assertEquals(2, sto.getRank(2));
        assertEquals(3, sto.getRank(0));
        assertEquals(2, state.getCurrentPlayer());
        FavourForRank action = new FavourForRank(1);
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(1, state.getPlayerHand(2).getSize());
        assertEquals(0, sto.nextPlayerAndPhase(state).a.intValue());
        fm.next(state, action);
        assertEquals(0, state.getPlayerHand(2).getSize());
        assertEquals(2, sto.getRank(1));
        assertEquals(1, sto.getRank(2));
        assertEquals(3, sto.getRank(0));
        assertEquals(0, state.getCurrentPlayer());

        action = new FavourForRank(2);
        fm.next(state, action);
        assertEquals(3, sto.getRank(1));
        assertEquals(1, sto.getRank(2));
        assertEquals(2, sto.getRank(0));
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void testNotAbleToUseTwoFavourCardsByMovingToLastPlace() {
        // we should change rank (but not the nextPlayer)
        // and the card should vanish
        state.addCardToHand(2, new SiriusCard("Favour", FAVOUR, 1));
        state.addCardToHand(2, new SiriusCard("Favour", FAVOUR, 1));
        state.addCardToHand(0, new SiriusCard("Favour", FAVOUR, 1));
        moveToPhase(Favour);
        // we have now shifted rank : p1, p2, p0
        assertEquals(2, state.getCurrentPlayer());
        FavourForRank action = new FavourForRank(3);
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, action);
        assertEquals(1, sto.getRank(1));
        assertEquals(3, sto.getRank(2));
        assertEquals(2, sto.getRank(0));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new PassOnFavour());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Move, state.getGamePhase());
        assertEquals(1, sto.getRank(1));
        assertEquals(3, sto.getRank(2));
        assertEquals(2, sto.getRank(0));
    }

    @Test
    public void testChangeCartelWithFavour() {
        state.addCardToHand(0, new SiriusCard("Favour", FAVOUR, 1));
        moveToPhase(Favour);
        FavourForCartel action = new FavourForCartel(1);
        assertEquals(-1, state.getMoon(1).getCartelOwner());
        assertEquals(1, state.getPlayerHand(0).stream().filter(c -> c.cardType == FAVOUR).count());
        fm.next(state, action);
        assertEquals(0, state.getMoon(1).getCartelOwner());
        assertTrue(state.getPlayerHand(0).stream().noneMatch(c -> c.cardType == FAVOUR));
    }

    @Test
    public void testCartelGivesExtraCardAtEndRound() {
        state.getMoon(1).setCartelOwner(0);
        state.getMoon(2).setCartelOwner(1);
        state.getMoon(3).setCartelOwner(0);
        state.getTurnOrder().endRound(state);
        assertEquals(2, state.getPlayerHand(0).getSize());
        assertEquals(1, state.getPlayerHand(1).getSize());
        assertEquals(0, state.getPlayerHand(2).getSize());
        assertEquals(1, state.getPlayerHand(0).stream().filter(c -> c.cardType == AMMONIA).count());
        assertEquals(1, state.getPlayerHand(0).stream().filter(c -> c.cardType == FAVOUR).count());
        assertEquals(1, state.getPlayerHand(1).stream().filter(c -> c.cardType == CONTRABAND).count());
    }
}
