package players.mcts;

import core.AbstractGameState;
import core.CoreConstants;

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

        // Termination is only checked once the decision player has left their tree; rollout length is then measured
        // from that point (counting rollout actions by any player)
        int rolloutTicks = actionsInRollout.size() - rolloutStartTick;
        assertTrue("Iteration ended while decision player was still in their tree", decisionPlayerOutOfTree(openLoopState));

        switch (params.rolloutTermination) {
            case EXACT:
                // in this case we just check that we have 10 actions in the rollout
                assertEquals(params.rolloutLength* (params.rolloutLengthPerPlayer ? openLoopState.getNPlayers() : 1), rolloutTicks);
                break;
            case START_ACTION:
                // in this case we have at least 10 actions, and finish at the end of a player's Turn
                // which means that the current player should be the MCTS player, and
                // the last player should be someone else
                assertTrue(rolloutTicks >= params.rolloutLength);
                assertEquals(0, openLoopState.getTurnOwner());
                assertNotEquals(0, lastActorInRollout);
                break;
            case END_ACTION:
                // in this case we have at least 10 actions, and finish at the end of a player's Turn
                // which means that the current player is not the same as the player who acted last
                // and the last player who acted should be the decision player
                assertTrue(rolloutTicks >= params.rolloutLength);
                assertNotEquals(openLoopState.getTurnOwner(), lastActorInRollout);
                assertEquals(0, lastActorInRollout);
                break;
            case END_TURN:
                assertTrue(rolloutTicks >= params.rolloutLength);
                assertNotEquals(openLoopState.getTurnCounter(), lastTurnInRollout);
                assertTrue(openLoopState.getTurnCounter() == lastTurnInRollout + 1 || openLoopState.getTurnCounter() == 0);
                break;
            case END_ROUND:
                assertTrue(rolloutTicks >= params.rolloutLength);
                assertNotEquals(openLoopState.getRoundCounter(), lastRoundInRollout);
                assertEquals(openLoopState.getRoundCounter(), lastRoundInRollout + 1);
                break;
        }
    }
}
