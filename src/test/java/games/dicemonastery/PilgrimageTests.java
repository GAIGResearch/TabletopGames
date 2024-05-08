package games.dicemonastery;

import core.Game;
import games.GameType;
import games.dicemonastery.*;
import games.dicemonastery.components.Monk;
import games.dicemonastery.components.Pilgrimage;
import org.junit.Test;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
import static games.dicemonastery.DiceMonasteryConstants.Season.*;
import static org.junit.Assert.*;

public class PilgrimageTests {

    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    Game game = GameType.DiceMonastery.createGameInstance(4, new DiceMonasteryParams(3));
    DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

    @Test
    public void basicFunctionalityShort() {
        Pilgrimage p = state.peekAtNextShortPilgrimage();
        DiceMonasteryConstants.Resource reward = p.finalReward;
        Monk pilgrim = state.createMonk(5, 0);
        assertEquals(0, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, reward, STOREROOM));
        assertEquals(6, state.getResource(0, SHILLINGS, STOREROOM));
        state.moveMonk(pilgrim.getComponentID(), DORMITORY, GATEHOUSE);
        p.startPilgrimage(pilgrim, state);

        assertEquals(PILGRIMAGE, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(5, pilgrim.getPiety());
        assertEquals(0, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, reward, STOREROOM));
        assertEquals(3, state.getResource(0, SHILLINGS, STOREROOM));

        // we now get the reward, but the monk does not return
        p.advance(state);
        assertEquals(PILGRIMAGE, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(5, pilgrim.getPiety());
        assertEquals(1, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, reward, STOREROOM));

        // the monk returns
        p.advance(state);
        assertEquals(DORMITORY, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(6, pilgrim.getPiety());
        assertEquals(2, state.getVictoryPoints(0));
        assertEquals(1, state.getResource(0, reward, STOREROOM));
    }

    @Test
    public void basicFunctionalityLong() {
        Pilgrimage p = state.peekAtNextLongPilgrimage();
        DiceMonasteryConstants.Resource reward = p.finalReward;
        Monk pilgrim = state.createMonk(5, 0);
        assertEquals(0, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, reward, STOREROOM));
        assertEquals(6, state.getResource(0, SHILLINGS, STOREROOM));
        state.moveMonk(pilgrim.getComponentID(), DORMITORY, GATEHOUSE);
        p.startPilgrimage(pilgrim, state);

        assertEquals(PILGRIMAGE, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(5, pilgrim.getPiety());
        assertEquals(0, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, reward, STOREROOM));
        assertEquals(0, state.getResource(0, SHILLINGS, STOREROOM));

        p.advance(state);
        assertEquals(PILGRIMAGE, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(5, pilgrim.getPiety());
        assertEquals(1, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, reward, STOREROOM));

        p.advance(state);
        assertEquals(PILGRIMAGE, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(5, pilgrim.getPiety());
        assertEquals(2, state.getVictoryPoints(0));
        assertEquals(0, state.getResource(0, reward, STOREROOM));

        // the monk returns
        p.advance(state);
        assertEquals(DORMITORY, state.getMonkLocation(pilgrim.getComponentID()));
        assertEquals(6, pilgrim.getPiety());
        assertEquals(3, state.getVictoryPoints(0));
        assertEquals(1, state.getResource(0, reward, STOREROOM));
    }

    @Test
    public void copyWorks() {
        Pilgrimage p = new Pilgrimage("ROME", 4, 3, "VIVID_GREEN_PIGMENT", new int[] {0, 1, 1} );
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
        fm.endSeason(state);
        assertEquals(AUTUMN, state.getSeason());
        fm.endSeason(state);
        fm.endSeason(state);  // gets to SPRING in Year 2
        assertEquals(SPRING, state.getSeason());

        Monk pilgrim1 = state.createMonk(5, 0);
        state.moveMonk(pilgrim1.getComponentID(), DORMITORY, GATEHOUSE);
        Pilgrimage destination1 = state.peekAtNextLongPilgrimage();
        Pilgrimage p1 = state.startPilgrimage(destination1, pilgrim1);

        Pilgrimage destination2 = state.peekAtNextShortPilgrimage();
        Monk pilgrim2 = state.createMonk(5, 1);
        state.moveMonk(pilgrim2.getComponentID(), DORMITORY, GATEHOUSE);
        Pilgrimage p2 = state.startPilgrimage(destination2, pilgrim2);
        assertTrue(p1.isActive());
        assertTrue(p2.isActive());

        fm.endSeason(state);  // SPRING -> SUMMER
        assertEquals(SUMMER, state.getSeason());
        assertTrue(p1.isActive());
        assertTrue(p2.isActive());

        fm.endSeason(state);  // SUMMER -> AUTUMN
        assertEquals(AUTUMN, state.getSeason());
        assertTrue(p1.isActive());
        assertTrue(p2.isActive());
        assertEquals(5, pilgrim1.getPiety());
        assertEquals(5, pilgrim2.getPiety());

        state.addResource(0, BREAD, 10);
        state.addResource(1, BREAD, 10);

        fm.endSeason(state);  // AUTUMN -> WINTER
        assertEquals(WINTER, state.getSeason());
        assertTrue(p1.isActive());
        assertFalse(p2.isActive());
        assertEquals(5, pilgrim1.getPiety());
        assertEquals(6, pilgrim2.getPiety());
    }

    @Test
    public void pilgrimsSkipChristmasFeast() {
        fm.endSeason(state); // SPRING -> AUTUMN

        Monk pilgrim1 = state.createMonk(5, 0);
        state.moveMonk(pilgrim1.getComponentID(), DORMITORY, GATEHOUSE);
        state.startPilgrimage(state.peekAtNextLongPilgrimage(), pilgrim1);

        state.addResource(0, BREAD, -state.getResource(0, BREAD, STOREROOM));
        state.addResource(0, HONEY, -state.getResource(0, HONEY, STOREROOM));
        int totalPiety = state.monksIn(null, 0).stream().mapToInt(Monk::getPiety).sum();
        int totalMonksAboveOne = (int) state.monksIn(null, 0).stream().filter(m -> m.getPiety() > 1).count();

        fm.endSeason(state); // AUTUMN -> WINTER
        assertEquals(totalPiety - totalMonksAboveOne + 1, state.monksIn(null, 0).stream().mapToInt(Monk::getPiety).sum());
        assertEquals(5, pilgrim1.getPiety());
    }

    @Test
    public void promotingAMonkMidPilgrimage() {
        Monk pilgrim1 = state.createMonk(6, 0);
        state.moveMonk(pilgrim1.getComponentID(), DORMITORY, GATEHOUSE);
        Pilgrimage p1 = state.startPilgrimage(state.peekAtNextLongPilgrimage(), pilgrim1);

        assertTrue(p1.isActive());
        assertEquals(0, state.getVictoryPoints(0));

        pilgrim1.promote(state);
        assertEquals(5, state.getVictoryPoints(0)); // retirement benefit
        assertTrue(p1.isActive());
        assertEquals(RETIRED, state.getMonkLocation(pilgrim1.getComponentID()));
        fm.endSeason(state);  // SPRING -> SUMMER
        assertFalse(p1.isActive());
        assertEquals(5, state.getVictoryPoints(0)); // no change
    }
}
