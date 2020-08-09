package games.coltexpress.actions;

import core.AbstractGameState;
import core.components.Component;

public class DrawComponents<T extends Component> extends core.actions.DrawComponents<T> {
    /**
     * Moves the first N components from one deck to another.
     *
     * @param deckFrom    - origin deck from which to move components.
     * @param deckTo      - destination deck to which to move components.
     * @param nComponents - how many components should be moved.
     */
    public DrawComponents(int deckFrom, int deckTo, int nComponents) {
        super(deckFrom, deckTo, nComponents);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Draw " + nComponents + " cards";
    }
}
