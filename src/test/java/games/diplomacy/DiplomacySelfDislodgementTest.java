package games.diplomacy;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Self-dislodgement and self-standoff (rulebook p.13-14, Diagrams 22-27; rules 12 and 16): a power cannot
 * dislodge its own unit, its support never counts towards dislodging one of its own units, but its supported
 * attacks on its own units can still cause standoffs. Spring 1901 orders on a cleared board.
 */
public class DiplomacySelfDislodgementTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    @Before
    public void setup() {
        state = newState();
        fm = new DiplomacyForwardModel();
        clearBoard(state);
    }

    @Test
    public void powerCannotDislodgeItsOwnUnit() {
        // Diagram 22: A Par-Bur supported by A Mar against France's own A Bur: attack strength 0 - fails.
        // A Mar's support is valid and uncut, so it counts (in vain)
        place(state, FRANCE, "A Par", "A Mar", "A Bur");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Par-Bur", "A Mar S A Par-Bur", "A Bur H");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Par")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bur")));
        assertEquals(0, totalDislodged(state));
        assertEquals(Set.of(result(state, FRANCE, "A Par-Bur", false), result(state, FRANCE, "A Mar S A Par-Bur", true),
                result(state, FRANCE, "A Bur H", true)), lastResults(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }

    @Test
    public void foreignSupportDoesNotLetAPowerDislodgeItsOwnUnit() {
        // Diagram 23: A Bur-Mar and the Italian A Mar-Bur fight head to head (1 vs defend 1: both fail), so A Bur
        // stays; A Par-Bur, though supported by Germany, attacks France's own unit: strength 0 - fails.
        // A Mar-Bur also meets A Par-Bur's prevent (2). Nothing moves
        place(state, FRANCE, "A Par", "A Bur");
        place(state, GERMANY, "A Ruh");
        place(state, ITALY, "A Mar");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Par-Bur", "A Bur-Mar", "A Ruh S A Par-Bur", "A Mar-Bur");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Par")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bur")));
        assertEquals(army(ITALY), state.getUnit(prov(state, "Mar")));
        assertEquals(0, totalDislodged(state));
        assertEquals(Set.of(result(state, FRANCE, "A Par-Bur", false), result(state, FRANCE, "A Bur-Mar", false),
                result(state, GERMANY, "A Ruh S A Par-Bur", true), result(state, ITALY, "A Mar-Bur", false)),
                lastResults(state));
    }

    @Test
    public void powersSupportDoesNotHelpDislodgeItsOwnUnit() {
        // Diagram 24: A Ruh-Bur with the French A Par's support against the French A Bur: France's support is not
        // counted, so attack 1 vs hold 1 - fails
        place(state, GERMANY, "A Ruh", "A Mun");
        place(state, FRANCE, "A Par", "A Bur");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Ruh-Bur", "A Mun H", "A Par S A Ruh-Bur", "A Bur H");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bur")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ruh")));
        assertEquals(0, totalDislodged(state));
        assertEquals(Set.of(result(state, GERMANY, "A Ruh-Bur", false), result(state, GERMANY, "A Mun H", true),
                result(state, FRANCE, "A Par S A Ruh-Bur", true), result(state, FRANCE, "A Bur H", true)),
                lastResults(state));
    }

    @Test
    public void ownSupportForTheAttackerDoesDislodge() {
        // Diagram 24, last sentence: with Germany's own A Mun supporting, attack 1 + 1 (Mun; Par's French support
        // still not counted) = 2 vs hold 1 - A Bur is dislodged
        place(state, GERMANY, "A Ruh", "A Mun");
        place(state, FRANCE, "A Par", "A Bur");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Ruh-Bur", "A Mun S A Ruh-Bur", "A Par S A Ruh-Bur", "A Bur H");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Bur")));
        assertNull(state.getUnit(prov(state, "Ruh")));
        assertEquals(army(FRANCE), state.getDislodged(prov(state, "Bur")));
        assertEquals(prov(state, "Ruh"), state.getDislodgedFrom(prov(state, "Bur")));
        assertEquals(Set.of(result(state, GERMANY, "A Ruh-Bur", true), result(state, GERMANY, "A Mun S A Ruh-Bur", true),
                result(state, FRANCE, "A Par S A Ruh-Bur", true), result(state, FRANCE, "A Bur H", false)),
                lastResults(state));
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertEquals(FRANCE, state.getCurrentPlayer());
    }

    @Test
    public void foreignSupportedAttackOnOwnUnitFailsAndNothingMoves() {
        // Diagram 25: A Mun-Tyr vs A Tyr-Mun head to head: A Mun-Tyr 1 vs defend 1 fails, so A Mun stays.
        // A Sil-Mun (Austrian support from Boh) attacks Germany's own A Mun: 0 - fails; A Ruh-Mun likewise.
        // A Tyr-Mun: 1 vs defend 1 - fails. Nothing moves
        place(state, GERMANY, "A Mun", "A Ruh", "A Sil");
        place(state, AUSTRIA, "A Tyr", "A Boh");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Mun-Tyr", "A Ruh-Mun", "A Sil-Mun", "A Tyr-Mun", "A Boh S A Sil-Mun");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Mun")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ruh")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Sil")));
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Tyr")));
        assertEquals(0, totalDislodged(state));
        assertEquals(Set.of(result(state, GERMANY, "A Mun-Tyr", false), result(state, GERMANY, "A Ruh-Mun", false),
                result(state, GERMANY, "A Sil-Mun", false), result(state, AUSTRIA, "A Tyr-Mun", false),
                result(state, AUSTRIA, "A Boh S A Sil-Mun", true)), lastResults(state));
    }

    @Test
    public void supportedAttackOnOwnUnitStillStandsOffAnotherAttack() {
        // Diagram 26: F Nth-Den (2, with F Hel) cannot dislodge England's own F Den (attack 0), but its prevent
        // strength 2 stands off the Russian F Ska-Den (attack 2: Den's unit is English, Bal's support Russian).
        // F Den-Kie and A Ber-Kie: 1 vs 1 - both fail. Nothing moves
        place(state, ENGLAND, "F Den", "F Nth", "F Hel");
        place(state, RUSSIA, "A Ber", "F Ska", "F Bal");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F Den-Kie", "F Nth-Den", "F Hel S F Nth-Den", "A Ber-Kie", "F Ska-Den", "F Bal S F Ska-Den");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Den")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Ska")));
        assertNull(state.getUnit(prov(state, "Kie")));
        assertEquals(0, totalDislodged(state));
        assertEquals(Set.of(result(state, ENGLAND, "F Den-Kie", false), result(state, ENGLAND, "F Nth-Den", false),
                result(state, ENGLAND, "F Hel S F Nth-Den", true), result(state, RUSSIA, "A Ber-Kie", false),
                result(state, RUSSIA, "F Ska-Den", false), result(state, RUSSIA, "F Bal S F Ska-Den", true)),
                lastResults(state));
    }

    @Test
    public void foreignSupportBreaksASelfStandoff() {
        // Diagram 27: A Ser-Bud with the Russian A Gal's support: 2 vs A Vie-Bud's prevent 1 (Bud empty) - succeeds
        place(state, AUSTRIA, "A Ser", "A Vie");
        place(state, RUSSIA, "A Gal");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Ser-Bud", "A Vie-Bud", "A Gal S A Ser-Bud");
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Bud")));
        assertNull(state.getUnit(prov(state, "Ser")));
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Vie")));
        assertEquals(Set.of(result(state, AUSTRIA, "A Ser-Bud", true), result(state, AUSTRIA, "A Vie-Bud", false),
                result(state, RUSSIA, "A Gal S A Ser-Bud", true)), lastResults(state));
    }

    @Test
    public void foreignSupportCannotDislodgeTheSupportedPowersOwnUnit() {
        // p.14 on Diagram 27: with an Austrian army already in Bud, A Ser-Bud attacks its own unit: 0 - fails
        place(state, AUSTRIA, "A Bud", "A Ser", "A Vie");
        place(state, RUSSIA, "A Gal");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Bud H", "A Ser-Bud", "A Vie-Bud", "A Gal S A Ser-Bud");
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Bud")));
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Ser")));
        assertEquals(0, totalDislodged(state));
        assertEquals(Set.of(result(state, AUSTRIA, "A Bud H", true), result(state, AUSTRIA, "A Ser-Bud", false),
                result(state, AUSTRIA, "A Vie-Bud", false), result(state, RUSSIA, "A Gal S A Ser-Bud", true)),
                lastResults(state));
    }
}
