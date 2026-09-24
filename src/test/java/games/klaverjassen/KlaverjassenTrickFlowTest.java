package games.klaverjassen;

import core.Game;
import games.klaverjassen.actions.ChooseTrump;
import org.junit.Test;

import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.klaverjassen.KlaverjassenTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * Completing a trick before the last: the winner's team takes the trick's card points, roem and a trick, the
 * cards go to the discard pile, and the winner leads the next trick. The arranged hands of two (or one) cards
 * each stand for a hand six (or seven) tricks in, so tricksWon is set to match.
 */
public class KlaverjassenTrickFlowTest {

    @Test
    public void playPassesClockwiseAndTheTrumpWinnerLeadsTheNextTrick() {
        Game game = newGame(7);
        KlaverjassenGameState state = (KlaverjassenGameState) game.getGameState();
        KlaverjassenForwardModel fm = (KlaverjassenForwardModel) game.getForwardModel();
        fm.next(state, new ChooseTrump(Hearts));
        assertEquals(0, state.getCurrentPlayer());
        // two cards each, as if six tricks were played (their 24 cards on the discard pile)
        giveHand(state, 0, "AS", "7D");
        giveHand(state, 1, "10S", "8D");
        giveHand(state, 2, "7H", "9D");
        giveHand(state, 3, "KS", "AD");
        state.handPoints = new int[]{50, 40};
        state.tricksWon = new int[]{3, 3};
        assertEquals(24, state.getDiscardPile().getSize());

        playCards(state, fm, "AS");
        assertEquals(1, state.getCurrentPlayer());
        playCards(state, fm, "10S");
        assertEquals(2, state.getCurrentPlayer());
        playCards(state, fm, "7H");                  // player 2, void in spades, trumps (partner winning: any card)
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(cards("AS", "10S", "7H"), cardsOf(state.getCurrentTrick()));
        assertEquals(Set.of(Spades), state.getKnownVoids().get(2));
        playCards(state, fm, "KS");

        // the 7 of Hearts (trump) beats the Ace of Spades led: player 2 (team 0) wins
        // AS 11 + 10S 10 + 7H 0 + KS 4 = 25 card points; not the last trick, so no bonus
        assertArrayEquals(new int[]{50 + 25, 40}, state.handPoints);
        assertArrayEquals(new int[]{4, 3}, state.tricksWon);
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(2, state.getCurrentTrick().getLeader());
        assertEquals(new KlaverjassenCardOrder(Hearts), state.getCurrentTrick().getOrder());
        assertEquals(28, state.getDiscardPile().getSize());   // 24 + the 4 trick cards
        for (String c : new String[]{"AS", "10S", "7H", "KS"})
            assertTrue(c + " on the discard pile", state.getDiscardPile().contains(card(c)));
        assertArrayEquals(new int[]{0, 0}, state.teamScores);
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);

