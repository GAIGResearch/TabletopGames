package games.descent2e.actions.monsterfeats;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import org.w3c.dom.Attr;

/**
 * Sometimes we might take an action that has the option of triggering an Attribute Test, but we don't want to.
 * e.g. Sir Alric's Overpower triggers a Might test every time he moves adjacent to a Hero, but he might not want to force a swap.
 * This executes a DoNothing action that causes TriggerAttributeTest to skip the Attribute Test it would otherwise call.
 */

public class AttributeTestSkip extends DoNothing {
    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public AbstractAction copy() {
        return new AttributeTestSkip();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof AttributeTestSkip;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
    @Override
    public String toString() {
        return "Skip Attribute Test";
    }
}
