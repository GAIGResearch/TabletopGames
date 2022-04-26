package test.players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.mcts.MultiTreeNode;
import players.mcts.SingleTreeNode;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class TestMCTSPlayer extends MCTSPlayer {

    public TestMCTSPlayer(MCTSParams params) {
        super(params, "TestMCTSPlayer");
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public SingleTreeNode getRoot(int player) {
        if (root instanceof MultiTreeNode)
            return ((MultiTreeNode) root).getRoot(player);
        return root;
    }

}
