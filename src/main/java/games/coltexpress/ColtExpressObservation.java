package games.coltexpress;

import core.actions.IAction;
import core.observations.IObservation;
import core.observations.IPrintable;

public class ColtExpressObservation implements IPrintable, IObservation {


    public ColtExpressObservation(){
    }


    public void printToConsole() {

    }

    @Override
    public IObservation copy() {
        return null;
    }

    @Override
    public IObservation next(IAction action) {
        return null;
    }
}
