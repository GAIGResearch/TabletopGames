package core.interfaces;

import core.actions.AbstractAction;

public interface IActionKey {

    /**
     * A string to summarise the action.
     * The idea is to use this to group actions into similar buckets
     *
     * @param action
     * @return
     */
    String key(AbstractAction action);

    /**
     * As for key - a hash has the potential advantage of being more performant
     * at the cost of losing interpretability
     * @param action
     * @return
     */
    default int hash(AbstractAction action) {
        return key(action).hashCode();
    }
}
