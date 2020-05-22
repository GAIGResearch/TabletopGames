package core.gamestates;

import core.components.GridBoard;

public interface GridGameState<T> {

    default int getWidth() { return getGridBoard().getWidth(); }

    default int getHeight() { return getGridBoard().getHeight(); }

    GridBoard<T> getGridBoard();
}
