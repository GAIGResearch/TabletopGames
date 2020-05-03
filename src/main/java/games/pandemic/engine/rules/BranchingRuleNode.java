package games.pandemic.engine.rules;

import core.AbstractGameState;
import games.pandemic.engine.Node;
import games.pandemic.engine.gameOver.GameOverCondition;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;

import static utilities.Utils.GameResult.GAME_ONGOING;

/**
 * Splits a node into n branches:
 *
 * parent
 *  - child 0
 *  - child 1
 *  ...
 *  - child n
 */
public abstract class BranchingRuleNode extends Node {
    int next = 0;
    Node[] children;
    private ArrayList<GameOverCondition> gameOverConditions;

    public BranchingRuleNode() {
        super();
        gameOverConditions = new ArrayList<>();
    }

    protected BranchingRuleNode(boolean actionNode) {
        super();
        this.actionNode = actionNode;
        gameOverConditions = new ArrayList<>();
    }

    protected abstract boolean run(AbstractGameState gs);

    public final void addGameOverCondition(GameOverCondition node) {
        gameOverConditions.add(node);
    }
    public final Node execute(AbstractGameState gs) {
        if (requireAction() && action == null) return null;

        boolean interrupted = !run(gs);
        if (gameOverConditions != null && gameOverConditions.size() > 0) {
            for (GameOverCondition goc: gameOverConditions) {  // TODO: this triggers first condition, maybe order matters/loss first
                Utils.GameResult result = goc.test(gs);
                if (result != GAME_ONGOING) {
                    gs.setGameStatus(result);
                    Arrays.fill(children, null);
                }
            }
        }
        if (!interrupted) return children[next++];
        return null;
    }
    public final void setNext(Node[] children) {
        this.children = children;
    }
    public final Node getNext() {
        return children[next];
    }
    public final Node[] getChildren() { return children; }
    public final int getNextIdx() { return next; }
}
