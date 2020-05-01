package core.actions;

import core.AbstractGameState;
import core.observations.IPrintable;
import core.components.Grid;

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
    public boolean execute(AbstractGameState gs) {
        return grid.setElement(x, y, value);
    }
}
