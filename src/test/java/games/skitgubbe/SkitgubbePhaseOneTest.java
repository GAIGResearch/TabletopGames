package games.skitgubbe;

import core.actions.AbstractAction;
import games.skitgubbe.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.skitgubbe.SkitgubbeTestUtils.*;
import static org.junit.Assert.*;

/**
 * Phase one played from hand: the actions, playing and drawing, trick resolution, bounces and the trump card.
 * Three players; the trick is between the leader and the player after them.
 */
public class SkitgubbePhaseOneTest {

    SkitgubbeGameState state;
    SkitgubbeForwardModel fm;

    @Before
    public void setup() {
        state = newState(3, 42, SkitgubbeTestUtils.valetParams());
        fm = new SkitgubbeForwardModel();
    }

    private void play(String code) {
        fm.next(state, new PlayCard(card(code)));
    }

    private static PlayCard pc(String code) {
        return new PlayCard(card(code));
    }

    // Action-set tests keep at most one card in the draw deck, so TurnUpCard (offered only with 2 or more) is not
    // in the exact sets.

    @Test
    public void leaderMayPlayAnyCardInHand() {
        giveHand(state, 0, "5H", "KS", "2C");
        leaveDrawDeck(state, 2, "9D");
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(pc("5H"), pc("KS"), pc("2C")), new HashSet<>(actions));
        assertEquals(3, actions.size());
    }

    @Test
    public void followerMayPlayAnyCardInHand() {
        moveTo(state, state.trick, "5H");            // player 0 has led the 5 of Hearts
        giveHand(state, 1, "7D", "7C", "AS");
        leaveDrawDeck(state, 2);
        state.setTurnOwner(1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(pc("7D"), pc("7C"), pc("AS")), new HashSet<>(actions));
        assertEquals(3, actions.size());
    }

    @Test
    public void leaderPlaysToTheTrickAndDrawsTheTopOfTheDrawDeck() {
        giveHand(state, 0, "5H", "KS", "2C");
        stackDrawDeck(state, "9D", "4S");
        assertEquals(43, state.drawDeck.getSize());   // 52 - 3 * 3

        play("5H");

        assertEquals(List.of(card("5H")), state.trick.getComponents());
        assertEquals(setOf("KS", "2C", "9D"), asSet(state.playerHands.get(0)));
        assertEquals(42, state.drawDeck.getSize());   // 43 - 1 drawn
        assertEquals(card("4S"), state.drawDeck.peek());
        assertEquals(1, state.getCurrentPlayer());    // the follower, 0 + 1
        assertAllCardsPresent(state);
    }

    @Test
    public void higherFollowerWinsBothCardsAndLeadsToThePlayerAfterThem() {
        giveHand(state, 0, "5H", "2S", "3S");
        giveHand(state, 1, "9H", "2D", "3D");

        play("5H");
        play("9H");                                   // 9 > 5: the follower wins

        assertEquals(setOf("5H", "9H"), asSet(state.collectedCards.get(1)));
        assertEquals(0, state.collectedCards.get(0).getSize());
        assertEquals(0, state.collectedCards.get(2).getSize());
        assertEquals(0, state.trick.getSize());
        assertEquals(3, state.playerHands.get(0).getSize());   // both refilled
        assertEquals(3, state.playerHands.get(1).getSize());
        assertEquals(41, state.drawDeck.getSize());   // 43 - 2 draws
        assertEquals(1, state.getCurrentPlayer());    // the winner leads

        play("2D");
        assertEquals(2, state.getCurrentPlayer());    // the winner's follower, 1 + 1
        assertAllCardsPresent(state);
    }

    @Test
    public void higherLeaderWinsAndLeadsAgainToTheSameFollower() {
        giveHand(state, 0, "KS", "2S", "3S");
        giveHand(state, 1, "5D", "2D", "3D");

        play("KS");
        play("5D");                                   // King 13 > 5: the leader wins

        assertEquals(setOf("KS", "5D"), asSet(state.collectedCards.get(0)));
        assertEquals(0, state.collectedCards.get(1).getSize());
        assertEquals(0, state.trick.getSize());
        assertEquals(0, state.getCurrentPlayer());

        play("2S");
        assertEquals(1, state.getCurrentPlayer());    // the same follower
    }

    @Test
    public void aceBeatsKing() {
        giveHand(state, 0, "KH", "2S", "3S");
        giveHand(state, 1, "AS", "2D", "3D");

        play("KH");
        play("AS");                                   // Ace 14 > King 13

        assertEquals(setOf("KH", "AS"), asSet(state.collectedCards.get(1)));
        assertEquals(0, state.collectedCards.get(0).getSize());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void suitsDoNotMatterInPhaseOne() {
        giveHand(state, 0, "2H", "4S", "5S");
        giveHand(state, 1, "3C", "4D", "5D");

        play("2H");
        play("3C");                                   // 3 > 2 although the suit differs

        assertEquals(setOf("2H", "3C"), asSet(state.collectedCards.get(1)));
        assertEquals(0, state.collectedCards.get(0).getSize());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void equalRanksBounceToEachPlayersHeldCardsAndTheSameLeaderLeadsAgain() {
        giveHand(state, 0, "7S", "9D", "2S");
        giveHand(state, 1, "7H", "4C", "2D");

        play("7S");
        play("7H");                                   // 7 = 7 in different suits: a bounce

        assertEquals(List.of(card("7S")), state.heldCards.get(0).getComponents());
        assertEquals(List.of(card("7H")), state.heldCards.get(1).getComponents());
        assertEquals(0, state.heldCards.get(2).getSize());
        assertEquals(0, state.trick.getSize());
        for (int p = 0; p < 3; p++)
            assertEquals("collected of " + p, 0, state.collectedCards.get(p).getSize());
        assertEquals(3, state.playerHands.get(0).getSize());
        assertEquals(3, state.playerHands.get(1).getSize());
        assertEquals(41, state.drawDeck.getSize());   // 43 - 2 draws
        assertEquals(0, state.getCurrentPlayer());

        play("9D");
        assertEquals(1, state.getCurrentPlayer());    // the same follower
        assertAllCardsPresent(state);
    }

    @Test
    public void theTrickAfterABounceCollectsTheHeldCards() {
        equalRanksBounceToEachPlayersHeldCardsAndTheSameLeaderLeadsAgain();   // 7S / 7H held, P0 has led 9D
        play("4C");                                   // 9 > 4: the leader wins

        // 2 trick cards + 2 held = 4
        assertEquals(setOf("7S", "7H", "9D", "4C"), asSet(state.collectedCards.get(0)));
        assertEquals(0, state.heldCards.get(0).getSize());
        assertEquals(0, state.heldCards.get(1).getSize());
        assertEquals(0, state.collectedCards.get(1).getSize());
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void aDoubleBounceThenAWinCollectsSixCardsIncludingTheLosersHeldCards() {
        giveHand(state, 0, "7S", "8S", "10C");
        giveHand(state, 1, "7H", "8H", "JC");

        play("7S");
        play("7H");                                   // bounce
        play("8S");
        play("8H");                                   // bounce again
        assertEquals(2, state.heldCards.get(0).getSize());
        assertEquals(2, state.heldCards.get(1).getSize());
        play("10C");
        play("JC");                                   // Jack 11 > 10: the follower wins

        // 3 tricks of 2 cards = 6, both players' held cards included
        assertEquals(setOf("7S", "7H", "8S", "8H", "10C", "JC"), asSet(state.collectedCards.get(1)));
        assertEquals(0, state.collectedCards.get(0).getSize());
        assertEquals(0, state.heldCards.get(0).getSize());
        assertEquals(0, state.heldCards.get(1).getSize());
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void theWinnerAlsoCollectsTheHeldCardsOfAPlayerOutsideTheTrick() {
        moveTo(state, state.heldCards.get(2), "QD");  // player 2 bounced earlier
        giveHand(state, 0, "5H", "2S", "3S");
        giveHand(state, 1, "9C", "2D", "3D");

        play("5H");
        play("9C");

        // 2 trick cards + player 2's 1 held = 3
        assertEquals(setOf("5H", "9C", "QD"), asSet(state.collectedCards.get(1)));
        assertEquals(0, state.heldCards.get(2).getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void theLastDrawDeckCardBecomesTheTrumpCardNotAHandCard() {
        giveHand(state, 0, "5H", "KS", "2C");
        leaveDrawDeck(state, 2, "9D");

        play("5H");

        assertEquals(setOf("KS", "2C"), asSet(state.playerHands.get(0)));   // one short of 3
        assertEquals(List.of(card("9D")), state.trumpCard.getComponents());
        assertEquals(0, state.trumpCard.getOwnerId());
        assertEquals(0, state.getTrumpPlayer());
        assertEquals(0, state.drawDeck.getSize());
        assertNull(state.getTrumpSuit());             // set only when phase one ends
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void noOneDrawsFromAnEmptyDrawDeck() {
        giveTrumpCard(state, 2, "9D");
        giveHand(state, 0, "5H", "KS", "2C");
        leaveDrawDeck(state, 1);

        play("5H");

        assertEquals(setOf("KS", "2C"), asSet(state.playerHands.get(0)));
        assertEquals(List.of(card("9D")), state.trumpCard.getComponents());
        assertEquals(2, state.trumpCard.getOwnerId());
        assertEquals(2, state.getTrumpPlayer());
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }
}
