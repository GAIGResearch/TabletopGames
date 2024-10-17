package players.mcts;

import core.AbstractGameState;
import core.CoreConstants;
import players.mcts.MCTSPlayer;
import players.mcts.MultiTreeNode;
import org.junit.Assert;

import java.util.Random;

import static org.junit.Assert.*;

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
            case DEFAULT:
                // in this case we just check that we have 10 actions in the rollout
                assertEquals(params.rolloutLength, actionsInRollout.size());
                break;
            case START_ACTION:
                // in this case we have at least 10 actions, and finish at the end of a player's Turn
                // which means that the current player should be the MCTS player, and
                // the last player should be someone else
                Assert.assertTrue(actionsInRollout.size() >= params.rolloutLength);
                assertEquals(0, openLoopState.getTurnOwner());
                assertNotSame(0, actionsInRollout.get(actionsInRollout.size() - 1).a.intValue());
                break;
            case END_ACTION:
                // in this case we have at least 10 actions, and finish at the end of a player's Turn
                // which means that the current player is not the same as the player who acted last
                // and the last player who acted should be the decision player
                Assert.assertTrue(actionsInRollout.size() >= params.rolloutLength);
                assertNotSame(openLoopState.getTurnOwner(), actionsInRollout.get(actionsInRollout.size() - 1).a.intValue());
                assertEquals(0, actionsInRollout.get(actionsInRollout.size() - 1).a.intValue());
                break;
            case END_TURN:
                Assert.assertTrue(actionsInRollout.size() >= params.rolloutLength);
                assertNotSame(openLoopState.getTurnCounter(), turnAtStartOfRollout);
                break;
            case END_ROUND:
                Assert.assertTrue(actionsInRollout.size() >= params.rolloutLength);
                assertNotSame(openLoopState.getRoundCounter(), roundAtStartOfRollout);
                break;
        }
    }
}
