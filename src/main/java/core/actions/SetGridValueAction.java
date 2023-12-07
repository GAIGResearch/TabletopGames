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
    private final T value;

    public SetGridValueAction (int gridBoard, int x, int y, T value){
        this.gridBoard = gridBoard;
        this.x = x;
        this.y = y;
        this.value = value;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println("Set " + value.toString() + " at pos (" + x + ", " + y + ")");
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return ((GridBoard<T>)gs.getComponentById(gridBoard)).setElement(x, y, value);
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SetGridValueAction)) return false;
        SetGridValueAction<?> that = (SetGridValueAction<?>) o;
        return gridBoard == that.gridBoard &&
                x == that.x &&
                y == that.y &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gridBoard, x, y, value);
    }

    @Override
    public String getString(AbstractGameState gameState) {
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

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "SetGridValueAction{" +
                "gridBoard=" + gridBoard +
                ", x=" + x +
                ", y=" + y +
                ", value=" + value +
                '}';
    }
}
