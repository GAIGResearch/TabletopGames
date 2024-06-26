package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameState;
import games.terraformingmars.components.TMCard;
import games.terraformingmars.rules.requirements.PlayableActionRequirement;
import games.terraformingmars.rules.requirements.Requirement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ChoiceAction extends TMAction implements IExtendedSequence {
    public TMAction[] actions;
    boolean finished;

    public ChoiceAction() { super(); } // This is needed for JSON Deserializer

    public ChoiceAction(int player, TMAction[] actions) {
        super(player, true);
        this.actions = actions;
        for (TMAction a: actions) {
            this.requirements.add(new PlayableActionRequirement(a));
        }
    }

    public ChoiceAction(int player, TMAction[] actions, boolean free, HashSet<Requirement<TMGameState>> requirements) {
        super(player, free, requirements);
        this.actions = actions;
    }

    @Override
    public boolean _execute(TMGameState gameState) {
        for (TMAction a: actions) {
            a.player = player;
            a.setCardID(getCardID());
        }
        gameState.setActionInProgress(this);
        return true;
    }

    @Override
    public boolean canBePlayed(TMGameState gs) {
        // "OR" behaviour on requirements instead of default "AND"
        boolean played = false;
        if (getCardID() != -1) {
            TMCard c = (TMCard) gs.getComponentById(getCardID());
            if (c != null && c.actionPlayed) played = true;
        }
        if (played && standardProject == null && basicResourceAction == null) return false;
        if (requirements != null && requirements.size() > 0) {
            for (Requirement r: requirements) {
                if (r.testCondition(gs)) return true;
            }
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return Arrays.asList(actions);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        finished = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return finished;
    }

    @Override
    public ChoiceAction _copy() {
        TMAction[] acopy = new TMAction[actions.length];
        for (int i = 0; i < actions.length; i++) {
            if (actions[i] != null) {
                acopy[i] = actions[i].copy();
            }
        }
        ChoiceAction copy = new ChoiceAction(player, acopy);
        copy.finished = finished;
        return copy;
    }

    @Override
    public ChoiceAction copy() {
        return (ChoiceAction) super.copy();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        StringBuilder s = new StringBuilder("Choose from: ");
        for (TMAction action: actions) {
            s.append(action.getString(gameState)).append(" or ");
        }
        return s.substring(0, s.length()-4);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Choose from: ");
        for (TMAction action: actions) {
            s.append(action.toString()).append(" or ");
        }
        return s.substring(0, s.length()-4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChoiceAction)) return false;
        if (!super.equals(o)) return false;
        ChoiceAction that = (ChoiceAction) o;
        return finished == that.finished && Arrays.equals(actions, that.actions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), finished);
        result = 31 * result + Arrays.hashCode(actions);
        return result;
    }
}
