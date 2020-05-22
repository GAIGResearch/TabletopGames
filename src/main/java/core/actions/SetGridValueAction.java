package core.actions;

import core.AbstractGameState;
import core.components.Card;
import core.observations.IPrintable;
import core.components.GridBoard;

public class SetGridValueAction<T> implements IAction, IPrintable {

    private final GridBoard<T> gridBoard;
    private final int x;
    private final int y;
    private final T value;

    public SetGridValueAction (GridBoard<T> gridBoard, int x, int y, T value){
        this.gridBoard = gridBoard;
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
        return gridBoard.setElement(x, y, value);
    }

    @Override
    public Card getCard() {
        return null;
    }

    public GridBoard<T> getGridBoard() {
        return gridBoard;
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
                "grid=" + gridBoard.toString() +
                ", x=" + x +
                ", y=" + y +
                ", value=" + value +
                '}';
    }
}
