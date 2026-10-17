package games.schwimmen;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.schwimmen.SchwimmenTestUtils.*;
import static org.junit.Assert.*;

/**
 * The chips game (livesGame = true), unit level: the starting chips, who loses a chip at the end of a deal
 * (dealLosers on arranged end-of-deal hands), the dropped-out encoding, the score and places by chips, and the
 * chips in copy / equals. Three players unless stated; the dealer is the last player.
 */
public class SchwimmenChipsTest {

    SchwimmenForwardModel fm;

    @Before
    public void setup() {
        fm = new SchwimmenForwardModel();
    }

    private SchwimmenGameState livesState(int nPlayers) {
        return newState(livesParams(), nPlayers, ordinarySeed(nPlayers, 0), fm);
    }

    /** A 3-player lives state at the end of a deal with these hands and table, 3 chips each. */
    private SchwimmenGameState endOfDeal(String[] table, String[]... hands) {
        SchwimmenGameState state = livesState(hands.length);
        arrangePlay(state, 0, table, hands);
        int[] chips = new int[hands.length];
        java.util.Arrays.fill(chips, 3);
        setChips(state, chips);
        return state;
    }

    // ---- setup ----

    @Test
    public void everyPlayerStartsWithThreeChipsAndIsInTheGame() {
        SchwimmenGameState state = livesState(4);
        for (int p = 0; p < 4; p++) {
            assertEquals("player " + p, 3, state.getChips(p));
            assertTrue(state.isInGame(p));
        }
    }

    @Test
    public void startingChipsSetsTheChipsEachPlayerStartsWith() {
        SchwimmenParameters params = livesParams();
        params.setParameterValue("startingChips", 5);
        SchwimmenGameState state = newState(params, 4, ordinarySeed(4, 0), fm);
        for (int p = 0; p < 4; p++)
            assertEquals("player " + p, 5, state.getChips(p));
    }

    @Test
    public void aSwimmingPlayerIsStillInAndADroppedOutPlayerIsNot() {
        SchwimmenGameState state = livesState(3);
        // player 1 dropped out when 2 players were left in: -1 - 2 = -3; player 2 is swimming (0 chips)
        setChips(state, 2, -3, 0);
        assertTrue(state.isInGame(0));
        assertFalse(state.isInGame(1));
        assertTrue("swimming (0 chips) is still in", state.isInGame(2));
    }

    // ---- who loses a chip ----

    @Test
    public void theWorstHandLosesAChip() {
        // 21 hearts, 10 spades, 20 diamonds: player 1 is worst
        SchwimmenGameState state = endOfDeal(h("7H", "9H", "KS"),
                h("AH", "KH", "7C"), h("8C", "9D", "JS"), h("QD", "10D", "8S"));
        assertEquals(List.of(1), fm.dealLosers(state));
    }

    @Test
    public void aLowerValueLosesWhateverItsSuit() {
        // 20 clubs (the highest suit) against 21 diamonds and 21 hearts: the 20 loses
        SchwimmenGameState state = endOfDeal(h("7C", "8C", "9C"),
                h("KC", "QC", "7H"), h("AD", "KD", "7S"), h("AH", "QH", "8S"));
        assertEquals(List.of(0), fm.dealLosers(state));
    }

    @Test
    public void anEqualTotalInALowerSuitLoses() {
        // 10 hearts (J) against 10 diamonds (Q); player 2 has 11 + 10 = 21 spades
        SchwimmenGameState state = endOfDeal(h("8H", "9H", "KC"),
                h("JH", "7C", "8S"), h("QD", "9C", "7H"), h("AS", "KS", "7D"));
        assertEquals("diamonds are below hearts", List.of(1), fm.dealLosers(state));
    }

    @Test
    public void aBestTotalReachedInTwoSuitsCountsAsTheHigherSuit() {
        // player 0: 10 in hearts and 10 in spades - counts as spades; player 1: 10 in hearts only
        // player 2: 11 + 10 = 21 clubs
        SchwimmenGameState state = endOfDeal(h("JD", "9H", "8S"),
                h("10H", "10S", "7D"), h("QH", "7C", "8D"), h("AC", "KC", "9S"));
        assertEquals("spades beat hearts, so player 1 is worst", List.of(1), fm.dealLosers(state));
    }

    @Test
    public void equalWorstTotalsInTheSameSuitBothLose() {
        // 10 hearts (K) and 10 hearts (J); player 2 has 21 clubs
        SchwimmenGameState state = endOfDeal(h("QS", "8C", "7D"),
                h("KH", "7C", "8D"), h("JH", "7S", "9D"), h("AC", "KC", "9S"));
        assertEquals(List.of(0, 1), fm.dealLosers(state));
    }

