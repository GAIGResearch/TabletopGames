package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.rules.requirements.Requirement;

import java.util.Arrays;
import java.util.List;

public class ActionChoice extends TMAction implements IExtendedSequence {
    public TMAction[] actions;
    boolean finished;

    public ActionChoice(int player, TMAction[] actions, boolean free) {
        super(player, free);
        this.actions = actions;
    }

    public ActionChoice(int player, TMAction[] actions, boolean free, Requirement requirement) {
        super(player, free, requirement);
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
    public ActionChoice copy() {
        TMAction[] copy = new TMAction[actions.length];
        for (int i = 0; i < actions.length; i++) {
            copy[i] = (TMAction) actions[i].copy();
        }
        return new ActionChoice(player, copy, free, requirement);
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
}
