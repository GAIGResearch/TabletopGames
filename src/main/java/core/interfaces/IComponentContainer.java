package core.interfaces;

import core.CoreConstants;
import core.components.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * An interface to be used on any Component that contains other Components.
 * The interface is 'read-only', and deliberately avoids specifying add/remove type methods, and has two purposes:
 *
 * i) To be used to gather information about game states for game metrics and comparisons (see GameReport as example)
 * ii) To indicate who can see the contents of the Container (Everyone, No-one, just the Owner)?
 * iii) As a holder of a few useful stream-related default methods - these are all read-only methods.
 *
 * @param <T> The Type of Component that the Container holds
 */
public interface IComponentContainer<T extends Component> {

    /**
     * @return A list of all the Components in the Container
     */
    List<T> getComponents();

    CoreConstants.VisibilityMode getVisibilityMode();

    default Stream<T> stream() {
        return getComponents().stream();
    }
    /**
     * @return the size of this deck (number of components in it).
     */
    default int getSize() {
        return getComponents().size();
    }

    default double sumDouble(Function<T, Double> lambda) {
        double retValue = 0.0;
        for (T c : getComponents()) {
            retValue += lambda.apply(c);
        }
        return retValue;
    }
    default int sumInt(Function<T, Integer> lambda) {
        int retValue = 0;
        for (T c : getComponents()) {
            retValue += lambda.apply(c);
        }
        return retValue;
    }
}
