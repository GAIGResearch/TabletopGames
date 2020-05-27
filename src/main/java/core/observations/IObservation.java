package core.observations;

import core.AbstractGameState;
import core.actions.IAction;

public interface IObservation {

    public IObservation copy();

    public IObservation next(IAction action);
}
