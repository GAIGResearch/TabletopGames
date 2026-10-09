package games.diplomacy;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * The starting position (rulebook p.2) and the map's moves (rulebook p.4-5, Diagrams 1-3).
 */
public class DiplomacySetupAndMapTest {

    DiplomacyGameState state;
    DiplomacyMap map;

    @Before
    public void setup() {
        state = newState();
        map = state.getMap();
    }

    private Set<DiplomacyProvince> provinces(String... names) {
        Set<DiplomacyProvince> set = new HashSet<>();
        for (String n : names) set.add(prov(state, n));
        return set;
    }

    private Set<DiplomacyLocation> locations(String... names) {
        Set<DiplomacyLocation> set = new HashSet<>();
        for (String n : names) set.add(loc(state, n));
        return set;
    }

    @Test
    public void boardHas75ProvincesAnd34SupplyCentres() {
        assertEquals(75, map.provinces().size());
        assertEquals(34, map.supplyCentres().size());
        assertEquals(1901, map.startYear());
        assertEquals(18, map.victoryCentres());
        assertEquals(List.of(POWER_NAMES), map.powers());
    }

    @Test
    public void startingUnitsAreAsInTheRulebookTable() {
        // rulebook p.2: 3 units each, Russia 4 with its fleet on St Petersburg's south coast: 22 units
        Map<String, DiplomacyUnit> expected = new HashMap<>();
        expected.put("Vie", army(AUSTRIA));
        expected.put("Bud", army(AUSTRIA));
        expected.put("Tri", fleet(AUSTRIA));
        expected.put("Lon", fleet(ENGLAND));
        expected.put("Edi", fleet(ENGLAND));
        expected.put("Lvp", army(ENGLAND));
        expected.put("Par", army(FRANCE));
        expected.put("Mar", army(FRANCE));
        expected.put("Bre", fleet(FRANCE));
        expected.put("Ber", army(GERMANY));
        expected.put("Mun", army(GERMANY));
        expected.put("Kie", fleet(GERMANY));
        expected.put("Rom", army(ITALY));
        expected.put("Ven", army(ITALY));
        expected.put("Nap", fleet(ITALY));
        expected.put("Mos", army(RUSSIA));
        expected.put("Sev", fleet(RUSSIA));
        expected.put("War", army(RUSSIA));
        expected.put("StP", fleet(RUSSIA, "sc"));
        expected.put("Ank", fleet(TURKEY));
        expected.put("Con", army(TURKEY));
        expected.put("Smy", army(TURKEY));
        for (DiplomacyProvince p : map.provinces())
            assertEquals("unit in " + p, expected.get(p.name()), state.getUnit(p));
        assertEquals(22, totalUnits(state));
        assertEquals(4, state.nUnits(RUSSIA));
    }

    @Test
    public void eachPowerStartsControllingItsHomeCentresAndTheOtherTwelveAreUnowned() {
        Map<String, Integer> homes = new HashMap<>();
        for (String c : List.of("Vie", "Bud", "Tri")) homes.put(c, AUSTRIA);
        for (String c : List.of("Lon", "Edi", "Lvp")) homes.put(c, ENGLAND);
        for (String c : List.of("Par", "Mar", "Bre")) homes.put(c, FRANCE);
        for (String c : List.of("Ber", "Mun", "Kie")) homes.put(c, GERMANY);
        for (String c : List.of("Rom", "Ven", "Nap")) homes.put(c, ITALY);
        for (String c : List.of("Mos", "Sev", "War", "StP")) homes.put(c, RUSSIA);
        for (String c : List.of("Ank", "Con", "Smy")) homes.put(c, TURKEY);
        // 34 centres - 22 home centres = 12 unowned
        Set<String> neutral = Set.of("Bel", "Bul", "Den", "Gre", "Hol", "Nwy", "Por", "Rum", "Ser", "Spa", "Swe", "Tun");
        assertEquals(12, neutral.size());
        // the rest of each home country is controlled too, but is not a centre
        Map<String, Integer> homeland = new HashMap<>();
        for (String c : List.of("Boh", "Gal", "Tyr")) homeland.put(c, AUSTRIA);
        for (String c : List.of("Cly", "Yor", "Wal")) homeland.put(c, ENGLAND);
        for (String c : List.of("Pic", "Bur", "Gas")) homeland.put(c, FRANCE);
        for (String c : List.of("Pru", "Sil", "Ruh")) homeland.put(c, GERMANY);
        for (String c : List.of("Pie", "Tus", "Apu")) homeland.put(c, ITALY);
        for (String c : List.of("Fin", "Lvn", "Ukr")) homeland.put(c, RUSSIA);
        for (String c : List.of("Arm", "Syr")) homeland.put(c, TURKEY);
        for (DiplomacyProvince p : map.provinces()) {
            int home = homes.getOrDefault(p.name(), homeland.getOrDefault(p.name(), -1));
            assertEquals("owner of " + p, home, state.getOwner(p));
            assertEquals("home of " + p, home, p.home());
            assertEquals("supply centre " + p, homes.containsKey(p.name()) || neutral.contains(p.name()), p.supplyCentre());
        }
        for (int power = 0; power < N_POWERS; power++) {
            assertEquals(power == RUSSIA ? 4 : 3, state.nCentres(power));
            assertEquals(power == RUSSIA ? 4 : 3, map.homeCentres(power).size());
        }
    }

