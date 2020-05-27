package players;

import core.AbstractGameState;
import core.AbstractPlayer;


public class HumanGUIPlayer extends AbstractPlayer {
    ActionController ac;

    public HumanGUIPlayer(ActionController ac){
        this.ac = ac;
    }

    @Override
    public int getAction(AbstractGameState observation) {
        return observation.getActions().indexOf(ac.getAction());
    }
}

