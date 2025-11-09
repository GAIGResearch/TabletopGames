package players.python;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.List;

/*
  Dummy agent to know when to return obs, info for python agent
  This allows implementing custom controllers in python and executing their getAction method
  as part of the normal TAG game loops.
 */
public class PythonAgent extends AbstractPlayer {

    Actor actor;
    public PythonAgent() {
        super(null, "PythonAgent");
        this.actor = new Actor() {
            @Override
            public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
                return null;
            }
        };
    }

    public PythonAgent(Actor actor){
        super(null, "PythonAgent");
        this.actor = actor;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        return actor.getAction(gameState, possibleActions);
    }

    @Override
    public AbstractPlayer copy() {
        return null;
    }
}
