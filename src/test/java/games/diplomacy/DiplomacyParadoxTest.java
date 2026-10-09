package games.diplomacy;

import org.junit.Test;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Convoy paradoxes (DATC issue 4.A.2, section 5.B.9) under each DiplomacyParameters.paradoxRule.
 * <p>
 * Of 6.F.14-6.F.24 only 6.F.17 gives a different result under the two settings. In 6.F.14, 6.F.15, 6.F.16 and
 * 6.G.11 rule 21 and Szykman both leave the support uncut and the army stopped. 6.F.19-21 are not paradoxes, as
 * rule 21 does not apply (the fleet attacked is not necessary, or the support is not of an attack on a convoying
 * fleet). In 6.F.18 and 6.F.22-24 rule 21 does not apply, so the default falls back on Szykman. The SZYKMAN
 * versions of 6.F.14, 6.F.16 and 6.G.11 check that the paradox is detected without rule 21's help.
 * <p>
 * Spring 1901 orders on a cleared board; a convoy order "ok" means its fleet was not dislodged (also when the
 * Szykman rule made it carry nothing); a support "ok" means it was not cut.
 */
public class DiplomacyParadoxTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm = new DiplomacyForwardModel();

    private void board(DiplomacyParadoxRule rule) {
        DiplomacyParameters params = helpingAnyUnit();
        params.setParameterValue("paradoxRule", rule);
        state = newState(params);
        clearBoard(state);
    }

    private void start() {
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
    }

    // ---------------------------------------------------------------- the parameter

    @Test
    public void paradoxRuleDefaultsToTheRulebookAndSurvivesCopy() {
        DiplomacyParameters params = new DiplomacyParameters();
        assertEquals(DiplomacyParadoxRule.RULEBOOK_2000, params.paradoxRule);
        assertEquals(DiplomacyParadoxRule.RULEBOOK_2000, params.getParameterValue("paradoxRule"));
        assertEquals(DiplomacyParadoxRule.RULEBOOK_2000, ((DiplomacyParameters) params.copy()).paradoxRule);

        params.setParameterValue("paradoxRule", DiplomacyParadoxRule.SZYKMAN);
        assertEquals(DiplomacyParadoxRule.SZYKMAN, params.paradoxRule);
        DiplomacyParameters copy = (DiplomacyParameters) params.copy();
        assertEquals(DiplomacyParadoxRule.SZYKMAN, copy.paradoxRule);
        assertEquals(DiplomacyParadoxRule.SZYKMAN, copy.getParameterValue("paradoxRule"));

        // and a copy of a game state keeps it
        DiplomacyGameState s = newState(params);
        DiplomacyGameState sc = (DiplomacyGameState) s.copy();
        assertEquals(DiplomacyParadoxRule.SZYKMAN, ((DiplomacyParameters) sc.getGameParameters()).paradoxRule);
    }

    // ---------------------------------------------------------------- 6.F.17: the settings differ

    private void pandinsExtendedParadox() {
        place(state, ENGLAND, "F Lon", "F Wal");
        place(state, FRANCE, "A Bre", "F Eng", "F Yor");
        place(state, GERMANY, "F Nth", "F Bel");
        start();
    }

    @Test
    public void f17PandinsExtendedParadoxUnderTheRulebook() {
        // rule 21: A Bre-Lon does not cut F Lon's support of F Wal-Eng, neither by attack nor by dislodging it.
        // F Wal-Eng 2 and F Bel-Eng 2 (F Nth) stand off, F Eng survives and the convoy carries A Bre-Lon: 2 (F Yor)
        // vs F Lon's hold 1 - F Lon is dislodged. It has no retreat (Wal, Yor, Nth, Eng all occupied), so it is
        // disbanded at once and there is no retreat phase
        board(DiplomacyParadoxRule.RULEBOOK_2000);
        pandinsExtendedParadox();
        playExpecting(state, fm, "F Lon S F Wal-Eng: ok", "F Wal-Eng: fails", "A Bre-Lon via convoy: ok",
                "F Eng C A Bre-Lon: ok", "F Yor S A Bre-Lon: ok", "F Nth S F Bel-Eng: ok", "F Bel-Eng: fails");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Lon")));
        assertNull(state.getUnit(prov(state, "Bre")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Eng")));
        assertNull(state.getDislodged(prov(state, "Lon")));
        assertEquals(6, totalUnits(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }

    @Test
    public void f17PandinsExtendedParadoxUnderSzykman() {
        // without rule 21 there is no consistent resolution: if the army arrives it dislodges F Lon (2 vs 1), the
        // support is cut, F Bel-Eng 2 beats F Wal-Eng 1 and dislodges F Eng - so the army does not arrive; if it
        // does not, the support stands, 2 vs 2, F Eng survives - so it arrives. A cycle with a Convoy order:
        // Szykman - F Eng carries nothing, A Bre-Lon fails, F Lon's support stands, F Wal-Eng and F Bel-Eng
        // (2 each) stand off and nothing is dislodged
        board(DiplomacyParadoxRule.SZYKMAN);
        pandinsExtendedParadox();
        playExpecting(state, fm, "F Lon S F Wal-Eng: ok", "F Wal-Eng: fails", "A Bre-Lon via convoy: fails",
                "F Eng C A Bre-Lon: ok", "F Yor S A Bre-Lon: ok", "F Nth S F Bel-Eng: ok", "F Bel-Eng: fails");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Lon")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bre")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Eng")));
        assertEquals(0, totalDislodged(state));
        assertEquals(7, totalUnits(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }

    // ---------------------------------------------------------------- the default where the settings agree

    @Test
    public void f18BetrayalParadox() {
        // rule 21 does not apply (F Bel supports F Nth's hold, not an attack on it). No consistent resolution:
        // if A Lon-Bel arrives it cuts F Bel's support, F Ska-Nth 2 (F Hel) beats F Nth's hold 1 and the convoy
        // fails; if it does not, F Nth holds with 2 and the convoy works. Szykman: F Nth carries nothing,
        // A Lon-Bel fails, F Bel's support stands, F Ska-Nth 2 vs 2 fails
        board(DiplomacyParadoxRule.RULEBOOK_2000);
        place(state, ENGLAND, "F Nth", "A Lon", "F Eng");
        place(state, FRANCE, "F Bel");
        place(state, GERMANY, "F Hel", "F Ska");
        start();
        playExpecting(state, fm, "F Nth C A Lon-Bel: ok", "A Lon-Bel via convoy: fails", "F Eng S A Lon-Bel: ok",
                "F Bel S F Nth: ok", "F Hel S F Ska-Nth: ok", "F Ska-Nth: fails");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Lon")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Bel")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void f22SecondOrderParadoxWithTwoResolutions() {
        // rule 21 does not apply: F Lon supports an attack on F Nth (not A Bre's fleet), F Bel one on F Eng (not
        // A Nwy's). Two resolutions (both supports cut and both fleets survive, or neither). Szykman: F Eng and
        // F Nth carry nothing, both armies fail, the supports stand: F Pic-Eng 2 dislodges F Eng and F Edi-Nth 2
        // dislodges F Nth. F Eng may retreat to Iri, Mid or Wal; F Nth to Ska, Hel, Den, Hol, Yor or Nrg
        board(DiplomacyParadoxRule.RULEBOOK_2000);
        place(state, ENGLAND, "F Edi", "F Lon");
        place(state, FRANCE, "A Bre", "F Eng");
        place(state, GERMANY, "F Bel", "F Pic");
        place(state, RUSSIA, "A Nwy", "F Nth");
        start();
        playExpecting(state, fm, "F Edi-Nth: ok", "F Lon S F Edi-Nth: ok", "A Bre-Lon via convoy: fails",
                "F Eng C A Bre-Lon: fails", "F Bel S F Pic-Eng: ok", "F Pic-Eng: ok",
                "A Nwy-Bel via convoy: fails", "F Nth C A Nwy-Bel: fails");
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Eng")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
        assertEquals(fleet(FRANCE), state.getDislodged(prov(state, "Eng")));
        assertEquals(fleet(RUSSIA), state.getDislodged(prov(state, "Nth")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bre")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Nwy")));
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
    }

    @Test
    public void f23SecondOrderParadoxWithTwoExclusiveConvoys() {
        // rule 21 does not apply (F Bel and F Lon support holds). Two resolutions (one convoy dislodged, the other
        // succeeding, either way round). Szykman: both armies fail, the supports stand, the convoying fleets hold
        // with support: F Mid-Eng 2 (F Iri) vs F Eng 2 (F Bel), F Edi-Nth 2 (F Yor) vs F Nth 2 (F Lon) - nothing
        // moves
        board(DiplomacyParadoxRule.RULEBOOK_2000);
        place(state, ENGLAND, "F Edi", "F Yor");
        place(state, FRANCE, "A Bre", "F Eng");
        place(state, GERMANY, "F Bel", "F Lon");
        place(state, ITALY, "F Mid", "F Iri");
        place(state, RUSSIA, "A Nwy", "F Nth");
        start();
        playExpecting(state, fm, "F Edi-Nth: fails", "F Yor S F Edi-Nth: ok", "A Bre-Lon via convoy: fails",
                "F Eng C A Bre-Lon: ok", "F Bel S F Eng: ok", "F Lon S F Nth: ok", "F Mid-Eng: fails",
                "F Iri S F Mid-Eng: ok", "A Nwy-Bel via convoy: fails", "F Nth C A Nwy-Bel: ok");
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Eng")));
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Nth")));
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Lon")));
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Bel")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void f24SecondOrderParadoxWithNoResolution() {
        // rule 21 does not apply. No consistent resolution. Szykman: both armies fail, the supports stand:
        // F Iri-Eng 2 (F Mid) vs F Eng 2 (F Bel) fails; F Edi-Nth 2 (F Lon) vs F Nth 1 dislodges F Nth (which may
        // retreat to Ska, Hel, Den, Hol, Yor or Nrg)
        board(DiplomacyParadoxRule.RULEBOOK_2000);
        place(state, ENGLAND, "F Edi", "F Lon", "F Iri", "F Mid");
        place(state, FRANCE, "A Bre", "F Eng", "F Bel");
        place(state, RUSSIA, "A Nwy", "F Nth");
        start();
        playExpecting(state, fm, "F Edi-Nth: ok", "F Lon S F Edi-Nth: ok", "F Iri-Eng: fails",
                "F Mid S F Iri-Eng: ok", "A Bre-Lon via convoy: fails", "F Eng C A Bre-Lon: ok",
                "F Bel S F Eng: ok", "A Nwy-Bel via convoy: fails", "F Nth C A Nwy-Bel: fails");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
        assertEquals(fleet(RUSSIA), state.getDislodged(prov(state, "Nth")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Eng")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Iri")));
        assertEquals(1, totalDislodged(state));
    }

    // ---------------------------------------------------------------- SZYKMAN where rule 21 decides the default

    @Test
    public void f14SimpleConvoyParadoxUnderSzykman() {
        // two resolutions without rule 21 (the support cut and F Eng surviving, or neither): Szykman - A Bre-Lon
        // fails, F Lon's support stands, F Wal-Eng 2 vs 1 dislodges F Eng (same result as the rulebook)
        board(DiplomacyParadoxRule.SZYKMAN);
        place(state, ENGLAND, "F Lon", "F Wal");
        place(state, FRANCE, "A Bre", "F Eng");
        start();
        playExpecting(state, fm, "F Lon S F Wal-Eng: ok", "F Wal-Eng: ok", "A Bre-Lon via convoy: fails",
                "F Eng C A Bre-Lon: fails");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Eng")));
        assertEquals(fleet(FRANCE), state.getDislodged(prov(state, "Eng")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bre")));
    }

    @Test
    public void f16PandinsParadoxUnderSzykman() {
        // no resolution without rule 21: Szykman - the army fails, F Lon's support stands, F Wal-Eng 2 and
        // F Bel-Eng 2 stand off, F Eng survives
        board(DiplomacyParadoxRule.SZYKMAN);
        place(state, ENGLAND, "F Lon", "F Wal");
        place(state, FRANCE, "A Bre", "F Eng");
        place(state, GERMANY, "F Nth", "F Bel");
        start();
        playExpecting(state, fm, "F Lon S F Wal-Eng: ok", "F Wal-Eng: fails", "A Bre-Lon via convoy: fails",
                "F Eng C A Bre-Lon: ok", "F Nth S F Bel-Eng: ok", "F Bel-Eng: fails");
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Eng")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Lon")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void g11ConvoyToAdjacentProvinceWithAParadoxUnderSzykman() {
        // A Swe-Nwy is convoyed (own F Ska). Without rule 21 two resolutions: the army arrives (2 vs 1, F Nwy
        // dislodged, support cut, F Nth-Ska 1 vs 1 fails) or it does not (support stands, F Ska dislodged).
        // Szykman: F Ska carries nothing, the army fails, F Nth-Ska 2 dislodges F Ska (it may retreat to Den)
        board(DiplomacyParadoxRule.SZYKMAN);
        place(state, ENGLAND, "F Nwy", "F Nth");
        place(state, RUSSIA, "A Swe", "F Ska", "F Bar");
        start();
        playExpecting(state, fm, "F Nwy S F Nth-Ska: ok", "F Nth-Ska: ok", "A Swe-Nwy: fails",
                "F Ska C A Swe-Nwy: fails", "F Bar S A Swe-Nwy: ok");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nwy")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Ska")));
        assertEquals(fleet(RUSSIA), state.getDislodged(prov(state, "Ska")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Swe")));
    }
}