    @Test
    public void theLowerThreeOfAKindLoses() {
        // 999, 777 and KKK are all 30.5: the Sevens are the lowest
        SchwimmenGameState state = endOfDeal(h("AH", "AD", "10C"),
                h("9H", "9D", "9C"), h("7H", "7D", "7C"), h("KH", "KD", "KC"));
        assertEquals(List.of(1), fm.dealLosers(state));
    }

    @Test
    public void onlyPlayersStillInAreRankedForTheWorstHand() {
        // 4 players; player 1 dropped out (with 3 left in: -1 - 3 = -4) and holds no cards - an empty hand would be
        // worst of all if it counted. In: 21 hearts, 10 spades, 20 diamonds - player 2 is worst.
        SchwimmenGameState state = livesState(4);
        arrangePlay(state, 0, h("7H", "9H", "KS"),
                h("AH", "KH", "7C"), h(), h("8C", "9D", "JS"), h("QD", "10D", "8S"));
        setChips(state, 3, -4, 3, 3);
        assertEquals(List.of(2), fm.dealLosers(state));
    }

    @Test
    public void feuerCostsEveryOtherPlayerStillInAChip() {
        // 4 players, player 1 out. Player 2 holds three Aces (Feuer); player 0 holds 31 (Schnauz) and still loses,
        // as does player 3 - but not the player who is out
        SchwimmenGameState state = livesState(4);
        arrangePlay(state, 0, h("10D", "JD", "QD"),
                h("AS", "KS", "QS"), h(), h("AH", "AD", "AC"), h("7H", "8H", "9H"));
        setChips(state, 3, -4, 3, 3);
        assertEquals(List.of(0, 3), fm.dealLosers(state));
    }

    // ---- score and places ----

    @Test
    public void inTheChipsGameTheScoreIsTheChipsAndEqualChipsSharePlaces() {
        // 4 players: chips 2, out (-4), 2, 0. Players 0 and 2 hold different hands (21 and 10) but share first place:
        // no tie-break by hand in the chips game. Player 3 (0 chips) is third, player 1 (out) last.
        SchwimmenGameState state = livesState(4);
        arrangePlay(state, 0, h("7H", "9H", "KS"),
                h("AH", "KH", "7C"), h(), h("8C", "9D", "JS"), h("QD", "10D", "8S"));
        setChips(state, 2, -4, 2, 0);
        assertEquals(2, state.getGameScore(0), 1e-9);
        assertEquals(-4, state.getGameScore(1), 1e-9);
        assertEquals(2, state.getGameScore(2), 1e-9);
        assertEquals(0, state.getGameScore(3), 1e-9);
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(1, state.getOrdinalPosition(2));
        assertEquals(3, state.getOrdinalPosition(3));
        assertEquals(4, state.getOrdinalPosition(1));
    }

    @Test
    public void playersWhoWentOutLaterRankHigher() {
        // 4 players, player 0 still in with 1 chip. Player 1 went out first (3 left in: -1 - 3 = -4), then player 3
        // (2 left in: -3), then player 2 (1 left in: -2)
        SchwimmenGameState state = livesState(4);
        arrangePlay(state, 0, h("7H", "9H", "KS"), h("AH", "KH", "7C"), h(), h(), h());
        setChips(state, 1, -4, -2, -3);
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(2));
        assertEquals(3, state.getOrdinalPosition(3));
        assertEquals(4, state.getOrdinalPosition(1));
    }

    // ---- copy / equals ----

    @Test
    public void aCopyHasTheSameChipsAndChangingThemMakesItDifferent() {
        SchwimmenGameState state = livesState(3);
        setChips(state, 3, 1, 0);
        SchwimmenGameState copy = (SchwimmenGameState) state.copy();
        for (int p = 0; p < 3; p++)
            assertEquals("player " + p, state.getChips(p), copy.getChips(p));
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        copy.chips[1] = 2;
        assertEquals("the original is independent of the copy", 1, state.getChips(1));
        assertNotEquals(state, copy);
        assertNotEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void aPlayersViewKeepsEveryonesChips() {
        // chips are public
        SchwimmenGameState state = livesState(3);
        setChips(state, 3, -3, 0);
        SchwimmenGameState view = (SchwimmenGameState) state.copy(0);
        assertEquals(3, view.getChips(0));
        assertEquals(-3, view.getChips(1));
        assertEquals(0, view.getChips(2));
    }
}