    @Test
    public void gameStartsInSpring1901WithAustriaToOrderAndNothingPending() {
        assertEquals(1901, state.getYear());
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        assertNoOrders(state);
        assertTrue(state.getLastResults().isEmpty());
        for (DiplomacyProvince p : map.provinces())
            assertNull(state.getDislodged(p));
        assertTrue(state.isNotTerminal());
        for (int power = 0; power < N_POWERS; power++)
            assertEquals(power == RUSSIA ? 4.0 : 3.0, state.getGameScore(power), 0.0);
    }

    @Test
    public void armyInParisMovesOnlyToBrestPicardyBurgundyAndGascony() {
        // Diagram 1
        assertEquals(provinces("Bre", "Pic", "Bur", "Gas"), new HashSet<>(map.armyMoves(prov(state, "Par"))));
        assertEquals(List.of(), map.fleetMoves(loc(state, "Par")));
    }

    @Test
    public void fleetInTheEnglishChannelMovesToItsEightNeighbours() {
        // Diagram 2
        assertEquals(locations("Iri", "Wal", "Lon", "Nth", "Bel", "Pic", "Bre", "Mid"),
                new HashSet<>(map.fleetMoves(loc(state, "Eng"))));
        assertEquals(List.of(), map.armyMoves(prov(state, "Eng")));
    }

    @Test
    public void fleetInRomeMovesAlongTheCoastOnly() {
        // Diagram 3: Tus, Nap, Tyn - not Ven or Apu, though adjacent by land
        assertEquals(locations("Tus", "Nap", "Tyn"), new HashSet<>(map.fleetMoves(loc(state, "Rom"))));
        assertTrue(map.armyMoves(prov(state, "Rom")).containsAll(provinces("Ven", "Apu")));
    }

    @Test
    public void splitCoastsMustBeNamedAndLimitAFleetsMoves() {
        Set<DiplomacyLocation> con = new HashSet<>(map.fleetMoves(loc(state, "Con")));
        assertTrue(con.containsAll(locations("Bul/ec", "Bul/sc")));
        assertFalse(con.contains(loc(state, "Bul")));
        Set<DiplomacyLocation> mid = new HashSet<>(map.fleetMoves(loc(state, "Mid")));
        assertTrue(mid.containsAll(locations("Spa/nc", "Spa/sc")));
        assertFalse(mid.contains(loc(state, "Spa")));
        // p.5: a fleet on Spain's north coast cannot reach Wes, GoL or Mar
        assertEquals(locations("Gas", "Mid", "Por"), new HashSet<>(map.fleetMoves(loc(state, "Spa/nc"))));
        assertEquals(locations("GoL", "Mar", "Mid", "Por", "Wes"), new HashSet<>(map.fleetMoves(loc(state, "Spa/sc"))));
        assertEquals(locations("Bar", "Nwy"), new HashSet<>(map.fleetMoves(loc(state, "StP/nc"))));
        assertEquals(locations("Bot", "Fin", "Lvn"), new HashSet<>(map.fleetMoves(loc(state, "StP/sc"))));
        // a fleet in a split-coast province always has a coast
        assertEquals(List.of(), map.fleetMoves(loc(state, "Spa")));
    }

    @Test
    public void kielAndConstantinopleHaveOneCoast() {
        assertEquals(locations("Bal", "Ber", "Den", "Hel", "Hol"), new HashSet<>(map.fleetMoves(loc(state, "Kie"))));
        assertTrue(map.fleetMoves(loc(state, "Bla")).contains(loc(state, "Con")));
        assertTrue(map.fleetMoves(loc(state, "Con")).contains(loc(state, "Aeg")));
    }

    @Test
    public void swedenDenmarkAdjacentButBalticSkagerrakAndDenmarkBerlinAreNot() {
        assertTrue(map.fleetMoves(loc(state, "Swe")).contains(loc(state, "Den")));
        assertTrue(map.armyMoves(prov(state, "Swe")).contains(prov(state, "Den")));
        assertFalse(map.adjacent(prov(state, "Bal")).contains(prov(state, "Ska")));
        assertFalse(map.adjacent(prov(state, "Den")).contains(prov(state, "Ber")));
    }

    @Test
    public void armiesNeverGoToSeaFleetsNeverInlandAndSwitzerlandIsNotOnTheMap() {
        for (DiplomacyProvince p : map.provinces()) {
            for (DiplomacyProvince to : map.armyMoves(p))
                assertNotEquals("army " + p + "-" + to, DiplomacyProvince.Type.SEA, to.type());
            List<DiplomacyLocation> froms = p.hasCoasts()
                    ? p.coasts().stream().map(c -> new DiplomacyLocation(p, c)).toList()
                    : List.of(new DiplomacyLocation(p));
            for (DiplomacyLocation from : froms)
                for (DiplomacyLocation to : map.fleetMoves(from))
                    assertNotEquals("fleet " + from + "-" + to, DiplomacyProvince.Type.LAND, to.province().type());
            if (p.type() == DiplomacyProvince.Type.LAND)
                assertEquals(List.of(), map.fleetMoves(new DiplomacyLocation(p)));
        }
        assertThrows(IllegalArgumentException.class, () -> map.province("Swi"));
    }
}
