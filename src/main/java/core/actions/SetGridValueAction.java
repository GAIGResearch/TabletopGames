package core.actions;

import core.AbstractGameState;
import core.components.Component;
import core.interfaces.IPrintable;
import core.components.GridBoard;

import java.util.Objects;

public class SetGridValueAction<T extends Component> extends AbstractAction implements IPrintable {

    private final int gridBoard;
    private final int x;
    private final int y;
    private final int valueID;

    public SetGridValueAction (int gridBoard, int x, int y, int valueID){
        this.gridBoard = gridBoard;
        this.x = x;
        this.y = y;
        this.valueID = valueID;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        T value = (T) gameState.getComponentById(valueID);
        System.out.println("Set " + value.toString() + " at pos (" + x + ", " + y + ")");
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        T value = (T) gs.getComponentById(valueID);
        return ((GridBoard<T>)gs.getComponentById(gridBoard)).setElement(x, y, value);
    }

    @Override
    public SetGridValueAction<T> copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SetGridValueAction<?> that)) return false;
        return gridBoard == that.gridBoard && x == that.x && y == that.y && valueID == that.valueID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gridBoard, x, y, valueID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        T value = (T) gameState.getComponentById(valueID);
        return "SetGridValueAction{" +
                "gridBoard=" + gameState.getComponentById(gridBoard).getComponentName() +
                ", x=" + x +
                ", y=" + y +
                ", value=" + value +
                '}';
    }

    public int getGridBoard() {
        return gridBoard;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getValueID() {
        return valueID;
    }
    public T getValue(AbstractGameState gameState) {
        return (T) gameState.getComponentById(valueID);
    }

    @Override
    public String toString() {
        return "SetGridValueAction{" +
                "gridBoard=" + gridBoard +
                ", x=" + x +
                ", y=" + y +
                ", valueID=" + valueID +
                '}';
    }
}
