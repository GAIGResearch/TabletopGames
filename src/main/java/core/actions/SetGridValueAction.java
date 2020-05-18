package core.actions;

import core.AbstractGameState;
import core.components.Card;
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
    public void printToConsole() {
        System.out.println("Set " + value.toString() + " at pos (" + x + ", " + y + ")");
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return grid.setElement(x, y, value);
    }

    @Override
    public Card getCard() {
        return null;
    }

    public Grid<T> getGrid() {
        return grid;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "SetGridValueAction{" +
                "grid=" + grid.toString() +
                ", x=" + x +
                ", y=" + y +
                ", value=" + value +
                '}';
    }
}
