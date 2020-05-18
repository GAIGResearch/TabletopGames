package games.pandemic.engine;

import core.AbstractGameState;
import core.actions.IAction;

public abstract class Node {
    private static int nextID = 0;

    private int id;
    protected boolean actionNode = false;
    protected IAction action;
    protected Node parent;

    public Node() {
        id = nextID++;
    }

    public abstract Node execute(AbstractGameState gs);
    public abstract Node getNext();

    public final void setAction(IAction action) {
        this.action = action;
    }
    public final boolean requireAction() { return actionNode; }
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
