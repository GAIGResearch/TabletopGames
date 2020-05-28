package core.interfaces;

import core.AbstractGameState;

public interface IPrintable {
    default String getString(AbstractGameState gameState) { return toString(); }
    default void printToConsole(AbstractGameState gameState){
        System.out.println(getString(gameState));
    }
    default void printToConsole() {
        System.out.println(toString());
    }
}
