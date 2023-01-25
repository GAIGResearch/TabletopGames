package test.games.sirius;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import games.sirius.*;
import games.sirius.actions.MoveToMoon;
import games.sirius.actions.TakeCard;
import org.apache.log4j.builders.appender.DailyRollingFileAppenderBuilder;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;

import static games.sirius.SiriusConstants.MoonType.*;
import static games.sirius.SiriusConstants.SiriusPhase.Draw;
import static org.junit.Assert.*;

public class TestTurnOrder {

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
    public void testCardsOnMiningMoons() {
        int base = params.cardsPerEmptyMoon;
        int increment = params.cardsPerNonEmptyMoon;
        // N at start
        for (Moon moon : state.getAllMoons()) {
            if (moon.getMoonType() == MINING)
                assertEquals(base, moon.getDeckSize());
        }
        // at endRound we expect more to be added
        sto.endRound(state);
        for (Moon moon : state.getAllMoons()) {
            if (moon.getMoonType() == MINING)
                assertEquals(base + increment, moon.getDeckSize());
        }
    }
    @Test
    public void testCardsOnEmptyMiningMoons() {
        int base = params.cardsPerEmptyMoon;
        // N at start
        for (Moon moon : state.getAllMoons()) {
            if (moon.getMoonType() == MINING) {
                assertEquals(base, moon.getDeckSize());
                for (int i = 0; i < base; i++)
                    moon.drawCard();
            }
        }
        // at endRound we expect more to be added
        sto.endRound(state);
        for (Moon moon : state.getAllMoons()) {
            if (moon.getMoonType() == MINING)
                assertEquals(base, moon.getDeckSize());
        }
    }

    @Test
    public void testCardsOnProcessingMoons() {
        int base = params.cardsPerEmptyMoon;
        int increment = params.cardsPerNonEmptyMoon;
        // N at start
        for (Moon moon : state.getAllMoons()) {
            if (moon.getMoonType() == PROCESSING)
                assertEquals(base, moon.getDeckSize());
        }
        // at endRound we expect more to be added
        sto.endRound(state);
        for (Moon moon : state.getAllMoons()) {
            if (moon.getMoonType() == PROCESSING)
                assertEquals(base + increment, moon.getDeckSize());
        }
    }
    @Test
    public void testCardsOnEmptyProcessingMoons() {
        int base = params.cardsPerEmptyMoon;
        // N at start
        for (Moon moon : state.getAllMoons()) {
            if (moon.getMoonType() == PROCESSING) {
                assertEquals(base, moon.getDeckSize());
                for (int i = 0; i < base; i++)
                    moon.drawCard();
            }
        }
        // at endRound we expect more to be added
        sto.endRound(state);
        for (Moon moon : state.getAllMoons()) {
            if (moon.getMoonType() == PROCESSING)
                assertEquals(base, moon.getDeckSize());
        }
    }

    @Test
    public void testNextPlayerAndSkipFavourPhase() {
        assertEquals(0, sto.getRoundCounter());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, sto.nextPlayerAndPhase(state).a.intValue());
        fm.next(state, new MoveToMoon(1));
        assertEquals(2, sto.nextPlayerAndPhase(state).a.intValue());
        fm.next(state, new MoveToMoon(1));
        assertEquals(0, sto.nextPlayerAndPhase(state).a.intValue());
        assertEquals(SiriusConstants.SiriusPhase.Move, state.getGamePhase());
        fm.next(state, new MoveToMoon(1));

        // All at the Mining moon - so first two can take cards, and the third does not get an action
        assertEquals(Draw, state.getGamePhase());
        assertEquals(1, sto.nextPlayerAndPhase(state).a.intValue());
        assertEquals(Draw, sto.nextPlayerAndPhase(state).b);
        fm.next(state, fm.computeAvailableActions(state).get(0)); // p0 Take Card
        assertEquals(2, sto.nextPlayerAndPhase(state).a.intValue());
        assertEquals(Draw, sto.nextPlayerAndPhase(state).b);
        fm.next(state, fm.computeAvailableActions(state).get(0)); // p1 Take Card

        // Round 2
        // which changes things...and we go straight to the Move phase
        // we skip the Favour phase, because no-one has any Favour cards
        assertEquals(1, sto.getRoundCounter());
        assertEquals(1, sto.getPlayerAtRank(1));  // we have moved on a round, and changed the ranking

        assertEquals(SiriusConstants.SiriusPhase.Move, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new MoveToMoon(4)); // p0 Move to Metropolis
        assertEquals(1, state.getCurrentPlayer());

        assertEquals(1, sto.getTurnOwner());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(2, sto.nextPlayerAndPhase(state).a.intValue());
        fm.next(state, new MoveToMoon(0));  // p1 Move to Sirius
        assertEquals(2, sto.getTurnOwner());
        assertEquals(1, sto.nextPlayerAndPhase(state).a.intValue());
        assertEquals(Draw, sto.nextPlayerAndPhase(state).b);
        fm.next(state, new MoveToMoon(2)); // p2 Move to Processing

        assertEquals(Draw, state.getGamePhase());
        assertEquals(1, sto.getTurnOwner());
        assertEquals(2, sto.nextPlayerAndPhase(state).a.intValue());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // p1 Sells stuff (or not)
        assertEquals(2, sto.getTurnOwner());
        assertEquals(0, sto.nextPlayerAndPhase(state).a.intValue());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // p2 Takes Card (one of 3)
        assertEquals(0, sto.getTurnOwner());
        assertEquals(2, sto.nextPlayerAndPhase(state).a.intValue()); // next is p2 as p1 has done stuff
        fm.next(state, fm.computeAvailableActions(state).get(0)); // p0 takes Favour card

        assertEquals(2, sto.getTurnOwner());
        assertEquals(2, sto.nextPlayerAndPhase(state).a.intValue()); // P2 now takes the final cards, one at a time
        fm.next(state, fm.computeAvailableActions(state).get(0)); // p2 takes card
        assertEquals(2, sto.getTurnOwner());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // p2 takes card

        // So now we should move into Favour Phase after drawing

        assertEquals(SiriusConstants.SiriusPhase.Favour, state.getGamePhase());
        assertEquals(0, sto.getTurnOwner()); // is the only player with a Favour card
        assertEquals(0, sto.nextPlayerAndPhase(state).a.intValue()); // and will then be the first to Move
        fm.next(state, fm.computeAvailableActions(state).get(0));

        assertEquals(0, sto.getTurnOwner()); // is the only player with a Favour card
        assertEquals(SiriusConstants.SiriusPhase.Move, state.getGamePhase());
        assertEquals(2, state.getTurnOrder().getRoundCounter());
    }

    @Test
    public void testRankCalculation() {
        assertEquals(1, sto.getRank(0));
        assertEquals(2, sto.getRank(1));
        assertEquals(3, sto.getRank(2));
        assertEquals(0, sto.getPlayerAtRank(1));
        assertEquals(1, sto.getPlayerAtRank(2));
        assertEquals(2, sto.getPlayerAtRank(3));

        sto.setRank(0, 3);
        sto.updatePlayerOrder();
        assertEquals(3, sto.getRank(0));
        assertEquals(1, sto.getRank(1));
        assertEquals(2, sto.getRank(2));
        assertEquals(1, sto.getPlayerAtRank(1));
        assertEquals(2, sto.getPlayerAtRank(2));
        assertEquals(0, sto.getPlayerAtRank(3));
    }

}
