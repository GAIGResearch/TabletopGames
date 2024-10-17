package core.interfaces;

import core.components.Component;
import core.components.GridBoard;

public interface IGridGameState<T extends Component> {

    default int getWidth() { return getGridBoard().getWidth(); }

    default int getHeight() { return getGridBoard().getHeight(); }

    GridBoard<T> getGridBoard();
}
