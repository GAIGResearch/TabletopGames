package updated_core.observations;

import updated_core.actions.IPrintable;

public class GridObservation<T> implements Observation, IPrintable {

    private final T[][] grid;

    public GridObservation(T[][] gridValues){
        this.grid = gridValues;
    }

    @Override
    public void PrintToConsole() {
        for (T[] ts : grid) {
            for (T t : ts) System.out.print(t.toString());
            System.out.println();
        }
    }
}
