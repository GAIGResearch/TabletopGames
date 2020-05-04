package core.observations;

import java.util.Arrays;

class ArrayObservation<T> implements IObservation, IPrintable {

    private T[] values;

    public ArrayObservation(T[] arrayValues){
        this.values = arrayValues;
    }

    @Override
    public void printToConsole() {
        System.out.println(Arrays.toString(values));
    }
}
