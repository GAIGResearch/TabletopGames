package games.diplomacy;

import games.diplomacy.actions.Hold;
import org.junit.Before;
import org.junit.Test;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * The support orders offered in an orders phase (rulebook p.7-8), including supports by and for fleets on split
 * coasts. Exact action sets are checked on boards with no fleet in a sea province next to a coastal army, where no
 * convoy order or move via convoy is offered.
 */
public class DiplomacySupportOrdersTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    @Before
    public void setup() {
        state = newState();
        fm = new DiplomacyForwardModel();
    }

    @Test
    public void startingArmyInBerlinMaySupportItsNeighboursAndTheRussianArmyInWarsaw() {
        // starting position (no fleet at sea yet). A Ber moves to Kie, Mun, Pru, Sil.
        // Hold support: the units in those provinces - F Kie, A Mun.
        // Move support: A Mun can move to Kie and Sil; A War (Russia) to Pru and Sil; F Kie to none of them.
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, GERMANY);
        assertEquals(orderSet(state, "A Ber H", "A Ber-Kie", "A Ber-Mun", "A Ber-Pru", "A Ber-Sil",
                        "A Ber S F Kie", "A Ber S A Mun", "A Ber S A Mun-Kie", "A Ber S A Mun-Sil",
                        "A Ber S A War-Pru", "A Ber S A War-Sil"),
                legalSet(state, fm));
    }

    @Test
    public void armySupportsOnlyIntoProvincesItCouldMoveTo() {
        // A Bur moves to Bel, Gas, Mar, Mun, Par, Pic, Ruh.
        // A Mun (Germany) could move to Ruh among those; A Bel (England) to Pic and Ruh (not Eng/Nth: sea)
        clearBoard(state);
        place(state, FRANCE, "A Bur");
        place(state, GERMANY, "A Mun");
        place(state, ENGLAND, "A Bel");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, FRANCE);
        assertEquals(orderSet(state, "A Bur H", "A Bur-Bel", "A Bur-Gas", "A Bur-Mar", "A Bur-Mun", "A Bur-Par",
                        "A Bur-Pic", "A Bur-Ruh", "A Bur S A Bel", "A Bur S A Mun", "A Bur S A Mun-Ruh",
                        "A Bur S A Bel-Pic", "A Bur S A Bel-Ruh"),
                legalSet(state, fm));
    }

    @Test
    public void supportForAnAttackOnOnesOwnUnitIsOffered() {
        // German A Ruh (A Mun already ordered): moves Bel, Bur, Hol, Kie, Mun.
        // Hold support for A Mun (own) and A Bur (French). Move support: A Mun to Bur or Kie; the French A Bur to
        // Bel or Mun - the last is support for an attack on Germany's own unit, offered (it never helps dislodge it)
        clearBoard(state);
        place(state, GERMANY, "A Ruh", "A Mun");
        place(state, FRANCE, "A Bur");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, GERMANY);
        state.addOrder(GERMANY, new Hold(prov(state, "Mun")));
        assertEquals(orderSet(state, "A Ruh H", "A Ruh-Bel", "A Ruh-Bur", "A Ruh-Hol", "A Ruh-Kie", "A Ruh-Mun",
                        "A Ruh S A Mun", "A Ruh S A Bur", "A Ruh S A Mun-Bur", "A Ruh S A Mun-Kie",
                        "A Ruh S A Bur-Bel", "A Ruh S A Bur-Mun"),
                legalSet(state, fm));
    }

    @Test
    public void fleetInMidAtlanticSupportsIntoSpainWhateverTheCoast() {
        // p.7: F Mid can support into Spa without regard to its coasts. F Mid moves to Bre, Eng, Gas, Iri, NAf,
        // NAt, Por, Spa/nc, Spa/sc, Wes; F Wes (Italy) moves to GoL, Mid, NAf, Spa/sc, Tun, Tyn.
        // Hold support for F Wes; move support for F Wes into NAf and Spa (the province, not the coast)
        clearBoard(state);
        place(state, FRANCE, "F Mid");
        place(state, ITALY, "F Wes");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, FRANCE);
        assertEquals(orderSet(state, "F Mid H", "F Mid-Bre", "F Mid-Eng", "F Mid-Gas", "F Mid-Iri", "F Mid-NAf",
                        "F Mid-NAt", "F Mid-Por", "F Mid-Spa/nc", "F Mid-Spa/sc", "F Mid-Wes",
                        "F Mid S F Wes", "F Mid S F Wes-NAf", "F Mid S F Wes-Spa"),
                legalSet(state, fm));
    }

    @Test
    public void fleetMaySupportAFleetOnTheOtherCoastOfAProvinceItReaches() {
        // F Mar reaches GoL, Pie, Spa/sc: it may support the Italian F Spa/nc in holding (Spa is a province it can
        // move to, whatever the coast). F Spa/nc reaches Gas, Mid, Por only: it may not support F Mar
        clearBoard(state);
        place(state, FRANCE, "F Mar");
        place(state, ITALY, "F Spa/nc");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, FRANCE);
        assertEquals(orderSet(state, "F Mar H", "F Mar-GoL", "F Mar-Pie", "F Mar-Spa/sc", "F Mar S F Spa"),
                legalSet(state, fm));
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, ITALY);
        assertEquals(orderSet(state, "F Spa/nc H", "F Spa/nc-Gas", "F Spa/nc-Mid", "F Spa/nc-Por"),
                legalSet(state, fm));
    }

    @Test
    public void armyInBrestCannotSupportIntoTheEnglishChannel() {
        // p.7: an army cannot move into a water province, so cannot support into one. A Bre moves to Gas, Par,
        // Pic; F Pic (own) may be supported in holding, but its moves (Bel, Bre, Eng) and F Lon's (Eng, Nth, Wal,
        // Yor) reach none of A Bre's provinces
        clearBoard(state);
        place(state, FRANCE, "A Bre", "F Pic");
        place(state, ENGLAND, "F Lon");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, FRANCE);
        assertEquals(orderSet(state, "A Bre H", "A Bre-Gas", "A Bre-Par", "A Bre-Pic", "A Bre S F Pic"),
                legalSet(state, fm));
    }

    @Test
    public void fleetInRomeCannotSupportIntoVenice() {
        // p.7: F Rom cannot move to Ven (fleet moves Nap, Tus, Tyn), so may not support A Tyr-Ven nor A Ven holding.
        // A Ven can move to Tus, which F Rom reaches: that support is offered
        clearBoard(state);
        place(state, ITALY, "F Rom", "A Ven");
        place(state, AUSTRIA, "A Tyr");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, ITALY);
        assertEquals(orderSet(state, "F Rom H", "F Rom-Nap", "F Rom-Tus", "F Rom-Tyn", "F Rom S A Ven-Tus"),
                legalSet(state, fm));
    }
}
