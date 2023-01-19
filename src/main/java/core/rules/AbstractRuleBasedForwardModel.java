package core.rules;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractGameStateWithTurnOrder;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.rules.nodetypes.BranchingRuleNode;
import core.rules.nodetypes.ConditionNode;
import core.rules.nodetypes.RuleNode;

import java.util.HashMap;

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
     * @param state - current game state, to be modified by the action.
     * @param action - action requested to be played by a player.
     */
    @Override
    protected void _next(AbstractGameState state, AbstractAction action) {
        if (state.getGameStatus() != CoreConstants.GameResult.GAME_ONGOING) return;
        if (!(state instanceof AbstractGameStateWithTurnOrder))
            throw new AssertionError("Rules Based Forward Model is only usable with AbstractGameStateWithTurnOrder");

        AbstractGameStateWithTurnOrder currentState = (AbstractGameStateWithTurnOrder) state;
        if (nextRule == null) {
            nextRule = lastRule.getNext();  // Go back to parent, skip it and go to next rule
            if (nextRule == null) nextRule = root;
            return;
        }

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

    /**
     * Copy root node with rule graph.
     * @return - New copy of root
     */
    protected Node copyRoot() {
        return copyNodeGraph(new HashMap<>(), root);
    }

    /**
     * Recursive copy of nodes in rule graph
     * @param visitedNodes - Map keeping track of visited nodes
     * @param node - Original node
     * @return - Node with copy of rule graph
     */
    private Node copyNodeGraph(HashMap<Integer, Node> visitedNodes, Node node) {

        if(node == null) {
            throw new AssertionError("Can't copy rule graph containing null nodes!");
        }
        if(visitedNodes.containsKey(node.getId())) {
            return visitedNodes.get(node.getId());
        }

        Node copy = node.copy();
        visitedNodes.put(node.getId(), copy);

        if(node instanceof RuleNode) {
            if(node instanceof BranchingRuleNode) {
                Node[] children = ((BranchingRuleNode) node).getChildren();
                Node[] copies = new Node[children.length];
                for (int j = 0; j < copies.length; j++) {
                    copies[j] = copyNodeGraph(visitedNodes, children[j].getNext());
                    if (children[j].parent != null) copies[j].setParent(copy);
                }
                ((BranchingRuleNode) copy).setNext(copies);
            } else {
                Node child = copyNodeGraph(visitedNodes, node.getNext());
                if (node.getNext().parent != null) child.setParent(copy);
                ((RuleNode) copy).setNext(child);
            }

        } else if(node instanceof ConditionNode) {
            Node childYes = copyNodeGraph(visitedNodes, ((ConditionNode) node).getYesNo()[0]);
            Node childNo = copyNodeGraph(visitedNodes, ((ConditionNode) node).getYesNo()[1]);
            if (((ConditionNode) node).getYesNo()[0].parent != null) childYes.setParent(copy);
            if (((ConditionNode) node).getYesNo()[1].parent != null) childNo.setParent(copy);
            ((ConditionNode) copy).setYesNo(childYes, childNo);
        }
        return copy;
    }
}
