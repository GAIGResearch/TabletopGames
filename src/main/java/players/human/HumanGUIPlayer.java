package players.human;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.actions.DoNothing;

import java.util.List;


public class HumanGUIPlayer extends AbstractPlayer {
    ActionController ac;

    public HumanGUIPlayer(ActionController ac){
        super(null, "HumanGUIPlayer");
        this.ac = ac;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState observation, List<AbstractAction> actions) {
        try {
            return ac.getAction();
        } catch (InterruptedException e) {
            // we have been interrupted - this means the game has been stopped, so we return DoNothing()
            // not Null - as this would indicate an unexpected result
            return new DoNothing();
        }
    }

    @Override
    public AbstractPlayer copy() {
        return this;
    }
}

