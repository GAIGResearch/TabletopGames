package games.pandemic.engine.rules;

import core.AbstractGameState;
import games.pandemic.Constants;
import games.pandemic.engine.Node;
import games.pandemic.engine.gameOver.GameOverCondition;

import java.util.ArrayList;

import static games.pandemic.Constants.GameResult.GAME_ONGOING;

public abstract class RuleNode extends Node {
    Node childNext;
    private ArrayList<GameOverCondition> gameOverConditions;

    public RuleNode() {
        super();
        gameOverConditions = new ArrayList<>();
    }

    protected RuleNode(boolean actionNode) {
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
                Constants.GameResult result = goc.test(gs);
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
