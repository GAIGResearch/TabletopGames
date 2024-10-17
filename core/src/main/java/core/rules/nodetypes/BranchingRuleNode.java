package core.rules.nodetypes;

import core.AbstractGameStateWithTurnOrder;
import core.rules.Node;

/**
 * Splits a node into n branches:
 *
 * parent
 *  - child 0
 *  - child 1
 *  ...
 *  - child n
 */
public abstract class BranchingRuleNode extends RuleNode {
    Node[] children;  // Array of child nodes

    public BranchingRuleNode() {
        super();
    }

    /**
     * Specialised constructor to use for rule nodes requiring actions.
     * @param actionNode - true if action node.
     */
    protected BranchingRuleNode(boolean actionNode) {
        super(actionNode);
    }

    /**
     * Copy constructor, does not copy childYes or childNo to avoid endless recursion in looping graphs.
     * @param node - Node to be copied
     */
    protected BranchingRuleNode(BranchingRuleNode node) {
        super(node);
    }

    /**
     * Apply the functionality of the rule in the given game state, and decide which of the children is to be executed
     * next.
     * @param gs - game state to modify.
     * @return - true if successfully executed, false if not and game loop should be interrupted after the execution.
     */
    protected abstract boolean run(AbstractGameStateWithTurnOrder gs);

    // Getters & Setters
    public final void setNext(Node[] children) {
        this.children = children;
    }
    public final Node[] getChildren() { return children; }
}
