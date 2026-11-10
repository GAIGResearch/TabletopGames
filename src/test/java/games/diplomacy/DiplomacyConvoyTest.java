package games.diplomacy;

import org.junit.Before;
import org.junit.Test;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Convoy resolution from the 2000 rulebook: Diagrams 19-21 and 28-32, the land-and-convoy route qualifiers (p.15),
 * a convoy order that does not match the move (p.11), and rule 18. A move to a province not adjacent by land is
 * offered only via convoy, so the rulebook's "A Lon-Nwy" is written "A Lon-Nwy via convoy". A convoy order
 * succeeds when its fleet is not dislodged. Spring 1901 orders on a cleared board.
 */
public class DiplomacyConvoyTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    @Before
    public void setup() {
        state = newState(helpingAnyUnit());
        fm = new DiplomacyForwardModel();
        clearBoard(state);
    }

    private void start() {
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
    }

    @Test
    public void diagram19ConvoyAcrossOneSea() {
        place(state, ENGLAND, "A Lon", "F Nth");
        start();
        playExpecting(state, fm, "A Lon-Nwy via convoy: ok", "F Nth C A Lon-Nwy: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Nwy")));
        assertNull(state.getUnit(prov(state, "Lon")));
    }

    @Test
    public void diagram20ConvoyAcrossSeveralSeasWithAForeignFleet() {
        place(state, ENGLAND, "A Lon", "F Eng", "F Mid");
        place(state, FRANCE, "F Wes");
        start();
        playExpecting(state, fm, "A Lon-Tun via convoy: ok", "F Eng C A Lon-Tun: ok", "F Mid C A Lon-Tun: ok",
                "F Wes C A Lon-Tun: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Tun")));
        assertNull(state.getUnit(prov(state, "Lon")));
    }

    @Test
    public void diagram21DislodgedFleetDisruptsTheConvoy() {
        // F Ion-Tyn 2 (F Tun) vs F Tyn's hold 1: dislodged, so the only route Spa-GoL-Tyn-Nap is broken
        place(state, FRANCE, "A Spa", "F GoL", "F Tyn");
        place(state, ITALY, "F Ion", "F Tun");
        start();
        playExpecting(state, fm, "A Spa-Nap via convoy: fails", "F GoL C A Spa-Nap: ok", "F Tyn C A Spa-Nap: fails",
                "F Ion-Tyn: ok", "F Tun S F Ion-Tyn: ok");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Spa")));
        assertNull(state.getUnit(prov(state, "Nap")));
        assertEquals(fleet(FRANCE), state.getDislodged(prov(state, "Tyn")));
    }

    @Test
    public void diagram28TwoConvoyedArmiesSwap() {
        // rule 19: no head-to-head battle when either unit is convoyed; each enters the province the other left
        place(state, ENGLAND, "A Lon", "F Nth");
        place(state, FRANCE, "A Bel", "F Eng");
        start();
        playExpecting(state, fm, "A Lon-Bel via convoy: ok", "F Nth C A Lon-Bel: ok", "A Bel-Lon via convoy: ok",
                "F Eng C A Bel-Lon: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Lon")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void diagram29ArmyArrivesWhileOneOfTwoRoutesIsOpen() {
        // rule 20: F Bre-Eng 2 (F Iri) dislodges F Eng, but the route via F Nth is still open
        place(state, ENGLAND, "A Lon", "F Eng", "F Nth");
        place(state, FRANCE, "F Bre", "F Iri");
        start();
        playExpecting(state, fm, "A Lon-Bel via convoy: ok", "F Eng C A Lon-Bel: fails", "F Nth C A Lon-Bel: ok",
                "F Bre-Eng: ok", "F Iri S F Bre-Eng: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Eng")));
        assertEquals(fleet(ENGLAND), state.getDislodged(prov(state, "Eng")));
    }

    @Test
    public void diagram30ConvoyedArmyDoesNotCutSupportForAnAttackOnItsConvoy() {
        // rule 21: A Tun-Nap does not cut F Nap's support of F Ion-Tyn, an attack on the only convoying fleet.
        // F Ion-Tyn 2 vs 1 dislodges F Tyn; the convoy fails and A Tun stays
        place(state, FRANCE, "A Tun", "F Tyn");
        place(state, ITALY, "F Ion", "F Nap");
        start();
        playExpecting(state, fm, "A Tun-Nap via convoy: fails", "F Tyn C A Tun-Nap: fails", "F Ion-Tyn: ok",
                "F Nap S F Ion-Tyn: ok");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Tun")));
        assertEquals(fleet(ITALY), state.getUnit(prov(state, "Tyn")));
        assertEquals(fleet(FRANCE), state.getDislodged(prov(state, "Tyn")));
    }

    @Test
    public void diagram31ArmyWithAnotherRouteCutsTheSupport() {
        // rule 22: the route via F Ion does not need F Tyn, so A Tun-Nap cuts F Nap's support of F Rom-Tyn.
        // F Rom-Tyn 1 vs hold 1 fails; A Tun-Nap 1 vs F Nap's hold 1 fails (standoff): nothing moves
        place(state, FRANCE, "A Tun", "F Tyn", "F Ion");
        place(state, ITALY, "F Rom", "F Nap");
        start();
        playExpecting(state, fm, "A Tun-Nap via convoy: fails", "F Tyn C A Tun-Nap: ok", "F Ion C A Tun-Nap: ok",
                "F Rom-Tyn: fails", "F Nap S F Rom-Tyn: fails");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Tun")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Tyn")));
        assertEquals(fleet(ITALY), state.getUnit(prov(state, "Nap")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void diagram32SupportedConvoyedArmyDislodgesTheSupporter() {
        // as Diagram 31 plus A Apu S A Tun-Nap: 2 vs F Nap's hold 1 - F Nap is dislodged (its support cut anyway)
        place(state, FRANCE, "A Tun", "F Tyn", "F Ion", "A Apu");
        place(state, ITALY, "F Rom", "F Nap");
        start();
        playExpecting(state, fm, "A Tun-Nap via convoy: ok", "F Tyn C A Tun-Nap: ok", "F Ion C A Tun-Nap: ok",
                "A Apu S A Tun-Nap: ok", "F Rom-Tyn: fails", "F Nap S F Rom-Tyn: fails");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Nap")));
        // F Nap cannot retreat (Apu, Rom, Ion and Tyn occupied; a fleet cannot reach Tun), so it is disbanded at once
        assertNull(state.getDislodged(prov(state, "Nap")));
        assertEquals(1, state.nUnits(ITALY));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Tyn")));
    }

    @Test
    public void ownFleetConvoyingMeansTheConvoyRouteWithNoFallbackToLand() {
        // p.15: England's own F Eng convoys A Pic-Bel (not marked via convoy), so the land route is disregarded.
        // F Bre-Eng 2 (F Mid) dislodges F Eng: the convoy fails and A Pic stays, although Bel is empty
        place(state, ENGLAND, "A Pic", "F Eng");
        place(state, FRANCE, "F Bre", "F Mid");
        start();
        playExpecting(state, fm, "A Pic-Bel: fails", "F Eng C A Pic-Bel: fails", "F Bre-Eng: ok",
                "F Mid S F Bre-Eng: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Pic")));
        assertNull(state.getUnit(prov(state, "Bel")));
    }

    @Test
    public void onlyAForeignFleetConvoyingMeansTheLandRoute() {
        // p.15: only England's fleet convoys France's A Pic-Bel (not via convoy): land route, a head-to-head
        // battle with A Bel-Pic, 1 vs 1 - neither moves
        place(state, FRANCE, "A Pic");
        place(state, ENGLAND, "F Eng", "A Bel");
        start();
        playExpecting(state, fm, "A Pic-Bel: fails", "F Eng C A Pic-Bel: ok", "A Bel-Pic: fails");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Pic")));
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
    }

    @Test
    public void viaConvoyUsesAForeignFleetsConvoy() {
        // p.15: marked via convoy, France's A Pic-Bel takes England's convoy: no head-to-head battle, the two
        // armies swap (rule 19)
        place(state, FRANCE, "A Pic");
        place(state, ENGLAND, "F Eng", "A Bel");
        start();
        playExpecting(state, fm, "A Pic-Bel via convoy: ok", "F Eng C A Pic-Bel: ok", "A Bel-Pic: ok");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bel")));
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Pic")));
    }

    @Test
    public void convoyOrderMustMatchTheMove() {
        // p.11: A Rum-Arm with F Bla C A Rum-Ank - no route for the move ordered, so the army stays in Rum
        place(state, RUSSIA, "A Rum", "F Bla");
        start();
        playExpecting(state, fm, "A Rum-Arm via convoy: fails", "F Bla C A Rum-Ank: ok");
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Rum")));
        assertNull(state.getUnit(prov(state, "Arm")));
    }

    @Test
    public void armyWhoseConvoyIsDisruptedStillHoldsItsProvince() {
        // F Hel-Nth 2 (F Den) dislodges F Nth: A Lon does not move and keeps hold strength 1, so the unsupported
        // F Eng-Lon (1) fails. Bel stays empty
        place(state, ENGLAND, "A Lon", "F Nth");
        place(state, GERMANY, "F Hel", "F Den");
        place(state, FRANCE, "F Eng");
        start();
        playExpecting(state, fm, "A Lon-Bel via convoy: fails", "F Nth C A Lon-Bel: fails", "F Hel-Nth: ok",
                "F Den S F Hel-Nth: ok", "F Eng-Lon: fails");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Lon")));
        assertNull(state.getUnit(prov(state, "Bel")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Eng")));
    }

    @Test
    public void convoyedArmyStandingOffStaysHomeAndCanBeDislodgedThere() {
        // rule 18: A Lon-Bel and A Pic-Bel stand off (1 vs 1); A Lon stays in Lon (hold 1) where F Eng-Lon 2
        // (F Wal) dislodges it. Bel is left vacant by the standoff
        place(state, ENGLAND, "A Lon", "F Nth");
        place(state, FRANCE, "A Pic", "F Eng", "F Wal");
        start();
        playExpecting(state, fm, "A Lon-Bel via convoy: fails", "F Nth C A Lon-Bel: ok", "A Pic-Bel: fails",
                "F Eng-Lon: ok", "F Wal S F Eng-Lon: ok");
        assertEquals(army(ENGLAND), state.getDislodged(prov(state, "Lon")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Lon")));
        assertTrue(state.isStandoff(prov(state, "Bel")));
    }
}
