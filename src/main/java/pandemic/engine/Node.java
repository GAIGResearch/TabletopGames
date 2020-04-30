package pandemic.engine;

import actions.Action;
import core.GameState;

public abstract class Node {
    private static int nextID = 0;

    private int id;
    protected boolean actionNode = false;
    protected Action action;

    public Node() {
        id = nextID++;
    }

    public abstract Node execute(GameState gs);
    public boolean requireAction() { return actionNode; }

    public void setAction(Action action) {
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
