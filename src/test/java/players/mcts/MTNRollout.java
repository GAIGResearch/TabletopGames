package players.mcts;

import core.AbstractGameState;
import core.CoreConstants;
import games.GameType;

import java.util.Random;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotEquals;

public class MTNRollout extends MultiTreeNode {
    public MTNRollout(MCTSPlayer player, AbstractGameState state, Random rnd) {
        super(player, state, rnd);
    }

    @Override
    public void oneSearchIteration() {
        super.oneSearchIteration();
        // Then we check the expected invariants

        // Now we can run some tests on this
        if (openLoopState == null || !openLoopState.isNotTerminalForPlayer(0) || !(openLoopState.getPlayerResults()[0] == CoreConstants.GameResult.GAME_ONGOING))
            return; // don't test terminal states

        switch (params.rolloutTermination) {
            case EXACT:
                // in this case we just check that we have 10 actions in the rollout
                assertEquals(params.rolloutLength, actionsInRollout.size());
                break;
            case START_ACTION:
                // in this case we have at least 10 actions, and finish at the end of a player's Turn
                // which means that the current player should be the MCTS player, and
                // the last player should be someone else
                assertTrue(actionsInRollout.size() >= params.rolloutLength);
                assertEquals(0, openLoopState.getTurnOwner());
                assertNotEquals(0, actionsInRollout.get(actionsInRollout.size() - 1).a.intValue());
                break;
            case END_ACTION:
                // in this case we have at least 10 actions, and finish at the end of a player's Turn
                // which means that the current player is not the same as the player who acted last
                // and the last player who acted should be the decision player
                assertTrue(actionsInRollout.size() >= params.rolloutLength);
                assertNotEquals(openLoopState.getTurnOwner(), actionsInRollout.get(actionsInRollout.size() - 1).a.intValue());
                assertEquals(0, actionsInRollout.get(actionsInRollout.size() - 1).a.intValue());
                break;
            case END_TURN:
                assertTrue(actionsInRollout.size() >= params.rolloutLength);
                assertNotEquals(openLoopState.getTurnCounter(), lastTurnInRollout);
                assertTrue(openLoopState.getTurnCounter() == lastTurnInRollout + 1 || openLoopState.getTurnCounter() == 0);
                break;
            case END_ROUND:
                assertTrue(actionsInRollout.size() >= params.rolloutLength);
                assertNotEquals(openLoopState.getRoundCounter(), lastRoundInRollout);
                if (openLoopState.getGameType() == GameType.Poker)
                    assertTrue(openLoopState.getRoundCounter() == lastRoundInRollout + 1 || openLoopState.getRoundCounter() == lastRoundInRollout + 2);
                else
                    assertEquals(openLoopState.getRoundCounter(), lastRoundInRollout + 1);
                break;
        }
    }
}
