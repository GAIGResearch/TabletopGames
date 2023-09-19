package games.serveTheKing.components;

import core.CoreConstants;
import core.components.Card;
import core.components.Component;

/**
 * <p>Components represent a game piece, or encompass some unit of game information (e.g. cards, tokens, score counters, boards, dice etc.)</p>
 * <p>Components in the game can (and should, if applicable) extend one of the other components, in package {@link core.components}.
 * Or, the game may simply reuse one of the existing core components.</p>
 * <p>They need to extend at a minimum the {@link Component} super class and implement the {@link Component#copy()} method.</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>may</b> keep references to other components or actions (but these should be deep-copied in the copy() method, watch out for infinite loops!).</p>
 */
public class PlateCard extends Card {


    protected int value;
    public PlateCard(int value) {
        super(String.valueOf(value));
        value=value;
    }

    protected PlateCard(String name, int value,  int componentID) {
        super( name, componentID);
        value = value;
    }

    public int getValue() {
        return value;
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
    public PlateCard copy() {
        PlateCard copy = new PlateCard( String.valueOf(value),  value, componentID);
        // TODO: copy here all non-final class variables.
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        // TODO: compare all class variables (if any).
        return (o instanceof PlateCard) && ((PlateCard) o).value==value && super.equals(o);
    }

    @Override
    public int hashCode() {
        // TODO: include all class variables (if any).
        return 2051*value*componentID;
    }
}