        // trick 2: player 2 leads, then play wraps round 3 -> 0
        playCards(state, fm, "9D");
        assertEquals(3, state.getCurrentPlayer());
        playCards(state, fm, "AD");
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(cards("9D", "AD"), cardsOf(state.getCurrentTrick()));
        assertArrayEquals(new int[]{75, 40}, state.handPoints);   // nothing scored for a trick in progress
    }

    @Test
    public void theSeventhTrickGoesToTeamOneWithoutEndingTheHand() {
        KlaverjassenGameState state = newState(3);
        KlaverjassenForwardModel fm = new KlaverjassenForwardModel();
        setTrumps(state, Clubs);
        giveHand(state, 0, "10D", "10S");
        giveHand(state, 1, "QD", "7S");
        giveHand(state, 2, "KD", "8S");
        giveHand(state, 3, "JC", "9S");
        arrangeTrick(state, 1);                      // player 1 leads the seventh trick
        state.handPoints = new int[]{40, 50};
        state.tricksWon = new int[]{2, 4};

        playCards(state, fm, "QD", "KD", "JC", "10D");
        // player 3 (team 1) trumps with the Jack of Clubs
        // QD 3 + KD 4 + JC 20 + 10D 10 = 37 card points
        assertArrayEquals(new int[]{40, 50 + 37}, state.handPoints);
        assertArrayEquals(new int[]{2, 5}, state.tricksWon);
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(3, state.getCurrentTrick().getLeader());
        assertEquals(new KlaverjassenCardOrder(Clubs), state.getCurrentTrick().getOrder());
        // one trick still to play: the hand is not scored yet
        assertTrue(state.isNotTerminal());
        assertArrayEquals(new int[]{0, 0}, state.teamScores);
        assertAllCardsPresent(state);
    }

    @Test
    public void theRoemOfATrickGoesToTheWinnersTeamNotTheLeaders() {
        KlaverjassenGameState state = newState(3);
        KlaverjassenForwardModel fm = new KlaverjassenForwardModel();
        setTrumps(state, Clubs);
        // two cards each, as if six tricks were played
        giveHand(state, 0, "8H", "7D");
        giveHand(state, 1, "9H", "8D");
        giveHand(state, 2, "7S", "9D");              // no Hearts and no Clubs: may play anything
        giveHand(state, 3, "10H", "10D");
        arrangeTrick(state, 0);                      // player 0 (team 0) leads
        state.handPoints = new int[]{50, 40};
        state.handRoem = new int[]{20, 0};
        state.tricksWon = new int[]{3, 3};

        playCards(state, fm, "8H", "9H", "7S", "10H");
        // the 10 of Hearts (player 3, team 1) wins; 8-9-10 of Hearts is a run of three: 20 roem to team 1
        // card points 8H 0 + 9H 0 + 7S 0 + 10H 10 = 10
        assertArrayEquals(new int[]{20, 0 + 20}, state.handRoem);
        assertArrayEquals(new int[]{50, 40 + 10}, state.handPoints);
        assertArrayEquals(new int[]{3, 4}, state.tricksWon);
        assertEquals(3, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
        assertArrayEquals(new int[]{0, 0}, state.teamScores);   // roem is scored with the hand, not before
    }

    @Test
    public void scoreTrickCreditsTheRoemToTheWinnersTeamAndAccumulates() {
        KlaverjassenGameState state = newState(3);
        KlaverjassenForwardModel fm = new KlaverjassenForwardModel();
        setTrumps(state, Spades);
        arrangeTrick(state, 0, "KS", "QS", "7H", "8D");
        state.handPoints = new int[]{0, 0};
        state.handRoem = new int[]{10, 30};
        state.tricksWon = new int[]{1, 1};

        // King and Queen of trumps: stuk 20; card points KS 4 + QS 3 = 7
        fm.scoreTrick(state, state.currentTrick, 0);
        assertArrayEquals(new int[]{10 + 20, 30}, state.handRoem);
        assertArrayEquals(new int[]{7, 0}, state.handPoints);

        // the same cards credited to player 1 go to team 1
        fm.scoreTrick(state, state.currentTrick, 1);
        assertArrayEquals(new int[]{30, 30 + 20}, state.handRoem);
        assertArrayEquals(new int[]{7, 7}, state.handPoints);
    }

    @Test
    public void scoreTrickCreditsTheCardPointsAndATrickToTheWinnersTeam() {
        KlaverjassenGameState state = newState(3);
        KlaverjassenForwardModel fm = new KlaverjassenForwardModel();
        setTrumps(state, Diamonds);
        giveHand(state, 0, "8H", "9H");
        giveHand(state, 1, "8C", "9C");
        giveHand(state, 2, "10H", "JH");
        giveHand(state, 3, "10C", "JC");
        arrangeTrick(state, 0, "AD", "7D", "KD", "QS");
        state.handPoints = new int[]{12, 30};
        state.tricksWon = new int[]{2, 2};           // well before the last trick, even after two calls

        // diamonds trumps: the Ace of Diamonds (player 0) wins; AD 11 + 7D 0 + KD 4 + QS 3 = 18, not the last trick
        fm.scoreTrick(state, state.currentTrick, 0);
        assertArrayEquals(new int[]{12 + 18, 30}, state.handPoints);
        assertArrayEquals(new int[]{3, 2}, state.tricksWon);

        // the same cards credited to player 3 go to team 1
        fm.scoreTrick(state, state.currentTrick, 3);
        assertArrayEquals(new int[]{30, 30 + 18}, state.handPoints);
        assertArrayEquals(new int[]{3, 3}, state.tricksWon);
    }
}
