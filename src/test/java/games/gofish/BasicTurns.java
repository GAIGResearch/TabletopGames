package games.gofish;

import core.components.PartialObservableDeck;
import core.components.FrenchCard;
import core.components.FrenchCard.Suite;
import core.components.FrenchCard.FrenchCardType;
import games.gofish.actions.GoFishAsk;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BasicTurns {

    GoFishForwardModel fm = new GoFishForwardModel();
    GoFishGameState state;
    GoFishParameters params = new GoFishParameters();

    @Before
    public void setup() {
        state = new GoFishGameState(params, 4);
        fm.setup(state);
    }


    @Test
    public void testSequenceOfAskAndThenDraw() {
        GoFishAsk askAction = getNextAction(true);
        long currentCards = state.playerHands.get(0).stream().filter(card -> card.number == askAction.rankAsked).count();
        long otherCards = state.playerHands.get(askAction.targetPlayer).stream().filter(card -> card.number == askAction.rankAsked).count();
        assertTrue(fm.computeAvailableActions(state).contains(askAction));
        fm.next(state, askAction);
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(currentCards + otherCards, state.playerHands.get(0).stream().filter(card -> card.number == askAction.rankAsked).count());
        assertEquals(0, state.playerHands.get(askAction.targetPlayer).stream().filter(card -> card.number == askAction.rankAsked).count());
    }

    @Test
    public void testSequenceOfAskThenFish() {
        GoFishAsk askAction = getNextAction(false);
        long currentCards = state.playerHands.get(0).getSize();
        long otherCards = state.playerHands.get(askAction.targetPlayer).getSize();
        assertTrue(fm.computeAvailableActions(state).contains(askAction));
        fm.next(state, askAction);
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(currentCards + 1, state.playerHands.get(0).getSize());
        assertEquals(otherCards, state.playerHands.get(askAction.targetPlayer).getSize());
    }

    @Test
    public void testSequenceOfAskAndThenDrawWithDrawPileExhausted() {
        state.drawDeck.clear();
        testSequenceOfAskAndThenDraw();
    }

    @Test
    public void testSequenceOfAskAndThenFishWithDrawPileExhausted() {
        state.drawDeck.clear();
        GoFishAsk askAction = getNextAction(false);
        long currentCards = state.playerHands.get(0).getSize();
        long otherCards = state.playerHands.get(askAction.targetPlayer).getSize();
        assertTrue(fm.computeAvailableActions(state).contains(askAction));
        fm.next(state, askAction);
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(currentCards, state.playerHands.get(0).getSize());
        assertEquals(otherCards, state.playerHands.get(askAction.targetPlayer).getSize());

    }

    @Test
    public void testFailedAskDoesNotChangeVisibilityOfExistingCards() {
        // Arrange: pick an ask that will fail
        GoFishAsk askAction = getNextAction(false);
        int asker = state.getCurrentPlayer();

        // Capture original hand (cards and their visibility)
        List<core.components.FrenchCard> originalCards = new ArrayList<>(state.playerHands.get(asker).getComponents());
        List<boolean[]> originalVisibility = new ArrayList<>();
        for (int i = 0; i < state.playerHands.get(asker).getSize(); i++) {
            boolean[] vis = state.playerHands.get(asker).getVisibilityOfComponent(i).clone();
            originalVisibility.add(vis);
        }

        fm.next(state, askAction);

        // Assert: for each original card, visibility remains the same
        List<core.components.FrenchCard> newCards = new ArrayList<>(state.playerHands.get(asker).getComponents());
        boolean[] used = new boolean[newCards.size()];
        for (int i = 0; i < originalCards.size(); i++) {
            core.components.FrenchCard oc = originalCards.get(i);
            boolean matched = false;
            for (int j = 0; j < newCards.size(); j++) {
                if (!used[j] && oc.equals(newCards.get(j))) {
                    assertArrayEquals("Visibility for card " + oc + " changed after failed ask",
                            originalVisibility.get(i), state.playerHands.get(asker).getVisibilityOfComponent(j));
                    used[j] = true;
                    matched = true;
                    break;
                }
            }
            assertTrue("Original card must still exist in hand after failed ask: " + oc, matched);
        }
    }

    @Test
    public void testDrawnCardAfterFailedAskVisibleOnlyToDrawingPlayer() {
        // Arrange: pick an ask that will fail
        GoFishAsk askAction = getNextAction(false);
        int asker = state.getCurrentPlayer();

        // Act
        fm.next(state, askAction);

        // The drawn card (if any) is placed at the top (index 0) of the asker's hand
        if (state.playerHands.get(asker).getSize() > 0) {
            int idx = 0; // newly drawn card should be at top
            for (int p = 0; p < state.getNPlayers(); p++) {
                if (p == asker)
                    assertTrue(state.playerHands.get(asker).isComponentVisible(idx, p));
                else
                    assertFalse(state.playerHands.get(asker).isComponentVisible(idx, p));
            }
        }
    }

    @Test
    public void testSuccessfulAskMakesTakenCardsVisibleToAll_whenAskerHadOne() {
        int asker = state.getCurrentPlayer();
        int[] tr = findTargetAndRank();
        int target = tr[0];
        int rank = tr[1];

        setPlayerRankCount(asker, rank, 1);
        int takenCount = totalOfRankInHand(target, rank);

        GoFishAsk ask = new GoFishAsk(target, rank);
        fm.next(state, ask);

        assertEquals(takenCount + 1, totalOfRankInHand(asker, rank));
        assertEquals(takenCount + 1, visibleToAllOfRankInHand(asker, rank));
    }

    @Test
    public void testSuccessfulAskMakesTakenCardsVisibleToAll_whenAskerHadTwo() {
        int asker = state.getCurrentPlayer();
        int[] tr = findTargetAndRank();
        int target = tr[0];
        int rank = tr[1];

        setPlayerRankCount(asker, rank, 2);
        int takenCount = totalOfRankInHand(target, rank);

        GoFishAsk ask = new GoFishAsk(target, rank);
        fm.next(state, ask);

        assertEquals(takenCount + 2, totalOfRankInHand(asker, rank));
        assertEquals(takenCount + 1, visibleToAllOfRankInHand(asker, rank));
    }

    @Test
    public void testSuccessfulAskPreservesPreexistingVisibility() {
        int asker = state.getCurrentPlayer();
        int[] tr = findTargetAndRank();
        int target = tr[0];
        int rank = tr[1];

        // Ensure asker has exactly 2 of that rank
        setPlayerRankCount(asker, rank, 2);
        setPlayerRankCount(target, rank, 1);

        // Record references and indices of the two pre-existing rank-cards
        PartialObservableDeck<FrenchCard> handBefore = state.playerHands.get(asker);
        List<core.components.FrenchCard> preexisting = new ArrayList<>();
        List<Integer> preIdx = new ArrayList<>();
        for (int i = 0; i < handBefore.getSize() && preexisting.size() < 2; i++) {
            core.components.FrenchCard c = handBefore.get(i);
            if (c.number == rank) {
                preexisting.add(c);
                preIdx.add(i);
            }
        }

        // Make the first pre-existing card visible to all players
        boolean[] allVisible = new boolean[state.getNPlayers()];
        Arrays.fill(allVisible, true);
        handBefore.setVisibilityOfComponent(preIdx.get(0), allVisible);

        // count how many cards target has of that rank
        int takenCount = totalOfRankInHand(target, rank);

        // Perform ask (should succeed)
        GoFishAsk ask = new GoFishAsk(target, rank);
        fm.next(state, ask);

        // After ask: find indices of the original card objects in the asker's hand
        PartialObservableDeck<FrenchCard> handAfter = state.playerHands.get(asker);
        int idxOrig0 = -1, idxOrig1 = -1;
        for (int i = 0; i < handAfter.getSize(); i++) {
            if (handAfter.get(i) == preexisting.get(0)) idxOrig0 = i;
            if (handAfter.get(i) == preexisting.get(1)) idxOrig1 = i;
        }

        // The first original card should remain visible to all
        for (int p = 0; p < state.getNPlayers(); p++) {
            assertTrue("Pre-existing visible card should remain visible to player " + p,
                    handAfter.isComponentVisible(idxOrig0, p));
        }

        // The second original card should retain its prior visibility (owner only)
        assertTrue("Owner should see their own card", handAfter.isComponentVisible(idxOrig1, asker));
        int other = (asker + 1) % state.getNPlayers();
        assertFalse("Other player should not see the second pre-existing card", handAfter.isComponentVisible(idxOrig1, other));

        // Total visible-to-all of that rank should equal takenCount + 1 (the one pre-existing that was set visible)
        assertEquals(takenCount + 1, visibleToAllOfRankInHand(asker, rank));
    }

    // Helper: ensure player has exactly 'count' cards of rank 'rank' in their hand
    private void setPlayerRankCount(int player, int rank, int count) {
        PartialObservableDeck<FrenchCard> hand = state.playerHands.get(player);
        int current = (int) hand.getComponents().stream().filter(c -> c.number == rank).count();
        // Remove extras from end to minimize position changes
        if (current > count) {
            for (int i = hand.getSize() - 1; i >= 0 && current > count; i--) {
                if (hand.get(i).number == rank) {
                    hand.pick(i); // removes at i
                    current--;
                }
            }
        } else if (current < count) {
            // Add missing cards to bottom so existing positions are unchanged
            for (int k = 0; k < count - current; k++) {
                FrenchCard c = new FrenchCard(FrenchCardType.Number, Suite.Diamonds, rank);
                hand.addToBottom(c);
            }
        }
    }

    // Helper: find a target player and a rank that the target currently has
    private int[] findTargetAndRank() {
        int player = state.getCurrentPlayer();
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (p == player) continue;
            for (core.components.FrenchCard c : state.playerHands.get(p).getComponents()) {
                return new int[]{p, c.number};
            }
        }
        // fallback - should not happen with initial deal
        return new int[]{(player + 1) % state.getNPlayers(), state.playerHands.get((player + 1) % state.getNPlayers()).get(0).number};
    }

    // Helper: count total cards of rank in player's hand
    private int totalOfRankInHand(int player, int rank) {
        return (int) state.playerHands.get(player).stream().filter(c -> c.number == rank).count();
    }

    // Helper: count how many cards of rank are visible to all players in the given player's hand
    private int visibleToAllOfRankInHand(int player, int rank) {
        PartialObservableDeck<FrenchCard> hand = state.playerHands.get(player);
        int visible = 0;
        for (int i = 0; i < hand.getSize(); i++) {
            FrenchCard c = hand.get(i);
            if (c.number == rank) {
                boolean allVisible = true;
                for (int p = 0; p < state.getNPlayers(); p++) {
                    if (!hand.isComponentVisible(i, p)) {
                        allVisible = false;
                        break;
                    }
                }
                if (allVisible) visible++;
            }
        }
        return visible;
    }

    private GoFishAsk getNextAction(boolean willSucceed) {
        int player = state.getCurrentPlayer();
        int rankToPick = -1;
        int playerToTarget = -1;
        for (int p = 0; p < 4; p++) {
            if (p == player) continue;
            for (int r = 2; r <= 14; r++) {
                int finalR = r;
                if (state.playerHands.get(player).stream().anyMatch(card -> card.number == finalR)) {
                    boolean matches = willSucceed ?
                            state.playerHands.get(p).stream().anyMatch(card -> card.number == finalR) :
                            state.playerHands.get(p).stream().noneMatch(card -> card.number == finalR);

                    if (matches) {
                        rankToPick = r;
                        playerToTarget = p;
                        break;
                    }
                }
            }
        }
        return new GoFishAsk(playerToTarget, rankToPick);
    }
}
