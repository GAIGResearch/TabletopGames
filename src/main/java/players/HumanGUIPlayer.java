package players;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;


public class HumanGUIPlayer extends AbstractPlayer {
    ActionController ac;

    public HumanGUIPlayer(ActionController ac){
        this.ac = ac;
    }

    @Override
    public AbstractAction getAction(AbstractGameState observation) {
        return ac.getAction();
    }
}

