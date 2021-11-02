package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.components.Treasure;

public class PayTreasure extends AbstractAction {

    final Treasure payment;

    public PayTreasure(Treasure loss) {
        payment = loss;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        int player = state.getCurrentPlayer();
        state.loseTreasure(player, payment);
        return true;
    }

    @Override
    public PayTreasure copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PayTreasure && ((PayTreasure) obj).payment == payment;
    }

    @Override
    public int hashCode() {
        return 8341 + payment.hashCode();
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Loses %s to vikings", payment.getComponentName());
    }
}
