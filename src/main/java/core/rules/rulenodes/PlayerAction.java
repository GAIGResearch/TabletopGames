package core.rules.rulenodes;

import core.AbstractGameStateWithTurnOrder;
import core.rules.Node;
import core.rules.nodetypes.RuleNode;

/**
 * Executes an action requested by a player, if the action was set. Interrupts the game loop if no action was provided,
 * but not if the action fails execution.
 */
public class PlayerAction extends RuleNode {

    public PlayerAction() {
        super(true);
    }

    /**
     * Copy constructor
     * @param playerAction - Node to be copied
     */
    public PlayerAction(PlayerAction playerAction) {
        super(playerAction);
    }

    @Override
    protected boolean run(AbstractGameStateWithTurnOrder gs) {
        if (action != null) {
            action.execute(gs);
            return true;
        }
        return false;
    }

    @Override
    protected Node _copy() {
        return new PlayerAction(this);
    }

}
