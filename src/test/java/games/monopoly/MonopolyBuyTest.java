package games.monopoly;

import games.monopoly.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.monopoly.MonopolyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Landing on an unowned property: BuyDecision, BuyProperty and DeclineProperty.
 */
public class MonopolyBuyTest {

    MonopolyGameState state;
    MonopolyForwardModel fm;
    final MonopolySquare angel = sq("The Angel Islington");

    @Before
    public void setup() {
        state = newState(3, 7, null);
        fm = new MonopolyForwardModel();
        startTurn(state, 1, sq("GO"));
    }

    @Test
    public void landingOnAnUnownedPropertyOffersTheLanderBuyOrDecline() {
        roll(state, fm, 2, 4);
        // GO + 6 = The Angel Islington, price 100
        assertTrue(state.isActionInProgress());
        assertEquals(new BuyDecision(1, angel), state.currentActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new BuyProperty(angel), new DeclineProperty(angel)), actionSet(fm, state));
    }

    @Test
    public void buyingTakesThePriceAndTheTitleAndTheTurnGoesOnToManage() {
        roll(state, fm, 2, 4);
        fm.next(state, new BuyProperty(angel));
        assertEquals(1, state.getOwner(angel));
        assertEquals(1500 - 100 /* The Angel Islington */, state.getCash(1));
        assertFalse(state.isActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        // the new property may be mortgaged in MANAGE
        assertEquals(Set.of(new EndTurn(), new Mortgage(angel)), actionSet(fm, state));
    }

    @Test
    public void afterBuyingOnADoubleManageOffersAnotherRoll() {
        state.setPosition(1, sq("Liverpool Street Station"));
        roll(state, fm, 1, 1);
        // 35 + 2 = Park Lane, price 350
        assertEquals(sq("Park Lane"), state.getPosition(1));
        fm.next(state, new BuyProperty(sq("Park Lane")));
        assertEquals(1500 - 350, state.getCash(1));
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new RollDice(), new Mortgage(sq("Park Lane"))), actionSet(fm, state));
    }

    @Test
    public void aPlayerCanBuyOnlyWithThePriceInCash() {
        state.setCash(1, 100 - 1);
        roll(state, fm, 2, 4);
        assertEquals(Set.of(new DeclineProperty(angel)), actionSet(fm, state));
    }

    @Test
    public void aPlayerWithExactlyThePriceCanBuy() {
        state.setCash(1, 100);
        roll(state, fm, 2, 4);
        assertEquals(Set.of(new BuyProperty(angel), new DeclineProperty(angel)), actionSet(fm, state));
        fm.next(state, new BuyProperty(angel));
        assertEquals(0, state.getCash(1));
        assertEquals(1, state.getOwner(angel));
    }

    @Test
    public void aDeclinedPropertyStaysWithTheBank() {
        roll(state, fm, 2, 4);
        decline(state, fm, angel);
        assertEquals(-1, state.getOwner(angel));
        assertEquals(1500, state.getCash(1));
        for (int p = 0; p < 3; p++)
            assertTrue(state.getProperties(p).isEmpty());
    }

    @Test
    public void landingOnOwnPropertyDoesNothing() {
        give(state, 1, angel);
        roll(state, fm, 2, 4);
        assertFalse(state.isActionInProgress());
        assertEquals(1500, state.getCash(1));
        assertEquals(MonopolyGamePhase.MANAGE, state.getGamePhase());
        assertEquals(Set.of(new EndTurn(), new Mortgage(angel)), actionSet(fm, state));
    }

    @Test
    public void stationsAndUtilitiesCanBeBought() {
        // GO + 5 = King's Cross Station (200)
        roll(state, fm, 1, 4);
        fm.next(state, new BuyProperty(sq("King's Cross Station")));
        assertEquals(1, state.getOwner(sq("King's Cross Station")));
        fm.next(state, new EndTurn());

        // player 2 from Jail (Just Visiting) + 2 = Electric Company (150)
        assertEquals(2, state.getCurrentPlayer());
        state.setPosition(2, sq("Jail"));
        roll(state, fm, 1, 1);
        assertEquals(Set.of(new BuyProperty(sq("Electric Company")), new DeclineProperty(sq("Electric Company"))),
                actionSet(fm, state));
        fm.next(state, new BuyProperty(sq("Electric Company")));
        assertEquals(2, state.getOwner(sq("Electric Company")));
        assertEquals(1500 - 150, state.getCash(2));
        assertEquals(1500 - 200, state.getCash(1));
    }
}
