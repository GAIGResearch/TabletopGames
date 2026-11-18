package games.diplomacy;

import games.diplomacy.actions.Convoy;
import games.diplomacy.actions.Move;
import org.junit.Before;
import org.junit.Test;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Exact action sets with convoys: Convoy orders for fleets in sea provinces on a convoy chain, moves via convoy
 * for coastal armies (also where the destination is adjacent by land), supports for moves an army could make by
 * convoy. Spring 1901 orders on a cleared board. By default (ownUnitsOnly) a power convoys and is convoyed only by its
 * own units; tests of help between powers use helpingAnyUnit.
 */
public class DiplomacyConvoyOrdersTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    @Before
    public void setup() {
        state = newState();
        fm = new DiplomacyForwardModel();
        clearBoard(state);
    }

    @Test
    public void armyNextToAFleetAtSeaMayMoveViaConvoyToEveryCoastBesideIt() {
        // A Lon (first in province order), F Nth. Land moves Wal, Yor. Nth borders the coasts Bel, Den, Edi, Hol,
        // Lon, Nwy, Yor: via convoy to all but Lon - Yor both by land and via convoy. Supports: no unit in Wal or
        // Yor to hold; F Nth could move to Yor
        place(state, ENGLAND, "A Lon", "F Nth");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        assertEquals(orderSet(state, "A Lon H", "A Lon-Wal", "A Lon-Yor",
                        "A Lon-Bel via convoy", "A Lon-Den via convoy", "A Lon-Edi via convoy", "A Lon-Hol via convoy",
                        "A Lon-Nwy via convoy", "A Lon-Yor via convoy",
                        "A Lon S F Nth-Yor"),
                legalSet(state, fm));
    }

    @Test
    public void fleetAtSeaMayConvoyAndSupportTheMovesItMakesPossible() {
        // F Nth: hold, its 11 moves, hold support for A Lon, support for every move A Lon could make into a
        // province Nth borders (Yor by land or convoy; Bel, Den, Edi, Hol, Nwy by convoy), and a convoy for each
        // of those 6 destinations (not Wal: Nth does not border it)
        place(state, ENGLAND, "A Lon", "F Nth");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        fm.next(state, order(state, "A Lon H"));
        assertEquals(orderSet(state, "F Nth H", "F Nth-Bel", "F Nth-Den", "F Nth-Edi", "F Nth-Eng", "F Nth-Hel",
                        "F Nth-Hol", "F Nth-Lon", "F Nth-Nrg", "F Nth-Nwy", "F Nth-Ska", "F Nth-Yor",
                        "F Nth S A Lon",
                        "F Nth S A Lon-Yor", "F Nth S A Lon-Bel", "F Nth S A Lon-Den", "F Nth S A Lon-Edi",
                        "F Nth S A Lon-Hol", "F Nth S A Lon-Nwy",
                        "F Nth C A Lon-Bel", "F Nth C A Lon-Den", "F Nth C A Lon-Edi", "F Nth C A Lon-Hol",
                        "F Nth C A Lon-Nwy", "F Nth C A Lon-Yor"),
                legalSet(state, fm));
    }

    @Test
    public void armyIsOfferedMovesViaConvoyOnlyAlongItsOwnFleets() {
        // Diagram 20's fleets: F Eng, F Mid (England), F Wes (France). From Lon (only Eng borders it; Nth empty):
        // Eng -> Bel, Bre, Pic, Wal; Mid -> Bre, Gas, NAf, Por, Spa. Not Tun, which only the French F Wes reaches
        place(state, ENGLAND, "A Lon", "F Eng", "F Mid");
        place(state, FRANCE, "F Wes");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        fm.next(state, order(state, "F Eng H"));
        assertEquals(orderSet(state, "A Lon H", "A Lon-Wal", "A Lon-Yor",
                        "A Lon-Bel via convoy", "A Lon-Bre via convoy", "A Lon-Gas via convoy", "A Lon-NAf via convoy",
                        "A Lon-Pic via convoy", "A Lon-Por via convoy", "A Lon-Spa via convoy",
                        "A Lon-Wal via convoy",
                        "A Lon S F Eng-Wal"),
                legalSet(state, fm));
    }

    @Test
    public void chainOfOwnAndForeignFleetsGivesMovesViaConvoyAlongItsLength() {
        // Diagram 20's fleets: F Eng, F Mid (England), F Wes (France). From Lon (only Eng borders it; Nth empty):
        // Eng -> Bel, Bre, Pic, Wal; Mid -> Bre, Gas, NAf, Por, Spa; Wes -> NAf, Spa, Tun. Order: Eng, Lon, Mid
        state = newState(helpingAnyUnit());
        clearBoard(state);
        place(state, ENGLAND, "A Lon", "F Eng", "F Mid");
        place(state, FRANCE, "F Wes");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        fm.next(state, order(state, "F Eng H"));
        assertEquals(orderSet(state, "A Lon H", "A Lon-Wal", "A Lon-Yor",
                        "A Lon-Bel via convoy", "A Lon-Bre via convoy", "A Lon-Gas via convoy", "A Lon-NAf via convoy",
                        "A Lon-Pic via convoy", "A Lon-Por via convoy", "A Lon-Spa via convoy", "A Lon-Tun via convoy",
                        "A Lon-Wal via convoy",
                        "A Lon S F Eng-Wal"),
                legalSet(state, fm));
    }

    @Test
    public void foreignFleetIsNotOfferedOrdersHelpingAnotherPowersUnits() {
        // Same board; France's F Wes: moves GoL, Mid, NAf, Spa/sc, Tun, Tyn, and no support or convoy for the
        // English units
        place(state, ENGLAND, "A Lon", "F Eng", "F Mid");
        place(state, FRANCE, "F Wes");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F Eng H", "A Lon H", "F Mid H");
        assertEquals(FRANCE, state.getCurrentPlayer());
        assertEquals(orderSet(state, "F Wes H", "F Wes-GoL", "F Wes-Mid", "F Wes-NAf", "F Wes-Spa/sc", "F Wes-Tun",
                        "F Wes-Tyn"),
                legalSet(state, fm));
    }

    @Test
    public void foreignFleetMayConvoyAnotherPowersArmy() {
        // Same board; France's F Wes: moves GoL, Mid, NAf, Spa/sc, Tun, Tyn; hold support for F Mid; F Mid could
        // move to NAf and Spa, F Eng to Mid; A Lon could reach NAf, Spa, Tun by convoy (via Eng, Mid and, for Tun, Wes); the
        // chains through Wes are Lon-Eng-Mid-Wes to NAf, Spa, Tun
        state = newState(helpingAnyUnit());
        clearBoard(state);
        place(state, ENGLAND, "A Lon", "F Eng", "F Mid");
        place(state, FRANCE, "F Wes");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F Eng H", "A Lon H", "F Mid H");
        assertEquals(FRANCE, state.getCurrentPlayer());
        assertEquals(orderSet(state, "F Wes H", "F Wes-GoL", "F Wes-Mid", "F Wes-NAf", "F Wes-Spa/sc", "F Wes-Tun",
                        "F Wes-Tyn",
                        "F Wes S F Mid",
                        "F Wes S F Mid-NAf", "F Wes S F Mid-Spa", "F Wes S F Eng-Mid",
                        "F Wes S A Lon-NAf", "F Wes S A Lon-Spa", "F Wes S A Lon-Tun",
                        "F Wes C A Lon-NAf", "F Wes C A Lon-Spa", "F Wes C A Lon-Tun"),
                legalSet(state, fm));
    }

    @Test
    public void fleetOnlyConvoysOnARouteThatDoesNotDoubleBack() {
        // DATC 6.F.12's fleets: F Eng, F Iri, A Lon. Iri borders no coast beside Lon or Bel, so it is on no chain
        // from Lon to Bel (Lon-Eng-Iri-Eng-Bel would use Eng twice); it is on Lon-Eng-Iri to Lvp and Wal
        place(state, ENGLAND, "A Lon", "F Eng", "F Iri");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        fm.next(state, order(state, "F Eng H"));
        assertEquals(orderSet(state, "F Iri C A Lon-Lvp", "F Iri C A Lon-Wal"), legalSetOf(state, fm, Convoy.class));
        fm.next(state, order(state, "F Iri H"));
        // A Lon: via Eng to Bel, Bre, Pic, Wal; via Eng and Iri to Lvp (Wal again)
        assertEquals(orderSet(state, "A Lon-Wal", "A Lon-Yor", "A Lon-Bel via convoy", "A Lon-Bre via convoy",
                "A Lon-Pic via convoy", "A Lon-Wal via convoy", "A Lon-Lvp via convoy"), legalSetOf(state, fm, Move.class));
    }

    @Test
    public void fleetsInCoastalProvincesNeitherConvoyNorLinkAChain() {
        // DATC 6.F.1's units (Turkey): F Aeg, F Bla, F Con, A Gre, ordered in that order. F Con: moves Aeg, Ank,
        // Bla, Bul/ec, Bul/sc, Smy; hold support for F Aeg and F Bla; F Aeg could move to Bul, Smy; F Bla to Ank,
        // Bul; A Gre to Bul (by land) and Smy (via Aeg). No Convoy order: Con is coastal
        place(state, TURKEY, "A Gre", "F Aeg", "F Con", "F Bla");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F Aeg H", "F Bla H");
        assertEquals(orderSet(state, "F Con H", "F Con-Aeg", "F Con-Ank", "F Con-Bla", "F Con-Bul/ec", "F Con-Bul/sc",
                        "F Con-Smy",
                        "F Con S F Aeg", "F Con S F Bla",
                        "F Con S F Aeg-Bul", "F Con S F Aeg-Smy", "F Con S F Bla-Ank", "F Con S F Bla-Bul",
                        "F Con S A Gre-Bul", "F Con S A Gre-Smy"),
                legalSet(state, fm));
        fm.next(state, order(state, "F Con H"));
        // A Gre: land Alb, Bul, Ser; via Aeg to Bul, Con, Smy - not to Sev (Aeg-Con-Bla is no chain); every fleet
        // could move to Bul
        assertEquals(orderSet(state, "A Gre H", "A Gre-Alb", "A Gre-Bul", "A Gre-Ser",
                        "A Gre-Bul via convoy", "A Gre-Con via convoy", "A Gre-Smy via convoy",
                        "A Gre S F Aeg-Bul", "A Gre S F Con-Bul", "A Gre S F Bla-Bul"),
                legalSet(state, fm));
    }

    @Test
    public void fleetAtSeaBesideACoastalArmyGetsItsConvoysFirstInOrder() {
        // Same board, F Aeg first: moves Bul/sc, Con, Eas, Gre, Ion, Smy; hold support for F Con, A Gre; F Bla
        // could move to Bul, Con; F Con to Bul, Smy; A Gre to Bul (land and convoy), Con, Smy (convoy); convoys
        // for A Gre to Bul, Con, Smy
        place(state, TURKEY, "A Gre", "F Aeg", "F Con", "F Bla");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        assertEquals(orderSet(state, "F Aeg H", "F Aeg-Bul/sc", "F Aeg-Con", "F Aeg-Eas", "F Aeg-Gre", "F Aeg-Ion",
                        "F Aeg-Smy",
                        "F Aeg S F Con", "F Aeg S A Gre",
                        "F Aeg S F Bla-Bul", "F Aeg S F Bla-Con", "F Aeg S F Con-Bul", "F Aeg S F Con-Smy",
                        "F Aeg S A Gre-Bul", "F Aeg S A Gre-Con", "F Aeg S A Gre-Smy",
                        "F Aeg C A Gre-Bul", "F Aeg C A Gre-Con", "F Aeg C A Gre-Smy"),
                legalSet(state, fm));
    }
}
