package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.DiceMonasteryTurnOrder;

public class Pass extends AbstractAction {

    final boolean asReward;

    public Pass() {
        this(false);
    }

    public Pass(boolean rewardStep) {
        asReward = rewardStep;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        DiceMonasteryTurnOrder turnOrder = (DiceMonasteryTurnOrder) gs.getTurnOrder();
        state.useAP(turnOrder.getActionPointsLeft());

        if (asReward) {
            DiceMonasteryTurnOrder dto = (DiceMonasteryTurnOrder) state.getTurnOrder();
            dto.setRewardTaken(state.getCurrentPlayer());
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        // no mutable state
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Pass && ((Pass) obj).asReward == asReward;
    }

    @Override
    public int hashCode() {
        return 2398734 + (asReward ? 31 : 0);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString(){
        return "Pass";
    }
}
