package games.klaverjassen;

import core.CoreConstants.GameResult;
import core.Game;
import core.components.Deck;
import core.components.FrenchCard;
import games.klaverjassen.actions.ChooseTrump;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.Suite.*;
import static games.klaverjassen.KlaverjassenTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.cardsOf;
import static org.junit.Assert.*;

/**
 * Games of several hands (KlaverjassenParameters.nHands), driven through fm.next with arranged last tricks: the
 * redeal, the next trump chooser, and the nat and scores of each hand. Random multi-hand games are in
 * KlaverjassenGameFlowTest.
 */
public class KlaverjassenMultiHandTest {

    private static KlaverjassenParameters hands(int nHands) {
        KlaverjassenParameters params = new KlaverjassenParameters();
        params.setParameterValue("nHands", nHands);
        return params;
    }

    /**
     * Plays the first hand's last trick (trumps Diamonds; player 1's Ace of Spades wins, team 1) with some roem and
     * a known void arranged beforehand. Hand points 60 v 102, roem 20 v 0: totals 80 v 102, so trump team 0 is nat
     * and team 1 scores 80 + 102 = 182.
     */
    private static void playFirstHand(KlaverjassenGameState state, KlaverjassenForwardModel fm) {
        setTrumps(state, Diamonds);
        // before: 60 + 78 = 138 = 152 - 14
        arrangeLastTrick(state, 0, new int[]{60, 78}, new int[]{3, 4}, "8S", "AS", "7S", "QS");
        state.handRoem = new int[]{20, 0};
        state.knownVoids.get(2).add(Hearts);
        playCards(state, fm, "8S", "AS", "7S", "QS");
    }

    private static Set<ChooseTrump> allTrumpChoices() {
        return Set.of(new ChooseTrump(Hearts), new ChooseTrump(Diamonds), new ChooseTrump(Clubs),
                new ChooseTrump(Spades));
    }

    @Test
    public void aHandThatIsNotTheLastIsScoredAndTheNextChooserIsDealtANewHand() {
        Game game = newGame(11, hands(2));
        KlaverjassenGameState state = (KlaverjassenGameState) game.getGameState();
        KlaverjassenForwardModel fm = (KlaverjassenForwardModel) game.getForwardModel();
        playFirstHand(state, fm);

        // the first hand is scored (nat: team 1 has 80 + 102 = 182) and the scores are kept
        assertArrayEquals(new int[]{0, 182}, state.teamScores);
        assertTrue("two hands: the game goes on after the first", state.isNotTerminal());

        // the deal passes left: dealer 0, so player 1 chooses trumps, is to play, and leads
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getDealer());
        assertEquals(1, state.getTrumpChooser());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(allTrumpChoices(), new HashSet<>(fm.computeAvailableActions(state)));

        // everything of the old hand is cleared
        assertNull(state.getTrumpSuit());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(1, state.getCurrentTrick().getLeader());
        assertEquals(new KlaverjassenCardOrder(null), state.getCurrentTrick().getOrder());
        assertArrayEquals(new int[]{0, 0}, state.handPoints);
        assertArrayEquals(new int[]{0, 0}, state.handRoem);
        assertArrayEquals(new int[]{0, 0}, state.tricksWon);
        for (int p = 0; p < 4; p++) {
            assertEquals("known voids of player " + p, Set.of(), state.knownVoids.get(p));
            assertEquals("cards of player " + p, 8, state.getPlayerHand(p).getSize());
        }
        assertEquals(0, state.getDiscardPile().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void theNextHandIsReshuffled() {
        // two identical states whose random number generators differ (a copy gets its own): the same last trick
        // must lead to different deals for the second hand
        Game game = newGame(11, hands(2));
        KlaverjassenGameState state = (KlaverjassenGameState) game.getGameState();
        KlaverjassenForwardModel fm = (KlaverjassenForwardModel) game.getForwardModel();
        setTrumps(state, Diamonds);
        arrangeLastTrick(state, 0, new int[]{60, 78}, new int[]{3, 4}, "8S", "AS", "7S", "QS");
        KlaverjassenGameState other = (KlaverjassenGameState) state.copy();
        assertEquals(state, other);

        playCards(state, fm, "8S", "AS", "7S", "QS");
        playCards(other, fm, "8S", "AS", "7S", "QS");
        assertEquals("redealt", 8, state.getPlayerHand(0).getSize());   // guard: both were redealt
        assertEquals("redealt", 8, other.getPlayerHand(0).getSize());
        List<List<FrenchCard>> deal = new ArrayList<>(), otherDeal = new ArrayList<>();
        for (Deck<FrenchCard> hand : state.playerHands) deal.add(cardsOf(hand));
        for (Deck<FrenchCard> hand : other.playerHands) otherDeal.add(cardsOf(hand));
        assertNotEquals(deal, otherDeal);
    }

    @Test
    public void theSecondHandsChooserChoosesTrumpsAndItsTeamIsNatWithFewerPoints() {
        Game game = newGame(11, hands(2));
        KlaverjassenGameState state = (KlaverjassenGameState) game.getGameState();
        KlaverjassenForwardModel fm = (KlaverjassenForwardModel) game.getForwardModel();
        playFirstHand(state, fm);
        assertArrayEquals(new int[]{0, 182}, state.teamScores);   // guard: hand 1, as the previous test

        // hand 2: player 1 chooses Hearts and leads
        fm.next(state, new ChooseTrump(Hearts));
        assertEquals(Hearts, state.getTrumpSuit());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getCurrentTrick().getLeader());

        // before: 90 + 50 = 140 = 152 - 12. Player 1 leads; play goes 1, 2, 3, 0
        arrangeLastTrick(state, 1, new int[]{90, 50}, new int[]{4, 3}, "JC", "10C", "7C", "8C");
        playCards(state, fm, "10C", "7C", "8C", "JC");
        // the 10 of Clubs (player 1) wins: 10 + 0 + 0 + 2 = 12, + 10 last trick = 22. No roem (7-8 and 10-J, no 9)
        assertArrayEquals(new int[]{90, 50 + 22}, state.handPoints);
        assertArrayEquals(new int[]{4, 4}, state.tricksWon);

        // trump team 1 (player 1's) has 72 < 90: nat, team 0 scores 90 + 72 = 162.
        // Totals 0 + 162 = 162 v 182 + 0 = 182. (With team 0 as the trump team: 90 v 254.)
        assertArrayEquals(new int[]{162, 182}, state.teamScores);
        assertFalse("two hands: the game is over after the second", state.isNotTerminal());
        assertEquals(162.0, state.getGameScore(0), 0.0);
        assertEquals(182.0, state.getGameScore(1), 0.0);
        // team 0 took hand 2 (162 v 0), but the result compares the scores of both hands: team 1 wins
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }
}
