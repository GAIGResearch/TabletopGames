package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.Resource.PRAYER;

public class Pray extends AbstractAction {

    public final int prayerCount;

    public Pray(int n) {
        prayerCount = n;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        if (prayerCount > 0) {
            state.addResource(state.getCurrentPlayer(), PRAYER, -prayerCount);
            state.addActionPoints(prayerCount * 2);
        }
        return true;
    }

    @Override
    public Pray copy() {
        // immutable
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Pray && ((Pray) obj).prayerCount == prayerCount;
    }

    @Override
    public int hashCode() {
        return 493 + prayerCount * 79;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }


    @Override
    public String toString() {
        return "Prays " + prayerCount + " times";
    }
}
