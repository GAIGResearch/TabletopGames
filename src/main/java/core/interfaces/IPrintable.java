package core.interfaces;

import core.AbstractGameState;

public interface IPrintable {
    /**
     * Retrieves a short string for this object, given an AbstractGameState for context.
     * @param gameState - game state provided for context.
     * @return - short String
     */
    default String getString(AbstractGameState gameState) { return toString(); }

    /**
     * Prints its state to console, given an AbstractGameState for context.
     * @param gameState - game state provided for context.
     */
    default void printToConsole(AbstractGameState gameState){
        System.out.println(getString(gameState));
    }

    /**
     * Prints itself to console.
     */
    default void printToConsole() {
        System.out.println(toString());
    }
}
