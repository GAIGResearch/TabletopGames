package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMTypes;
import games.terraformingmars.rules.requirements.PlayableActionRequirement;

public class CompoundAction extends TMAction{
    public TMAction[] actions;

    public CompoundAction(int player, TMAction[] actions) {
        super(player, true);
        this.actions = actions;
        for (TMAction a: actions) {
            this.requirements.add(new PlayableActionRequirement(a));
        }
    }

    public CompoundAction(TMTypes.ActionType actionType, int player, TMAction[] actions, int cost) {
        super(actionType, player, false);
        this.actions = actions;
        this.setActionCost(TMTypes.Resource.MegaCredit, cost, -1);
        for (TMAction a: actions) {
            this.requirements.add(new PlayableActionRequirement(a));
        }
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        if (player == -1) player = gameState.getCurrentPlayer();
        for (TMAction a: actions) {
            a.player = player;
            a.execute(gameState);
        }
        return super.execute(gameState);
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        StringBuilder s = new StringBuilder();
        for (TMAction action: actions) {
            s.append(action.getString(gameState)).append(" and ");
        }
        return s.substring(0, s.length()-5);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (TMAction action: actions) {
            s.append(action.toString()).append(" and ");
        }
        return s.substring(0, s.length()-5);
    }
}
