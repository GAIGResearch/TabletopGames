package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.rules.requirements.Requirement;

public class CompoundAction extends TMAction{
    TMAction[] actions;

    public CompoundAction(int player, TMAction[] actions, boolean free) {
        super(player, free);
        this.actions = actions;
    }

    public CompoundAction(int player, TMAction[] actions, boolean free, Requirement requirement) {
        super(player, free, requirement);
        this.actions = actions;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        for (TMAction a: actions) {
            a.execute(gameState);
        }
        return super.execute(gameState);
    }

    @Override
    public AbstractAction copy() {
        TMAction[] copy = new TMAction[actions.length];
        for (int i = 0; i < actions.length; i++) {
            copy[i] = (TMAction) actions[i].copy();
        }
        return new CompoundAction(player, copy, free, requirement);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        StringBuilder s = new StringBuilder("");
        for (TMAction action: actions) {
            s.append(action.getString(gameState)).append(" and ");
        }
        return s.substring(0, s.length()-5);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("");
        for (TMAction action: actions) {
            s.append(action.toString()).append(" and ");
        }
        return s.substring(0, s.length()-5);
    }
}
