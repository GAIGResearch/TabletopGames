package games.pandemic.engine.rules;

import core.AbstractGameState;
import games.pandemic.engine.Node;
import games.pandemic.engine.gameOver.GameOverCondition;
import utilities.Utils;

import java.util.ArrayList;

import static utilities.Utils.GameResult.GAME_ONGOING;

/**
 * Executes a piece of game logic and moves on to the next Node. Can trigger game end if it has game over conditions
 * attached. Does not execute if it requires an action and did not receive one. Can be interrupted, which will mean
 * the external game loop is interrupted after this rule, and this rule is skipped next time the loop resumes.
 *
 * parent -> childNext
 */
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
                Utils.GameResult result = goc.test(gs);
                if (result != GAME_ONGOING) {
                    gs.setGameStatus(result);
                    childNext = null;
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
    public final ArrayList<GameOverCondition> getGameOverConditions() {
        return gameOverConditions;
    }
}
