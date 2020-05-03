package games.pandemic.engine.conditions;

import core.AbstractGameState;
import games.pandemic.engine.Node;

/**
 * Tests condition, next node depends on the result of the test:
 *
 * parent
 *  - yes (condition == true)
 *  - no (condition == false
 */
public abstract class ConditionNode extends Node {
    Node childYes, childNo;
    boolean passed;

    protected abstract boolean test(AbstractGameState gs);

    public final Node execute(AbstractGameState gs) {
        passed = test(gs);
        return getNext();
    }
    public final Node getNext() {
        if (passed) return childYes;
        else return childNo;
    }
    public final void setYesNo(Node childYes, Node childNo) {
        this.childYes = childYes;
        this.childNo = childNo;
    }
    public final Node[] getYesNo() {
        return new Node[]{childYes, childNo};
    }
}
