package games.sueca;

import core.components.FrenchCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static games.sueca.SuecaTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * Completing a trick: who wins it, where its cards go and who leads the next. Each case stands for the 9th trick:
 * three cards already played, the fourth player to play, and every player keeping one low club for the last trick.
 * Hearts are trumps (trump card 5H, played in an earlier trick) unless stated.
 */
public class SuecaTrickTest {

    private static final String[] KEPT = {"2C", "3C", "4C", "5C"};

    private SuecaGameState state;

    /**
     * Arrange the trick (codes in play order, from the leader) with the first three cards played, and play the
     * fourth through fm.next. Returns the team piles' cards before the trick, [team 0, team 1].
     */
    private List<List<FrenchCard>> playTrick(String trumpCard, int leader, String... codes) {
        state = newState(3);
        SuecaForwardModel fm = new SuecaForwardModel();
        List<List<FrenchCard>> hands = new ArrayList<>();
        for (int p = 0; p < 4; p++)
            hands.add(new ArrayList<>(cards(KEPT[p])));
        int last = (leader + 3) % 4;
        hands.get(last).add(card(codes[3]));
        arrangeHands(state, hands.get(0), hands.get(1), hands.get(2), hands.get(3));
        arrangeTrick(state, leader, codes[0], codes[1], codes[2]);
        setTrumpCard(state, trumpCard);
        assertEquals(last, state.getCurrentPlayer());
        assertAllCardsPresent(state);
        List<List<FrenchCard>> before = List.of(cardsOf(state.getTeamPile(0)), cardsOf(state.getTeamPile(1)));

        playCards(state, fm, codes[3]);
        assertAllCardsPresent(state);
        return before;
    }

    /**
     * Asserts that the winner took the trick of these cards, worth the given card points, and leads the next.
     */
    private void assertWonBy(int winner, int points, List<List<FrenchCard>> before, String... codes) {
        int team = winner % 2;
        assertEquals("winner to play", winner, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals("new trick's leader", winner, state.getCurrentTrick().getLeader());
        assertEquals(SuecaUtils.CARD_ORDER, state.getCurrentTrick().getOrder());
        List<FrenchCard> expectedPile = new ArrayList<>(before.get(team));
        expectedPile.addAll(cards(codes));
        assertEquals("winning team's pile", new java.util.HashSet<>(expectedPile),
                new java.util.HashSet<>(cardsOf(state.getTeamPile(team))));
        assertEquals(expectedPile.size(), state.getTeamPile(team).getSize());
        assertEquals("losing team's pile", before.get(1 - team), cardsOf(state.getTeamPile(1 - team)));
        assertEquals(expectedCardPoints(before.get(team)) + points, state.getCardPoints(team));
        assertTrue(state.isNotTerminal());
        for (int p = 0; p < 4; p++)
            assertEquals(1, state.getPlayerHand(p).getSize());
    }

    @Test
    public void withNoTrumpTheSevenBeatsTheKing() {
        String[] trick = {"KS", "7S", "JS", "QS"};
        List<List<FrenchCard>> before = playTrick("5H", 0, trick);
        // player 1's 7S wins (team 1): K 4 + 7 10 + J 3 + Q 2 = 19
        assertWonBy(1, 19, before, trick);
    }

    @Test
    public void withNoTrumpTheJackBeatsTheQueen() {
        String[] trick = {"QD", "6D", "JD", "2D"};
        List<List<FrenchCard>> before = playTrick("5H", 0, trick);
        // player 2's JD wins (team 0): Q 2 + 6 0 + J 3 + 2 0 = 5
        assertWonBy(2, 5, before, trick);
    }

    @Test
    public void aHigherCardOfAnotherPlainSuitDoesNotWin() {
        String[] trick = {"3S", "AD", "2S", "7C"};
        List<List<FrenchCard>> before = playTrick("5H", 0, trick);
        // spades led; the Ace of Diamonds and 7 of Clubs are discards: player 0's 3S wins (team 0)
        // 3 0 + A 11 + 2 0 + 7 10 = 21
        assertWonBy(0, 21, before, trick);
    }

    @Test
    public void theLowestTrumpBeatsTheAceOfTheSuitLed() {
        String[] trick = {"AS", "2H", "7S", "KS"};
        List<List<FrenchCard>> before = playTrick("5H", 0, trick);
        // player 1 trumps with the 2 of Hearts (team 1): A 11 + 2 0 + 7 10 + K 4 = 25
        assertWonBy(1, 25, before, trick);
    }

    @Test
    public void theHighestOfSeveralTrumpsWins() {
        String[] trick = {"5D", "KH", "7H", "JH"};
        List<List<FrenchCard>> before = playTrick("5H", 0, trick);
        // trumps K, 7, J: the 7 of Hearts (player 2, team 0) ranks highest: 5 0 + K 4 + 7 10 + J 3 = 17
        assertWonBy(2, 17, before, trick);
    }

    @Test
    public void whenTrumpsAreLedTheHighestTrumpWinsOverAnOffSuitAce() {
        String[] trick = {"QH", "6H", "4H", "AS"};
        List<List<FrenchCard>> before = playTrick("5H", 0, trick);
        // player 0's Queen is the highest heart; player 3 discards the Ace of Spades: Q 2 + 0 + 0 + A 11 = 13
        assertWonBy(0, 13, before, trick);
    }

    @Test
    public void trumpsFollowTheTrumpCardsSuit() {
        String[] trick = {"AS", "2H", "7S", "2D"};
        // the trump card 3D makes diamonds trumps: player 3's 2 of Diamonds wins, not the 2 of Hearts
        List<List<FrenchCard>> before = playTrick("3D", 0, trick);
        // A 11 + 2 0 + 7 10 + 2 0 = 21 to team 1
        assertWonBy(3, 21, before, trick);
    }

    @Test
    public void aTrickLedByPlayerTwoGoesRoundToPlayerOneAndCountsFromTheLeader() {
        String[] trick = {"6S", "5S", "2H", "7S"};
        // led by player 2, then 3, 0 and 1: player 0 trumps with the 2 of Hearts (team 0): 0 + 0 + 0 + 7 10 = 10
        List<List<FrenchCard>> before = playTrick("5H", 2, trick);
        assertWonBy(0, 10, before, trick);
    }
}
