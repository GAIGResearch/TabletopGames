package games.schwimmen;

import core.CoreConstants.GameResult;
import games.schwimmen.actions.Pass;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.schwimmen.SchwimmenTestUtils.*;
import static org.junit.Assert.*;

/**
 * Tie-breaks between hands of equal value (pagat), checked through getOrdinalPosition and the results when the deal
 * ends (every player passes with 2 cards left in the draw deck).
 */
public class SchwimmenTiebreakTest {

    SchwimmenForwardModel fm = new SchwimmenForwardModel();

    private SchwimmenGameState arranged(int n, String[] table, String[]... hands) {
        SchwimmenGameState state = newState(n, ordinarySeed(n, 0), fm);
        arrangePlay(state, 0, table, hands);
        return state;
    }

    /** Every player passes with 2 cards left in the draw deck: the deal (the game) ends. */
    private void endByPasses(SchwimmenGameState state) {
        leaveInDrawDeck(state, 2);
        for (int i = 0; i < state.getNPlayers(); i++)
            takeTurn(state, fm, new Pass());
        assertFalse(state.isNotTerminal());
    }

    private void assertOrdinals(SchwimmenGameState state, int... ordinals) {
        for (int p = 0; p < ordinals.length; p++)
            assertEquals("ordinal of player " + p, ordinals[p], state.getOrdinalPosition(p));
    }

    private void assertResults(SchwimmenGameState state, GameResult... results) {
        assertArrayEquals(results, state.getPlayerResults());
    }

    @Test
    public void equalTotalsRankClubsThenSpadesThenHeartsThenDiamonds() {
        // every hand 10 + 10 = 20 in one suit, with a Seven of another suit
        SchwimmenGameState state = arranged(4, h("9H", "9S", "9D"),
                h("KD", "10D", "7C"),     // 20 in Diamonds
                h("KH", "10H", "7S"),     // 20 in Hearts
                h("KS", "10S", "7H"),     // 20 in Spades
                h("KC", "10C", "7D"));    // 20 in Clubs
        assertOrdinals(state, 4, 3, 2, 1);
        endByPasses(state);
        assertResults(state, LOSE_GAME, LOSE_GAME, LOSE_GAME, WIN_GAME);
    }

    @Test
    public void thirtyInClubsBeatsThirtyInSpades() {
        // pagat: K Q J of Clubs (30) beats K Q J of Spades (30); player 2 has 9 + 7 = 16 in Hearts
        SchwimmenGameState state = arranged(3, h("8D", "9D", "10D"),
                h("KS", "QS", "JS"), h("KC", "QC", "JC"), h("9H", "7H", "8C"));
        endByPasses(state);
        assertResults(state, LOSE_GAME, WIN_GAME, LOSE_GAME);
    }

    @Test
    public void aBestTotalReachedInTwoSuitsCountsAsTheHigherSuit() {
        SchwimmenGameState state = arranged(4, h("9C", "9S", "9H"),
                h("KC", "QH", "8D"),      // 10 in Clubs and 10 in Hearts: counts as Clubs
                h("KH", "9D", "8S"),      // 10 in Hearts (9 Diamonds, 8 Spades)
                h("QD", "7S", "7H"),      // 10 in Diamonds
                h("QS", "JD", "7C"));     // 10 in Spades and 10 in Diamonds: counts as Spades
        // Clubs (p0) > Spades (p3) > Hearts (p1) > Diamonds (p2)
        assertOrdinals(state, 1, 3, 4, 2);
        endByPasses(state);
        assertResults(state, WIN_GAME, LOSE_GAME, LOSE_GAME, LOSE_GAME);
    }

    @Test
    public void aHigherThreeOfAKindBeatsALowerOneWhateverTheSuits() {
        // all 30.5; by rank K > Q > J > 10 > 7. The suits would say otherwise: the Kings have no Club, the others do
        // (except the Sevens), so a suit tie-break would put the Kings behind the Queens, Jacks and Tens.
        SchwimmenGameState state = arranged(5, h("8H", "8D", "8S"),
                h("7H", "7D", "7S"),
                h("10C", "10S", "10H"),
                h("JC", "JS", "JD"),
                h("QC", "QH", "QD"),
                h("KH", "KD", "KS"));
        assertOrdinals(state, 5, 4, 3, 2, 1);
        endByPasses(state);
        assertResults(state, LOSE_GAME, LOSE_GAME, LOSE_GAME, LOSE_GAME, WIN_GAME);
    }

    @Test
    public void theSameTotalInTheSameSuitSharesFirstPlace() {
        SchwimmenGameState state = arranged(3, h("7D", "8D", "8S"),
                h("AH", "9H", "7C"),      // 11 + 9 = 20 in Hearts
                h("KH", "10H", "7S"),     // 10 + 10 = 20 in Hearts
                h("QD", "JD", "8C"));     // 10 + 10 = 20 in Diamonds: the lower suit
        assertOrdinals(state, 1, 1, 3);
        endByPasses(state);
        assertResults(state, DRAW_GAME, DRAW_GAME, LOSE_GAME);
    }

    @Test
    public void aHigherValueBeatsAHigherSuit() {
        // 19 in Clubs (K + 9) loses to 20 in Diamonds (K + 10): the tie-break only splits equal values
        SchwimmenGameState state = arranged(3, h("7H", "8H", "8S"),
                h("KC", "9C", "7D"), h("KD", "10D", "7S"), h("QH", "8C", "7C"));
        assertOrdinals(state, 2, 1, 3);
        endByPasses(state);
        assertResults(state, LOSE_GAME, WIN_GAME, LOSE_GAME);
    }
}
