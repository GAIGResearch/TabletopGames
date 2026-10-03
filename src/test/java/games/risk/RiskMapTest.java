package games.risk;

import games.risk.components.RiskCard;
import org.junit.Test;

import java.util.*;

import static games.risk.WorldMap.*;
import static games.risk.RiskTestUtils.*;
import static org.junit.Assert.*;

public class RiskMapTest {

    static final String TEST_MAP = "src/test/resources/risk_testMap.json";

    @Test
    public void mapHasEightyThreeSymmetricConnections() {
        assertEquals(42, MAP.nTerritories());
        assertEquals(42, MAP.getBoard().getBoardNodes().size());
        int ends = 0;
        for (RiskTerritory a : ALL) {
            assertFalse(a + " adjacent to itself", MAP.adjacent(a, a));
            for (RiskTerritory b : MAP.neighbours(a)) {
                assertTrue(b + " -> " + a, MAP.adjacent(b, a));
                ends++;
            }
        }
        assertEquals(83, ends / 2);
    }

    @Test
    public void pdfAdjacencyExamples() {
        assertEquals(Set.of(NORTHWEST_TERRITORY, ONTARIO, QUEBEC, ICELAND), new HashSet<>(MAP.neighbours(GREENLAND)));
        assertTrue(MAP.neighbours(NORTH_AFRICA).containsAll(List.of(EGYPT, WESTERN_EUROPE, BRAZIL)));
        assertTrue(MAP.adjacent(ALASKA, KAMCHATKA));
        assertFalse(MAP.adjacent(ALASKA, GREENLAND));
    }

    @Test
    public void continentsHaveTheRightNumberOfTerritoriesAndBonuses() {
        assertEquals(6, MAP.continents().size());
        assertEquals(9, continent(WorldContinents.NORTH_AMERICA).size());
        assertEquals(4, continent(WorldContinents.SOUTH_AMERICA).size());
        assertEquals(7, continent(WorldContinents.EUROPE).size());
        assertEquals(6, continent(WorldContinents.AFRICA).size());
        assertEquals(12, continent(WorldContinents.ASIA).size());
        assertEquals(4, continent(WorldContinents.AUSTRALIA).size());
        // pdf: North America 5, South America 2, Europe 5, Africa 3, Asia 7, Australia 2
        assertEquals(List.of(5, 2, 5, 3, 7, 2), MAP.continents().stream().map(RiskContinent::bonus).toList());
    }

    @Test
    public void everyTerritoryCanBeReachedFromAlaska() {
        Set<RiskTerritory> seen = new HashSet<>(List.of(ALASKA));
        Deque<RiskTerritory> queue = new ArrayDeque<>(seen);
        while (!queue.isEmpty())
            for (RiskTerritory n : MAP.neighbours(queue.poll()))
                if (seen.add(n)) queue.add(n);
        assertEquals(42, seen.size());
    }

    @Test
    public void aMapLoadedTwiceGivesEqualTerritories() {
        RiskMap again = new RiskMap("data/risk/worldMap.json");
        assertEquals(ALL, again.territories());
        assertEquals(SIAM, again.territory("Siam"));
        assertEquals(MAP.neighbours(SIAM), again.neighbours(again.territory("Siam")));
    }

    @Test
    public void neighboursAreInIndexOrderWhateverTheComponentIds() {
        // the graph holds neighbours by component id, which depends on how many components were made before; the
        // action lists follow this order, so a game with a given seed would otherwise depend on it
        for (int load = 0; load < 5; load++) {
            for (int i = 0; i < 7 * load; i++)
                new core.components.Token("spacer");
            RiskMap map = new RiskMap("data/risk/worldMap.json");
            for (RiskTerritory t : map.territories()) {
                List<RiskTerritory> sorted = new ArrayList<>(map.neighbours(t));
                sorted.sort(Comparator.comparingInt(RiskTerritory::index));
                assertEquals(t + " in load " + load, sorted, map.neighbours(t));
            }
        }
    }

    @Test
    public void aVariantMapFileGivesItsOwnTerritoriesContinentsAndConnections() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("mapFile", TEST_MAP);
        RiskMap map = params.getMap();
        assertEquals(List.of("A", "B", "C", "D", "E", "F"), map.territories().stream().map(RiskTerritory::name).toList());
        assertEquals(2, map.continents().size());
        RiskContinent north = map.continents().get(0);
        assertEquals("North", north.name());
        assertEquals(4, north.bonus());
        assertEquals(List.of(map.territory("A"), map.territory("B"), map.territory("C")), map.territories(north));
        assertEquals(RiskCard.Symbol.ARTILLERY, map.territory("F").symbol());
        // C - D is the only link between the continents; F is a dead end
        assertEquals(List.of(map.territory("A"), map.territory("B"), map.territory("D")), map.neighbours(map.territory("C")));
        assertEquals(List.of(map.territory("E")), map.neighbours(map.territory("F")));
        assertFalse(map.adjacent(map.territory("A"), map.territory("D")));
        // a copy of the parameters keeps the map
        assertEquals(map.territories(), ((RiskParameters) params.copy()).getMap().territories());
    }

    @Test
    public void aGameOnAVariantMapUsesThatMap() {
        RiskParameters params = new RiskParameters();
        params.setParameterValue("mapFile", TEST_MAP);
        RiskGameState state = newState(3, 7, params);
        RiskMap map = state.getMap();
        assertEquals(6, state.getTerritories(-1).size());
        assertEquals(6 + 2, state.getDrawDeck().getSize()); // a card per territory and the 2 wild cards

        // holding North: max(3, 3 / 3) + 4
        give(state, 0, 1, map.territories(map.continents().get(0)));
        give(state, 1, 1, map.territories(map.continents().get(1)));
        assertEquals(7, state.getReinforcements(0));
        // holding South: 3 + 1
        assertEquals(4, state.getReinforcements(1));
    }

    @Test
    public void aMapWithAnUnknownNeighbourIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new RiskMap("src/test/resources/risk_badMap.json"));
        assertTrue(e.getMessage(), e.getMessage().contains("Atlantis"));
    }
}
