package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.mcts.MultiTreeNode;
import players.mcts.SingleTreeNode;
import utilities.Utils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static players.mcts.MCTSEnums.OpponentTreePolicy.MultiTree;

public class TestMCTSPlayer extends MCTSPlayer {

    protected Supplier<? extends SingleTreeNode> factory;
    public boolean rolloutTest = true;

    public TestMCTSPlayer(MCTSParams params, Supplier<? extends SingleTreeNode> factory) {
        super(params, "TestMCTSPlayer");
        this.factory = factory;
    }

    @Override
    protected Supplier<? extends SingleTreeNode> getFactory() {
        if (factory == null)
            return super.getFactory();
        return factory;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public SingleTreeNode getRoot() {
        return root;
    }

    public SingleTreeNode getRoot(int player) {
        if (root instanceof MultiTreeNode)
            return ((MultiTreeNode) root).getRoot(player);
        return root;
    }

    @Override
    protected void createRootNode(AbstractGameState gameState) {
        if (rolloutTest && getParameters().opponentTreePolicy == MultiTree)
            root = new MTNRollout(this, gameState, rnd);
        else
            super.createRootNode(gameState);
    }

    public boolean wasRemoved(Object key) {
        return ((MCGSNode) root).keysTorRemove.contains(key);
    }

}
