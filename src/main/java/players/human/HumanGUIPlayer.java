package players.human;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.List;


public class HumanGUIPlayer extends AbstractPlayer {
    ActionController ac;

    public HumanGUIPlayer(ActionController ac){
        this.ac = ac;
    }

    @Override
    public AbstractAction getAction(AbstractGameState observation, List<AbstractAction> actions) {
        return ac.getAction();
    }

    @Override
    public AbstractPlayer copy() {
        return this;
    }
}

