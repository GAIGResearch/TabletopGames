package games.terraformingmars.actions;

import core.AbstractGameState;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.requirements.PlayableActionRequirement;

import java.util.Arrays;

public class CompoundAction extends TMAction{
    public TMAction[] actions;

    public CompoundAction() { super(); }  // This is needed for JSON Deserializer

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
    public boolean _execute(TMGameState gameState) {
        boolean s = true;
        TMCard c = null;
        if (getCardID() != -1) {
            c = (TMCard) gameState.getComponentById(getCardID());
        }
        for (TMAction a: actions) {
            if (c != null) {
                c.actionPlayed = false;  // This is set by each action, preventing the next ones, but we want all to be executed before flag is set
            }
            a.player = player;
            a.setCardID(getCardID());
            s &= a.execute(gameState);
        }
        return s;
    }

    @Override
    public CompoundAction _copy() {
        TMAction[] acopy = new TMAction[actions.length];
        for (int i = 0; i < actions.length; i++) {
            if (actions[i] != null) {
                acopy[i] = actions[i].copy();
            }
        }
        return new CompoundAction(player, acopy);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompoundAction)) return false;
        if (!super.equals(o)) return false;
        CompoundAction that = (CompoundAction) o;
        return Arrays.equals(actions, that.actions);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(actions);
        return result;
    }
}
