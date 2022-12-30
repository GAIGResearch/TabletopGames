package test.games.sirius;

import core.AbstractPlayer;
import core.Game;
import core.actions.*;
import core.components.PartialObservableDeck;
import games.GameType;
import games.sirius.*;
import games.sirius.actions.*;
import org.junit.*;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.util.*;

import static games.sirius.SiriusConstants.SiriusCardType.*;
import static games.sirius.SiriusConstants.SiriusPhase.*;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;


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
        state.setGamePhase(Favour);
        for (int p = 0; p < 3; p++) {
            assertEquals(p, state.getCurrentPlayer());
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            if (p == 1) {
                assertEquals(6, actions.size());
                assertEquals(new PassOnFavour(), actions.get(0));
                assertEquals(new FavourForRank(1), actions.get(1));
                assertEquals(new FavourForRank(3), actions.get(2));
                assertEquals(new FavourForCartel(1), actions.get(3));
                assertEquals(new FavourForCartel(2), actions.get(4));
                assertEquals(new FavourForCartel(3), actions.get(5));
            } else {
                assertEquals(1, actions.size());
                assertEquals(new PassOnFavour(), actions.get(0));
            }
            fm.next(state, new PassOnFavour());
        }
    }

    @Test
    public void testCartelInhibitsFurtherCartel() {
        state.addCardToHand(0, new SiriusCard("Favour", FAVOUR, 1));
        state.getMoon(1).setCartelOwner(0);
        state.getMoon(2).setCartelOwner(1);
        state.setGamePhase(Favour);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(5, actions.size());
        assertEquals(new PassOnFavour(), actions.get(0));
        assertEquals(new FavourForRank(2), actions.get(1));
        assertEquals(new FavourForRank(3), actions.get(2));
        assertEquals(new FavourForCartel(2), actions.get(3));
        assertEquals(new FavourForCartel(3), actions.get(4));
    }


    @Test
    public void testChangeRankWithFavour() {
        // we should change rank (but not the nextPlayer)
        // and the card should vanish
        state.addCardToHand(2, new SiriusCard("Favour", FAVOUR, 1));
        state.setGamePhase(Favour);
        fm.next(state, new PassOnFavour());
        fm.next(state, new PassOnFavour());
        FavourForRank action = new FavourForRank(1);
        assertEquals(1, state.getPlayerHand(2).getSize());
        assertEquals(3, sto.getRank(2));
        assertEquals(0, sto.nextPlayerAndPhase(state).a.intValue());
        fm.next(state, action);
        assertEquals(0, state.getPlayerHand(2).getSize());
        assertEquals(1, sto.getRank(2));
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void testChangeCartelWithFavour() {
        state.setGamePhase(Favour);
        state.addCardToHand(0, new SiriusCard("Favour", FAVOUR, 1));
        FavourForCartel action = new FavourForCartel(1);
        assertEquals(-1, state.getMoon(1).getCartelOwner());
        fm.next(state, action);
        assertEquals(0, state.getMoon(1).getCartelOwner());
        assertEquals(0, state.getPlayerHand(0).getSize());
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
