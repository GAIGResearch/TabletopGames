package games.hearts;

import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static games.tricktaking.TrickTakingTestUtils.card;
import static games.tricktaking.TrickTakingTestUtils.cards;
import static org.junit.Assert.*;

/**
 * Hearts may not be led until they have been broken, i.e. until a heart has been played to a trick
 * (as a discard by a player unable to follow suit, or - once broken - as a lead). The exception is a
 * player whose hand holds nothing but hearts; they may lead one. Hearts are unbroken again at the
 * start of each round.
 */
public class TestHeartsBreakingHearts {

    private HeartsForwardModel forwardModel;
    private HeartsGameState gameState;
    private HeartsParameters gameParameters;

    @Before
    public void setUp() {
        gameParameters = new HeartsParameters();
        gameParameters.setRandomSeed(3457);
        forwardModel = new HeartsForwardModel();
        gameState = new HeartsGameState(gameParameters, 3);
        forwardModel.setup(gameState);
        gameState.setGamePhase(HeartsGameState.Phase.PLAYING);
        // On the very first trick of a round only the holder of the 2 of Clubs may play, and only that
        // card; every test here is about a later trick, so we record a trick as already taken.
        gameState.trickDecks.get(0).add(card("5D"));
        assertFalse(gameState.heartsBroken);
    }

    /**
     * Replaces a player's hand with exactly these cards (the tests here do not track the whole deck).
     */
    private void giveHand(int player, String... codes) {
        Deck<FrenchCard> hand = gameState.getPlayerDecks().get(player);
        hand.clear();
        for (FrenchCard c : cards(codes))
            hand.add(c);
    }

    /**
     * An empty trick for this player to lead, with the turn theirs.
     */
    private void toLead(int player) {
        gameState.currentTrick.reset(player);
        gameState.setFirstPlayer(player);
    }

    private Set<AbstractAction> playsOf(String... codes) {
        Set<AbstractAction> expected = new HashSet<>();
        for (FrenchCard c : cards(codes))
            expected.add(new PlayCard(c));
        return expected;
    }

    private void assertActionsAre(Set<AbstractAction> expected) {
        List<AbstractAction> actual = forwardModel._computeAvailableActions(gameState);
        assertEquals("Wrong number of actions: " + actual, expected.size(), actual.size());
        assertEquals(expected, new HashSet<>(actual));
    }

    @Test
    public void heartsMayNotBeLedWhileUnbroken() {
        giveHand(0, "KH", "3H", "5C", "KS", "9D");
        toLead(0);
        // hearts unbroken, so the two hearts are not on offer; the other three cards are
        assertActionsAre(playsOf("5C", "KS", "9D"));
    }

    @Test
    public void aHandOfNothingButHeartsMayLeadAHeartWhileUnbroken() {
        giveHand(0, "AH", "7H", "2H");
        toLead(0);
        // nothing else to lead, so every heart is legal even though hearts are unbroken
        assertActionsAre(playsOf("AH", "7H", "2H"));
    }

    @Test
    public void aPlayerUnableToFollowSuitMayDiscardAHeartWhileUnbroken() {
        giveHand(1, "4H", "QH", "6C");
        gameState.currentTrick.reset(0);
        gameState.currentTrick.play(card("KS"));  // player 0 has led spades
        gameState.setTurnOwner(1);
        // player 1 holds no spade, so the whole hand is legal - the lead restriction applies only to leading
        assertActionsAre(playsOf("4H", "QH", "6C"));
    }

    @Test
    public void discardingAHeartBreaksHearts() {
        giveHand(1, "4H", "QH", "6C");
        gameState.currentTrick.reset(0);
        gameState.currentTrick.play(card("KS"));  // player 0 has led spades
        gameState.setTurnOwner(1);

        forwardModel.next(gameState, new PlayCard(card("4H")));

        assertTrue(gameState.heartsBroken);
    }

    @Test
    public void leadingAHeartOnceBrokenKeepsHeartsBroken() {
        gameState.heartsBroken = true;
        giveHand(0, "9H", "8C");
        toLead(0);

        forwardModel.next(gameState, new PlayCard(card("9H")));

        assertTrue(gameState.heartsBroken);
    }

    /**
     * A scripted trick: the leader is offered no heart, a player unable to follow discards one, and
     * from the next trick on hearts may be led.
     */
    @Test
    public void heartsMayBeLedOnceAHeartHasBeenDiscarded() {
        giveHand(0, "KC", "5C", "3H", "2H");
        giveHand(1, "9H", "4H", "8D");   // no clubs, so player 1 cannot follow
        giveHand(2, "7C", "6C", "5D");
        toLead(0);

        // player 0 leads: the two hearts are withheld
        assertActionsAre(playsOf("KC", "5C"));
        forwardModel.next(gameState, new PlayCard(card("KC")));

        // player 1 cannot follow clubs and discards a heart, breaking hearts
        assertEquals(1, gameState.getCurrentPlayer());
        assertActionsAre(playsOf("9H", "4H", "8D"));
        forwardModel.next(gameState, new PlayCard(card("9H")));
        assertTrue(gameState.heartsBroken);

        // player 2 follows suit; KC beats 7C so player 0 wins the trick and leads the next one
        assertEquals(2, gameState.getCurrentPlayer());
        forwardModel.next(gameState, new PlayCard(card("7C")));
        assertEquals(0, gameState.getCurrentPlayer());
        assertEquals(0, gameState.currentTrick.getLeader());
        assertEquals(0, gameState.currentTrick.getSize());

        // hearts are broken, so player 0 may now lead either of them
        assertActionsAre(playsOf("5C", "3H", "2H"));
    }

    @Test
    public void heartsAreUnbrokenAtTheStartOfANewRound() {
        gameState.heartsBroken = true;
        forwardModel._setupRound(gameState);
        assertFalse(gameState.heartsBroken);
    }

    /**
     * The same, in a real game: every card is played in a round, so hearts are certainly broken by the
     * end of it, and must be unbroken again once the next round has been dealt.
     */
    @Test
    public void heartsAreUnbrokenAgainAfterARealRound() {
        gameParameters.setRandomSeed(88);
        gameState = new HeartsGameState(gameParameters, 3);
        forwardModel.setup(gameState);
        Random rnd = new Random(88);
        boolean brokenDuringRound = false;
        for (int i = 0; i < 300 && gameState.isNotTerminal(); i++) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            forwardModel.next(gameState, actions.get(rnd.nextInt(actions.size())));
            if (gameState.heartsBroken)
                brokenDuringRound = true;
            if (gameState.getRoundCounter() == 1) {
                assertTrue("Hearts were never broken during the round", brokenDuringRound);
                assertFalse("Hearts are still broken in the new round", gameState.heartsBroken);
                return;
            }
        }
        fail("The first round did not finish within the actions played");
    }
}
