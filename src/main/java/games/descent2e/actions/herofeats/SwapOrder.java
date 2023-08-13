package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Hero;

public class SwapOrder extends DescentAction {

    HeroicFeatExtraMovement action;
    Hero first, second;
    boolean swap;
    public SwapOrder(HeroicFeatExtraMovement action, Hero first, Hero second, boolean swap) {
        super(Triggers.ANYTIME);
        this.action = action;
        this.first = first;
        this.second = second;
        this.swap = swap;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if (!swap)
            return first.getName().replace("Hero: ", "") + " moves first";
        else
            return second.getName().replace("Hero: ", "") + " moves first";
    }

    @Override
    public boolean execute(DescentGameState gs) {
        action.swap(swap);
        // Regardless of if we swap or not, we ensure we don't swap again
        action.setSwapOption(true);
        return true;
    }

    @Override
    public DescentAction copy() {
        return new SwapOrder(action, first, second, swap);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return !action.getSwapOption();
    }
}
