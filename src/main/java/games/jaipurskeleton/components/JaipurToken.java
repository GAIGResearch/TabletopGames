package games.jaipurskeleton.components;

import core.CoreConstants;
import core.components.Component;

import java.util.Objects;

/**
 * <p>Components represent a game piece, or encompass some unit of game information (e.g. cards, tokens, score counters, boards, dice etc.)</p>
 * <p>Components in the game can (and should, if applicable) extend one of the other components, in package {@link core.components}.
 * Or, the game may simply reuse one of the existing core components.</p>
 * <p>They need to extend at a minimum the {@link Component} super class and implement the {@link Component#copy()} method.</p>
 * <p>They also need to include {@link Object#equals(Object)} and {@link Object#hashCode()} methods.</p>
 * <p>They <b>may</b> keep references to other components or actions (but these should be deep-copied in the copy() method, watch out for infinite loops!).</p>
 */
public class JaipurToken extends Component {

    final public JaipurCard.GoodType goodType;
    final public int tokenValue;

    public JaipurToken(int value) {
        super(CoreConstants.ComponentType.TOKEN, ""+value);
        this.tokenValue = value;
        this.goodType = null;
    }

    public JaipurToken(JaipurCard.GoodType type, int value) {
        super(CoreConstants.ComponentType.TOKEN, ""+value);
        this.tokenValue = value;
        this.goodType = type;
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
    public JaipurToken copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JaipurToken)) return false;
        if (!super.equals(o)) return false;
        JaipurToken that = (JaipurToken) o;
        return tokenValue == that.tokenValue && goodType == that.goodType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), goodType, tokenValue);
    }

    @Override
    public String toString() {
        if (goodType != null) return goodType.name() + " (" + tokenValue + ")";
        return ""+tokenValue;
    }
}
