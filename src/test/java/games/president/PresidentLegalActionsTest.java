package games.president;

import core.actions.AbstractAction;
import games.president.actions.Pass;
import games.president.actions.PlayCards;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.president.PresidentTestUtils.*;
import static org.junit.Assert.*;

/**
 * PresidentUtils.rank, and the legal actions of the leader and of a follower.
 */
public class PresidentLegalActionsTest {

    PresidentForwardModel fm;
    PresidentGameState state;

    @Before
    public void setup() {
        fm = new PresidentForwardModel();
        state = newState(4, 11, fm);
    }

    @Test
    public void rankPutsTheTwoAboveTheAce() {
        assertEquals(3, PresidentUtils.rank(3));
        assertEquals(10, PresidentUtils.rank(10));
        assertEquals(13, PresidentUtils.rank(13));
        assertEquals(14, PresidentUtils.rank(14));   // Ace
        assertEquals(15, PresidentUtils.rank(2));    // Two
        assertEquals(15, PresidentUtils.rank(card("2H")));
        assertEquals(14, PresidentUtils.rank(card("AS")));
        assertEquals(11, PresidentUtils.rank(card("JD")));
    }

    @Test
    public void leaderMayPlayEverySizeOfSetOfEveryRankHeldButMayNotPass() {
        // the deal leaves player 0 to lead with an empty play pile
        giveHand(state, 0, "3H", "3D", "3C", "7S", "KH", "KD", "2C");
        Set<AbstractAction> expected = Set.of(
                new PlayCards(3, 1), new PlayCards(3, 2), new PlayCards(3, 3),
                new PlayCards(7, 1),
                new PlayCards(13, 1), new PlayCards(13, 2),
                new PlayCards(2, 1));
        assertEquals(expected, actionSet(state, fm));
    }

    @Test
    public void followingASingleAllowsPassOrAHigherSingle() {
        // player 3 played a single 10; player 0 to act.
        // 9 is lower and 10 equal - not allowed; the Jacks may go only one at a time; A and 2 are higher
        arrangeTrick(state, 3, 1, 0, 0, "10S");
        giveHand(state, 0, "9H", "10D", "JH", "JC", "AS", "2D");
        Set<AbstractAction> expected = Set.of(new Pass(), new PlayCards(11, 1), new PlayCards(14, 1), new PlayCards(2, 1));
        assertEquals(expected, actionSet(state, fm));
    }

    @Test
    public void onlyAPairOfTwosBeatsAPairOfAces() {
        // player 3 played a pair of Aces: Kings are lower, a single Ace is the wrong size (and equal),
        // the Twos rank above the Ace but only a pair of them is allowed, not one or three
        arrangeTrick(state, 3, 2, 0, 0, "AH", "AD");
        giveHand(state, 0, "KH", "KD", "2H", "2S", "2D", "AC");
        assertEquals(Set.of(new Pass(), new PlayCards(2, 2)), actionSet(state, fm));
    }

    @Test
    public void nothingBeatsASingleTwo() {
        // a Two is the top rank: the Ace is lower and another Two is only equal
        arrangeTrick(state, 3, 1, 0, 0, "2S");
        giveHand(state, 0, "AH", "2H", "KC", "KD");
        assertEquals(Set.of(new Pass()), actionSet(state, fm));
    }

    @Test
    public void aTripleIsFollowedOnlyByAHigherTriple() {
        // player 3 played three Fives: the pair of Sixes is too small, the four Eights may be played only as three,
        // and the Four is lower
        arrangeTrick(state, 3, 3, 0, 0, "5H", "5D", "5C");
        giveHand(state, 0, "6H", "6D", "8H", "8D", "8C", "8S", "4S");
        assertEquals(Set.of(new Pass(), new PlayCards(8, 3)), actionSet(state, fm));
    }

    @Test
    public void theRankToBeatIsThatOfTheTopSet() {
        // the pile holds a 4 under a 9 (the top set): the 5 beats the 4 but not the 9
        arrangeTrick(state, 3, 1, 0, 0, "4H", "9C");
        giveHand(state, 0, "5D", "10H");
        assertEquals(Set.of(new Pass(), new PlayCards(10, 1)), actionSet(state, fm));
    }
}
