package core.observations;

import core.interfaces.IObservation;
import core.interfaces.IPrintable;

public class GridObservation<T> implements IObservation, IPrintable {

    private final T[][] grid;

    public GridObservation(T[][] gridValues){
        this.grid = gridValues;
    }

    @Override
    public void printToConsole() {
        for (T[] ts : grid) {
            for (T t : ts) System.out.print(t.toString());
            System.out.println();
        }
    }
}
