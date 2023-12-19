package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;

import java.util.Objects;

public class SwapOrder extends DescentAction {

    HeroicFeatExtraMovement action;
    int first, second;
    boolean swap;
    public SwapOrder(HeroicFeatExtraMovement action, int first, int second, boolean swap) {
        super(Triggers.ANYTIME);
        this.action = action;
        this.first = first;
        this.second = second;
        this.swap = swap;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        String name;
        if (!swap)
            name = gameState.getComponentById(first).getComponentName();
        else
            name = gameState.getComponentById(second).getComponentName();
        return name.replace("Hero: ", "") + " moves first";
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
        return new SwapOrder(action.copy(), first, second, swap);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        return !action.getSwapOption();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SwapOrder swapOrder = (SwapOrder) o;
        return first == swapOrder.first && second == swapOrder.second && swap == swapOrder.swap && Objects.equals(action, swapOrder.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), action, first, second, swap);
    }
}
