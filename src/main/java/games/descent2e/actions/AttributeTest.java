package games.descent2e.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

// TODO: perform an attribute test
public class AttributeTest extends AbstractAction {
    @Override
    public boolean execute(AbstractGameState gs) {
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new AttributeTest();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AttributeTest;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Attribute Test";
    }
}
