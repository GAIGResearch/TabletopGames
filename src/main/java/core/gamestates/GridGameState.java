package core.gamestates;

import core.components.Grid;

public interface GridGameState<T> {

    default int getWidth() { return getGrid().getWidth(); }

    default int getHeight() { return getGrid().getHeight(); }

    Grid<T> getGrid();
}
