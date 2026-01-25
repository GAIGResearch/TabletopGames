package core.interfaces;

import core.components.GridBoard;

public interface IGridGameState {

    default int getWidth() { return getGridBoard().getWidth(); }

    default int getHeight() { return getGridBoard().getHeight(); }

    GridBoard getGridBoard();
}
