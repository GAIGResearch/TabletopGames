package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class HireNovice extends UseMonk {

    public HireNovice() {
        super(3);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        int player = state.getCurrentPlayer();
        int cash = state.getResource(player, SHILLINGS, STOREROOM);
        int cost = state.monksIn(null, player).size();
        if (cash >= cost) {
            state.addResource(player, SHILLINGS, -cost);
            state.createMonk(1, player);
            return true;
        }
        throw new AssertionError("Not enough money to hire a novice");
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HireNovice;
    }

    @Override
    public int hashCode() {
        return 309823;
    }

    @Override
    public String toString() {
        return "Hire Novice";
    }

}
