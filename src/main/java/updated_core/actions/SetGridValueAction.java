package updated_core.actions;

import core.GameState;
import updated_core.components.Grid;
import updated_core.gamestates.GridGameState;

public class SetGridValueAction<T> implements IAction, IPrintableAction {

    private Grid<T> grid;
    private int x;
    private int y;
    private T value;

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
    public boolean Execute(GameState gs) {
        return grid.setElement(x, y, value);
    }
}
