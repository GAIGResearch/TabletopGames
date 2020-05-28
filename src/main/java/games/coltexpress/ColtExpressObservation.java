package games.coltexpress;

import core.actions.AbstractAction;
import core.interfaces.IObservation;
import core.interfaces.IPrintable;

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
    public IObservation next(AbstractAction action) {
        return null;
    }
}
