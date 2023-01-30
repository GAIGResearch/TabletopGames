package core.rules;

import core.AbstractGameStateWithTurnOrder;
import core.actions.AbstractAction;

/**
 * A node in a tree of game rules. Receives a unique ID on creation, and keeps track of node properties.
 */
public abstract class Node {
    private static int nextID = 0;

    private int id;  // Unique id for this node
    protected boolean actionNode;  // True if this node requires an action to execute
    protected boolean nextPlayerNode;  // True if this action changes active player
    protected AbstractAction action;  // Action to execute if this node requires one
    protected Node parent;  // Parent node, can be used to retrieve parameters set by a previous node

    public Node() {
        id = nextID++;
    }

    /**
     * Copy constructor, does not copy parent node to avoid endless recursion in looping graphs.
     * @param node - Node to be copied
     */
    public Node(Node node) {
        this.id = node.id;
        this.actionNode = node.actionNode;
        this.nextPlayerNode = node.nextPlayerNode;
        this.action = node.action;
    }

    /**
     * Create a copy of the Node by calling subclass _copy()
     * @return - New Node object with the same properties
     */
    public Node copy() {
        return _copy();
    }

    protected abstract Node _copy();

    /**
     * Executes the functionality of this node.
     * @param gs - game state to apply functionality in.
     * @return - Node, the next node to execute afterwards.
     */
    public abstract Node execute(AbstractGameStateWithTurnOrder gs);

    /**
     * Retrieves the next node to execute after this.
     * @return - Node, next child to execute.
     */
    public abstract Node getNext();

    // Getters & setters
    public final void setAction(AbstractAction action) {
        this.action = action;
    }
    public final boolean requireAction() { return actionNode; }
    public final void setNextPlayerNode() {
        nextPlayerNode = true;
    }
    public final boolean isNextPlayerNode() {
        return nextPlayerNode;
    }
    public final int getId() { return id; }
    public final Node getParent() { return parent; }
    public final void setParent(Node parent) { this.parent = parent; }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return id == node.id;
    }

    @Override
    public final int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return this.getClass().toString();
    }
}
