package core.rules;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;

public abstract class AbstractRuleBasedForwardModel extends AbstractForwardModel {

    // Rule executed last, rule to be executed next, and first rule to be executed in a turn (root)
    protected Node lastRule, nextRule, root;

    /**
     * Default constructor. Any classes extending this should initialise the root node variable to the first rule
     * to execute in the game. The flow is then controlled by adding children nodes to the root (conditions or
     * rules, check core.rules.nodetypes), loops allowed.
     *
     * Rule nodes can contain GameOverCondition objects, which would allow them to end the game (by interrupting
     * any loops in the game flow and setting game status to the result returned by the condition check).
     *
     *      - Use core.rules.rulenodes.EndPlayerTurn.java type rules to change active player
     *      - Use core.rules.rulenodes.PlayerAction.java type rules to execute player actions (and apply any other
     *      effects depending on action executed).
     *      - Use core.rules.rulenodes.ForceAllPlayerReaction.java type rules to force all players to react (if using
     *      a ReactiveTurnOrder).
     *
     * Can use utilities.GameFlowDiagram.java class to visualise game flow, given a root node (and all children assigned)
     */
    protected AbstractRuleBasedForwardModel() {}

    /**
     * Copy constructor from root node.
     * @param root - root rule node.
     */
    protected AbstractRuleBasedForwardModel(Node root) {
        this.root = root;
        this.nextRule = root;
    }

    /**
     * Combines both super class and sub class setup methods. Called from the game loop.
     * @param firstState - initial state.
     */
    protected void abstractSetup(AbstractGameState firstState) {
        super.abstractSetup(firstState);
        nextRule = root;
        lastRule = null;
    }

    /**
     * Applies the given action to the game state and executes any other game rules.
     * @param currentState - current game state, to be modified by the action.
     * @param action - action requested to be played by a player.
     */
    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        if (nextRule == null) return;

        do {
            if (nextRule.requireAction()) {
                if (action != null) {
                    nextRule.setAction(action);
                    action = null;
                } else {
                    return;  // Wait for action to be sent to execute this rule requiring action
                }
            }
            lastRule = nextRule;
            nextRule = nextRule.execute(currentState);
        } while (nextRule != null);

        nextRule = lastRule.getNext();  // Go back to parent, skip it and go to next rule
    }
}
