package core.observations;

import core.interfaces.IObservation;
import core.interfaces.IPrintable;

import java.util.Arrays;

class VectorObservation<T> implements IObservation, IPrintable {

    private T[] values;

    public VectorObservation(T[] arrayValues){
        this.values = arrayValues;
    }

    @Override
    public void printToConsole() {
        System.out.println(Arrays.toString(values));
    }
}
