package games.golfsix;

import org.junit.Test;

import static games.golfsix.GolfSixTestUtils.cards;
import static org.junit.Assert.assertEquals;

/**
 * GolfSixUtils.gridScore on grids listed by position (0-2 top row, 3-5 bottom row; column c is c and c+3).
 * Card values: A 1, 2 -2, 3-10 face value, J and Q 10, K 0; a column of two cards of the same rank scores 0.
 */
public class GolfSixScoringTest {

    final GolfSixParameters params = new GolfSixParameters();

    private int score(String... codes) {
        return GolfSixUtils.gridScore(cards(codes), params);
    }

    @Test
    public void unpairedCardsScoreTheirValues() {
        // columns A/10, 2/J, 5/Q: (1 + 10) + (-2 + 10) + (5 + 10) = 34
        assertEquals(34, score("AH", "2S", "5C", "10D", "JH", "QS"));
        // columns K/9, 7/4, 3/8: (0 + 9) + (7 + 4) + (3 + 8) = 31
        assertEquals(31, score("KH", "7S", "3C", "9D", "4H", "8S"));
        // an Ace is low: columns A/K, 2/3, 6/7: (1 + 0) + (-2 + 3) + (6 + 7) = 15
        assertEquals(15, score("AS", "2C", "6D", "KD", "3H", "7C"));
    }

    @Test
    public void aColumnPairScoresNothing() {
        // columns 7/7 (pair), 2/3, 9/10: 0 + (-2 + 3) + (9 + 10) = 20
        assertEquals(20, score("7H", "2S", "9C", "7D", "3H", "10S"));
        // the same with the 7D swapped for an 8D, breaking the pair: (7 + 8) + 1 + 19 = 35
        assertEquals(35, score("7H", "2S", "9C", "8D", "3H", "10S"));
        // a pair in the same row is not a pair: columns 7/3, 7/8, 9/10: 10 + 15 + 19 = 44
        assertEquals(44, score("7H", "7D", "9C", "3H", "8D", "10S"));
    }

    @Test
    public void pairsOfTwosAndOfKingsAlsoScoreNothing() {
        // columns 5/5, 2/2, K/K: all pairs; the twos would otherwise score -4
        assertEquals(0, score("5H", "2S", "KC", "5D", "2H", "KD"));
        // columns 2/2 (pair), 4/K, A/6: 0 + (4 + 0) + (1 + 6) = 11
        assertEquals(11, score("2C", "4S", "AC", "2D", "KS", "6H"));
    }

    @Test
    public void aJackAndAQueenAreNotAPair() {
        // columns J/Q, Q/J, A/A (pair): 20 + 20 + 0 = 40
        assertEquals(40, score("JH", "QS", "AC", "QH", "JS", "AD"));
        // columns J/J (pair), Q/Q (pair), 10/10 (pair): 0
        assertEquals(0, score("JH", "QS", "10C", "JD", "QH", "10D"));
    }
}
