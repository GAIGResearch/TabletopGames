package utilities;

import core.AbstractGameState;
import core.interfaces.IPrintable;

import java.util.Arrays;

public class VectorObservation<T> implements IPrintable {

    private T[] values;

    public VectorObservation(T[] arrayValues){
        this.values = arrayValues;
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(Arrays.toString(values));
    }

}
