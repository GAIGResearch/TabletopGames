package games.dicemonastery.test;

import games.dicemonastery.*;
import org.junit.Test;
import players.simple.RandomPlayer;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;
import static games.dicemonastery.Pilgrimage.DESTINATION.JERUSALEM;
import static games.dicemonastery.Pilgrimage.DESTINATION.ROME;
import static org.junit.Assert.*;

public class PilgrimageTests {

    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    DiceMonasteryGame game = new DiceMonasteryGame(fm, new DiceMonasteryGameState(new DiceMonasteryParams(3), 4));
    DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
    DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) game.getGameState().getTurnOrder();
    RandomPlayer rnd = new RandomPlayer();

    @Test
    public void basicFunctionalityShort() {
        Pilgrimage p = new Pilgrimage(ROME, false);
        Monk pilgrim = state.createMonk(5, 0);
        assertEquals(0, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, VIVID_GREEN_PIGMENT, STOREROOM));
        assertEquals(6, state.getResource(0, SHILLINGS, STOREROOM));
        state.moveMonk(pilgrim.getComponentID(), DORMITORY, GATEHOUSE);
        p.startPilgrimage(pilgrim, state);

        assertEquals(PILGRIMAGE, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(5, pilgrim.getPiety());
        assertEquals(1, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, VIVID_GREEN_PIGMENT, STOREROOM));
        assertEquals(3, state.getResource(0, SHILLINGS, STOREROOM));

        // we now get the reward, but the monk does not return
        p.advance(state);
        assertEquals(PILGRIMAGE, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(5, pilgrim.getPiety());
        assertEquals(3, state.getVictoryPoints(0));
        assertEquals(1, state.getResource(0, VIVID_GREEN_PIGMENT, STOREROOM));

        // the monk returns
        p.advance(state);
        assertEquals(DORMITORY, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(6, pilgrim.getPiety());
    }

    @Test
    public void basicFunctionalityLong() {
        Pilgrimage p = new Pilgrimage(JERUSALEM, true);
        Monk pilgrim = state.createMonk(5, 0);
        assertEquals(0, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, VIVID_BLUE_PIGMENT, STOREROOM));
        assertEquals(6, state.getResource(0, SHILLINGS, STOREROOM));
        state.moveMonk(pilgrim.getComponentID(), DORMITORY, GATEHOUSE);
        p.startPilgrimage(pilgrim, state);

        assertEquals(PILGRIMAGE, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(5, pilgrim.getPiety());
        assertEquals(1, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, VIVID_BLUE_PIGMENT, STOREROOM));
        assertEquals(0, state.getResource(0, SHILLINGS, STOREROOM));

        p.advance(state);
        assertEquals(PILGRIMAGE, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(5, pilgrim.getPiety());
        assertEquals(2, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, VIVID_BLUE_PIGMENT, STOREROOM));

        p.advance(state);
        assertEquals(PILGRIMAGE, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(5, pilgrim.getPiety());
        assertEquals(7, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, VIVID_BLUE_PIGMENT, STOREROOM));

        // the monk returns
        p.advance(state);
        assertEquals(DORMITORY, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(6, pilgrim.getPiety());
    }

    @Test
    public void copyWorks() {
        Pilgrimage p = new Pilgrimage(ROME, false);
        Monk pilgrim = state.createMonk(5, 0);
        state.moveMonk(pilgrim.getComponentID(), DORMITORY, GATEHOUSE);

        Pilgrimage pCopy = p.copy();
        assertEquals(pCopy.hashCode(), p.hashCode());
        assertEquals(pCopy, p);
        int copyHash = pCopy.hashCode();

        p.startPilgrimage(pilgrim, state);

        assertNotSame(pCopy.hashCode(), p.hashCode());
        assertNotSame(pCopy, p);
        assertEquals(pCopy.hashCode(), copyHash);
        int startHash = p.hashCode();

        p.advance(state);
        assertEquals(pCopy.hashCode(), copyHash);
        assertNotSame(pCopy.hashCode(), p.hashCode());
        assertNotSame(startHash, p.hashCode());
    }

    @Test
    public void pilgrimageAdvancesInSpringAndSummerOnly() {
        turnOrder.endRound(state);
        turnOrder.endRound(state);
        turnOrder.endRound(state);  // gets to SPRING in Year 2

        Monk pilgrim1 = state.createMonk(5, 0);
        state.moveMonk(pilgrim1.getComponentID(), DORMITORY, GATEHOUSE);
        Pilgrimage p1 = state.startPilgrimage(JERUSALEM, pilgrim1);

        Monk pilgrim2 = state.createMonk(5, 1);
        state.moveMonk(pilgrim2.getComponentID(), DORMITORY, GATEHOUSE);
        Pilgrimage p2 = state.startPilgrimage(ROME, pilgrim2);
        assertTrue(p1.isActive());
        assertTrue(p2.isActive());

        turnOrder.endRound(state);  // SPRING -> SUMMER
        assertEquals(SUMMER, turnOrder.getSeason());
        assertTrue(p1.isActive());
        assertTrue(p2.isActive());

        turnOrder.endRound(state);  // SUMMER -> AUTUMN
        assertEquals(AUTUMN, turnOrder.getSeason());
        assertTrue(p1.isActive());
        assertTrue(p2.isActive());
        assertEquals(5, pilgrim1.getPiety());
        assertEquals(5, pilgrim2.getPiety());

        state.addResource(0, BREAD, 10);
        state.addResource(1, BREAD, 10);

        turnOrder.endRound(state);  // AUTUMN -> WINTER
        assertEquals(WINTER, turnOrder.getSeason());
        assertTrue(p1.isActive());
        assertFalse(p2.isActive());
        assertEquals(5, pilgrim1.getPiety());
        assertEquals(6, pilgrim2.getPiety());
    }

    @Test
    public void pilgrimsSkipChristmasFeast() {
        turnOrder.endRound(state); // SPRING -> AUTUMN

        Monk pilgrim1 = state.createMonk(5, 0);
        state.moveMonk(pilgrim1.getComponentID(), DORMITORY, GATEHOUSE);
        state.startPilgrimage(JERUSALEM, pilgrim1);

        state.addResource(0, BREAD, -state.getResource(0, BREAD, STOREROOM));
        state.addResource(0, HONEY, -state.getResource(0, HONEY, STOREROOM));
        state.addResource(0, BERRIES, -state.getResource(0, BERRIES, STOREROOM));
        int totalPiety = state.monksIn(null, 0).stream().mapToInt(Monk::getPiety).sum();
        int totalMonksAboveOne = (int) state.monksIn(null, 0).stream().filter(m -> m.getPiety() > 1).count();

        turnOrder.endRound(state); // AUTUMN -> WINTER
        assertEquals(totalPiety - totalMonksAboveOne + 1, state.monksIn(null, 0).stream().mapToInt(Monk::getPiety).sum());
        assertEquals(5, pilgrim1.getPiety());
    }

    @Test
    public void promotingAMonkMidPilgrimage() {
        Monk pilgrim1 = state.createMonk(6, 0);
        state.moveMonk(pilgrim1.getComponentID(), DORMITORY, GATEHOUSE);
        Pilgrimage p1 = state.startPilgrimage(JERUSALEM, pilgrim1);

        assertTrue(p1.isActive());
        assertEquals(1, state.getVictoryPoints(0));

        pilgrim1.promote(state);
        assertEquals(6, state.getVictoryPoints(0)); // retirement benefit
        assertTrue(p1.isActive());
        assertEquals(RETIRED, state.getMonkLocation(pilgrim1.getComponentID()));
        turnOrder.endRound(state);  // SPRING -> SUMMER
        assertFalse(p1.isActive());
        assertEquals(6, state.getVictoryPoints(0)); // no change
    }
}
