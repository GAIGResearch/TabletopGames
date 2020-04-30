package pandemic.engine.rules;

import core.GameState;
import pandemic.engine.Node;
import pandemic.engine.gameOver.GameOverCondition;

import java.util.ArrayList;

import static pandemic.Constants.GAME_ONGOING;

public abstract class RuleNode extends Node {
    Node childNext;
    private ArrayList<GameOverCondition> gameOverConditions;

    public RuleNode(Node next) {
        super();
        childNext = next;
        gameOverConditions = new ArrayList<>();
    }

    protected RuleNode(Node next, boolean actionNode) {
        super();
        childNext = next;
        this.actionNode = actionNode;
        gameOverConditions = new ArrayList<>();
    }

    protected abstract boolean run(GameState gs);

    public final void addGameOverCondition(GameOverCondition node) {
        gameOverConditions.add(node);
    }
    public final Node execute(GameState gs) {
        if (requireAction() && action == null) return null;

        boolean interrupted = !run(gs);
        if (gameOverConditions != null && gameOverConditions.size() > 0) {
            for (GameOverCondition goc: gameOverConditions) {  // TODO: this triggers first condition, maybe order matters/loss first
                int result = goc.test(gs);
                if (result != GAME_ONGOING) {
                    gs.setGameOver(result);
                    return null;
                }
            }
        }
        if (!interrupted) return childNext;
        return null;
    }
    public final void setNext(Node childNext) {
        this.childNext = childNext;
    }
    public final Node getNext() {
        return childNext;
    }
}
