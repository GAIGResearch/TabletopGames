package core.rules.nodetypes;

import core.AbstractGameStateWithTurnOrder;
import core.CoreConstants;
import core.rules.GameOverCondition;
import core.rules.Node;

import java.util.ArrayList;

import static core.CoreConstants.GameResult.GAME_ONGOING;

/**
 * Executes a piece of game logic and moves on to the next Node. Can trigger game end if it has game over conditions
 * attached. Does not execute if it requires an action and did not receive one. Can be interrupted, which will mean
 * the external game loop is interrupted after this rule, and this rule is skipped next time the loop resumes.
 *
 * parent -> childNext
 */
public abstract class RuleNode extends Node {
    Node childNext;  // Child to execute next after this node
    private ArrayList<GameOverCondition> gameOverConditions;  // List of game over conditions to check after this node is executed

    public RuleNode() {
        super();
        gameOverConditions = new ArrayList<>();
    }


    /**
     * Specialised constructor to use for rule nodes requiring actions.
     * @param actionNode - true if action node.
     */
    protected RuleNode(boolean actionNode) {
        super();
        this.actionNode = actionNode;
        gameOverConditions = new ArrayList<>();
    }

    /**
     * Copy constructor, does not copy childNext to avoid endless recursion in looping graphs.
     * @param node - Node to be copied
     */
    protected RuleNode(RuleNode node) {
        super(node);
        this.gameOverConditions = node.gameOverConditions;
    }

    /**
     * Apply the functionality of the rule in the given game state.
     * @param gs - game state to modify.
     * @return - true if successfully executed, false if not and game loop should be interrupted after the execution.
     */
    protected abstract boolean run(AbstractGameStateWithTurnOrder gs);

    /**
     * Adds a new game over condition to this node.
     * @param condition - game over condition to add.
     */
    public final void addGameOverCondition(GameOverCondition condition) {
        gameOverConditions.add(condition);
    }

    /**
     * Executes the rule if all requirements met, and tests any game over conditions included with the rule. If any
     * game over conditions trigger, the child of this rule is set to null to break the game loop.
     * @param gs - game state to apply functionality in.
     * @return - the next child to execute if the rule did not request an interruption, or null otherwise (and if
     * requirements for execution are not met, or the game is over).
     */
    public final Node execute(AbstractGameStateWithTurnOrder gs) {
        if (requireAction() && action == null) return null;

        boolean interrupted = !run(gs);
        if (gameOverConditions != null && gameOverConditions.size() > 0) {
            for (GameOverCondition goc: gameOverConditions) {  // TODO: this triggers first condition, maybe order matters/loss first
                CoreConstants.GameResult result = goc.test(gs);
                if (result != GAME_ONGOING) {
                    gs.setGameStatus(result);
//                    childNext = null;
                }
            }
        }
        if (!interrupted) return childNext;
        return null;
    }

    // Getters & Setters
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
