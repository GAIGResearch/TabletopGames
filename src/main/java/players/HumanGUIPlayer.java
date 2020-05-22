package players;

import core.AbstractPlayer;
import core.actions.AbstractAction;
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
    public int getAction(IObservation observation, List<AbstractAction> actions) {
        return actions.indexOf(ac.getAction());
    }

    @Override
    public void registerUpdatedObservation(IObservation observation) {
        
    }
}

