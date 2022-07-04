package games.descent2e.actions.searchcards;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;

public class UseWardingTalisman extends DescentAction {

    public UseWardingTalisman() {
        super(Triggers.ROLL_DEFENCE_DICE);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        return false;
    }

    @Override
    public DescentAction copy() {
        return null;
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return false;
    }
}
