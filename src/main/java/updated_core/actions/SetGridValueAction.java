package updated_core.actions;

import core.GameState;
import updated_core.components.Grid;
import updated_core.gamestates.AbstractGameState;
import updated_core.gamestates.GridGameState;
import updated_core.turn_order.TurnOrder;

public class SetGridValueAction<T> implements IAction, IPrintable {

    private final Grid<T> grid;
    private final int x;
    private final int y;
    private final T value;

    public SetGridValueAction (Grid<T> grid, int x, int y, T value){
        this.grid = grid;
        this.x = x;
        this.y = y;
        this.value = value;
    }

    @Override
    public void PrintToConsole() {
        System.out.println("Set " + value.toString() + " at pos (" + x + ", " + y + ")");
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        return grid.setElement(x, y, value);
    }
}
