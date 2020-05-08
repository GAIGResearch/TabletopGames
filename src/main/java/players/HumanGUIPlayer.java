package players;

import core.AbstractPlayer;
import core.actions.IAction;
import core.observations.IObservation;
import java.util.List;


public class HumanGUIPlayer extends AbstractPlayer {
    ActionController ac;

    public HumanGUIPlayer(ActionController ac){
        this.ac = ac;
    }

    @Override
    public void initializePlayer(IObservation observation) {

    }

    @Override
    public void finalizePlayer(IObservation observation) {

    }

    @Override
    public int getAction(IObservation observation, List<IAction> actions) {
        return actions.indexOf(ac.getAction());
    }
}

