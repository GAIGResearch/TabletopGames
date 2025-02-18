package games.descent2e.actions.herofeats;

import core.AbstractGameState;
import core.interfaces.IExtendedSequence;
import games.descent2e.DescentGameState;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.Figure;

import java.util.Objects;

public class SwapOrder extends DescentAction {

    int first, second;
    boolean swap;
    public SwapOrder(int first, int second, boolean swap) {
        super(Triggers.ANYTIME);
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

    public String toString() {
        return "SwapOrder: " + first + " " + second + " " + swap;
    }

    @Override
    public boolean execute(DescentGameState dgs) {
        IExtendedSequence action = dgs.currentActionInProgress();
        if (action instanceof HeroicFeatExtraMovement) {
            // Regardless of if we swap or not, we ensure we don't swap again
            ((HeroicFeatExtraMovement) action).setSwapOption(true);

            ((HeroicFeatExtraMovement) action).swap(swap);
        }
        ((Figure) dgs.getComponentById(first)).addActionTaken(getString(dgs));
        return true;
    }

    @Override
    public DescentAction copy() {
        return new SwapOrder(first, second, swap);
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        IExtendedSequence action = dgs.currentActionInProgress();
        if (action instanceof HeroicFeatExtraMovement)
            return !((HeroicFeatExtraMovement) action).getSwapOption();
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof SwapOrder that) {
            return first == that.first && second == that.second && swap == that.swap && super.equals(o);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), first, second, swap);
    }
}
