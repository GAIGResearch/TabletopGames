package observations;

import java.util.Arrays;

class ArrayObservation<T> implements Observation, IPrintable {

    private T[] values;

    public ArrayObservation(T[] arrayValues){
        this.values = arrayValues;
    }

    @Override
    public void PrintToConsole() {
        System.out.println(Arrays.toString(values));
    }
}
