package core.interfaces;

import core.AbstractGameState;

public interface IAbstractLogic {

    /**
     * Executes a piece of logic that modifies the given game state. Can access any component IDs stored
     * through the AbstractGameState.getComponentById(int id) method.
     * This is different to actions as no player can find these in their action space, but it permits encapsulation
     * of logic to keep the game state fully data-driven
     * @param gs - game state which should be modified by this piece of logic.
     */
    void execute(AbstractGameState gs);

}
