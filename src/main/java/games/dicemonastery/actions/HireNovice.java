package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.Resource.SHILLINGS;

public class HireNovice extends UseMonk {

    /**
     * This is used for hiring a free monk if you run out of them; generally the
     * argument-less constructor is used with the default AP cost
     *
     * @param ap Action Points
     */
    public HireNovice(int ap) {
        super(ap);
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
        return obj instanceof HireNovice && ((HireNovice) obj).actionPoints == actionPoints;
    }

    @Override
    public int hashCode() {
        return 309823 + actionPoints * 37;
    }

    @Override
    public String toString() {
        return "Hire Novice";
    }

}
