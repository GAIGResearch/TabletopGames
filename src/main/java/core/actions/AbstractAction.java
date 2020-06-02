package core.actions;

import core.AbstractGameState;
import core.components.Card;
import core.interfaces.IPrintable;

public abstract class AbstractAction implements IPrintable {

    /**
     * Executes this action, applying its effect to the given game state. Can access any component IDs stored
     * through the AbstractGameState.getComponentById(int id) method.
     * @param gs - game state which should be modified by this action.
     * @return - true if successfully executed, false otherwise.
     */
    public abstract boolean execute(AbstractGameState gs);

    /**
     * Create a copy of this action, with all of its variables.
     * NO REFERENCES TO COMPONENTS TO BE KEPT IN ACTIONS, PRIMITIVE TYPES ONLY.
     * @return - new AbstractAction object with the same properties.
     */
    public abstract AbstractAction copy();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String getString(AbstractGameState gameState);

    /**
     * Returns the card used to play this action. Null if no card was needed (as default).
     * @return - Card, to be discarded.
     */
    public Card getCard(AbstractGameState gs) {
        return null;
    }
}
