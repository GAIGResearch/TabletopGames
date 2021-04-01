package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameState;
import games.terraformingmars.rules.requirements.PlayableActionRequirement;
import games.terraformingmars.rules.requirements.Requirement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ChoiceAction extends TMAction implements IExtendedSequence {
    public TMAction[] actions;
    boolean finished;

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
    public boolean execute(AbstractGameState gameState) {
        if (player == -1) player = gameState.getCurrentPlayer();
        for (TMAction a: actions) a.player = player;
        gameState.setActionInProgress(this);
        return true;
    }

    @Override
    public boolean canBePlayed(TMGameState gs) {
        // "OR" behaviour on requirements instead of default "AND"
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
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        finished = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return finished;
    }

    @Override
    public ChoiceAction copy() {
        return this;
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
