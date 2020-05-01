package games.pandemic.engine;

import core.AbstractGameState;
import core.actions.IAction;

public abstract class Node {
    private static int nextID = 0;

    private int id;
    protected boolean actionNode = false;
    protected IAction action;

    public Node() {
        id = nextID++;
    }

    public abstract Node execute(AbstractGameState gs);
    public boolean requireAction() { return actionNode; }

    public void setAction(IAction action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return id == node.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public abstract Node getNext();
}
