package games.perudo;

import games.perudo.actions.Bid;
import games.perudo.actions.Calza;
import games.perudo.actions.Dudo;
import org.junit.Test;

import static games.perudo.PerudoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Setup, copy / equals / hashCode, redeterminisation and the text of the actions.
 */
public class PerudoStateTest {

    @Test
    public void everyPlayerStartsWithNDicePerPlayerAndNoBid() {
        // default 5 dice each: 4 players hold 4 * 5 = 20
        PerudoGameState state = newState(4, 11);
        for (int p = 0; p < 4; p++) {
            assertEquals(5, state.getNDice(p));
            assertEquals(6, state.getDice(p).length);
            for (int f = 1; f <= 6; f++)
                assertTrue(state.getDiceShowing(p, f) >= 0);
            assertEquals(-1, state.getEliminationRound(p));
        }
        assertEquals(20, state.getTotalDice());
        assertFalse(state.hasBid());
        assertEquals(0, state.getBidFace());
        assertEquals(-1, state.getBidder());
        assertEquals(-1, state.getNextRoundStarter());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void nDicePerPlayerSetsTheStartingDice() {
        // 3 dice each: 3 players hold 3 * 3 = 9
        PerudoParameters params = new PerudoParameters();
        params.setParameterValue("nDicePerPlayer", 3);
        params.setRandomSeed(11);
        PerudoGameState state = newState(params, 3);
        for (int p = 0; p < 3; p++)
            assertEquals(3, state.getNDice(p));
        assertEquals(9, state.getTotalDice());
    }

    @Test
    public void copyEqualsTheOriginalAndIsIndependentOfIt() {
        PerudoGameState state = newState(3, 5);
        setAllDice(state, faces(1, 1, 1, 1, 1, 0), faces(0, 2, 0, 3, 0, 0), faces(5, 0, 0, 0, 0, 0));
        state.setBid(4, 3, 2);
        PerudoGameState copy = (PerudoGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        copy.setBid(5, 6, 0);
        copy.setDice(1, faces(0, 0, 0, 0, 0, 5));
        copy.removeDie(2);
        copy.setEliminationRound(0, 3);
        copy.setNextRoundStarter(1);
        assertNotEquals(state, copy);
        assertEquals(4, state.getBidQuantity());
        assertEquals(3, state.getBidFace());
        assertEquals(2, state.getBidder());
        assertArrayEquals(faces(0, 2, 0, 3, 0, 0), state.getDice(1));
        assertEquals(5, state.getNDice(2));
        assertEquals(-1, state.getEliminationRound(0));
        assertEquals(-1, state.getNextRoundStarter());
    }

    @Test
    public void setupWithPalificoOnStartsWithANormalRoundAndNoPalificoHad() {
        PerudoParameters params = palificoParams();
        params.setRandomSeed(11);
        PerudoGameState state = newState(params, 4);
        assertFalse(state.isPalificoRound());
        for (int p = 0; p < 4; p++)
            assertFalse(state.hasHadPalifico(p));
    }

    @Test
    public void copyCarriesThePalificoStateAndEqualsDependsOnIt() {
        PerudoGameState state = newState(3, 5);
        state.setPalificoRound(true);
        state.setPalificoUsed(1, true);
        PerudoGameState copy = (PerudoGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertTrue(copy.isPalificoRound());
        assertTrue(copy.hasHadPalifico(1));
        assertFalse(copy.hasHadPalifico(0));

        copy.setPalificoRound(false);
        assertNotEquals(state, copy);
        assertNotEquals(state.hashCode(), copy.hashCode());
        assertTrue(state.isPalificoRound());

        PerudoGameState copy2 = (PerudoGameState) state.copy();
        copy2.setPalificoUsed(2, true);
        assertNotEquals(state, copy2);
        assertNotEquals(state.hashCode(), copy2.hashCode());
        assertFalse(state.hasHadPalifico(2));
        // the redeterminised copy keeps the public Palifico state too
        PerudoGameState copy3 = (PerudoGameState) state.copy(0);
        assertTrue(copy3.isPalificoRound());
        assertTrue(copy3.hasHadPalifico(1));
    }

    @Test
    public void redeterminiseKeepsOwnDiceAndCountsAndRerollsTheOthers() {
        PerudoGameState state = newState(3, 5);
        // player 2 has lost two dice; the bid is public
        setAllDice(state, faces(1, 1, 1, 1, 1, 0), faces(0, 2, 0, 3, 0, 0), faces(3, 0, 0, 0, 0, 0));
        state.setBid(4, 3, 2);
        boolean[] changed = new boolean[3];
        // A re-roll of 5 dice (or 3) repeats one fixed set of counts with chance at most 5!/6^5 = 0.016 (3!/6^3 = 0.028),
        // so 30 copies all repeat it with chance below 0.03^30
        for (int i = 0; i < 30; i++) {
            PerudoGameState copy = (PerudoGameState) state.copy(0);
            assertArrayEquals(faces(1, 1, 1, 1, 1, 0), copy.getDice(0));
            assertEquals(5, copy.getNDice(1));
            assertEquals(3, copy.getNDice(2));
            assertEquals(4, copy.getBidQuantity());
            assertEquals(3, copy.getBidFace());
            assertEquals(2, copy.getBidder());
            for (int p = 1; p < 3; p++)
                if (!java.util.Arrays.equals(state.getDice(p), copy.getDice(p))) changed[p] = true;
        }
        assertTrue("player 1's dice never re-rolled", changed[1]);
        assertTrue("player 2's dice never re-rolled", changed[2]);
    }

    @Test
    public void bidAndDudoHideNothingInTheirText() {
        PerudoGameState state = newState(2, 5);
        Bid bid = new Bid(3, 4);
        assertEquals(bid.toString(), bid.getString(state));
        assertTrue(bid.getString(state).contains("3"));
        assertTrue(bid.getString(state).contains("4"));
        assertEquals(new Dudo().toString(), new Dudo().getString(state));
        assertEquals(new Calza().toString(), new Calza().getString(state));
        assertEquals("Calza", new Calza().getString(state));
    }

    @Test
    public void calzaActionsAreEqualByValueAndDifferFromDudo() {
        assertEquals(new Calza(), new Calza());
        assertEquals(new Calza().hashCode(), new Calza().hashCode());
        assertNotEquals(new Calza(), new Dudo());
        assertNotEquals(new Dudo(), new Calza());
        assertNotEquals(new Calza().hashCode(), new Dudo().hashCode());
    }
}
