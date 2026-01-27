package games.thegame.components;

import core.CoreConstants;
import core.components.Card;
import core.components.Component;
import gametemplate.components.GTComponent;

public class TheGameCard extends Card {

    public TheGameCard(CoreConstants.ComponentType type, String name) {
        super(name);
    }

    protected TheGameCard(CoreConstants.ComponentType type, String name, int componentID) {
        super(name, componentID);
    }


    /**
     * @return Make sure to return an exact <b>deep</b> copy of the object, including all of its variables.
     * Make sure the return type is this class (e.g. GTComponent) and NOT the super class Component.
     * <p>
     * <b>IMPORTANT</b>: This should have the same componentID
     * (using the protected constructor on the Component super class which takes this as an argument).
     * </p>
     * <p>The function should also call the {@link Component#copyComponentTo(Component)} method, passing in as an
     * argument the new copy you've made.</p>
     * <p>If all variables in this class are final or effectively final, then you can just return <code>`this`</code>.</p>
     */
    @Override
    public TheGameCard copy() {
        TheGameCard copy = new TheGameCard(type, componentName, componentID);
        // TODO: copy here all non-final class variables.
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        // TODO: compare all class variables (if any).
        return (o instanceof GTComponent) && super.equals(o);
    }

    @Override
    public int hashCode() {
        // TODO: include all class variables (if any).
        return super.hashCode();
    }
}
