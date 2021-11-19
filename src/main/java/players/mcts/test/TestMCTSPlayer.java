package players.mcts.test;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
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

    public SingleTreeNode getRoot() {
        return root;
    }

    public List<SingleTreeNode> allNodesInTree() {
        List<SingleTreeNode> retValue = new ArrayList<>();
        Queue<SingleTreeNode> nodeQueue = new ArrayDeque<>();
        nodeQueue.add(root);
        while (!nodeQueue.isEmpty()) {
            SingleTreeNode node = nodeQueue.poll();
            retValue.add(node);
            nodeQueue.addAll(node.getChildren().values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(Arrays::stream)
                    .filter(Objects::nonNull)
                    .collect(toList()));
        }
        return retValue;
    }
}
