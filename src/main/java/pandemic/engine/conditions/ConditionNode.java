package pandemic.engine.conditions;

import core.GameState;
import pandemic.engine.Node;

public abstract class ConditionNode extends Node {
    Node childYes, childNo;

    public ConditionNode(Node yes, Node no) {
        super();
        childNo = no;
        childYes = yes;
    }

    protected abstract boolean test(GameState gs);

    public final Node execute(GameState gs) {
        if (test(gs)) return childYes;
        else return childNo;
    }
    public final Node getNext() {
        return childYes;
    }

}
