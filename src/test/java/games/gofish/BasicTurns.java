package games.gofish;

import games.gofish.actions.GoFishAsk;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
